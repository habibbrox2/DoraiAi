package com.dorai.ai.github

/**
 * iOS has no shell/git executable surface for the Workspace guest — local git
 * operations on iOS are unavailable. Every call fails cleanly (exit 127) instead of
 * pretending to succeed, matching the wasmJs target's honest no-op behavior.
 */
actual fun createGitCommandRunner(): GitCommandRunner = object : GitCommandRunner {
    override suspend fun run(
        project: String,
        args: List<String>,
        timeoutSeconds: Long,
        writeEnabled: Boolean,
    ): GitCommandResult = GitCommandResult(
        success = false,
        stdout = "",
        stderr = "Local Git commands are not available on iOS.",
        exitCode = 127,
    )

    override suspend fun isGitAvailable(): Boolean = false
}
