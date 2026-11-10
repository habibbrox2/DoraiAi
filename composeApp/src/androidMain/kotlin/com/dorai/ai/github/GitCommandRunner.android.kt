package com.dorai.ai.github

/**
 * Android actual for [createGitCommandRunner].
 *
 * The Android Git integration does not go through this runner — clone/status/fetch/pull are
 * implemented against the proot Workspace via the `WorkspaceController` and
 * `WorkspaceEnvironmentManager` (Task 12), with one-shot executors and strict URL/project-name
 * validation. This actual exists so `createGitCommandRunner()` resolves on Android; it
 * deliberately reports git as unavailable and fails every invocation instead of spawning a
 * second, unaudited git path.
 */
actual fun createGitCommandRunner(): GitCommandRunner = UnsupportedGitCommandRunner

private object UnsupportedGitCommandRunner : GitCommandRunner {
    override suspend fun run(
        project: String,
        args: List<String>,
        timeoutSeconds: Long,
        writeEnabled: Boolean,
    ): GitCommandResult = GitCommandResult(
        success = false,
        stdout = "",
        stderr = "GitCommandRunner is not used on Android — use the Workspace Git commands instead.",
        exitCode = -1,
    )

    override suspend fun isGitAvailable(): Boolean = false
}
