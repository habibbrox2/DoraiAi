package com.dorai.ai.github

/**
 * iOS cannot spawn a local git executable, so there is nothing to feed a credential
 * helper — callers get a hard failure instead of a quietly-broken clone.
 */
actual fun createGitCredentialBroker(): GitCredentialBroker = object : GitCredentialBroker {
    override fun credentialGitConfigArgs(command: List<String>): GitCredentialBroker.CredentialContext = throw UnsupportedOperationException("Git credential helpers are not available on iOS.")
}
