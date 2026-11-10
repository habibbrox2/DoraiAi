package com.dorai.ai.github

import kotlin.time.Instant

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
 * State of a repository clone operation triggered from the GitHub settings section.
 */
sealed class CloneState {
    object Idle : CloneState()
    data class Cloning(val project: String, val url: String) : CloneState()
    data class Success(val project: String) : CloneState()
    data class Failure(val error: String) : CloneState()
}

/**
 * UI domain state for the GitHub integration settings screen.
 *
 * [writeOperationsEnabled] defaults to `false` — write subcommands (`commit`, `push`, `add`,
 * `restore`) are absent from [GitCommandPolicy.allowedGitSubcommands] and no write tool is
 * callable until an explicit maintainer opt-in flips this flag.
 *
 * [selectedRepository] is the repository the user has chosen to clone.
 * [cloneState] tracks the in-flight / completed / failed clone operation.
 * [lastSync] records when the selected project was last synced (fetch + fast-forward pull).
 * [lastError] holds a human-readable message for the most recent clone / sync / status failure.
 */
data class GitHubIntegrationState(
    val connectionStatus: GitHubConnectionStatus = GitHubConnectionStatus.NotConnected,
    val user: GitHubUser? = null,
    val installations: List<GitHubInstallation> = emptyList(),
    val repositories: List<GitHubRepository> = emptyList(),
    val selectedRepository: GitHubRepository? = null,
    val selectedFullName: String? = null,
    val cloneState: CloneState = CloneState.Idle,
    val lastSync: Instant? = null,
    val lastError: String? = null,
    val isLoading: Boolean = false,
    val error: GitHubDeviceFlowError? = null,
    val writeOperationsEnabled: Boolean = false,
)
