package com.dorai.ai.github

object GitHubAppConfig {
    const val USER_AGENT_PRODUCT = "DORAI-AI"
    const val API_VERSION = "2026-03-10"

    /**
     * Client ID of the GitHub (Device Flow) OAuth app for this build.
     *
     * Deliberately never checked in — the value is injected at build time (Gradle property
     * `dorai.github.clientId`, or the `DORAI_GITHUB_CLIENT_ID` environment variable that CI
     * fills from a repository secret) into the generated [GitHubBuildConfig] constant. When no
     * value is supplied the generated constant is null and the source tree stays client-id free.
     *
     * Fail-closed: while this is null, the integration performs no network I/O at all and
     * [GitHubDeviceFlow.isConfigured] is false; connect() surfaces
     * [GitHubDeviceFlowError.MissingClientId] immediately instead of a confusing network
     * failure, and a "Connected" state can never be entered.
     */
    val clientId: String? = GitHubBuildConfig.clientId
}
