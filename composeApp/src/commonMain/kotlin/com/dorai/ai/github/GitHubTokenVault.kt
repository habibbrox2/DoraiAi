package com.dorai.ai.github

/**
 * Platform-backed storage for the GitHub OAuth tokens and account metadata.
 *
 * Implementations must store the values encrypted at rest (or refuse to store
 * them entirely on platforms without secure storage). Tokens must never be
 * copied into [com.dorai.ai.data.AppSettings], logs, or analytics.
 */
interface GitHubTokenVault {
    fun readAccessToken(): String?
    fun readRefreshToken(): String?
    fun readLogin(): String?
    fun store(
        accessToken: String,
        refreshToken: String?,
        expiresAtEpochMs: Long?,
        refreshExpiresAtEpochMs: Long?,
        login: String,
        installationIds: List<Long>,
    )
    fun clear()
    fun isConnected(): Boolean = readAccessToken() != null && readLogin() != null
}

/** Creates the platform-backed vault. Platforms without secure storage return a no-op vault. */
expect fun createGitHubTokenVault(): GitHubTokenVault
