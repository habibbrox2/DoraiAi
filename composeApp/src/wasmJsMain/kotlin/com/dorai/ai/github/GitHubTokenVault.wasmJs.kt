package com.dorai.ai.github

// Web/WasmJs has no secure storage — GitHub connect must show "Android and Windows only".
// The vault reports unsupported everywhere and never persists tokens.
actual fun createGitHubTokenVault(): GitHubTokenVault = UnsupportedGitHubTokenVault()

private class UnsupportedGitHubTokenVault : GitHubTokenVault {
    override fun readAccessToken(): String? = null
    override fun readRefreshToken(): String? = null
    override fun readLogin(): String? = null

    override fun store(
        accessToken: String,
        refreshToken: String?,
        expiresAtEpochMs: Long?,
        refreshExpiresAtEpochMs: Long?,
        login: String,
        installationIds: List<Long>,
    ) {
        // No-op — tokens are never persisted on web.
    }

    override fun clear() {
        // No-op — nothing is stored.
    }

    override fun isConnected(): Boolean = false
}
