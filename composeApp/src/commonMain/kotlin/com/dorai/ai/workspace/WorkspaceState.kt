package com.dorai.ai.workspace

import androidx.compose.runtime.Immutable
import com.dorai.ai.workspace.terminal.TerminalSnapshot
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/** How git reports a changed path inside a project. */
enum class WorkspaceGitStatus {
    Added,
    Modified,
    Deleted,
    Renamed,
    Untracked,
    Conflicted,
}

/** One path from `git status` — [path] is the guest-absolute path inside the project. */
@Immutable
data class WorkspaceGitChange(
    val path: String,
    val status: WorkspaceGitStatus,
    /** True when the index (staged) column of the porcelain XY pair reports a change. */
    val staged: Boolean = false,
    /** True when the worktree (unstaged) column reports a change. Untracked paths have neither. */
    val unstaged: Boolean = false,
)

/** Why a `git status` read produced no usable porcelain output. */
enum class WorkspaceGitError {
    /** The folder is not a git repository (or does not exist). */
    NotRepository,

    /** No git binary inside the guest rootfs. */
    MissingGit,

    /** Any other failure (proot error, timeout, malformed output). */
    Failed,
}

/** One entry from `git log --oneline` output. */
@Immutable
data class WorkspaceGitLogEntry(
    val hash: String,
    val subject: String,
    val authorName: String,
    val dateIso: String,
)

/**
 * One `git status` read: the changed paths plus the repository facts the UI shows
 * (current branch, whether this is a repository at all). [branch] is null for a
 * detached HEAD or a repository with no commits yet.
 */
@Immutable
data class WorkspaceGitResult(
    val changes: List<WorkspaceGitChange> = emptyList(),
    val branch: String? = null,
    val error: WorkspaceGitError? = null,
    val isRepository: Boolean = true,
)

/**
 * Unified diffs for one changed path, read inside the guest. [stagedDiff] is
 * `git diff --cached` (index vs HEAD); [unstagedDiff] is `git diff`
 * (worktree vs index). Either may be empty when there is no diff on that side.
 * [error] is non-null when the diff could not be read.
 */
@Immutable
data class WorkspaceGitDiff(
    val path: String,
    val unstagedDiff: String = "",
    val stagedDiff: String = "",
    val error: WorkspaceGitError? = null,
)

/** Ordered steps of a Workspace install, shown as progress on the setup screen. */
enum class WorkspaceStep {
    Download,
    Extract,
    Configure,
    BasePackages,
    Agent,
}

@Immutable
sealed interface WorkspaceEnvironmentState {
    data object NotInstalled : WorkspaceEnvironmentState

    /** [progress] is null while a step has no measurable progress. */
    data class Installing(
        val step: WorkspaceStep,
        val progress: Float? = null,
        val agentId: String? = null,
    ) : WorkspaceEnvironmentState

    data object Ready : WorkspaceEnvironmentState
}

/**
 * Lifecycle of one PTY session. [Starting] covers the window before the PTY bridge
 * exists (agent probe, helper writes); [Running] means the bridge handle is live.
 * [Stopping] is the transient close path; [Stopped]/[Failed] are terminal — the
 * session stays in the strip for inspection until the user closes or restarts it.
 */
enum class WorkspaceSessionStatus {
    Starting,
    Running,
    Stopping,
    Stopped,
    Failed,
    ;

    /** True while the runtime is still owned by this session. */
    val isLive: Boolean get() = this == Starting || this == Running || this == Stopping

    /** True once the runtime has ended for good. */
    val isTerminal: Boolean get() = this == Stopped || this == Failed

    /** True when typed input can still reach the runtime. */
    val allowsInput: Boolean get() = this == Starting || this == Running
}

/**
 * Derives the terminal status from a shell exit and/or a launch failure.
 * A non-null [error] (or a non-zero [exitCode]) means the session failed;
 * otherwise a clean exit is a normal stop.
 */
fun deriveSessionStatus(exitCode: Int?, error: String?): WorkspaceSessionStatus = when {
    error != null -> WorkspaceSessionStatus.Failed
    exitCode != null && exitCode != 0 -> WorkspaceSessionStatus.Failed
    else -> WorkspaceSessionStatus.Stopped
}

/**
 * One live shell inside a project — a tab in the terminal's session strip.
 * [agentId] is null for a plain shell; [number] disambiguates same-kind tabs.
 * [status] is the lifecycle truth; [busy] is derived for compat with callers
 * that only need "still alive".
 */
@Immutable
data class WorkspaceTerminalSession(
    val id: String,
    val project: String,
    val agentId: String?,
    val number: Int,
    val terminal: TerminalSnapshot = TerminalSnapshot.blank(),
    val status: WorkspaceSessionStatus = WorkspaceSessionStatus.Stopped,
    val exitCode: Int? = null,
    val error: String? = null,
) {
    /** Legacy busy flag, derived from [status]. */
    val busy: Boolean get() = status.isLive
}

/** Capture-file prefixes written per session into the bind-mounted tmp dir. */
private val CAPTURE_PREFIXES = listOf("dorai-pid-", "dorai-winsize-", "dorai-open-url-")

/**
 * Session id encoded in a capture-file name, or null for legacy bare names
 * (e.g. `dorai-pid` from the one-shot probe executor) and unrelated files.
 * The bare names are deliberately not swept: they belong to short-lived
 * probes, not to sessions, and deleting one mid-probe would break it.
 */
fun sessionIdFromCaptureFile(name: String): String? {
    val prefix = CAPTURE_PREFIXES.firstOrNull { name.startsWith(it) } ?: return null
    return name.removePrefix(prefix).takeIf { it.isNotEmpty() && !it.startsWith("/") && "/" !in it }
}

/**
 * Capture files with no live session — the stale set a startup sweep may delete.
 * Pure selection (no I/O) so the decision is unit-testable; callers perform
 * and log the actual deletes.
 */
fun staleSessionFileNames(fileNames: List<String>, liveSessionIds: Set<String>): List<String> = fileNames.filter { name -> sessionIdFromCaptureFile(name)?.let { it !in liveSessionIds } == true }

/**
 * The session to show for [project]: the globally active one when it belongs to
 * the project, otherwise the first session of the project (insertion order).
 * Fixes the mismatch where the global active id points at another project and
 * the strip shows no selection while the grid shows an arbitrary tab.
 */
fun activeSessionForProject(
    sessions: List<WorkspaceTerminalSession>,
    activeSessionId: String?,
    project: String,
): WorkspaceTerminalSession? {
    val inProject = sessions.filter { it.project == project }
    return inProject.firstOrNull { it.id == activeSessionId } ?: inProject.firstOrNull()
}

/** What the installed Debian actually is, shown on the project list. */
@Immutable
data class WorkspaceSystemInfo(
    /** e.g. "Debian GNU/Linux 12 (bookworm)". */
    val distribution: String,
    /** Linux architecture name of the rootfs (arm64, amd64, …). */
    val architecture: String,
    val packageCount: Int,
    /** Bytes taken by the Debian rootfs itself. */
    val systemBytes: Long,
    /** Bytes taken by the project folders (kept when Linux is uninstalled). */
    val projectsBytes: Long,
    val freeBytes: Long,
)

/** Everything the Workspace screen renders, in one snapshot. */
@Immutable
data class WorkspaceState(
    val environment: WorkspaceEnvironmentState = WorkspaceEnvironmentState.NotInstalled,
    val installedAgents: ImmutableSet<String> = persistentSetOf(),
    /** Folder names under the Linux home's `projects` directory. */
    val projects: ImmutableList<String> = persistentListOf(),
    /** Live shells, oldest first. Sessions of every open project, not just the visible one. */
    val sessions: ImmutableList<WorkspaceTerminalSession> = persistentListOf(),
    val activeSessionId: String? = null,
    /** Null until Debian is installed and its facts have been read. */
    val systemInfo: WorkspaceSystemInfo? = null,
    /** Last install failure, in shell/English wording — cleared when work restarts. */
    val lastError: String? = null,
) {
    val isReady: Boolean get() = environment is WorkspaceEnvironmentState.Ready
    val isInstalling: Boolean get() = environment is WorkspaceEnvironmentState.Installing
    val activeSession: WorkspaceTerminalSession? get() = sessions.firstOrNull { it.id == activeSessionId }
}
