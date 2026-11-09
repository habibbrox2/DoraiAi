package com.dorai.ai.github

import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubIntegrationViewModelTest {

    /** Vault that records store/clear calls and reports connection state. */
    private class FakeVault : GitHubTokenVault {
        var accessToken: String? = null
        var refreshToken: String? = null
        var login: String? = null
        var cleared = false
        var storeCalls = 0

        override fun readAccessToken(): String? = accessToken
        override fun readRefreshToken(): String? = refreshToken
        override fun readLogin(): String? = login
        override fun store(
            accessToken: String,
            refreshToken: String?,
            expiresAtEpochMs: Long?,
            refreshExpiresAtEpochMs: Long?,
            login: String,
            installationIds: List<Long>,
        ) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
            this.login = login
            storeCalls++
        }
        override fun clear() {
            accessToken = null
            refreshToken = null
            login = null
            cleared = true
        }
        override fun isConnected(): Boolean = accessToken != null && login != null
    }

    /** Device flow that returns canned states. */
    private class FakeDeviceFlow : GitHubDeviceFlow(clientId = "test-client-id") {
        var requestDeviceCodeResult: Result<GitHubDeviceFlowState.AwaitingUser> =
            Result.success(
                GitHubDeviceFlowState.AwaitingUser(
                    userCode = "ABCD-EFGH",
                    verificationUri = "https://github.com/login/device",
                    expiresIn = 900,
                    interval = 5,
                ),
            )
        var pollForTokenResult: GitHubDeviceFlowState =
            GitHubDeviceFlowState.Success(login = "dorai-user")

        override suspend fun requestDeviceCode(): Result<GitHubDeviceFlowState.AwaitingUser> = requestDeviceCodeResult

        override suspend fun pollForToken(
            deviceCode: String,
            intervalSeconds: Long,
            expiresAtEpochMs: Long,
        ): GitHubDeviceFlowState = pollForTokenResult
    }

    /** API client that returns canned results. */
    private class FakeApiClient : GitHubApiClient(vault = FakeVault()) {
        var listInstallationsResult: GitHubApiResult<List<GitHubInstallation>> =
            GitHubApiResult.Success(
                listOf(
                    GitHubInstallation(
                        id = 1,
                        account = GitHubInstallationAccount(login = "org-a", id = 10, type = "Organization"),
                    ),
                ),
            )
        var listRepositoriesResult: GitHubApiResult<List<GitHubRepository>> =
            GitHubApiResult.Success(emptyList())

        override suspend fun listInstallations(): GitHubApiResult<List<GitHubInstallation>> = listInstallationsResult

        override suspend fun listInstallationRepositories(
            installationId: Long,
        ): GitHubApiResult<List<GitHubRepository>> = listRepositoriesResult
    }

    private fun buildVm(
        vault: GitHubTokenVault = FakeVault(),
        deviceFlow: GitHubDeviceFlow = FakeDeviceFlow(),
        apiClient: GitHubApiClient = FakeApiClient(),
    ): GitHubIntegrationViewModel = GitHubIntegrationViewModel(vault, deviceFlow, apiClient, Dispatchers.Unconfined)

    @Test
    fun `connect starts device flow and advances past NotConnected`() {
        val vault = FakeVault()
        val deviceFlow = FakeDeviceFlow()
        val vm = buildVm(vault, deviceFlow)

        assertEquals(GitHubConnectionStatus.NotConnected, vm.state.value.connectionStatus)
        vm.connect()

        assertNotEquals(GitHubConnectionStatus.NotConnected, vm.state.value.connectionStatus)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `connect stores tokens and sets Connected when poll succeeds`() {
        val vault = FakeVault()
        val deviceFlow = FakeDeviceFlow()
        deviceFlow.pollForTokenResult = GitHubDeviceFlowState.Success(
            login = "dorai-user",
            accessToken = "ghs_token_123",
            refreshToken = "ghr_refresh_456",
        )
        val vm = buildVm(vault, deviceFlow)

        vm.connect()

        assertEquals(GitHubConnectionStatus.Connected, vm.state.value.connectionStatus)
        assertNull(vm.state.value.error)
        assertEquals("ghs_token_123", vault.accessToken)
        assertEquals("ghr_refresh_456", vault.refreshToken)
        assertEquals("dorai-user", vault.login)
        assertEquals(1, vault.storeCalls)
    }

    @Test
    fun `connect sets Error when device code request fails`() {
        val vault = FakeVault()
        val deviceFlow = FakeDeviceFlow()
        deviceFlow.requestDeviceCodeResult =
            Result.failure(GitHubDeviceFlowException(GitHubDeviceFlowError.MissingClientId))
        val vm = buildVm(vault, deviceFlow)

        vm.connect()

        assertEquals(GitHubConnectionStatus.Error, vm.state.value.connectionStatus)
        assertIs<GitHubDeviceFlowError.MissingClientId>(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `disconnect clears vault and resets state`() {
        val vault = FakeVault()
        vault.accessToken = "stored-token"
        vault.login = "stored-user"
        val vm = buildVm(vault = vault)

        vm.disconnect()

        assertTrue(vault.cleared)
        assertEquals(GitHubConnectionStatus.NotConnected, vm.state.value.connectionStatus)
        assertNull(vm.state.value.selectedFullName)
        assertTrue(vm.state.value.repositories.isEmpty())
        assertTrue(vm.state.value.installations.isEmpty())
    }

    @Test
    fun `selectRepository stores full name`() {
        val vm = buildVm()
        vm.selectRepository("owner/repo")
        assertEquals("owner/repo", vm.state.value.selectedFullName)
    }

    @Test
    fun `refreshRepositories returns empty when no installations`() {
        val vm = buildVm()
        vm.refreshRepositories()
        assertTrue(vm.state.value.repositories.isEmpty())
    }

    @Test
    fun `retry restarts connect from error state`() {
        val deviceFlow = FakeDeviceFlow()
        deviceFlow.requestDeviceCodeResult =
            Result.failure(GitHubDeviceFlowException(GitHubDeviceFlowError.MissingClientId))
        val vm = buildVm(deviceFlow = deviceFlow)

        vm.connect()
        assertEquals(GitHubConnectionStatus.Error, vm.state.value.connectionStatus)

        deviceFlow.requestDeviceCodeResult = Result.success(
            GitHubDeviceFlowState.AwaitingUser(
                userCode = "NEW-CODE",
                verificationUri = "https://github.com/login/device",
                expiresIn = 900,
                interval = 5,
            ),
        )
        vm.retry()
        assertNotEquals(GitHubConnectionStatus.NotConnected, vm.state.value.connectionStatus)
        assertNull(vm.state.value.error)
    }
}
