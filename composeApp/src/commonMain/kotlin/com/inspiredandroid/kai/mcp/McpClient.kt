package com.inspiredandroid.kai.mcp

import com.inspiredandroid.kai.httpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

internal fun isSameOriginMcpRedirect(source: String, target: String): Boolean = try {
    val sourceUrl = Url(source)
    val targetUrl = Url(target)
    sourceUrl.protocol == targetUrl.protocol &&
        sourceUrl.host.equals(targetUrl.host, ignoreCase = true) &&
        sourceUrl.port == targetUrl.port
} catch (_: Exception) {
    false
}

internal const val MCP_CURRENT_PROTOCOL_VERSION = "2026-07-28"

internal fun withMcpRequestMetadata(params: JsonElement?, protocolVersion: String): JsonObject {
    val original = params as? JsonObject ?: JsonObject(emptyMap())
    return buildJsonObject {
        original.forEach { (key, value) ->
            if (key != "_meta") put(key, value)
        }
        put(
            "_meta",
            buildJsonObject {
                put("io.modelcontextprotocol/protocolVersion", JsonPrimitive(protocolVersion))
                put(
                    "io.modelcontextprotocol/clientInfo",
                    buildJsonObject {
                        put("name", JsonPrimitive("Brox Kai"))
                        put("version", JsonPrimitive("2.0"))
                    },
                )
                put("io.modelcontextprotocol/clientCapabilities", buildJsonObject {})
            },
        )
    }
}

internal fun supportsMcpSkillsExtension(capabilities: JsonObject?): Boolean = (capabilities?.get("extensions") as? JsonObject)?.containsKey("io.modelcontextprotocol/skills") == true

class McpClient(
    private val url: String,
    private val headers: Map<String, String>,
    private val allowPrivateIps: Boolean = false,
) {
    init {
        // Fail fast: validate URL at construction time
        val urlResult = McpUrlValidator.validate(url, allowPrivateIps)
        if (!urlResult.isValid) {
            throw McpException("Invalid MCP server URL: ${urlResult.error}")
        }
        val headerResult = McpUrlValidator.validateHeaders(headers)
        if (!headerResult.isValid) {
            throw McpException("Invalid MCP server headers: ${headerResult.error}")
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = true
        explicitNulls = false
    }

    private val client: HttpClient = httpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
        }
        // Disable automatic redirect following — we validate each redirect target manually
        followRedirects = false
    }
    private var sessionId: String? = null
    private var requestId = 0
    private var protocolVersion: String? = null
    private var supportsSkills = false

    private fun nextId(): Int = ++requestId

    private suspend fun sendRequest(
        method: String,
        params: JsonElement? = null,
        protocolVersionOverride: String? = protocolVersion,
    ): JsonRpcResponse {
        // Connection-time validation (mitigates DNS rebinding; not a guarantee)
        val urlResult = McpUrlValidator.validate(url, allowPrivateIps)
        if (!urlResult.isValid) {
            throw McpException("MCP server URL validation failed: ${urlResult.error}")
        }

        val request = JsonRpcRequest(
            id = nextId(),
            method = method,
            params = protocolVersionOverride?.let { withMcpRequestMetadata(params, it) } ?: params,
        )
        val requestBody = json.encodeToString(JsonRpcRequest.serializer(), request)

        // Manual redirect handling (max 3 hops)
        var currentUrl = url
        var redirectCount = 0
        val maxRedirects = 3

        while (true) {
            val response = client.post(currentUrl) {
                contentType(ContentType.Application.Json)
                header("Accept", "application/json, text/event-stream")
                protocolVersionOverride?.let { header("MCP-Protocol-Version", it) }
                if (protocolVersionOverride == null) {
                    sessionId?.let { header("Mcp-Session-Id", it) }
                }
                this@McpClient.headers.keys.forEach { key ->
                    header(key, this@McpClient.headers[key]!!)
                }
                setBody(requestBody)
            }

            if (protocolVersionOverride == null) {
                response.headers["Mcp-Session-Id"]?.let { sessionId = it }
            }

            // Handle redirects manually
            if (response.status == HttpStatusCode.MovedPermanently ||
                response.status == HttpStatusCode.Found ||
                response.status == HttpStatusCode.SeeOther ||
                response.status == HttpStatusCode.TemporaryRedirect ||
                response.status == HttpStatusCode.PermanentRedirect
            ) {
                redirectCount++
                if (redirectCount > maxRedirects) {
                    throw McpException("Too many redirects (max $maxRedirects)")
                }
                val location = response.headers["Location"]
                    ?: throw McpException("Redirect without Location header")
                if (!isSameOriginMcpRedirect(currentUrl, location)) {
                    throw McpException("Cross-origin MCP redirect rejected to protect configured credentials")
                }
                val redirectResult = McpUrlValidator.validate(location, allowPrivateIps)
                if (!redirectResult.isValid) {
                    throw McpException("Redirect target rejected: ${redirectResult.error}")
                }
                currentUrl = location
                continue
            }

            val responseText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw McpException("MCP HTTP ${response.status.value}: ${responseText.take(400)}")
            }
            if (responseText.isBlank()) {
                throw McpException("MCP server returned an empty response")
            }

            if (response.headers["Content-Type"]?.contains("text/event-stream") == true) {
                return parseSseResponse(responseText)
            }
            return json.decodeFromString(JsonRpcResponse.serializer(), responseText)
        }
    }

    private fun parseSseResponse(sseText: String): JsonRpcResponse {
        val lines = sseText.lines()
        val dataLines = lines.filter { it.startsWith("data: ") }
        for (line in dataLines) {
            val data = line.removePrefix("data: ").trim()
            if (data.isEmpty()) continue
            try {
                return json.decodeFromString(JsonRpcResponse.serializer(), data)
            } catch (_: Exception) {
                // Not a valid JSON-RPC response, continue
            }
        }
        throw McpException("No valid JSON-RPC response found in SSE stream")
    }

    suspend fun initialize() {
        // Probe the current stateless MCP protocol first. Cloudflare's managed MCP endpoint
        // requires per-request metadata and does not accept the legacy initialize handshake.
        val discovery = try {
            sendRequest(
                method = "server/discover",
                protocolVersionOverride = MCP_CURRENT_PROTOCOL_VERSION,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        val discoveryResult = discovery?.result as? JsonObject
        val supportedVersions = (discoveryResult?.get("supportedVersions") as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .orEmpty()
        if (discovery?.error == null && MCP_CURRENT_PROTOCOL_VERSION in supportedVersions) {
            protocolVersion = MCP_CURRENT_PROTOCOL_VERSION
            supportsSkills = supportsMcpSkillsExtension(
                discoveryResult?.get("capabilities") as? JsonObject,
            )
            return
        }

        // Backward compatibility for servers that still use the stateful initialize flow.
        val params = buildJsonObject {
            put("protocolVersion", JsonPrimitive("2024-11-05"))
            put("capabilities", buildJsonObject {})
            put(
                "clientInfo",
                buildJsonObject {
                    put("name", JsonPrimitive("Brox Kai"))
                    put("version", JsonPrimitive("2.0"))
                },
            )
        }
        val response = sendRequest(
            method = "initialize",
            params = params,
            protocolVersionOverride = null,
        )
        if (response.error != null) {
            throw McpException("Initialize failed: ${response.error.message}")
        }
        protocolVersion = null
        supportsSkills = false
        sendNotification("notifications/initialized")
    }

    private suspend fun sendNotification(method: String) {
        val body = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("method", JsonPrimitive(method))
        }
        val requestBody = json.encodeToString(JsonObject.serializer(), body)

        var currentUrl = url
        var redirectCount = 0
        val maxRedirects = 3

        while (true) {
            val response = client.post(currentUrl) {
                contentType(ContentType.Application.Json)
                sessionId?.let { header("Mcp-Session-Id", it) }
                this@McpClient.headers.keys.forEach { key ->
                    header(key, this@McpClient.headers[key]!!)
                }
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.MovedPermanently ||
                response.status == HttpStatusCode.Found ||
                response.status == HttpStatusCode.SeeOther ||
                response.status == HttpStatusCode.TemporaryRedirect ||
                response.status == HttpStatusCode.PermanentRedirect
            ) {
                redirectCount++
                if (redirectCount > maxRedirects) {
                    throw McpException("Too many redirects (max $maxRedirects)")
                }
                val location = response.headers["Location"]
                    ?: throw McpException("Redirect without Location header")
                if (!isSameOriginMcpRedirect(currentUrl, location)) {
                    throw McpException("Cross-origin MCP redirect rejected to protect configured credentials")
                }
                val redirectResult = McpUrlValidator.validate(location, allowPrivateIps)
                if (!redirectResult.isValid) {
                    throw McpException("Redirect target rejected: ${redirectResult.error}")
                }
                currentUrl = location
                continue
            }
            return
        }
    }

    suspend fun listTools(): List<McpToolDefinition> {
        val response = sendRequest("tools/list")
        if (response.error != null) {
            throw McpException("tools/list failed: ${response.error.message}")
        }
        val result = response.result ?: return emptyList()
        val toolsResult = json.decodeFromJsonElement<McpToolsResult>(result)
        return toolsResult.tools
    }

    /**
     * Discover server-published agent skills through the optional Skills-over-MCP extension.
     * Servers that do not implement this extension return an MCP method-not-found error.
     * Returned metadata is untrusted input and must be displayed as data, never executed.
     */
    suspend fun listSkills(): JsonArray {
        if (!supportsSkills) return JsonArray(emptyList())

        val skills = mutableListOf<JsonElement>()
        val seenCursors = mutableSetOf<String>()
        var cursor: String? = null
        var pageCount = 0
        do {
            val params = buildJsonObject {
                cursor?.let { put("cursor", JsonPrimitive(it)) }
            }
            val response = sendRequest("skills/list", params)
            if (response.error != null) {
                throw McpException("skills/list failed: ${response.error.message}")
            }
            val result = response.result as? JsonObject
                ?: throw McpException("skills/list returned an invalid response")
            val pageSkills = result["skills"] as? JsonArray
                ?: throw McpException("skills/list response does not contain a skills array")
            skills.addAll(pageSkills)
            val nextCursor = (result["nextCursor"] as? JsonPrimitive)?.contentOrNull
                ?.takeIf { it.isNotBlank() }
            if (nextCursor != null && !seenCursors.add(nextCursor)) {
                throw McpException("skills/list returned a repeated pagination cursor")
            }
            cursor = nextCursor
            pageCount++
            if (pageCount >= 100 && cursor != null) {
                throw McpException("skills/list exceeded the 100-page safety limit")
            }
        } while (cursor != null)

        return JsonArray(skills)
    }

    /** Fetch the authoritative metadata and resource manifest for one skill URI. */
    suspend fun getSkill(uri: String): JsonObject {
        validateSkillUri(uri)
        if (!supportsSkills) {
            throw McpException("MCP server does not advertise the Skills extension")
        }
        val params = buildJsonObject { put("uri", JsonPrimitive(uri)) }
        val response = sendRequest("skills/get", params)
        if (response.error != null) {
            throw McpException("skills/get failed: ${response.error.message}")
        }
        val result = response.result as? JsonObject
            ?: throw McpException("skills/get returned an invalid response")
        return result["skill"] as? JsonObject
            ?: throw McpException("skills/get response does not contain a skill entry")
    }

    /**
     * Read a skill's SKILL.md through the server's resources/read method.
     * The content remains untrusted; reading it does not approve or activate the skill.
     */
    suspend fun readSkill(uri: String): String {
        getSkill(uri)
        val params = buildJsonObject { put("uri", JsonPrimitive(uri)) }
        val response = sendRequest("resources/read", params)
        if (response.error != null) {
            throw McpException("resources/read failed: ${response.error.message}")
        }
        val result = response.result as? JsonObject
            ?: throw McpException("resources/read returned an invalid response")
        val contents = result["contents"] as? JsonArray
            ?: throw McpException("resources/read response does not contain contents")
        return contents.mapNotNull { item ->
            (item as? JsonObject)?.get("text")?.let { it as? JsonPrimitive }?.contentOrNull
        }.joinToString("\n")
    }

    private fun validateSkillUri(uri: String) {
        if (uri.isBlank() || uri.length > 2048 ||
            uri.any { it.code < 0x20 || it.code == 0x7f } ||
            uri.split('/').any { it == ".." }
        ) {
            throw McpException("Invalid skill resource URI")
        }
    }

    suspend fun callTool(name: String, arguments: JsonObject): String {
        val params = buildJsonObject {
            put("name", JsonPrimitive(name))
            put("arguments", arguments)
        }
        val response = sendRequest("tools/call", params)
        if (response.error != null) {
            throw McpException("tools/call failed: ${response.error.message}")
        }
        val result = response.result ?: return ""
        val callResult = json.decodeFromJsonElement<McpCallToolResult>(result)
        if (callResult.isError) {
            val errorText = callResult.content.mapNotNull { it.text }.joinToString("\n")
            throw McpException("Tool error: $errorText")
        }
        return callResult.content.mapNotNull { it.text }.joinToString("\n")
    }

    fun close() {
        client.close()
    }
}

class McpException(message: String) : Exception(message)
