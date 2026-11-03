package com.inspiredandroid.kai.mcp

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpProtocolCompatibilityTest {

    @Test
    fun `modern request metadata is merged without trusting caller metadata`() {
        val params = buildJsonObject {
            put("uri", JsonPrimitive("skill://demo/SKILL.md"))
            put(
                "_meta",
                buildJsonObject {
                    put("untrusted", JsonPrimitive("caller-value"))
                },
            )
        }

        val result = withMcpRequestMetadata(params, MCP_CURRENT_PROTOCOL_VERSION)
        val metadata = result["_meta"] as JsonObject

        assertEquals(
            MCP_CURRENT_PROTOCOL_VERSION,
            (metadata["io.modelcontextprotocol/protocolVersion"] as JsonPrimitive).content,
        )
        assertEquals(
            "Brox Kai",
            ((metadata["io.modelcontextprotocol/clientInfo"] as JsonObject)["name"] as JsonPrimitive).content,
        )
        assertTrue(metadata.containsKey("io.modelcontextprotocol/clientCapabilities"))
        assertFalse(metadata.containsKey("untrusted"))
        assertEquals("skill://demo/SKILL.md", (result["uri"] as JsonPrimitive).content)
    }

    @Test
    fun `skills extension is enabled only when server advertises it`() {
        val capabilities = buildJsonObject {
            put(
                "extensions",
                buildJsonObject {
                    put("io.modelcontextprotocol/skills", buildJsonObject {})
                },
            )
        }
        assertTrue(supportsMcpSkillsExtension(capabilities))
        assertFalse(supportsMcpSkillsExtension(buildJsonObject { put("tools", buildJsonObject {}) }))
        assertFalse(supportsMcpSkillsExtension(null))
    }

    @Test
    fun `redirects are restricted to the same origin`() {
        assertTrue(
            isSameOriginMcpRedirect(
                "https://mcp.cloudflare.com/mcp",
                "https://mcp.cloudflare.com/oauth/callback",
            ),
        )
        assertFalse(
            isSameOriginMcpRedirect(
                "https://mcp.cloudflare.com/mcp",
                "https://attacker.example/collect",
            ),
        )
        assertFalse(
            isSameOriginMcpRedirect(
                "https://mcp.cloudflare.com/mcp",
                "http://mcp.cloudflare.com/mcp",
            ),
        )
        assertFalse(
            isSameOriginMcpRedirect(
                "https://mcp.cloudflare.com/mcp",
                "https://mcp.cloudflare.com:8443/mcp",
            ),
        )
    }
}
