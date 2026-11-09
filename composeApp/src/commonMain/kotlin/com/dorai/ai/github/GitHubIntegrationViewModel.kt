package com.dorai.ai.github

import androidx.lifecycle.ViewModel
import com.dorai.ai.httpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
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
    private val dispatcher: CoroutineContext = Dispatchers.Main,
) : ViewModel() {

    private val _state = MutableStateFlow(GitHubIntegrationState())
    val state: StateFlow<GitHubIntegrationState> = _state

    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + dispatcher)

    /** Background polling job; cancelled on disconnect or when a new connect starts. */
    private var pollingJob: Job? = null

    init {
        if (vault.isConnected()) {
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
        _state.value = _state.value.copy(
            connectionStatus = GitHubConnectionStatus.NotConnected,
            error = null,
            isLoading = true,
        )
        scope.launch {
            val result = deviceFlow.requestDeviceCode()
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    connectionStatus = GitHubConnectionStatus.Error,
                    error = result.exceptionOrNull()?.let { (it as? GitHubDeviceFlowException)?.error ?: GitHubDeviceFlowError.Network(it.message ?: "request failed") },
                    isLoading = false,
                )
                return@launch
            }
            val awaiting = result.getOrThrow()
            _state.value = _state.value.copy(
                connectionStatus = GitHubConnectionStatus.WaitingForDevice,
                isLoading = false,
            )
            pollDeviceCode(awaiting.userCode, awaiting.verificationUri, awaiting.expiresIn, awaiting.interval)
        }
    }

    /**
     * Polls GitHub for the device token until success, expiry, or terminal error.
     * Sleeps `interval` between polls.
     */
    private fun pollDeviceCode(
        userCode: String,
        verificationUri: String,
        expiresIn: Long,
        initialInterval: Long,
    ) {
        val deadlineMs = System.currentTimeMillis() + expiresIn * 1000
        pollingJob = scope.launch {
            var interval = initialInterval
            while (coroutineContext.isActive) {
                if (System.currentTimeMillis() >= deadlineMs) {
                    _state.value = _state.value.copy(
                        connectionStatus = GitHubConnectionStatus.Error,
                        error = GitHubDeviceFlowError.ExpiredToken,
                    )
                    return@launch
                }
                val state = deviceFlow.pollForToken(userCode, interval, deadlineMs)
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
                            vault.store(
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                expiresAtEpochMs = null,
                                refreshExpiresAtEpochMs = null,
                                login = login,
                                installationIds = emptyList(),
                            )
                        }
                        _state.value = _state.value.copy(
                            connectionStatus = GitHubConnectionStatus.Connected,
                            error = null,
                        )
                        scope.launch { refreshInstallations() }
                        return@launch
                    }

                    is GitHubDeviceFlowState.Error -> {
                        _state.value = _state.value.copy(
                            connectionStatus = GitHubConnectionStatus.Error,
                            error = state.error,
                        )
                        return@launch
                    }

                    else -> { /* Idle / AwaitingUser — not expected from pollForToken */ }
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

    /** Refreshes the list of GitHub installations for the connected account. */
    fun refreshInstallations() {
        if (_state.value.connectionStatus != GitHubConnectionStatus.Connected) return
        _state.value = _state.value.copy(isLoading = true, error = null)
        scope.launch {
            val result = apiClient.listInstallations()
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
                    connectionStatus = GitHubConnectionStatus.Error,
                    error = GitHubDeviceFlowError.Network("rate limited"),
                    isLoading = false,
                )

                else -> _state.value.copy(
                    connectionStatus = GitHubConnectionStatus.Error,
                    error = GitHubDeviceFlowError.Network("failed to list installations"),
                    isLoading = false,
                )
            }
        }
    }

    /** Selects an installation repository by full name (`owner/repo`). */
    fun selectRepository(fullName: String) {
        _state.value = _state.value.copy(selectedFullName = fullName)
    }

    /** Refreshes repositories for the selected installation. */
    fun refreshRepositories() {
        val installationId = _state.value.installations.firstOrNull()?.id
            ?: return
        _state.value = _state.value.copy(isLoading = true, error = null)
        scope.launch {
            val result = apiClient.listInstallationRepositories(installationId)
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
                    connectionStatus = GitHubConnectionStatus.Error,
                    error = GitHubDeviceFlowError.Network("rate limited"),
                    isLoading = false,
                )

                else -> _state.value.copy(
                    connectionStatus = GitHubConnectionStatus.Error,
                    error = GitHubDeviceFlowError.Network("failed to list repositories"),
                    isLoading = false,
                )
            }
        }
    }

    /** Retries from an error state by restarting the Device Flow. */
    fun retry() {
        if (_state.value.connectionStatus == GitHubConnectionStatus.Connected) return
        connect()
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
