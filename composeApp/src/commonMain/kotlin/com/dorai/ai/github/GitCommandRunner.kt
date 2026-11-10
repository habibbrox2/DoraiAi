package com.dorai.ai.github

/**
 * Result of running one git command through [GitCommandRunner].
 */
data class GitCommandResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val timedOut: Boolean = false,
)

/**
 * Runs validated, read-only git commands inside a project directory.
 *
 * Implementations must never place the GitHub token in arguments or environment;
 * network commands (clone, fetch, pull) use [GitCredentialBroker] instead.
 */
interface GitCommandRunner {

    /**
     * Runs `git <args>` inside `projectsRoot/<project>`.
     *
     * [args] must already satisfy [GitCommandPolicy.isAllowedGitArgs]; the runner
     * rejects unsafe invocations before spawning.
     *
     * Write subcommands are only admitted when the caller explicitly passes
     * `writeEnabled = true` (the maintainer opt-in); then exact write shapes are
     * enforced (`add` explicit paths, `commit -m <msg>` only, `push origin HEAD`
     * only). Defaults to `false`, so every unqualified call is read-only.
     */
    suspend fun run(project: String, args: List<String>, timeoutSeconds: Long = 60, writeEnabled: Boolean = false): GitCommandResult

    /** True when a usable `git` binary is available on this platform. */
    suspend fun isGitAvailable(): Boolean
}

/** Creates the platform-backed git command runner. */
expect fun createGitCommandRunner(): GitCommandRunner
