package com.dorai.ai.mcp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpOAuthTest {

    @Test
    fun `protectedResourceMetadataUrl maps the resource path correctly`() {
        assertEquals(
            "https://mcp.desktopcommander.app/.well-known/oauth-protected-resource/mcp",
            McpOAuth.protectedResourceMetadataUrl("https://mcp.desktopcommander.app/mcp"),
        )
        assertEquals(
            "https://example.com/.well-known/oauth-protected-resource",
            McpOAuth.protectedResourceMetadataUrl("https://example.com/"),
        )
        assertEquals(
            "https://example.com/.well-known/oauth-protected-resource/a/b",
            McpOAuth.protectedResourceMetadataUrl("https://example.com/a/b"),
        )
    }

    @Test
    fun `authorizationServerMetadataUrl appends the well-known path`() {
        assertEquals(
            "https://mcp.desktopcommander.app/.well-known/oauth-authorization-server",
            McpOAuth.authorizationServerMetadataUrl("https://mcp.desktopcommander.app"),
        )
        assertEquals(
            "https://mcp.desktopcommander.app/.well-known/oauth-authorization-server",
            McpOAuth.authorizationServerMetadataUrl("https://mcp.desktopcommander.app/"),
        )
    }

    @Test
    fun `randomUrlSafeString is exactly the requested length and contains only base64url chars`() {
        repeat(10) {
            val token = randomUrlSafeString(32)
            assertEquals(32, token.length)
            assertTrue(token.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        }
    }

    @Test
    fun `randomUrlSafeString tokens are unique`() {
        val tokens = (1..20).map { randomUrlSafeString(32) }.toSet()
        assertTrue(tokens.size > 1, "tokens collided")
    }

    @Test
    fun `base64UrlEncode omits padding and uses url-safe alphabet`() {
        // RFC 4648 vectors — the encoder must be byte-accurate for PKCE S256 of a known hash.
        assertEquals("bWlja2Vs", base64UrlEncode(byteArrayOf(0x6d, 0x69, 0x63, 0x6b, 0x65, 0x6c)))
        assertEquals("bWI", base64UrlEncode(byteArrayOf(0x6d, 0x62)))
        assertEquals("bXA_", base64UrlEncode(byteArrayOf(0x6d, 0x70, 0x3f)))
        // The 62-byte PKCE verifier is encoded with no trailing '=' padding.
        val verifier = randomUrlSafeString(64)
        val encoded = base64UrlEncode(sha256(verifier.encodeToByteArray()))
        assertFalse(encoded.contains("="))
    }

    @Test
    fun `pkcePair challenge equals base64url of sha256 of verifier`() {
        val (verifier, challenge) = McpOAuth.pkcePair()
        assertEquals(base64UrlEncode(sha256(verifier.encodeToByteArray())), challenge)
        assertTrue(challenge.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun `buildAuthorizationUrl emits required parameters`() {
        val url = McpOAuth.buildAuthorizationUrl(
            authorizationEndpoint = "https://auth.example.com/authorize",
            clientId = "client-123",
            redirectUri = "http://127.0.0.1:4321/callback",
            state = "state-abc",
            codeChallenge = "challenge-xyz",
            scopes = listOf("mcp:tools"),
            resource = "https://example.com/mcp",
        )
        assertTrue(url.startsWith("https://auth.example.com/authorize?"))
        assertTrue("response_type=code" in url)
        assertTrue("client_id=client-123" in url)
        assertTrue("redirect_uri=http%3A%2F%2F127.0.0.1%3A4321%2Fcallback" in url)
        assertTrue("state=state-abc" in url)
        assertTrue("code_challenge=challenge-xyz" in url)
        assertTrue("code_challenge_method=S256" in url)
        assertTrue("scope=mcp%3Atools" in url)
        assertTrue("resource=https%3A%2F%2Fexample.com%2Fmcp" in url)
    }

    @Test
    fun `McpServerConfig roundtrips OAuth session through serialization`() {
        val session = McpOAuthSession(
            resource = "https://mcp.desktopcommander.app/mcp",
            authorizationEndpoint = "https://auth.example.com/authorize",
            tokenEndpoint = "https://auth.example.com/token",
            clientId = "desktop-123",
            scopes = listOf("mcp:tools"),
            accessToken = "access-abc",
            refreshToken = "refresh-xyz",
            expiresAtEpochMs = 1700000000000L,
        )
        val original = McpServerConfig(
            id = "desktop_commander",
            name = "Desktop Commander",
            url = "https://mcp.desktopcommander.app/mcp",
            headers = emptyMap(),
            isEnabled = true,
            oauth = session,
        )
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(McpServerConfig.serializer(), original)
        val decoded = json.decodeFromString(McpServerConfig.serializer(), encoded)
        assertEquals(original, decoded)
        assertEquals("access-abc", decoded.oauth?.accessToken)
        assertEquals("refresh-xyz", decoded.oauth?.refreshToken)
    }

    @Test
    fun `desktop commander popular preset requires auth and is not a silently-added one-tap entry`() {
        val dc = popularMcpServers.single { it.name == "Desktop Commander" }
        assertTrue(dc.requiresAuth)
        assertEquals("https://mcp.desktopcommander.app/mcp", dc.url)
        assertTrue(dc.headers.isEmpty())
    }
}
