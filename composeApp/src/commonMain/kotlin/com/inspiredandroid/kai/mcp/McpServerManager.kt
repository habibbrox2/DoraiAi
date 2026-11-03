package com.inspiredandroid.kai.mcp

import com.inspiredandroid.kai.data.AppSettings
import com.inspiredandroid.kai.data.SettingsJsonList
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

private val serverIdRegex = Regex("[^a-z0-9]")

class McpServerManager(private val appSettings: AppSettings) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val mutex = Mutex()
    private val clients = mutableMapOf<String, McpClient>()
    private val discoveredTools = mutableMapOf<String, List<McpToolMetadata>>()
    private val discoveredSkills = mutableMapOf<String, JsonArray>()

    // NOTE: the mutators below are read-modify-write without a lock. Making them safe means making
    // them suspend, which ripples through DataRepository and the settings UI — tracked separately.
    private val servers = SettingsJsonList(
        read = appSettings::getMcpServersJson,
        write = appSettings::setMcpServersJson,
        itemSerializer = McpServerConfig.serializer(),
        label = "McpServerManager",
        onCorrupt = { appSettings.backupCorruptJson("McpServerManager", it) },
        json = json,
    )

    init {
        // One-shot: fill missing popular default headers (e.g. Jina Authorization) without
        // overwriting headers the user already configured.
        migratePopularDefaultHeaders()
    }

    fun getServers(): List<McpServerConfig> = servers.get()

    private fun saveServers(servers: List<McpServerConfig>) = this.servers.set(servers)

    private fun migratePopularDefaultHeaders() {
        val current = servers.get()
        val updated = applyPopularDefaultHeaders(current)
        if (updated !== current) {
            saveServers(updated)
        }
    }

    fun addServer(name: String, url: String, headers: Map<String, String>, allowPrivateIps: Boolean = false): McpServerConfig {
        // Validate URL before adding
        val urlResult = McpUrlValidator.validate(url, allowPrivateIps)
        if (!urlResult.isValid) {
            throw McpException("Invalid MCP server URL or headers")
        }
        val headerResult = McpUrlValidator.validateHeaders(headers)
        if (!headerResult.isValid) {
            throw McpException("Invalid MCP server URL or headers")
        }
        val servers = getServers().toMutableList()
        val id = generateServerId(name, servers)
        // If the user adds a popular endpoint manually without headers, still apply defaults
        // for missing keys only (never clobber explicit headers they typed).
        val popularDefaults = popularMcpServers
            .firstOrNull { matchesPopularMcpUrl(url, it.url) }
            ?.headers
            .orEmpty()
        val mergedHeaders = mergeMissingHeaders(headers, popularDefaults)
        val config = McpServerConfig(id = id, name = name, url = url, headers = mergedHeaders)
        servers.add(config)
        saveServers(servers)
        return config
    }

    fun removeServer(serverId: String) {
        val servers = getServers().toMutableList()
        servers.removeAll { it.id == serverId }
        saveServers(servers)
        clients[serverId]?.close()
        clients.remove(serverId)
        discoveredTools.remove(serverId)
        discoveredSkills.remove(serverId)
    }

    fun setServerEnabled(serverId: String, enabled: Boolean) {
        val servers = getServers().toMutableList()
        val index = servers.indexOfFirst { it.id == serverId }
        if (index >= 0) {
            servers[index] = servers[index].copy(isEnabled = enabled)
            saveServers(servers)
        }
        if (!enabled) {
            clients[serverId]?.close()
            clients.remove(serverId)
            discoveredTools.remove(serverId)
            discoveredSkills.remove(serverId)
        }
    }

    suspend fun connectAndDiscoverTools(serverId: String): Result<List<McpToolMetadata>> {
        val server = getServers().find { it.id == serverId }
            ?: return Result.failure(McpException("Server not found: $serverId"))

        // Close existing client if any
        mutex.withLock { clients[serverId] }?.close()

        // Validate URL before connecting
        val urlResult = McpUrlValidator.validate(server.url, false)
        if (!urlResult.isValid) {
            return Result.failure(McpException("Invalid server URL: ${urlResult.error}"))
        }
        val client = McpClient(server.url, server.headers)
        return try {
            client.initialize()
            val toolDefs = client.listTools()
            // Skills-over-MCP is optional. A server without the extension must still connect
            // normally and expose its tools.
            val skills = try {
                client.listSkills()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                JsonArray(emptyList())
            }
            val metadata = toolDefs.map { def ->
                McpToolMetadata(
                    serverId = serverId,
                    name = def.name,
                    description = def.description ?: "",
                    inputSchema = def.inputSchema,
                )
            }
            mutex.withLock {
                clients[serverId] = client
                discoveredTools[serverId] = metadata
                discoveredSkills[serverId] = skills
            }
            Result.success(metadata)
        } catch (e: CancellationException) {
            client.close()
            mutex.withLock {
                clients.remove(serverId)
                discoveredTools.remove(serverId)
                discoveredSkills.remove(serverId)
            }
            throw e
        } catch (e: Exception) {
            client.close()
            mutex.withLock {
                clients.remove(serverId)
                discoveredTools.remove(serverId)
                discoveredSkills.remove(serverId)
            }
            Result.failure(e)
        }
    }

    /**
     * Enabled MCP tools, named so none collides with [reservedNames] (the built-in tools) or with
     * another server's tool. Duplicate function names make providers reject the request, and the
     * executor would always run the first match.
     */
    fun getEnabledMcpTools(reservedNames: Set<String> = emptySet()): List<Tool> {
        val enabledServers = getServers().filter { it.isEnabled }.map { it.id }.toSet()
        val taken = reservedNames.toMutableSet()
        return buildList {
            for ((serverId, tools) in discoveredTools) {
                if (serverId !in enabledServers) continue
                val client = clients[serverId] ?: continue
                for (meta in tools) {
                    val toolId = McpTool.toolId(serverId, meta.name)
                    if (appSettings.isToolEnabled(toolId)) {
                        val name = uniqueMcpToolName(serverId, meta.name, taken)
                        taken += name
                        add(McpTool(client, meta, advertisedName = name))
                    }
                }
            }
        }
    }

    /** Skills advertised by a connected server via the optional Skills-over-MCP extension. */
    fun getSkillsForServer(serverId: String): JsonArray = discoveredSkills[serverId] ?: JsonArray(emptyList())

    /** Read a server-published skill resource. The client enforces the skill:// URI scheme. */
    suspend fun readSkill(serverId: String, uri: String): Result<String> {
        val client = mutex.withLock { clients[serverId] }
            ?: return Result.failure(McpException("MCP server is not connected: $serverId"))
        return try {
            Result.success(client.readSkill(uri))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getToolsForServer(serverId: String): List<ToolInfo> {
        val tools = discoveredTools[serverId] ?: return emptyList()
        return tools.map { meta ->
            val toolId = McpTool.toolId(serverId, meta.name)
            ToolInfo(
                id = toolId,
                name = meta.name,
                description = meta.description,
                isEnabled = appSettings.isToolEnabled(toolId),
            )
        }
    }

    suspend fun connectEnabledServers() {
        val enabledServers = getServers().filter { it.isEnabled }
        coroutineScope {
            enabledServers
                .filter { !clients.containsKey(it.id) }
                .map { server ->
                    async {
                        try {
                            connectAndDiscoverTools(server.id)
                        } catch (_: Exception) {
                            // Individual server failures shouldn't block others
                        }
                    }
                }
                .awaitAll()
        }
    }

    fun isConnected(serverId: String): Boolean = clients.containsKey(serverId)

    private fun generateServerId(name: String, existing: List<McpServerConfig>): String {
        val base = name.lowercase().replace(serverIdRegex, "_").take(30)
        val existingIds = existing.map { it.id }.toSet()
        if (base !in existingIds) return base
        var counter = 2
        while ("${base}_$counter" in existingIds) counter++
        return "${base}_$counter"
    }
}

private val toolNameInvalidChars = Regex("[^A-Za-z0-9_-]")

/**
 * [toolName] itself when free; otherwise `<serverId>_<toolName>` (then `_2`, `_3`, …), restricted
 * to the characters and 64-char length providers accept for function names.
 */
internal fun uniqueMcpToolName(serverId: String, toolName: String, taken: Set<String>): String {
    if (toolName !in taken) return toolName
    val base = "${serverId}_$toolName".replace(toolNameInvalidChars, "_").take(64)
    if (base !in taken) return base
    var n = 2
    while (true) {
        val suffix = "_$n"
        val candidate = base.take(64 - suffix.length) + suffix
        if (candidate !in taken) return candidate
        n++
    }
}
