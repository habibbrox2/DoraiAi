package com.dorai.ai.github

import androidx.lifecycle.ViewModel
import com.dorai.ai.AuthorizationDestination
import com.dorai.ai.NoOpWorkspaceController
import com.dorai.ai.WorkspaceController
import com.dorai.ai.httpClient
import com.dorai.ai.sendAuthorizationNotification
import com.dorai.ai.tools.PermissionController
import com.dorai.ai.workspace.WorkspaceGitError
import com.dorai.ai.workspace.WorkspaceGitResult
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.notification_github_connected_body
import dorai.composeapp.generated.resources.notification_github_connected_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for the GitHub integration settings screen. Owns the Device Flow lifecycle
 * (request device code → poll for token → store → connected) and read-only repository
 * browsing (installations → repositories).
 *
 * Tokens are handled exclusively by [GitHubTokenVault]; this ViewModel never touches raw
 * token values and never passes them to tools, logs, or the agent timeline.
 *
 * [dispatcher] defaults to [Dispatchers.Main] in production; pass a test dispatcher (e.g.
 * [Dispatchers.Unconfined]) in tests so coroutines run synchronously.
 */
class GitHubIntegrationViewModel(
    private val vault: GitHubTokenVault,
    private val deviceFlow: GitHubDeviceFlow,
    private val apiClient: GitHubApiClient = GitHubApiClient(vault, httpClient()),
    private val workspaceController: WorkspaceController = NoOpWorkspaceController(),
    private val dispatcher: CoroutineContext = Dispatchers.Main,
    /**
     * Asked for the notification permission before the browser is opened, so the
     * "authorization completed" notice can actually be posted (Android 13+ gates it).
     * Null when the platform has no notification surface to offer.
     */
    private val notificationPermissionController: PermissionController? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(GitHubIntegrationState())
    val state: StateFlow<GitHubIntegrationState> = _state

    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + dispatcher)

    /** Background polling job; cancelled on disconnect or when a new connect starts. */
    private var pollingJob: Job? = null

    init {
        // A platform vault can fail during first access (for example, Android Keystore
        // state can be invalidated after backup/restore). Never let that failure escape
        // ViewModel construction: opening Settings must remain safe and recoverable.
        val connected = runCatching { vault.isConnected() }.getOrDefault(false)
        if (connected) {
            _state.value = _state.value.copy(connectionStatus = GitHubConnectionStatus.Connected)
            scope.launch { refreshInstallations() }
        }
    }

    /**
     * Starts the Device Flow: requests a device code, shows the user code + verification URI,
     * then begins polling for the token. No-op when already connected.
     */
    fun connect() {
        if (_state.value.connectionStatus == GitHubConnectionStatus.Connected) return
        stopPolling()
        // Fail closed: a build without a client ID must never reach the network or show a
        // "connecting" state — surface the configuration error immediately.
        if (!deviceFlow.isConfigured) {
            _state.value = _state.value.copy(
                connectionStatus = GitHubConnectionStatus.Error,
                error = GitHubDeviceFlowError.MissingClientId,
                isLoading = false,
                userCode = null,
                verificationUri = null,
            )
            return
        }
        _state.value = _state.value.copy(
            connectionStatus = GitHubConnectionStatus.NotConnected,
            error = null,
            isLoading = true,
        )
        scope.launch {
            // The consent happens in a browser, so the app will be in the background when it
            // completes. Ask for the notification permission now, while the user is still
            // looking at the app.
            runCatching { notificationPermissionController?.requestPermission() }
            val result = deviceFlow.requestDeviceCode()
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    connectionStatus = GitHubConnectionStatus.Error,
                    error = result.exceptionOrNull()?.let { (it as? GitHubDeviceFlowException)?.error ?: GitHubDeviceFlowError.Network(it.message ?: "request failed") },
                    isLoading = false,
                    userCode = null,
                    verificationUri = null,
                )
                return@launch
            }
            val awaiting = result.getOrThrow()
            _state.value = _state.value.copy(
                connectionStatus = GitHubConnectionStatus.WaitingForDevice,
                isLoading = false,
                userCode = awaiting.userCode,
                verificationUri = awaiting.verificationUri,
            )
            pollDeviceCode(awaiting.deviceCode, awaiting.verificationUri, awaiting.expiresIn, awaiting.interval)
        }
    }

    /**
     * Polls GitHub for the device token until success, expiry, or terminal error.
     * Sleeps `interval` between polls.
     *
     * [deviceCode] is the OAuth device code returned by [requestDeviceCode]; the user-facing
     * user code is never polled (GitHub would answer `incorrect_device_code` forever).
     */
    private fun pollDeviceCode(
        deviceCode: String,
        verificationUri: String,
        expiresIn: Long,
        initialInterval: Long,
    ) {
        val deadlineMs = Clock.System.now().toEpochMilliseconds() + expiresIn * 1000
        pollingJob = scope.launch {
            var interval = initialInterval
            while (coroutineContext.isActive) {
                if (Clock.System.now().toEpochMilliseconds() >= deadlineMs) {
                    _state.value = _state.value.copy(
                        connectionStatus = GitHubConnectionStatus.Error,
                        error = GitHubDeviceFlowError.ExpiredToken,
                        userCode = null,
                        verificationUri = null,
                    )
                    return@launch
                }
                val state = deviceFlow.pollForToken(deviceCode, interval, deadlineMs)
                when (state) {
                    is GitHubDeviceFlowState.Polling -> {
                        interval = state.interval.coerceAtLeast(5)
                        kotlinx.coroutines.delay(interval.seconds)
                    }

                    is GitHubDeviceFlowState.Success -> {
                        // Persist tokens in the vault, mark connected, then refresh installations.
                        val accessToken = state.accessToken
                        val refreshToken = state.refreshToken
                        val login = state.login
                        if (accessToken != null) {
                            // A vault write that throws (Keystore invalidation after backup/restore,
                            // disk pressure, tamper) must not crash this coroutine nor mark the app
                            // connected while nothing was persisted.
                            try {
                                vault.store(
                                    accessToken = accessToken,
                                    refreshToken = refreshToken,
                                    expiresAtEpochMs = null,
                                    refreshExpiresAtEpochMs = null,
                                    login = login,
                                    installationIds = emptyList(),
                                )
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                println("GitHubIntegrationViewModel: vault store failed (${e.message?.take(120)})")
                                _state.value = _state.value.copy(
                                    connectionStatus = GitHubConnectionStatus.Error,
                                    error = GitHubDeviceFlowError.Network("token could not be saved"),
                                    isLoading = false,
                                    userCode = null,
                                    verificationUri = null,
                                )
                                return@launch
                            }
                            // Only mark Connected when the vault actually holds a readable token —
                            // a platform vault that silently no-ops must surface an error, not a
                            // fake Connected state.
                            val persisted = runCatching { vault.isConnected() }.getOrDefault(false)
                            if (!persisted) {
                                _state.value = _state.value.copy(
                                    connectionStatus = GitHubConnectionStatus.Error,
                                    error = GitHubDeviceFlowError.Network("token could not be saved"),
                                    isLoading = false,
                                    userCode = null,
                                    verificationUri = null,
                                )
                                return@launch
                            }
                        }
                        _state.value = _state.value.copy(
                            connectionStatus = GitHubConnectionStatus.Connected,
                            error = null,
                            userCode = null,
                            verificationUri = null,
                        )
                        // The consent happened in the browser, so the app is in the background.
                        // Tell the user it worked and that they can come back.
                        scope.launch {
                            runCatching {
                                val title = getString(Res.string.notification_github_connected_title)
                                val body = getString(Res.string.notification_github_connected_body)
                                sendAuthorizationNotification(title, body, AuthorizationDestination.GitHub)
                            }
                        }
                        scope.launch {
                            // Best effort: load the authenticated user's profile for display.
                            // A failure here must not break the connection — the token works.
                            runCatching { apiClient.getAuthenticatedUser() }
                                .getOrNull()
                                ?.let { userResult ->
                                    if (userResult is GitHubApiResult.Success) {
                                        _state.value = _state.value.copy(user = userResult.data)
                                    }
                                }
                            refreshInstallations()
                        }
                        return@launch
                    }

                    is GitHubDeviceFlowState.Error -> {
                        _state.value = _state.value.copy(
                            connectionStatus = GitHubConnectionStatus.Error,
                            error = state.error,
                            userCode = null,
                            verificationUri = null,
                        )
                        return@launch
                    }

                    else -> {
                        // Idle / AwaitingUser — not expected from pollForToken. Avoid a busy
                        // loop by backing off briefly before the next poll attempt.
                        kotlinx.coroutines.delay(1.seconds)
                    }
                }
            }
        }
    }

    /** Clears the vault and resets all UI state. */
    fun disconnect() {
        stopPolling()
        vault.clear()
        _state.value = GitHubIntegrationState(
            connectionStatus = GitHubConnectionStatus.NotConnected,
        )
    }

    /** Cancels the device flow (user pressed Cancel) and returns to a clean NotConnected state. */
    fun cancelDeviceFlow() {
        stopPolling()
        _state.value = GitHubIntegrationState(
            connectionStatus = GitHubConnectionStatus.NotConnected,
        )
    }

    /**
     * Refreshes the list of GitHub installations for the connected account.
     * Auth failures destroy the session (the token is dead); rate limits and transient
     * network/serialization failures keep it — a valid session must not flip to Error
     * because of a recoverable condition.
     */
    fun refreshInstallations() {
        // The vault is the source of truth for "do we have a token" — the UI status may
        // lag behind (e.g. a previous failure), and refresh must work from Error too.
        val hasToken = runCatching { vault.isConnected() }.getOrDefault(false)
        if (!hasToken) return
        _state.value = _state.value.copy(isLoading = true, error = null)
        scope.launch {
            // CancellationException must propagate to the scope — a cancellation is not a
            // network failure and must not surface as an Error state.
            val result = runCatching { apiClient.listInstallations() }
                .getOrElse { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    GitHubApiResult.Failed
                }
            _state.value = when (result) {
                is GitHubApiResult.Success -> _state.value.copy(
                    installations = result.data,
                    isLoading = false,
                )

                GitHubApiResult.Unauthorized -> _state.value.copy(
                    connectionStatus = GitHubConnectionStatus.Error,
                    error = GitHubDeviceFlowError.Network("unauthorized"),
                    isLoading = false,
                )

                is GitHubApiResult.RateLimited -> _state.value.copy(
                    // Recoverable and non-fatal — surface a dismissible banner, keep Connected.
                    error = GitHubDeviceFlowError.Network("rate limited"),
                    isLoading = false,
                )

                else -> _state.value.copy(
                    // Network/serialization failure: keep the session too, be honest about it.
                    error = GitHubDeviceFlowError.Network("failed to list installations"),
                    isLoading = false,
                )
            }
            // No separate UI trigger exists; the repositories list must load as soon as the
            // installations are known or it stays empty forever.
            if (result is GitHubApiResult.Success) {
                refreshRepositories()
            }
        }
    }

    /** Selects an installation repository by full name (`owner/repo`). */
    fun selectRepository(fullName: String) {
        _state.value = _state.value.copy(selectedFullName = fullName)
    }

    /** Refreshes repositories for the selected installation (falls back to installations refresh). */
    fun refreshRepositories() {
        val installationId = _state.value.installations.firstOrNull()?.id
            ?: run {
                // Nothing known yet — load installations first (which chains a repository refresh).
                refreshInstallations()
                return
            }
        _state.value = _state.value.copy(isLoading = true, error = null)
        scope.launch {
            val result = runCatching { apiClient.listInstallationRepositories(installationId) }
                .getOrElse { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    GitHubApiResult.Failed
                }
            _state.value = when (result) {
                is GitHubApiResult.Success -> _state.value.copy(
                    repositories = result.data,
                    isLoading = false,
                )

                GitHubApiResult.Unauthorized -> _state.value.copy(
                    connectionStatus = GitHubConnectionStatus.Error,
                    error = GitHubDeviceFlowError.Network("unauthorized"),
                    isLoading = false,
                )

                is GitHubApiResult.RateLimited -> _state.value.copy(
                    // Recoverable and non-fatal — surface a dismissible banner, keep Connected.
                    error = GitHubDeviceFlowError.Network("rate limited"),
                    isLoading = false,
                )

                else -> _state.value.copy(
                    error = GitHubDeviceFlowError.Network("failed to list repositories"),
                    isLoading = false,
                )
            }
        }
    }

    /**
     * Selects [repository] and clones it into `projects/<projectName>` (depth=1, read-only).
     *
     * The project name is validated with [GitUrlValidator.isValidProjectName] before any guest
     * call. On success the repository is recorded as selected, [cloneState] becomes
     * [CloneState.Success], and [lastSync] is stamped. On failure [cloneState] becomes
     * [CloneState.Failure] and [lastError] holds the message.
     */
    fun selectAndClone(repository: GitHubRepository, projectName: String) {
        if (!GitUrlValidator.isValidProjectName(projectName)) {
            val message = "Invalid project name: $projectName"
            _state.value = _state.value.copy(
                selectedRepository = null,
                cloneState = CloneState.Failure(message),
                lastError = message,
            )
            return
        }
        if (_state.value.connectionStatus != GitHubConnectionStatus.Connected) {
            val message = "Not connected to GitHub"
            _state.value = _state.value.copy(
                selectedRepository = null,
                cloneState = CloneState.Failure(message),
                lastError = message,
            )
            return
        }
        val cloneUrl = repository.htmlUrl.takeIf { it.endsWith(".git") } ?: repository.htmlUrl + ".git"
        _state.value = _state.value.copy(
            selectedRepository = repository,
            cloneState = CloneState.Cloning(projectName, cloneUrl),
            lastError = null,
        )
        scope.launch {
            val result: WorkspaceGitResult = workspaceController.gitClone(projectName, cloneUrl)
            _state.value = when (result.error) {
                null -> _state.value.copy(
                    cloneState = CloneState.Success(projectName),
                    lastSync = Clock.System.now(),
                )

                else -> _state.value.copy(
                    selectedRepository = null,
                    cloneState = CloneState.Failure(result.error.toString()),
                    lastError = result.error.toString(),
                )
            }
        }
    }

    /** Refreshes `git status` for the selected project, recording any failure in [lastError]. */
    fun refreshGitStatus() {
        val project = projectNameForSelected()
        if (project.isNullOrBlank()) return
        _state.value = _state.value.copy(lastError = null)
        scope.launch {
            val result: WorkspaceGitResult = workspaceController.gitStatus(project)
            if (result.error != null) {
                _state.value = _state.value.copy(lastError = result.error.toString())
            }
        }
    }

    /**
     * Syncs the selected project: `git fetch --quiet --prune` then `git pull --ff-only --quiet`.
     * Stamps [lastSync] on success, records [lastError] on failure.
     */
    fun sync() {
        val project = projectNameForSelected()
        if (project.isNullOrBlank()) return
        _state.value = _state.value.copy(lastError = null)
        scope.launch {
            val fetch = workspaceController.gitFetch(project)
            if (fetch.error != null) {
                _state.value = _state.value.copy(lastError = fetch.error.toString())
                return@launch
            }
            val pull = workspaceController.gitPullFfOnly(project)
            _state.value = when (pull.error) {
                null -> _state.value.copy(lastSync = Clock.System.now())
                else -> _state.value.copy(lastError = pull.error.toString())
            }
        }
    }

    private fun projectNameForSelected(): String? = _state.value.selectedRepository?.name?.takeIf { it.isNotBlank() }

    /** Retries from an error state by restarting the Device Flow. */
    fun retry() {
        if (_state.value.connectionStatus == GitHubConnectionStatus.Connected) return
        connect()
    }

    /** Clears a dismissible non-fatal error banner (e.g. a rate limit on a background refresh). */
    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Flips [GitHubIntegrationState.writeOperationsEnabled]. Off by default; the opt-in is
     * maintainer-controlled and session-scoped (in-memory) — never persisted.
     */
    fun setWriteOperationsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(writeOperationsEnabled = enabled)
    }

    /**
     * Stages the explicit relative [files] and commits with [message] (single line, 1..200 chars).
     * Refused — nothing is staged, nothing is committed — unless write operations were enabled
     * via [setWriteOperationsEnabled] first.
     */
    fun gitCommit(files: List<String>, message: String) {
        if (!_state.value.writeOperationsEnabled) {
            _state.value = _state.value.copy(lastError = "Write operations are disabled")
            return
        }
        val project = projectNameForSelected()
        val trimmed = message.trim()
        if (project.isNullOrBlank()) return
        if (files.isEmpty() || trimmed.isEmpty() || trimmed.length > 200 ||
            trimmed.any { it == '\n' || it == '\r' || it == '\u0000' }
        ) {
            _state.value = _state.value.copy(lastError = "Invalid commit message or empty file list")
            return
        }
        _state.value = _state.value.copy(lastError = null)
        scope.launch {
            val result: WorkspaceGitResult = workspaceController.gitCommit(project, files, trimmed)
            if (result.error != null) {
                _state.value = _state.value.copy(lastError = result.error.toString())
            }
        }
    }

    /**
     * Creates a pull request on the selected repository (`POST /repos/{owner}/{repo}/pulls`).
     *
     * Refused unless write operations were enabled via [setWriteOperationsEnabled] first.
     * Also refused — no HTTP call is made — when the installation that backs this session
     * has no `pull_requests: write` permission; the user is told to grant it in the GitHub
     * App settings instead.
     */
    fun createPullRequest(title: String, head: String, base: String, body: String = "") {
        if (!_state.value.writeOperationsEnabled) {
            _state.value = _state.value.copy(lastError = "Write operations are disabled")
            return
        }
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty() || trimmedTitle.length > 256) {
            _state.value = _state.value.copy(lastError = "Pull request title must be 1..256 characters")
            return
        }
        if (head.isBlank() || base.isBlank()) {
            _state.value = _state.value.copy(lastError = "Head and base branches are required")
            return
        }
        val repo = _state.value.selectedRepository
        if (repo == null) {
            _state.value = _state.value.copy(lastError = "Select a repository first")
            return
        }

        // The repository list comes from the first installation (see refreshRepositories).
        val canWrite = _state.value.installations.firstOrNull()?.permissions?.get("pull_requests") == "write"
        if (!canWrite) {
            _state.value = _state.value.copy(
                lastError = "Grant write permission in GitHub App settings (Pull requests: Write), then try again",
            )
            return
        }

        val owner = repo.fullName.substringBefore('/')
        val name = repo.fullName.substringAfter('/', missingDelimiterValue = "")
        if (owner.isBlank() || name.isBlank()) {
            _state.value = _state.value.copy(lastError = "Repository full name is malformed")
            return
        }

        _state.value = _state.value.copy(lastError = null)
        scope.launch {
            val result = apiClient.createPullRequest(
                owner = owner,
                repo = name,
                title = trimmedTitle,
                head = head.trim(),
                base = base.trim(),
                body = body.trim(),
                writeEnabled = true,
            )
            _state.value = when (result) {
                is GitHubApiResult.Success -> _state.value.copy(lastPullRequestUrl = result.data.htmlUrl, lastError = null)

                GitHubApiResult.Forbidden -> _state.value.copy(
                    lastError = "Grant write permission in GitHub App settings (Pull requests: Write)",
                )

                GitHubApiResult.Unauthorized -> _state.value.copy(
                    connectionStatus = GitHubConnectionStatus.Error,
                    error = GitHubDeviceFlowError.Network("unauthorized"),
                )

                else -> _state.value.copy(lastError = "Failed to create pull request")
            }
        }
    }

    /**
     * Pushes the current branch as `git push origin HEAD`. Refused — no process is spawned —
     * unless write operations were enabled via [setWriteOperationsEnabled] first.
     */
    fun gitPush() {
        if (!_state.value.writeOperationsEnabled) {
            _state.value = _state.value.copy(lastError = "Write operations are disabled")
            return
        }
        val project = projectNameForSelected()
        if (project.isNullOrBlank()) return
        _state.value = _state.value.copy(lastError = null)
        scope.launch {
            val result: WorkspaceGitResult = workspaceController.gitPush(project)
            _state.value = when (result.error) {
                null -> _state.value.copy(lastSync = Clock.System.now())
                else -> _state.value.copy(lastError = result.error.toString())
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
        scopeJob.cancel()
    }
}
