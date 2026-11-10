package com.dorai.ai.github

/** Browser builds cannot create an OS-level Git credential helper. */
actual fun createGitCredentialBroker(): GitCredentialBroker = object : GitCredentialBroker {
    override fun credentialGitConfigArgs(command: List<String>): GitCredentialBroker.CredentialContext = throw UnsupportedOperationException("Git credential helpers are not available in browser builds.")
}
