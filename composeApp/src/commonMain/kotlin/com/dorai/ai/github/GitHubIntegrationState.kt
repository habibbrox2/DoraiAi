package com.dorai.ai.github

/**
 * Connection status for the GitHub integration. Mirrors the Device Flow lifecycle:
 * [NotConnected] → [WaitingForDevice] (user shown the user_code) → [Connected] (token stored)
 * → [Refreshing] (background token refresh) → [Error] (auth/rate-limit failure).
 *
 * [Unsupported] is set on platforms without secure token storage (WasmJs no-op vault).
 */
enum class GitHubConnectionStatus {
    Unsupported,
    NotConnected,
    WaitingForDevice,
    Connected,
    Refreshing,
    Error,
}

/**
 * UI domain state for the GitHub integration settings screen.
 *
 * [writeOperationsEnabled] defaults to `false` — write subcommands (`commit`, `push`, `add`,
 * `restore`) are absent from [GitCommandPolicy.allowedGitSubcommands] and no write tool is
 * callable until an explicit maintainer opt-in flips this flag.
 */
data class GitHubIntegrationState(
    val connectionStatus: GitHubConnectionStatus = GitHubConnectionStatus.NotConnected,
    val user: GitHubUser? = null,
    val installations: List<GitHubInstallation> = emptyList(),
    val repositories: List<GitHubRepository> = emptyList(),
    val selectedFullName: String? = null,
    val isLoading: Boolean = false,
    val error: GitHubDeviceFlowError? = null,
    val writeOperationsEnabled: Boolean = false,
)
