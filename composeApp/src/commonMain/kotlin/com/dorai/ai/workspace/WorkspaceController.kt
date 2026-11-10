package com.dorai.ai

import com.dorai.ai.workspace.WorkspaceGitChange
import com.dorai.ai.workspace.WorkspaceGitDiff
import com.dorai.ai.workspace.WorkspaceGitError
import com.dorai.ai.workspace.WorkspaceGitLogEntry
import com.dorai.ai.workspace.WorkspaceGitResult
import com.dorai.ai.workspace.WorkspaceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Workspace's Debian-under-proot environment. Separate from [SandboxController]
 * on purpose: different rootfs, different home, and resetting one must never
 * touch the other.
 */
interface WorkspaceController {
    val state: StateFlow<WorkspaceState>

    /** The Debian file tree, for the Files tab. Empty on platforms without Workspace. */
    val files: FileBrowserSource

    /** Installs Debian if needed, then the given agents. Safe to call when already installed. */
    fun install(agentIds: Set<String>)
    fun cancel()
    fun uninstall()

    /** Re-scans projects, installed agents, and system facts. */
    fun refresh()

    /**
     * Asks the environment for `git status` of `projects/<project>`.
     *
     * The result distinguishes a clean repository ([WorkspaceGitResult.error] is
     * null, [WorkspaceGitResult.changes] is empty) from failure modes — missing
     * git, not a repository, timeout — so the UI never reports "No changes" for
     * a repository it could not read.
     */
    suspend fun gitStatus(project: String): WorkspaceGitResult

    /** Clones a GitHub repository into `projects/<project>` (read-only, depth=1). */
    suspend fun gitClone(project: String, url: String): WorkspaceGitResult

    /** Reads `git log --oneline` for `projects/<project>`, up to [maxCount] entries. */
    suspend fun gitLog(project: String, maxCount: Int = 20): List<WorkspaceGitLogEntry>

    /** Fetches from origin for `projects/<project>` (fast-forward only). */
    suspend fun gitFetch(project: String): WorkspaceGitResult

    /** Pulls from origin with --ff-only for `projects/<project>`. */
    suspend fun gitPullFfOnly(project: String): WorkspaceGitResult

    /** Reads staged + unstaged diffs for one guest-absolute [path] inside [project]. */
    suspend fun gitDiff(project: String, path: String): WorkspaceGitDiff

    /** Stages [path]; true on success. Never stages anything else. */
    suspend fun gitStage(project: String, path: String): Boolean

    /** Unstages [path]; true on success. */
    suspend fun gitUnstage(project: String, path: String): Boolean

    /**
     * Discards all changes to [path] (staged and unstaged). Destructive — the
     * UI must confirm first. Never commits and never touches unrelated files.
     */
    suspend fun gitDiscard(project: String, path: String): Boolean

    /** Creates `projects/<name>` and returns the sanitized folder name, or null if invalid. */
    fun createProject(name: String): String?

    /** Deletes `projects/<name>` and everything in it. The project's sessions are closed with it. */
    fun deleteProject(name: String)

    /**
     * Renames `projects/<name>`, returning the sanitized new folder name — or null
     * when the name is unusable or already taken. The project's sessions are closed:
     * their working directory is the folder that just moved.
     */
    fun renameProject(name: String, newName: String): String?

    /**
     * Starts an interactive PTY session in [project] and makes it the active one.
     * A non-null [agentId] launches that agent's CLI first, leaving a shell behind
     * when it exits.
     */
    fun startSession(project: String, agentId: String?)

    /** Brings an existing session to the front; the terminal renders the active one. */
    fun selectSession(id: String)

    /** Ends one session and activates its neighbour, if any. */
    fun closeSession(id: String)

    /**
     * Re-enters [project]: activates the session it was left on and stops its
     * shells from being reaped. False when the project has none running, which is
     * the caller's cue to start one.
     */
    fun resumeProject(project: String): Boolean

    /**
     * Steps out of [project] with its shells still running — a command keeps going
     * while the user is elsewhere, and only the user closes a session. They are
     * ended for them if the project is never reopened; see the idle window in the
     * Android implementation.
     */
    fun leaveProject(project: String)

    /**
     * Sends raw bytes to the active session — typed text, a submitted line
     * (terminated with `\r`), or an encoded key sequence.
     */
    fun writeToTerminal(text: String)

    /**
     * Updates the active session's VT geometry and its live PTY winsize
     * (rows/cols). Call when the terminal viewport is measured or resized.
     */
    fun resizeTerminal(columns: Int, rows: Int)

    /**
     * Ends every session and releases the manager scope. Process-scoped:
     * only the app owner's final teardown should call this — leaving the
     * Workspace screen must not, or resume-after-leave breaks.
     */
    fun close()
}

/** Workspace is Android-only; every other target gets this. */
class NoOpWorkspaceController : WorkspaceController {
    override val state = MutableStateFlow(WorkspaceState())
    override val files: FileBrowserSource = NoOpFileBrowserSource

    override fun install(agentIds: Set<String>) {}
    override fun cancel() {}
    override fun uninstall() {}
    override fun refresh() {}
    override suspend fun gitStatus(project: String): WorkspaceGitResult = WorkspaceGitResult()
    override suspend fun gitClone(project: String, url: String): WorkspaceGitResult = WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = false)
    override suspend fun gitLog(project: String, maxCount: Int): List<WorkspaceGitLogEntry> = emptyList()
    override suspend fun gitFetch(project: String): WorkspaceGitResult = WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = false)
    override suspend fun gitPullFfOnly(project: String): WorkspaceGitResult = WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = false)
    override suspend fun gitDiff(project: String, path: String): WorkspaceGitDiff = WorkspaceGitDiff(path = path, error = WorkspaceGitError.Failed)
    override suspend fun gitStage(project: String, path: String): Boolean = false
    override suspend fun gitUnstage(project: String, path: String): Boolean = false
    override suspend fun gitDiscard(project: String, path: String): Boolean = false
    override fun createProject(name: String): String? = null
    override fun deleteProject(name: String) {}
    override fun renameProject(name: String, newName: String): String? = null
    override fun startSession(project: String, agentId: String?) {}
    override fun selectSession(id: String) {}
    override fun closeSession(id: String) {}
    override fun resumeProject(project: String): Boolean = false
    override fun leaveProject(project: String) {}
    override fun writeToTerminal(text: String) {}
    override fun resizeTerminal(columns: Int, rows: Int) {}
    override fun close() {}
}

expect fun createWorkspaceController(): WorkspaceController
