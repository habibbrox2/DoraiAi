package com.dorai.ai.github

object GitHubAppConfig {
    const val USER_AGENT_PRODUCT = "DORAI-AI"
    const val API_VERSION = "2026-03-10"

    /**
     * Client ID of the GitHub (Device Flow) OAuth app for this build.
     *
     * Deliberately `null` in the source tree — the registered client ID is supplied at build
     * time, never checked in (Device Flow still requests no client secret, but an unchecked-in
     * value keeps the app free to reconfigure without pushing to the repo).
     *
     * Fail-closed: while this is null, the integration performs no network I/O at all and
     * [GitHubDeviceFlow.isConfigured] is false; connect() surfaces
     * [GitHubDeviceFlowError.MissingClientId] immediately instead of a confusing network
     * failure, and a "Connected" state can never be entered.
     */
    val clientId: String? = null
}
