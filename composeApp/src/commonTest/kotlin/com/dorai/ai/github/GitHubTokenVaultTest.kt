package com.dorai.ai.github

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubTokenVaultTest {

    @Test
    fun `stored vault round trips tokens and login`() {
        val vault = InMemoryGitHubTokenVault()
        vault.store("access", "refresh", 1_000L, 2_000L, "octocat", listOf(1L))
        assertEquals("access", vault.readAccessToken())
        assertEquals("refresh", vault.readRefreshToken())
        assertEquals("octocat", vault.readLogin())
        assertTrue(vault.isConnected())
    }

    @Test
    fun `clear removes all token state`() {
        val vault = InMemoryGitHubTokenVault()
        vault.store("access", "refresh", null, null, "octocat", emptyList())
        vault.clear()
        assertNull(vault.readAccessToken())
        assertNull(vault.readRefreshToken())
        assertFalse(vault.isConnected())
    }
}

private class InMemoryGitHubTokenVault : GitHubTokenVault {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var accessExpiresAtEpochMs: Long? = null
    private var refreshExpiresAtEpochMs: Long? = null
    private var login: String? = null
    private var installationIds: List<Long> = emptyList()

    override fun readAccessToken(): String? = accessToken
    override fun readRefreshToken(): String? = refreshToken
    override fun readLogin(): String? = login

    override fun store(
        accessToken: String,
        refreshToken: String?,
        expiresAtEpochMs: Long?,
        refreshExpiresAtEpochMs: Long?,
        login: String,
        installationIds: List<Long>,
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.accessExpiresAtEpochMs = expiresAtEpochMs
        this.refreshExpiresAtEpochMs = refreshExpiresAtEpochMs
        this.login = login
        this.installationIds = installationIds
    }

    override fun clear() {
        accessToken = null
        refreshToken = null
        accessExpiresAtEpochMs = null
        refreshExpiresAtEpochMs = null
        login = null
        installationIds = emptyList()
    }

    override fun isConnected(): Boolean = accessToken != null && login != null
}
