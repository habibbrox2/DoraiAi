package com.dorai.ai.workspace

import com.dorai.ai.source.GitCommandResult
import com.dorai.ai.source.GitCommandRunner
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopWorkspaceControllerTest {

    private class MockGitRunner(
        var mockResult: GitCommandResult = GitCommandResult(true, "", "", 0),
    ) : GitCommandRunner {
        var lastArgs: List<String> = emptyList()
        var lastProject: String = ""

        override suspend fun run(
            project: String,
            args: List<String>,
            timeoutSeconds: Long,
            writeEnabled: Boolean,
        ): GitCommandResult {
            lastProject = project
            lastArgs = args
            return mockResult
        }

        override suspend fun isGitAvailable(): Boolean = true
    }

    @Test
    fun `rejects invalid project name without running git command`() = runTest {
        val mock = MockGitRunner()
        val controller = DesktopWorkspaceController(gitRunner = mock)

        val result = controller.gitStatus("../invalid")
        assertEquals(WorkspaceGitError.NotRepository, result.error)
        assertFalse(result.isRepository)
        assertEquals(emptyList(), mock.lastArgs)
    }

    @Test
    fun `rejects invalid git url on clone without running git command`() = runTest {
        val mock = MockGitRunner()
        val controller = DesktopWorkspaceController(gitRunner = mock)

        val result = controller.gitClone("valid-project", "http://insecure-url.com/repo")
        assertEquals(WorkspaceGitError.NotRepository, result.error)
        assertFalse(result.isRepository)
        assertEquals(emptyList(), mock.lastArgs)
    }

    @Test
    fun `gitFetch delegates allowed arguments to gitRunner`() = runTest {
        val mock = MockGitRunner()
        val controller = DesktopWorkspaceController(gitRunner = mock)

        controller.gitFetch("my-project")
        assertEquals("my-project", mock.lastProject)
        assertEquals(listOf("fetch", "--quiet"), mock.lastArgs)
    }

    @Test
    fun `gitPullFfOnly delegates pull arguments to gitRunner`() = runTest {
        val mock = MockGitRunner()
        val controller = DesktopWorkspaceController(gitRunner = mock)

        controller.gitPullFfOnly("my-project")
        assertEquals("my-project", mock.lastProject)
        assertEquals(listOf("pull", "--ff-only", "--quiet"), mock.lastArgs)
    }

    @Test
    fun `gitPush delegates push arguments with writeEnabled to gitRunner`() = runTest {
        val mock = MockGitRunner()
        val controller = DesktopWorkspaceController(gitRunner = mock)

        controller.gitPush("my-project")
        assertEquals("my-project", mock.lastProject)
        assertEquals(listOf("push", "origin", "HEAD"), mock.lastArgs)
    }
}
