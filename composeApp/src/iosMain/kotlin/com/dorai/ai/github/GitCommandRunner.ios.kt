package com.dorai.ai.github

/** iOS has no local Git executable integration; repository operations are unavailable. */
actual fun createGitCommandRunner(): GitCommandRunner = UnsupportedIosGitCommandRunner

private object UnsupportedIosGitCommandRunner : GitCommandRunner {
    override suspend fun run(
        project: String,
        args: List<String>,
        timeoutSeconds: Long,
        writeEnabled: Boolean,
    ): GitCommandResult = GitCommandResult(
        success = false,
        stdout = "",
        stderr = "GitCommandRunner is not available on iOS.",
        exitCode = -1,
    )

    override suspend fun isGitAvailable(): Boolean = false
}