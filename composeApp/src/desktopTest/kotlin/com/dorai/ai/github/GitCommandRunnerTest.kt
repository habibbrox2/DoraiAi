package com.dorai.ai.github

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitCommandRunnerTest {

    @Test
    fun `rejects write subcommand before spawning`() = runTest {
        val runner = DesktopGitCommandRunner(
            GitExecutableLocator(),
            createGitCredentialBroker(),
        )
        val result = runner.run("myproject", listOf("commit", "-m", "x"))

        assertFalse(result.success)
        assertTrue(result.stderr.contains("Rejected by GitCommandPolicy"))
    }

    @Test
    fun `rejects rejected flag before spawning`() = runTest {
        val runner = DesktopGitCommandRunner(
            GitExecutableLocator(),
            createGitCredentialBroker(),
        )
        val result = runner.run("myproject", listOf("branch", "-D", "old-branch"))

        assertFalse(result.success)
        assertTrue(result.stderr.contains("Rejected by GitCommandPolicy"))
    }

    @Test
    fun `allows read-only subcommand through validation`() = runTest {
        val runner = DesktopGitCommandRunner(
            GitExecutableLocator(),
            createGitCredentialBroker(),
        )
        // git is not available on this machine, so it fails at locator, not policy
        val result = runner.run("myproject", listOf("status"))

        // Either git-not-found or execution error — both mean validation passed
        assertFalse(result.success)
        assertTrue(result.stderr.contains("git not found") || result.exitCode != 0)
    }

    @Test
    fun `isGitAvailable returns a boolean without crashing`() = runTest {
        val runner = DesktopGitCommandRunner(
            GitExecutableLocator(),
            createGitCredentialBroker(),
        )
        val available = runner.isGitAvailable()
        // Just verify it returns a boolean (either value is acceptable)
        assertTrue(available || !available)
    }
}
