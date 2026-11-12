package com.dorai.ai.mcp

import kotlinx.serialization.Serializable

/**
 * Runtime OAuth state for one MCP server. Persisted inside [McpServerConfig] (the whole
 * config blob is stored under the platform's encrypted settings — EncryptedSharedPreferences
 * on Android, DPAPI-backed AES-256-GCM on Desktop).
 *
 * Only the [accessToken] is sent (as a `Bearer` token) — refresh tokens never leave the device
 * except to the resource's registration endpoint during refresh.
 */
@Serializable
data class McpOAuthSession(
    /** The resource this token is scoped to, e.g. `https://mcp.desktopcommander.app/mcp`. */
    val resource: String,
    /** Authorization endpoint discovered from the resource's metadata document. */
    val authorizationEndpoint: String,
    /** Token endpoint discovered from the authorization server's metadata document. */
    val tokenEndpoint: String,
    /** Dynamic Client Registration id for this app (or null when the flow used a public client id). */
    val clientId: String,
    /** Scopes requested at authorize time, e.g. `["mcp:tools"]`. */
    val scopes: List<String> = emptyList(),
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAtEpochMs: Long? = null,
)

@Serializable
data class McpServerConfig(
    val id: String,
    val name: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true,
    /** When non-null the server requires an OAuth flow before tools will work. */
    val oauth: McpOAuthSession? = null,
)
