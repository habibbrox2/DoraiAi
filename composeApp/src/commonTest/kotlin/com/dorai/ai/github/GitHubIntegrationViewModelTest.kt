package com.dorai.ai.github

import com.dorai.ai.NoOpWorkspaceController
import com.dorai.ai.WorkspaceController
import com.dorai.ai.workspace.WorkspaceGitError
import com.dorai.ai.workspace.WorkspaceGitResult
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
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

    /** Workspace controller that records git calls and returns canned results. */
    private class FakeWorkspaceController : WorkspaceController by NoOpWorkspaceController() {
        val gitCloneCalls = mutableListOf<Pair<String, String>>()
        val gitStatusCalls = mutableListOf<String>()
        val gitFetchCalls = mutableListOf<String>()
        val gitPullCalls = mutableListOf<String>()
        var cloneResult: WorkspaceGitResult = WorkspaceGitResult(isRepository = true)
        var gitStatusResult: WorkspaceGitResult = WorkspaceGitResult()
        var fetchResult: WorkspaceGitResult = WorkspaceGitResult(isRepository = true)
        var pullResult: WorkspaceGitResult = WorkspaceGitResult(isRepository = true)

        override suspend fun gitClone(project: String, url: String): WorkspaceGitResult {
            gitCloneCalls += project to url
            return cloneResult
        }
        override suspend fun gitStatus(project: String): WorkspaceGitResult {
            gitStatusCalls += project
            return gitStatusResult
        }
        override suspend fun gitFetch(project: String): WorkspaceGitResult {
            gitFetchCalls += project
            return fetchResult
        }
        override suspend fun gitPullFfOnly(project: String): WorkspaceGitResult {
            gitPullCalls += project
            return pullResult
        }
    }

    private fun buildVm(
        vault: GitHubTokenVault = FakeVault(),
        deviceFlow: GitHubDeviceFlow = FakeDeviceFlow(),
        apiClient: GitHubApiClient = FakeApiClient(),
        workspaceController: WorkspaceController = FakeWorkspaceController(),
    ): GitHubIntegrationViewModel = GitHubIntegrationViewModel(vault, deviceFlow, apiClient, workspaceController, Dispatchers.Unconfined)

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

    private fun testRepo() = GitHubRepository(
        id = 1,
        name = "repo",
        fullName = "owner/repo",
        owner = GitHubRepositoryOwner(login = "owner", id = 1, type = "Organization"),
        `private` = false,
        defaultBranch = "main",
        htmlUrl = "https://github.com/owner/repo",
    )

    private fun connectedVm(
        controller: FakeWorkspaceController = FakeWorkspaceController(),
    ): Pair<GitHubIntegrationViewModel, FakeWorkspaceController> {
        val vault = FakeVault().apply {
            accessToken = "ghs_tok_123"
            login = "dorai-user"
        }
        val vm = buildVm(vault = vault, workspaceController = controller)
        return vm to controller
    }

    @Test
    fun `selectAndClone clones repository and marks success`() {
        val (vm, controller) = connectedVm()
        val repo = testRepo()

        vm.selectAndClone(repo, "myproject")

        assertEquals(repo, vm.state.value.selectedRepository)
        assertIs<CloneState.Success>(vm.state.value.cloneState)
        assertEquals("myproject", (vm.state.value.cloneState as CloneState.Success).project)
        assertNotNull(vm.state.value.lastSync)
        assertNull(vm.state.value.lastError)
        assertEquals(listOf("myproject" to "https://github.com/owner/repo.git"), controller.gitCloneCalls)
    }

    @Test
    fun `selectAndClone rejects invalid project name`() {
        val controller = FakeWorkspaceController()
        val vm = buildVm(workspaceController = controller)
        val repo = testRepo()

        vm.selectAndClone(repo, "../evil")

        assertIs<CloneState.Failure>(vm.state.value.cloneState)
        assertNotNull(vm.state.value.lastError)
        assertNull(vm.state.value.selectedRepository)
        assertTrue(controller.gitCloneCalls.isEmpty())
    }

    @Test
    fun `selectAndClone fails when not connected`() {
        val controller = FakeWorkspaceController()
        val vm = buildVm(workspaceController = controller)
        val repo = testRepo()

        vm.selectAndClone(repo, "myproject")

        assertIs<CloneState.Failure>(vm.state.value.cloneState)
        assertEquals("Not connected to GitHub", vm.state.value.lastError)
        assertNull(vm.state.value.selectedRepository)
        assertTrue(controller.gitCloneCalls.isEmpty())
    }

    @Test
    fun `clone failure rolls back selected repository and sets error`() {
        val controller = FakeWorkspaceController().apply {
            cloneResult = WorkspaceGitResult(error = WorkspaceGitError.Failed)
        }
        val (vm, _) = connectedVm(controller)
        val repo = testRepo()

        vm.selectAndClone(repo, "myproject")

        assertIs<CloneState.Failure>(vm.state.value.cloneState)
        assertNotNull(vm.state.value.lastError)
        assertNull(vm.state.value.selectedRepository)
        assertEquals(1, controller.gitCloneCalls.size)
    }

    @Test
    fun `sync stamps lastSync on fetch and pull success`() {
        val controller = FakeWorkspaceController()
        val (vm, _) = connectedVm(controller)
        val repo = testRepo()

        vm.selectAndClone(repo, "myproject")
        vm.sync()

        assertEquals(listOf("repo"), controller.gitFetchCalls)
        assertEquals(listOf("repo"), controller.gitPullCalls)
        assertNotNull(vm.state.value.lastSync)
        assertNull(vm.state.value.lastError)
    }

    @Test
    fun `refreshGitStatus records error when git status fails`() {
        val controller = FakeWorkspaceController().apply {
            gitStatusResult = WorkspaceGitResult(error = WorkspaceGitError.NotRepository)
        }
        val (vm, _) = connectedVm(controller)
        val repo = testRepo()

        vm.selectAndClone(repo, "myproject")
        vm.refreshGitStatus()

        assertEquals(listOf("repo"), controller.gitStatusCalls)
        assertEquals("NotRepository", vm.state.value.lastError)
    }
}
