package com.dorai.ai.ui.workspace

import app.cash.turbine.test
import com.dorai.ai.FileBrowserSource
import com.dorai.ai.NoOpFileBrowserSource
import com.dorai.ai.WorkspaceController
import com.dorai.ai.testutil.FakeDataRepository
import com.dorai.ai.workspace.WorkspaceEnvironmentState
import com.dorai.ai.workspace.WorkspaceGitResult
import com.dorai.ai.workspace.WorkspaceSessionStatus
import com.dorai.ai.workspace.WorkspaceState
import com.dorai.ai.workspace.WorkspaceTerminalSession
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDataRepository
    private lateinit var fakeController: FakeWorkspaceController

    private class FakeWorkspaceController : WorkspaceController {
        override val state = MutableStateFlow(
            WorkspaceState(
                environment = WorkspaceEnvironmentState.Ready,
                installedAgents = persistentSetOf("claude-code", "grok"),
            ),
        )
        override val files: FileBrowserSource = NoOpFileBrowserSource

        /** Agent ids passed to [startSession], oldest first. */
        val startedWith = mutableListOf<String?>()

        /** Project names passed to [deleteProject], oldest first. */
        val deleted = mutableListOf<String>()

        /** Old-to-new pairs passed to [renameProject], oldest first. */
        val renamed = mutableListOf<Pair<String, String>>()

        /** (project, path) pairs passed to [gitStage], oldest first. */
        val staged = mutableListOf<Pair<String, String>>()

        /** (project, path) pairs passed to [gitUnstage], oldest first. */
        val unstaged = mutableListOf<Pair<String, String>>()

        /** (project, path) pairs passed to [gitDiscard], oldest first. */
        val discarded = mutableListOf<Pair<String, String>>()

        /** Number of [gitStatus] calls (refreshes after git actions). */
        var gitStatusCalls = 0

        /** Project names passed to [createProject], oldest first. */
        val created = mutableListOf<String>()

        /** Sanitizer under test: maps the raw requested name to what the environment returns. */
        var createProjectResult: (String) -> String? = { it }

        /** Whether [close] was called. */
        var closed = false

        /** Session ids passed to [closeSession], oldest first. */
        val closedSessions = mutableListOf<String>()

        override fun install(agentIds: Set<String>) {}
        override fun cancel() {}
        override fun uninstall() {}
        override fun refresh() {}
        override suspend fun gitStatus(project: String): WorkspaceGitResult {
            gitStatusCalls += 1
            return WorkspaceGitResult()
        }
        override suspend fun gitDiff(project: String, path: String): com.dorai.ai.workspace.WorkspaceGitDiff = com.dorai.ai.workspace.WorkspaceGitDiff(path = path, unstagedDiff = "unstaged", stagedDiff = "staged")
        override suspend fun gitStage(project: String, path: String): Boolean {
            staged += project to path
            return true
        }
        override suspend fun gitUnstage(project: String, path: String): Boolean {
            unstaged += project to path
            return true
        }
        override suspend fun gitDiscard(project: String, path: String): Boolean {
            discarded += project to path
            return true
        }
        override fun createProject(name: String): String? {
            created += name
            return createProjectResult(name)
        }
        override fun deleteProject(name: String) {
            deleted += name
        }

        override fun renameProject(name: String, newName: String): String {
            renamed += name to newName
            return newName
        }

        override fun startSession(project: String, agentId: String?) {
            startedWith += agentId
        }

        override fun selectSession(id: String) {}
        override fun closeSession(id: String) {
            closedSessions += id
        }
        override fun resumeProject(project: String): Boolean = false
        override fun leaveProject(project: String) {}
        override fun writeToTerminal(text: String) {}
        override fun resizeTerminal(columns: Int, rows: Int) {}
        override fun close() {
            closed = true
        }
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDataRepository()
        fakeController = FakeWorkspaceController()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setLaunchAgent persists the choice`() = runTest {
        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)

        viewModel.uiState.test {
            assertNull(awaitItem().launchAgentId)

            viewModel.setLaunchAgent("grok")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("grok", awaitItem().launchAgentId)
            assertEquals("grok", fakeRepository.getWorkspaceLaunchAgent())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launch agent is restored on the next app start`() = runTest {
        fakeRepository.storedWorkspaceLaunchAgent = "claude-code"

        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)

        viewModel.uiState.test {
            assertEquals("claude-code", awaitItem().launchAgentId)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.openProject("demo")
        assertEquals(listOf<String?>("claude-code"), fakeController.startedWith)
    }

    @Test
    fun `picking the shell again clears the stored agent`() = runTest {
        fakeRepository.storedWorkspaceLaunchAgent = "grok"

        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)
        viewModel.setLaunchAgent(null)

        assertNull(fakeRepository.getWorkspaceLaunchAgent())
        viewModel.openProject("demo")
        assertEquals(listOf<String?>(null), fakeController.startedWith)
    }

    @Test
    fun `deleting and renaming reach the environment`() = runTest {
        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)

        viewModel.deleteProject("old-demo")
        viewModel.renameProject("demo", "demo-2")

        assertEquals(listOf("old-demo"), fakeController.deleted)
        assertEquals(listOf("demo" to "demo-2"), fakeController.renamed)
    }

    @Test
    fun `a remembered agent that is no longer installed falls back to a shell`() = runTest {
        fakeRepository.storedWorkspaceLaunchAgent = "opencode"

        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)

        viewModel.uiState.test {
            assertNull(awaitItem().launchAgentId)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.openProject("demo")
        assertEquals(listOf<String?>(null), fakeController.startedWith)
    }

    @Test
    fun `loadGitDiff publishes staged and unstaged diffs and clearGitDiff clears them`() = runTest {
        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)

        viewModel.gitDiff.test {
            assertNull(awaitItem())

            viewModel.loadGitDiff("demo", "/root/projects/demo/a.txt")
            val diff = awaitItem()
            assertEquals("/root/projects/demo/a.txt", diff?.path)
            assertEquals("staged", diff?.stagedDiff)
            assertEquals("unstaged", diff?.unstagedDiff)

            viewModel.clearGitDiff()
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stageGitFile reaches the environment and refreshes the status`() = runTest {
        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)
        val before = fakeController.gitStatusCalls

        viewModel.gitActionInProgress.test {
            assertEquals(false, awaitItem())

            viewModel.stageGitFile("demo", "/root/projects/demo/a.txt")
            assertEquals(true, awaitItem())
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("demo" to "/root/projects/demo/a.txt"), fakeController.staged)
        viewModel.gitState.test {
            var refreshed = false
            while (!refreshed) {
                val state = awaitItem()
                refreshed = state.loaded && !state.loading
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(true, fakeController.gitStatusCalls > before)
    }

    @Test
    fun `unstage and discard reach the environment`() = runTest {
        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)

        viewModel.gitActionInProgress.test {
            assertEquals(false, awaitItem())

            viewModel.unstageGitFile("demo", "/root/projects/demo/b.txt")
            assertEquals(true, awaitItem())
            assertEquals(false, awaitItem())

            viewModel.discardGitFile("demo", "/root/projects/demo/c.txt")
            assertEquals(true, awaitItem())
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("demo" to "/root/projects/demo/b.txt"), fakeController.unstaged)
        assertEquals(listOf("demo" to "/root/projects/demo/c.txt"), fakeController.discarded)
    }

    @Test
    fun `createProject opens the sanitized name the environment returns without optimistic append`() = runTest {
        fakeController.createProjectResult = { "sanitized-folder" }
        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)

        viewModel.uiState.test {
            // First emission is the initial state; draining until openProject settles.
            var state = awaitItem()
            viewModel.createProject("My Demo!")
            testDispatcher.scheduler.advanceUntilIdle()
            var attempts = 0
            while (state.openProject == null && attempts < 5) {
                state = awaitItem()
                attempts += 1
            }
            assertEquals("sanitized-folder", state.openProject)
            // The projects list is unchanged until the async rescan lands — no optimistic append.
            assertEquals(true, state.build.projects.none { it == "My Demo!" || it == "sanitized-folder" })
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("My Demo!"), fakeController.created)
    }

    @Test
    fun `restartSession starts the replacement before closing the dead session`() = runTest {
        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)
        viewModel.openProject("demo")
        fakeController.state.value = WorkspaceState(
            environment = WorkspaceEnvironmentState.Ready,
            installedAgents = persistentSetOf("grok"),
            sessions = kotlinx.collections.immutable.persistentListOf(
                WorkspaceTerminalSession(
                    id = "s1",
                    project = "demo",
                    agentId = "grok",
                    number = 1,
                    status = WorkspaceSessionStatus.Failed,
                    exitCode = 1,
                ),
            ),
            activeSessionId = "s1",
        )

        viewModel.restartSession("s1")

        // openProject already started one shell (null agent); restart adds the
        // replacement for the dead grok session and then closes the dead tab.
        assertEquals(listOf<String?>(null, "grok"), fakeController.startedWith)
        assertEquals(listOf("s1"), fakeController.closedSessions)
    }

    @Test
    fun `closeEnvironment delegates to the controller`() = runTest {
        val viewModel = WorkspaceViewModel(fakeController, fakeRepository)

        viewModel.closeEnvironment()

        assertEquals(true, fakeController.closed)
    }
}
