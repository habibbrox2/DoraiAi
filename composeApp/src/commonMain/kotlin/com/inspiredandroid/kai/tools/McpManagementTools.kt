package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.mcp.McpServerManager
import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolSchema
import kotlinx.serialization.json.Json

/**
 * Agent-facing tools for managing MCP (Model Context Protocol) servers at runtime.
 *
 * These let the chat agent add, remove, toggle, connect and inspect MCP servers on
 * behalf of the user, instead of requiring them to open Settings.
 */
object McpManagementTools {

    fun getMcpManagementTools(mcpServerManager: McpServerManager): List<Tool> = listOf(
        listMcpServersTool(mcpServerManager),
        addMcpServerTool(mcpServerManager),
        removeMcpServerTool(mcpServerManager),
        toggleMcpServerTool(mcpServerManager),
        connectMcpServerTool(mcpServerManager),
    )

    private fun listMcpServersTool(mcp: McpServerManager) = object : Tool {
        override val schema = ToolSchema(
            name = "mcp_list_servers",
            description = "List all configured MCP servers with their id, name, url, enabled state and connection status.",
            parameters = emptyMap(),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val servers = mcp.getServers()
            return mapOf(
                "servers" to servers.map { s ->
                    mapOf(
                        "id" to s.id,
                        "name" to s.name,
                        "url" to s.url,
                        "enabled" to s.isEnabled,
                        "connected" to mcp.isConnected(s.id),
                    )
                },
                "count" to servers.size,
            )
        }
    }

    private fun addMcpServerTool(mcp: McpServerManager) = object : Tool {
        override val schema = ToolSchema(
            name = "mcp_add_server",
            description = "Add a new MCP server by name and URL (optionally with headers). Validates the URL before saving. Returns the new server id.",
            parameters = mapOf(
                "name" to ParameterSchema(type = "string", description = "Display name for the server", required = true),
                "url" to ParameterSchema(type = "string", description = "MCP server endpoint URL (http/https)", required = true),
                "headers" to ParameterSchema(type = "string", description = "Optional JSON object of request headers, e.g. {\"Authorization\":\"Bearer ...\"}", required = false),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val name = args["name"]?.toString()?.trim()
                ?: return mapOf("success" to false, "error" to "Missing name")
            val url = args["url"]?.toString()?.trim()
                ?: return mapOf("success" to false, "error" to "Missing url")
            if (name.isEmpty() || url.isEmpty()) {
                return mapOf("success" to false, "error" to "name and url are required")
            }
            val headers = args["headers"]?.toString()?.trim()?.let { raw ->
                if (raw.isEmpty()) {
                    emptyMap()
                } else {
                    try {
                        Json.decodeFromString<Map<String, String>>(raw)
                    } catch (e: Exception) {
                        return mapOf("success" to false, "error" to "Invalid headers JSON: ${e.message}")
                    }
                }
            } ?: emptyMap()

            return try {
                val config = mcp.addServer(name, url, headers)
                mapOf("success" to true, "id" to config.id, "name" to config.name, "url" to config.url)
            } catch (e: Exception) {
                mapOf("success" to false, "error" to (e.message ?: "Failed to add MCP server"))
            }
        }
    }

    private fun removeMcpServerTool(mcp: McpServerManager) = object : Tool {
        override val schema = ToolSchema(
            name = "mcp_remove_server",
            description = "Remove an MCP server by its id.",
            parameters = mapOf(
                "id" to ParameterSchema(type = "string", description = "MCP server id (from mcp_list_servers)", required = true),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val id = args["id"]?.toString()?.trim()
                ?: return mapOf("success" to false, "error" to "Missing id")
            if (mcp.getServers().none { it.id == id }) {
                return mapOf("success" to false, "error" to "Server not found: $id")
            }
            return try {
                mcp.removeServer(id)
                mapOf("success" to true, "removed" to id)
            } catch (e: Exception) {
                mapOf("success" to false, "error" to (e.message ?: "Failed to remove MCP server"))
            }
        }
    }

    private fun toggleMcpServerTool(mcp: McpServerManager) = object : Tool {
        override val schema = ToolSchema(
            name = "mcp_toggle_server",
            description = "Enable or disable an MCP server by id.",
            parameters = mapOf(
                "id" to ParameterSchema(type = "string", description = "MCP server id", required = true),
                "enabled" to ParameterSchema(type = "boolean", description = "true to enable, false to disable", required = true),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val id = args["id"]?.toString()?.trim()
                ?: return mapOf("success" to false, "error" to "Missing id")
            val enabled = when (val v = args["enabled"]) {
                is Boolean -> v
                is String -> v.equals("true", ignoreCase = true)
                else -> return mapOf("success" to false, "error" to "enabled must be boolean")
            }
            if (mcp.getServers().none { it.id == id }) {
                return mapOf("success" to false, "error" to "Server not found: $id")
            }
            return try {
                mcp.setServerEnabled(id, enabled)
                mapOf("success" to true, "id" to id, "enabled" to enabled)
            } catch (e: Exception) {
                mapOf("success" to false, "error" to (e.message ?: "Failed to toggle MCP server"))
            }
        }
    }

    private fun connectMcpServerTool(mcp: McpServerManager) = object : Tool {
        override val schema = ToolSchema(
            name = "mcp_connect_server",
            description = "Connect to an MCP server and discover its tools. Server must be enabled.",
            parameters = mapOf(
                "id" to ParameterSchema(type = "string", description = "MCP server id", required = true),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val id = args["id"]?.toString()?.trim()
                ?: return mapOf("success" to false, "error" to "Missing id")
            val server = mcp.getServers().firstOrNull { it.id == id }
                ?: return mapOf("success" to false, "error" to "Server not found: $id")
            if (!server.isEnabled) {
                return mapOf("success" to false, "error" to "Server is disabled; enable it first")
            }
            return try {
                val result = mcp.connectAndDiscoverTools(id)
                if (result.isSuccess) {
                    val tools = result.getOrNull() ?: emptyList()
                    mapOf(
                        "success" to true,
                        "id" to id,
                        "tools" to tools.map { mapOf("name" to it.name, "description" to it.description) },
                        "toolCount" to tools.size,
                    )
                } else {
                    mapOf("success" to false, "error" to (result.exceptionOrNull()?.message ?: "Connection failed"))
                }
            } catch (e: Exception) {
                mapOf("success" to false, "error" to (e.message ?: "Connection failed"))
            }
        }
    }
}
