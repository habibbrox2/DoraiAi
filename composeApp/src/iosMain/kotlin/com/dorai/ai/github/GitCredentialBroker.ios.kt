package com.dorai.ai.github

/** iOS does not spawn local Git processes, so no credential helper is available. */
actual fun createGitCredentialBroker(): GitCredentialBroker = UnsupportedIosGitCredentialBroker

private object UnsupportedIosGitCredentialBroker : GitCredentialBroker {
    override fun credentialGitConfigArgs(command: List<String>): GitCredentialBroker.CredentialContext = throw UnsupportedOperationException("Git credential brokering is not available on iOS.")
}
