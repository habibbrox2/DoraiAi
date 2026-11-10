package com.dorai.ai.github

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitCredentialBrokerTest {

    private class FakeVault : GitHubTokenVault {
        var accessToken: String? = "ghs_test_token_12345"
        override fun readAccessToken(): String? = accessToken
        override fun readRefreshToken(): String? = null
        override fun readLogin(): String? = "test-user"
        override fun store(
            accessToken: String,
            refreshToken: String?,
            expiresAtEpochMs: Long?,
            refreshExpiresAtEpochMs: Long?,
            login: String,
            installationIds: List<Long>,
        ) {}
        override fun clear() {}
    }

    private class TestBroker(
        private val vault: GitHubTokenVault = FakeVault(),
    ) : GitCredentialBroker {
        override fun credentialGitConfigArgs(command: List<String>): GitCredentialBroker.CredentialContext {
            val token = vault.readAccessToken() ?: error("No token")
            val dir = File(java.io.File(java.lang.System.getProperty("java.io.tmpdir")), "dorai-git-cred-test")
            dir.mkdirs()
            val random = java.util.UUID.randomUUID().toString().substring(0, 8)
            val tokenFile = File(dir, "git-credential-token-$random")
            tokenFile.writeText(token)
            val helper = File(dir, "git-credential-helper-$random.sh")
            helper.writeText(
                """#!/bin/sh
printf 'username=x-access-token\n'
printf 'password=%s\n' "$(cat '${tokenFile.absolutePath}')"
                """.trimIndent(),
            )
            helper.setExecutable(true, false)
            return GitCredentialBroker.CredentialContext(
                gitArgs = listOf("-c", "credential.helper=!${helper.absolutePath}"),
                release = {
                    helper.delete()
                    tokenFile.delete()
                },
            )
        }
    }

    @Test
    fun `credential args never contain token literal`() {
        val broker = TestBroker()
        val ctx = broker.credentialGitConfigArgs(listOf("clone", "https://github.com/owner/repo"))
        try {
            val joined = ctx.gitArgs.joinToString(" ")
            assertFalse(joined.contains("ghs_test_token_12345"))
            assertTrue(ctx.gitArgs.any { it.contains("credential") })
        } finally {
            ctx.release()
        }
    }

    @Test
    fun `ephemeral helper file is deleted after release`() {
        val broker = TestBroker()
        val ctx = broker.credentialGitConfigArgs(listOf("clone", "https://github.com/owner/repo"))
        val helperPath = ctx.gitArgs
            .first { it.startsWith("credential.helper=!") }
            .removePrefix("credential.helper=!")
        val helperFile = File(helperPath)
        assertTrue(helperFile.exists(), "helper script should exist before release")

        // Find the token sidecar before release.
        val dir = helperFile.parentFile
        val tokenFileBefore = dir?.listFiles()
            ?.firstOrNull { it.name.startsWith("git-credential-token-") }
        assertNotNull(tokenFileBefore, "token sidecar should exist before release")
        assertTrue(tokenFileBefore.exists())

        ctx.release()

        assertFalse(helperFile.exists(), "helper script should be deleted after release")
        assertFalse(tokenFileBefore.exists(), "token sidecar should be deleted after release")
    }
}
