package com.dorai.ai.github

/** Browser builds cannot spawn a local Git executable. */
actual fun createGitCommandRunner(): GitCommandRunner = object : GitCommandRunner {
    override suspend fun run(
        project: String,
        args: List<String>,
        timeoutSeconds: Long,
        writeEnabled: Boolean,
    ): GitCommandResult = GitCommandResult(
        success = false,
        stdout = "",
        stderr = "Local Git commands are not available in browser builds.",
        exitCode = 127,
    )

    override suspend fun isGitAvailable(): Boolean = false
}
