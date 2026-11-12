package com.dorai.ai.mcp

import com.dorai.ai.openUrl
import io.ktor.client.HttpClient
import kotlin.time.Clock

internal object McpOAuthFlow {
    /**
     * Full authorization-code + PKCE flow against one server:
     * discover → DCR (if available) → open browser → wait for loopback callback → exchange → session.
     */
    suspend fun authorize(
        server: McpServerConfig,
        clientFactory: () -> HttpClient,
    ): Result<McpServerConfig> {
        val listener = createOauthCallbackListener()
        return runCatching {
            listener.start()
            val endpoints = McpOAuth.discover(server.url, clientFactory())
            val port = listener.port
            val redirectUri = "http://127.0.0.1:$port/callback"
            val (verifier, challenge) = McpOAuth.pkcePair()
            val state = randomUrlSafeString(32)

            val clientId = endpoints.registrationEndpoint?.let { registrationEndpoint ->
                McpOAuth.registerClient(registrationEndpoint, "DORAI AI", redirectUri, endpoints.scopes, clientFactory())
            } ?: "dorai-android" // Public client ID — OAuth supports only what the resource owner advertises.

            val authorizeUrl = McpOAuth.buildAuthorizationUrl(
                authorizationEndpoint = endpoints.authorizationEndpoint,
                clientId = clientId,
                redirectUri = redirectUri,
                state = state,
                codeChallenge = challenge,
                scopes = endpoints.scopes.ifEmpty { listOf("mcp:tools") },
                resource = endpoints.resource,
            )

            if (!openUrl(authorizeUrl)) {
                throw McpException("The system could not open a browser for authorization")
            }

            val callback = listener.awaitHandshake(180)
            when {
                callback.error != null -> throw McpException("Authorization failed: ${callback.errorDescription ?: callback.error}")
                callback.code == null -> throw McpException("The authorization server returned no code")
                callback.state != state -> throw McpException("Authorization state mismatch — already used or CSRF; try again")
            }

            val tokens = McpOAuth.exchangeCode(endpoints.tokenEndpoint, clientId, callback.code!!, verifier, redirectUri, clientFactory())
            val now = Clock.System.now().toEpochMilliseconds()
            val expiresAt = tokens.expiresInSeconds?.let { now + it * 1000 }
            server.copy(
                oauth = McpOAuthSession(
                    resource = endpoints.resource,
                    authorizationEndpoint = endpoints.authorizationEndpoint,
                    tokenEndpoint = endpoints.tokenEndpoint,
                    clientId = clientId,
                    scopes = endpoints.scopes,
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresAtEpochMs = expiresAt,
                ),
            )
        }.onFailure { listener.stop() }
    }

    /** Refresh the session if expired (or about to expire in the next 5 minutes). */
    suspend fun refreshSessionIfExpired(
        server: McpServerConfig,
        clientFactory: () -> HttpClient,
    ): Result<McpServerConfig> {
        val session = server.oauth ?: return Result.failure(McpException("Server is not authorized"))
        val refreshToken = session.refreshToken
            ?: return Result.failure(McpException("The server did not issue a refresh token; re-authorize in Settings"))
        val now = Clock.System.now().toEpochMilliseconds()
        val expiresAt = session.expiresAtEpochMs ?: (now - 1) // treat unknown as expired
        if (expiresAt - now > 5 * 60 * 1000) return Result.success(server) // still fresh

        return runCatching {
            val tokens = McpOAuth.refreshAccessToken(session.tokenEndpoint, session.clientId, refreshToken, clientFactory())
            val expiresIn = tokens.expiresInSeconds
            server.copy(
                oauth = session.copy(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken ?: refreshToken,
                    expiresAtEpochMs = expiresIn?.let { now + it * 1000 } ?: session.expiresAtEpochMs,
                ),
            )
        }.onFailure { e ->
            // A failed refresh surfaces as 401 with a clear message in the caller.
            println("McpOAuthFlow: refresh failed for ${server.name}: ${e.message}")
        }
    }
}
