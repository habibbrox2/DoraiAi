package com.dorai.ai.github

/**
 * Brokers git credential helper configuration so the GitHub token never appears
 * literally in git command arguments, environment variables, logs, or tool output.
 *
 * [credentialGitConfigArgs] creates an ephemeral helper script (mode 0600 /
 * owner-only equivalent) and returns git `-c credential.helper=!...` args that
 * point to it. Git invokes the helper to obtain `username`/`password`; the
 * helper reads the token from a sidecar file and writes it to git's stdout.
 * Call [CredentialContext.release] after the git process finishes so both
 * ephemeral files are deleted.
 */
interface GitCredentialBroker {

    data class CredentialContext(
        /** Git args, e.g. `["-c", "credential.helper=!/path/to/helper-script"]`. */
        val gitArgs: List<String>,
        /** Deletes the ephemeral helper script and token sidecar. Call after the git process finishes. */
        val release: () -> Unit,
    )

    /**
     * Returns git args that configure an ephemeral credential helper for [command].
     * The returned args never contain the token literal.
     */
    fun credentialGitConfigArgs(command: List<String>): CredentialContext
}

/** Creates the platform-backed credential broker. */
expect fun createGitCredentialBroker(): GitCredentialBroker
