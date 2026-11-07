package com.dorai.ai.ui.workspace

import com.dorai.ai.workspace.WorkspaceSessionStatus
import com.dorai.ai.workspace.WorkspaceTerminalSession
import com.dorai.ai.workspace.activeSessionForProject
import com.dorai.ai.workspace.deriveSessionStatus
import com.dorai.ai.workspace.sessionIdFromCaptureFile
import com.dorai.ai.workspace.staleSessionFileNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure lifecycle decisions behind Phase 5, extracted from the environment manager
 * so they run on the JVM without Android or proot.
 */
class WorkspaceSessionLifecycleTest {

    @Test
    fun `deriveSessionStatus maps clean exit to Stopped`() {
        assertEquals(WorkspaceSessionStatus.Stopped, deriveSessionStatus(exitCode = 0, error = null))
        assertEquals(WorkspaceSessionStatus.Stopped, deriveSessionStatus(exitCode = null, error = null))
    }

    @Test
    fun `deriveSessionStatus maps non-zero exit to Failed`() {
        assertEquals(WorkspaceSessionStatus.Failed, deriveSessionStatus(exitCode = 1, error = null))
        assertEquals(WorkspaceSessionStatus.Failed, deriveSessionStatus(exitCode = 127, error = null))
    }

    @Test
    fun `deriveSessionStatus maps any error to Failed`() {
        assertEquals(WorkspaceSessionStatus.Failed, deriveSessionStatus(exitCode = 0, error = "proot boom"))
        assertEquals(WorkspaceSessionStatus.Failed, deriveSessionStatus(exitCode = null, error = "spawn failed"))
    }

    @Test
    fun `busy is derived from status and matches the old flag`() {
        assertEquals(true, WorkspaceTerminalSession(id = "s1", project = "p", agentId = null, number = 1, status = WorkspaceSessionStatus.Starting).busy)
        assertEquals(true, WorkspaceTerminalSession(id = "s1", project = "p", agentId = null, number = 1, status = WorkspaceSessionStatus.Running).busy)
        assertEquals(true, WorkspaceTerminalSession(id = "s1", project = "p", agentId = null, number = 1, status = WorkspaceSessionStatus.Stopping).busy)
        assertEquals(false, WorkspaceTerminalSession(id = "s1", project = "p", agentId = null, number = 1, status = WorkspaceSessionStatus.Stopped).busy)
        assertEquals(false, WorkspaceTerminalSession(id = "s1", project = "p", agentId = null, number = 1, status = WorkspaceSessionStatus.Failed).busy)
    }

    @Test
    fun `only starting and running accept input`() {
        assertEquals(true, WorkspaceSessionStatus.Starting.allowsInput)
        assertEquals(true, WorkspaceSessionStatus.Running.allowsInput)
        assertEquals(false, WorkspaceSessionStatus.Stopping.allowsInput)
        assertEquals(false, WorkspaceSessionStatus.Stopped.allowsInput)
        assertEquals(false, WorkspaceSessionStatus.Failed.allowsInput)
    }

    @Test
    fun `sessionIdFromCaptureFile reads per-session names only`() {
        assertEquals("s12", sessionIdFromCaptureFile("dorai-pid-s12"))
        assertEquals("s3", sessionIdFromCaptureFile("dorai-winsize-s3"))
        assertEquals("s7", sessionIdFromCaptureFile("dorai-open-url-s7"))
    }

    @Test
    fun `sessionIdFromCaptureFile ignores bare probe names and unrelated files`() {
        assertNull(sessionIdFromCaptureFile("dorai-pid"))
        assertNull(sessionIdFromCaptureFile("dorai-winsize"))
        assertNull(sessionIdFromCaptureFile("dorai-open-url"))
        assertNull(sessionIdFromCaptureFile("other-file.txt"))
        assertNull(sessionIdFromCaptureFile("dorai-pid-"))
        assertNull(sessionIdFromCaptureFile("kai-open-url-s1"))
    }

    @Test
    fun `stale sweep selects only files without a live session`() {
        val files = listOf(
            "dorai-pid-s1",
            "dorai-winsize-s1",
            "dorai-open-url-s1",
            "dorai-pid-s2",
            "dorai-winsize-s2",
            "dorai-open-url-s2",
            "dorai-pid",
            "dorai-winsize",
            "unrelated.log",
        )

        assertEquals(
            listOf("dorai-pid-s2", "dorai-winsize-s2", "dorai-open-url-s2"),
            staleSessionFileNames(files, liveSessionIds = setOf("s1")),
        )
        assertEquals(emptyList(), staleSessionFileNames(files, liveSessionIds = setOf("s1", "s2")))
        // Startup sweep: no live sessions, so every per-session file is stale.
        assertEquals(
            listOf("dorai-pid-s1", "dorai-winsize-s1", "dorai-open-url-s1", "dorai-pid-s2", "dorai-winsize-s2", "dorai-open-url-s2"),
            staleSessionFileNames(files, liveSessionIds = emptySet()),
        )
    }

    @Test
    fun `activeSessionForProject prefers the globally active session in the project`() {
        val other = session(id = "s1", project = "alpha")
        val mine = session(id = "s2", project = "beta")
        val mine2 = session(id = "s3", project = "beta")

        assertEquals("s2", activeSessionForProject(listOf(other, mine, mine2), "s2", "beta")?.id)
    }

    @Test
    fun `activeSessionForProject falls back to the first session when the global active is another project`() {
        val other = session(id = "s1", project = "alpha")
        val mine = session(id = "s2", project = "beta")
        val mine2 = session(id = "s3", project = "beta")

        // Global active points at alpha, but beta's strip must still show a selection.
        assertEquals("s2", activeSessionForProject(listOf(other, mine, mine2), "s1", "beta")?.id)
        assertNull(activeSessionForProject(listOf(other), "s1", "beta"))
    }

    private fun session(id: String, project: String) = WorkspaceTerminalSession(
        id = id,
        project = project,
        agentId = null,
        number = 1,
    )
}
