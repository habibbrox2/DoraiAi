package com.dorai.ai.workspace.runtime

import android.os.StatFs
import android.util.Log
import com.dorai.ai.github.GitUrlValidator
import com.dorai.ai.linux.DEFAULT_GUEST_PATH
import com.dorai.ai.linux.DebianSpec
import com.dorai.ai.linux.InstallStep
import com.dorai.ai.linux.LinuxDistro
import com.dorai.ai.linux.LinuxInstaller
import com.dorai.ai.linux.LinuxPaths
import com.dorai.ai.linux.ProotHandle
import com.dorai.ai.linux.ProotLauncher
import com.dorai.ai.workspace.WorkspaceAgent
import com.dorai.ai.workspace.WorkspaceAgents
import com.dorai.ai.workspace.WorkspaceEnvironmentState
import com.dorai.ai.workspace.WorkspaceGitDiff
import com.dorai.ai.workspace.WorkspaceGitError
import com.dorai.ai.workspace.WorkspaceGitLogEntry
import com.dorai.ai.workspace.WorkspaceGitResult
import com.dorai.ai.workspace.WorkspaceState
import com.dorai.ai.workspace.WorkspaceStep
import com.dorai.ai.workspace.WorkspaceSystemInfo
import com.dorai.ai.workspace.WorkspaceTerminalSession
import com.dorai.ai.workspace.gitRelativePath
import com.dorai.ai.workspace.parseGitLogOneline
import com.dorai.ai.workspace.parseGitStatusPorcelain
import com.dorai.ai.workspace.shellQuote
import com.dorai.ai.workspace.terminal.DEFAULT_COLUMNS
import com.dorai.ai.workspace.terminal.DEFAULT_ROWS
import com.dorai.ai.workspace.terminal.MAX_COLUMNS
import com.dorai.ai.workspace.terminal.MAX_ROWS
import com.dorai.ai.workspace.terminal.MIN_COLUMNS
import com.dorai.ai.workspace.terminal.MIN_ROWS
import com.dorai.ai.workspace.terminal.TerminalScreen
import com.dorai.ai.workspace.terminal.TerminalSnapshot
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "Workspace"

/** Device-code pattern shown on Grok's login TUI (e.g. PNX4-ZGCX). */
private val DEVICE_CODE_REGEX = Regex("""\b([A-Z0-9]{4}-[A-Z0-9]{4})\b""")

private val INVALID_NAME_CHARS = Regex("[^A-Za-z0-9._-]+")

/** Floor on the gap between two terminal repaints — roughly one display frame. */
private const val REPAINT_INTERVAL_MS = 16L

/** Captured login / OSC 8 links leave the bar after this (matches the UI hide). */
private const val HYPERLINK_TTL_MS = 2 * 60 * 1000L

/**
 * How long a project's shells keep running after the user steps out of it. Long
 * enough that leaving a build to read something else, or to work in another
 * project, never costs the session; short enough that a phone is not holding a
 * proot process per project the user has forgotten about.
 */
private const val IDLE_PROJECT_TTL_MS = 60 * 60 * 1000L

/**
 * One live PTY session: its own VT screen, geometry, and capture files. Several
 * can run at once, which is what the terminal's session tabs switch between.
 */
private class BuildSession(
    val id: String,
    val project: String,
    val agentId: String?,
    val number: Int,
    columns: Int,
    rows: Int,
) {
    private val screenLock = Any()
    private val screen = TerminalScreen(columns, rows)

    val columns = AtomicInteger(columns)
    val rows = AtomicInteger(rows)

    /** Host-side file names inside the bind-mounted tmp dir — one set per session. */
    val winsizeFile: String get() = "dorai-winsize-$id"
    val openUrlFile: String get() = "dorai-open-url-$id"
    val pidFile: String get() = "dorai-pid-$id"

    @Volatile
    var handle: ProotHandle? = null

    @Volatile
    var job: Job? = null

    /** Set by [stop] (close path) so a startup still in its probe phase aborts. */
    @Volatile
    var closed: Boolean = false

    @Volatile
    var status: com.dorai.ai.workspace.WorkspaceSessionStatus = com.dorai.ai.workspace.WorkspaceSessionStatus.Starting

    @Volatile
    var exitCode: Int? = null

    @Volatile
    var error: String? = null

    @Volatile
    var lastDeviceCode: String? = null

    /** Cancels/reschedules when a new URL is captured so the bar stays for a full TTL. */
    var hyperlinkClearJob: Job? = null

    /** Last published screen — recomputed only for the session that produced output. */
    @Volatile
    var snapshot: TerminalSnapshot = TerminalSnapshot.blank()
        private set

    fun <T> withScreen(block: (TerminalScreen) -> T): T = synchronized(screenLock) { block(screen) }

    /**
     * Caches a fresh snapshot and hands back the visible text (for device-code
     * scans). URLs are scraped from a space-trimmed join of rows so a line-wrapped
     * OAuth URL reassembles into one link instead of one stump per row.
     * Snapshotting allocates a cell object per grid position, so the second
     * snapshot only happens when a new link actually changed the screen.
     */
    fun refreshSnapshot(): String = synchronized(screenLock) {
        val snap = screen.snapshot()
        val text = visibleText(snap)
        val urlText = visibleTextForUrlScan(snap)
        snapshot = if (screen.noteTextUrls(urlText)) screen.snapshot() else snap
        text
    }

    fun resize(cols: Int, rws: Int) {
        columns.set(cols)
        rows.set(rws)
        synchronized(screenLock) { screen.resize(cols, rws) }
    }

    fun stop() {
        closed = true
        val current = status
        if (current != com.dorai.ai.workspace.WorkspaceSessionStatus.Stopped &&
            current != com.dorai.ai.workspace.WorkspaceSessionStatus.Failed
        ) {
            status = com.dorai.ai.workspace.WorkspaceSessionStatus.Stopping
        }
        runCatching { handle?.cancel() }.onFailure { Log.w(TAG, "session $id: handle cancel failed", it) }
        handle = null
        job?.cancel()
        job = null
        hyperlinkClearJob?.cancel()
        hyperlinkClearJob = null
    }

    fun toUiSession() = WorkspaceTerminalSession(
        id = id,
        project = project,
        agentId = agentId,
        number = number,
        terminal = snapshot,
        status = status,
        exitCode = exitCode,
        error = error,
    )
}

private fun visibleText(snap: TerminalSnapshot): String = buildString(snap.columns * snap.rows + snap.rows) {
    for (row in 0 until snap.rows) {
        for (col in 0 until snap.columns) {
            append(snap.cellAt(col, row).char)
        }
        append('\n')
    }
}

/**
 * Like [visibleText] but trims trailing spaces on each row and joins without
 * newlines. Terminal wrap pads the rest of the line with spaces; keeping those
 * (or the newline) splits one long OAuth URL into many partial matches.
 */
private fun visibleTextForUrlScan(snap: TerminalSnapshot): String = buildString(snap.columns * snap.rows) {
    for (row in 0 until snap.rows) {
        val start = length
        for (col in 0 until snap.columns) {
            append(snap.cellAt(col, row).char)
        }
        // Trim trailing spaces from this row only.
        var end = length
        while (end > start && this[end - 1] == ' ') end--
        if (end < length) deleteRange(end, length)
    }
}

/**
 * Owns the Debian rootfs, agent installs, and the project terminal sessions for
 * Workspace. Process-scoped: held by the Koin-managed AndroidWorkspaceController.
 */
class WorkspaceEnvironmentManager(
    /**
     * The install to work in. When the chat sandbox is also Debian this is the
     * *same* install it uses, so setting up either one sets up both.
     */
    private val paths: LinuxPaths,
) {

    private val installer = LinuxInstaller(paths)
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.IO)

    /**
     * Fired whenever this install appears or disappears. Set when the rootfs is
     * shared with the chat sandbox, which otherwise has no way to learn that
     * Workspace just gave it a Linux (or took one away).
     */
    @Volatile
    var onEnvironmentChanged: (() -> Unit)? = null
    private var installJob: Job? = null

    private val sessionsLock = Any()
    private val sessions = LinkedHashMap<String, BuildSession>()
    private var activeSessionId: String? = null
    private val sessionCounter = AtomicInteger()

    /** Pending reaps, one per project the user has stepped out of. Guarded by [sessionsLock]. */
    private val reapJobs = HashMap<String, Job>()

    /**
     * Project whose terminal is on screen, if any. Only its sessions push repaints
     * to the UI — a background shell still parses everything it is sent, but a
     * screen nobody is looking at should not recompose the app sixty times a
     * second. Its snapshot is current whenever the project is opened again.
     */
    @Volatile
    private var foregroundProject: String? = null

    /** Geometry of the last measured viewport — new sessions start there, not at 80×24. */
    private val lastColumns = AtomicInteger(DEFAULT_COLUMNS)
    private val lastRows = AtomicInteger(DEFAULT_ROWS)

    private val _state = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = _state.asStateFlow()

    init {
        paths.ensureLayout()
        sweepStaleCaptureFiles()
        scope.launch { sync() }
    }

    /**
     * Ends every session and cancels the manager scope. Process-scoped: the owners
     * call this only when the whole app is going away — leaving the Workspace
     * screen must not close sessions, or resume-after-leave breaks.
     */
    fun close() {
        runCatching { closeAllSessions() }.onFailure { Log.w(TAG, "close: closeAllSessions failed", it) }
        runCatching { installJob?.cancel() }.onFailure { Log.w(TAG, "close: install cancel failed", it) }
        installJob = null
        runCatching { scopeJob.cancel() }.onFailure { Log.w(TAG, "close: scope cancel failed", it) }
    }

    /**
     * Deletes capture files whose session no longer exists. Runs once at startup
     * when the live set is empty, so every `dorai-pid-*`/`dorai-winsize-*-*`/
     * `dorai-open-url-*` file is stale. Legacy bare names from one-shot probes
     * are left alone (see [com.dorai.ai.workspace.sessionIdFromCaptureFile]).
     * Failures are logged, never fatal.
     */
    private fun sweepStaleCaptureFiles() {
        val dir = paths.tmpDir
        if (!dir.isDirectory) return
        val names = dir.list()?.toList() ?: return
        val stale = com.dorai.ai.workspace.staleSessionFileNames(names, emptySet())
        for (name in stale) {
            val file = File(dir, name)
            runCatching {
                if (!file.delete()) Log.w(TAG, "stale sweep: could not delete $name")
            }.onFailure { Log.w(TAG, "stale sweep: failed to delete $name", it) }
        }
    }

    fun install(agentIds: Set<String>) {
        if (installJob?.isActive == true) return
        installJob = scope.launch {
            try {
                installInternal(agentIds) { isActive }
            } catch (e: CancellationException) {
                cleanUpPartialInstall(error = null)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Install failed", e)
                cleanUpPartialInstall(error = e.message ?: "Install failed")
            }
        }
    }

    fun cancel() {
        installJob?.cancel()
        installJob = null
    }

    /**
     * Removes the Linux, not the projects. When this install is shared with the
     * chat sandbox that sandbox loses its Linux too — the setup card says so.
     */
    fun uninstall() {
        if (installJob?.isActive == true) return
        installJob = scope.launch {
            closeAllSessions()
            paths.deleteInstall()
            paths.ensureLayout()
            _state.value = WorkspaceState(projects = scanProjects())
            onEnvironmentChanged?.invoke()
        }
    }

    /** Called when something else is about to delete the rootfs under us. */
    fun onEnvironmentRemoved() {
        closeAllSessions()
        _state.value = WorkspaceState(projects = scanProjects())
    }

    fun refresh() {
        scope.launch { sync() }
    }

    fun createProject(name: String): String? {
        val folder = sanitizeProjectName(name) ?: return null
        File(paths.projectsDir, folder).mkdirs()
        scope.launch { _state.update { it.copy(projects = scanProjects()) } }
        return folder
    }

    /**
     * Deletes the project folder and everything in it. Its shells go first: they
     * are rooted in that folder, and one whose working directory has been unlinked
     * is no longer a session anybody can use.
     */
    fun deleteProject(name: String) {
        val dir = projectDir(name) ?: return
        closeProjectSessions(name)
        scope.launch {
            deleteTree(dir)
            _state.update { it.copy(projects = scanProjects()) }
        }
    }

    /**
     * Renames the project folder, returning the sanitized new name — or null when
     * that name is unusable or already taken. Shells are closed for the same reason
     * as a delete: their working directory is the path that just moved.
     */
    fun renameProject(name: String, newName: String): String? {
        val dir = projectDir(name) ?: return null
        val folder = sanitizeProjectName(newName) ?: return null
        if (folder == name) return folder
        val target = File(paths.projectsDir, folder)
        if (target.exists()) return null
        closeProjectSessions(name)
        if (!dir.renameTo(target)) return null
        scope.launch { _state.update { it.copy(projects = scanProjects()) } }
        return folder
    }

    private fun sanitizeProjectName(name: String): String? = name.trim()
        .replace(INVALID_NAME_CHARS, "-")
        .trim('-', '.')
        .take(64)
        .takeIf { it.isNotEmpty() }

    /**
     * An existing project folder, addressed by the name the list shows. Matched
     * as-is rather than sanitized — the list is a directory listing, so it can hold
     * names a shell created that sanitizing would rewrite into a different folder —
     * but never one that reaches outside the projects directory.
     */
    private fun projectDir(name: String): File? {
        if (name.isEmpty() || name == "." || name == ".." || name.contains('/')) return null
        return File(paths.projectsDir, name).takeIf { it.isDirectory }
    }

    /**
     * Deletes [dir] and its contents without following symlinks. A project can hold
     * a link into the rootfs (agents leave plenty), and a walk that followed one
     * would delete the Debian install instead of the project.
     */
    private fun deleteTree(dir: File) {
        runCatching {
            Files.walkFileTree(
                dir.toPath(),
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        runCatching { Files.deleteIfExists(file) }
                            .onFailure { Log.w(TAG, "deleteTree: could not delete $file", it) }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE

                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        runCatching { Files.deleteIfExists(dir) }
                            .onFailure { Log.w(TAG, "deleteTree: could not delete $dir", it) }
                        return FileVisitResult.CONTINUE
                    }
                },
            )
        }.onFailure { Log.w(TAG, "deleteTree: walk failed for $dir", it) }
    }

    // --- sessions --------------------------------------------------------

    /**
     * Opens another PTY session in [project] and makes it active. A non-null
     * [agentId] runs that agent first and drops back to a shell when it exits.
     */
    fun startSession(project: String, agentId: String?) {
        if (!_state.value.isReady) return
        foregroundProject = project
        val id = "s${sessionCounter.incrementAndGet()}"
        val session = synchronized(sessionsLock) {
            val number = (sessions.values.filter { it.project == project }.maxOfOrNull { it.number } ?: 0) + 1
            BuildSession(
                id = id,
                project = project,
                agentId = agentId?.takeIf { WorkspaceAgents.get(it) != null },
                number = number,
                columns = lastColumns.get(),
                rows = lastRows.get(),
            ).also {
                sessions[id] = it
                activeSessionId = id
            }
        }
        // Launch before publishing so the job exists before the UI can close it.
        // The handle is still null until runShell has probed for the agent and
        // started the PTY; runShell checks `closed` before and after that probe
        // so a close during startup aborts instead of leaking a PTY.
        session.job = scope.launch { runShell(session) }
        if (session.closed) {
            session.job?.cancel()
            Log.w(TAG, "session $id closed during startup; launch aborted")
        }
        publishSessions()
    }

    fun selectSession(id: String) {
        synchronized(sessionsLock) {
            if (!sessions.containsKey(id)) return
            activeSessionId = id
        }
        publishSessions()
    }

    fun closeSession(id: String) {
        val session = synchronized(sessionsLock) {
            val removed = sessions.remove(id) ?: return
            // Its neighbour in the same project, or nothing: other projects have
            // sessions of their own now, and none of them is what the user is
            // looking at.
            if (activeSessionId == id) {
                activeSessionId = sessions.values.lastOrNull { it.project == removed.project }?.id
            }
            removed
        }
        endSession(session)
        publishSessions()
    }

    /**
     * Re-enters [project]: its shells kept running while the user was away, so
     * this only calls off the reaper and puts the tab they left on back in front.
     * Only a live session counts as resumable — a stopped/failed shell resurrected
     * as if alive would show an active tab whose runtime is gone; dead ones stay
     * in the strip for restart, and the caller starts a fresh session instead.
     */
    fun resumeProject(project: String): Boolean {
        foregroundProject = project
        val resumed = synchronized(sessionsLock) {
            reapJobs.remove(project)?.cancel()
            val last = sessions.values.lastOrNull {
                it.project == project && it.status.isLive
            }
            if (last != null) activeSessionId = last.id
            last != null
        }
        if (resumed) publishSessions()
        return resumed
    }

    /**
     * Steps out of [project] without touching its shells — a build keeps building
     * while the user reads something else, and closing a session stays the user's
     * call. The reaper is the backstop: each session is a proot process with a PTY
     * and a shell behind it, and a project reopened days later is not one the user
     * is still waiting on.
     */
    fun leaveProject(project: String) {
        if (foregroundProject == project) foregroundProject = null
        synchronized(sessionsLock) {
            reapJobs.remove(project)?.cancel()
            if (sessions.values.none { it.project == project }) return
            reapJobs[project] = scope.launch {
                delay(IDLE_PROJECT_TTL_MS)
                // Drop the handle before closing: cancelling the job that is running
                // this block is not how it should end.
                synchronized(sessionsLock) { reapJobs.remove(project) }
                closeProjectSessions(project)
            }
        }
    }

    private fun closeProjectSessions(project: String) {
        val closing = synchronized(sessionsLock) {
            reapJobs.remove(project)?.cancel()
            val matches = sessions.values.filter { it.project == project }
            matches.forEach { sessions.remove(it.id) }
            if (sessions[activeSessionId] == null) activeSessionId = sessions.values.lastOrNull()?.id
            matches
        }
        closing.forEach(::endSession)
        publishSessions()
    }

    /**
     * Writes text to the active session's shell/agent (UTF-8). Callers that want
     * a submitted line should include the trailing carriage return themselves.
     * No-ops for a stopped/failed session — its handle is gone and input would
     * only look delivered.
     */
    fun writeToTerminal(text: String) {
        val session = activeSession() ?: return
        if (!session.status.allowsInput) return
        runCatching { session.handle?.writeText(text) }
            .onFailure { Log.w(TAG, "session ${session.id}: write failed", it) }
    }

    /**
     * Resize the active session's VT buffer and notify its live PTY
     * (desktop-style). Safe to call from the UI on every layout pass — no-ops
     * when geometry is unchanged.
     */
    fun resizeTerminal(columns: Int, rows: Int) {
        val c = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        val r = rows.coerceIn(MIN_ROWS, MAX_ROWS)
        lastColumns.set(c)
        lastRows.set(r)
        val session = activeSession() ?: return
        if (session.status.isTerminal) return
        // Cheap enough to answer on the caller's thread, and the common case: the
        // UI calls this on every layout pass, including each keyboard-animation frame.
        if (c == session.columns.get() && r == session.rows.get()) return
        scope.launch {
            // Read the geometry back rather than using the captured one: two resizes
            // in flight at once would otherwise race, and the stale one could land
            // last. Re-reading makes every pending pass apply the newest viewport.
            val currentC = lastColumns.get()
            val currentR = lastRows.get()
            session.resize(currentC, currentR)
            publishScreen(session)
            writeWinsize(session, columns = currentC, rows = currentR)
        }
    }

    /**
     * Hands the session's PTY bridge its new geometry — it polls this file and
     * applies TIOCSWINSZ + SIGWINCH.
     *
     * Written to a sibling and renamed rather than in place: the bridge reads on
     * its own clock, and a read landing inside a truncate-then-write sees a torn
     * number — "24 6" for "24 62" — which resizes the guest to a width nothing
     * asked for until the next tick.
     */
    private fun writeWinsize(session: BuildSession, columns: Int, rows: Int) {
        runCatching {
            paths.tmpDir.mkdirs()
            val target = File(paths.tmpDir, session.winsizeFile)
            val staged = File(paths.tmpDir, "${session.winsizeFile}.tmp")
            staged.writeText("$rows $columns\n")
            if (!staged.renameTo(target)) {
                target.writeText("$rows $columns\n")
                staged.delete()
            }
        }.onFailure { Log.w(TAG, "session ${session.id}: writeWinsize failed", it) }
    }

    private fun activeSession(): BuildSession? = synchronized(sessionsLock) { sessions[activeSessionId] }

    private fun closeAllSessions() {
        foregroundProject = null
        val closing = synchronized(sessionsLock) {
            reapJobs.values.forEach { it.cancel() }
            reapJobs.clear()
            val all = sessions.values.toList()
            sessions.clear()
            activeSessionId = null
            all
        }
        closing.forEach(::endSession)
        publishSessions()
    }

    /** Kills the process and drops the per-session capture files. */
    private fun endSession(session: BuildSession) {
        session.stop()
        deleteCaptureFiles(session)
    }

    /** Best-effort delete with logging — a leftover capture file is swept at next startup. */
    private fun deleteCaptureFiles(session: BuildSession) {
        for (name in listOf(session.winsizeFile, session.openUrlFile, session.pidFile)) {
            runCatching {
                val file = File(paths.tmpDir, name)
                if (file.exists() && !file.delete()) Log.w(TAG, "session ${session.id}: could not delete $name")
            }.onFailure { Log.w(TAG, "session ${session.id}: failed to delete $name", it) }
        }
    }

    private fun publishSessions() {
        val list: List<WorkspaceTerminalSession>
        val active: String?
        synchronized(sessionsLock) {
            list = sessions.values.map { it.toUiSession() }
            active = activeSessionId
        }
        _state.update { it.copy(sessions = list.toImmutableList(), activeSessionId = active) }
    }

    // --- install ---------------------------------------------------------

    private suspend fun installInternal(agentIds: Set<String>, isActive: () -> Boolean) {
        // Whoever installs first wins: a Debian chat sandbox already put a rootfs
        // here, in which case this only adds the agents the user ticked.
        if (paths.readMarker() == null) {
            installDebian()
            ensureAgentPathProfile()
            onEnvironmentChanged?.invoke()
        }

        val failed = mutableListOf<String>()
        for (agent in agentIds.mapNotNull { WorkspaceAgents.get(it) }) {
            if (!isActive()) throw CancellationException()
            if (!installAgent(agent)) failed += agent.title
        }
        sync(error = if (failed.isEmpty()) null else "Could not install ${failed.joinToString()}")
    }

    private suspend fun installDebian() {
        installer.install(LinuxDistro.DEBIAN) { step ->
            when (step) {
                is InstallStep.Download -> setStep(WorkspaceStep.Download, progress = step.fraction)
                is InstallStep.Extract -> setStep(WorkspaceStep.Extract)
                is InstallStep.Configure -> setStep(WorkspaceStep.Configure)
                is InstallStep.Packages -> setStep(WorkspaceStep.BasePackages)
            }
        }
    }

    private suspend fun installAgent(agent: WorkspaceAgent): Boolean = LinuxInstaller.packageLock.withLock {
        setStep(WorkspaceStep.Agent, agentId = agent.id)
        ensureAgentPathProfile()
        val executor = executor()
        executor.execute(
            "mkdir -p /root/.local/bin /root/.grok/bin /root/.grok/downloads " +
                "/root/.opencode/bin /root/.claude/downloads /usr/local/bin",
            timeoutSeconds = 30,
        )
        val result = executor.execute(agent.installCommand, timeoutSeconds = 900)
        // Vendor scripts may leave the binary only in their private dir and only
        // update shell rc files — which Workspace never sources. Link into PATH
        // and re-probe so a successful download still counts as installed.
        val installed = executor.ensureAgentBinary(agent.binary)
        if (!installed) {
            Log.w(TAG, "${agent.id} install failed (exit detail): ${result.failureDetail()}")
            val probe = executor.execute(
                "command -v ${agent.binary} 2>/dev/null; " +
                    "ls -la /root/.local/bin /root/.grok/bin /root/.opencode/bin /usr/local/bin 2>/dev/null; " +
                    "ls -la /root/.local/share/claude/versions 2>/dev/null | tail -5; " +
                    "ls -la /root/.grok/downloads 2>/dev/null | tail -5; " +
                    "ls -la /root/.opencode/bin 2>/dev/null",
                timeoutSeconds = 30,
            )
            Log.w(TAG, "${agent.id} post-install probe: ${probe.stdout} ${probe.stderr}")
        }
        installed
    }

    /** The installer already removes a rootfs it could not finish; this reports it. */
    private fun cleanUpPartialInstall(error: String?) {
        sync(error)
    }

    // --- terminal --------------------------------------------------------

    /** Snapshot the screen, scan it for a device code, and push it to the UI. */
    private fun publishScreen(session: BuildSession) {
        val linksBefore = session.snapshot.hyperlinks
        val text = session.refreshSnapshot()
        noteDeviceCodeFromText(session, text)
        val linksAfter = session.snapshot.hyperlinks
        // New URLs (OSC 8 / stream / device code) restart the auto-hide TTL.
        if (linksAfter.isNotEmpty() && linksAfter != linksBefore) {
            scheduleHyperlinkClear(session)
        }
        // The snapshot above is kept current either way; only a session the user
        // is actually looking at is worth pushing to the UI.
        if (session.project == foregroundProject) publishSessions()
    }

    private fun runShell(session: BuildSession) {
        session.refreshSnapshot()
        publishSessions()
        var urlPollJob: Job? = null
        // Repaint is coalesced to at most one per frame. A repaint snapshots the
        // whole grid and re-lays out the text, and the PTY hands us a chunk every
        // few hundred microseconds under heavy output — repainting per chunk is
        // what made the app unusable while a command was running. CONFLATED means
        // a burst collapses into a single repaint without ever dropping the last
        // state: whatever arrived during the wait is still pending afterwards.
        val repaint = Channel<Unit>(Channel.CONFLATED)
        val repaintJob = scope.launch(Dispatchers.Default) {
            while (true) {
                if (repaint.receiveCatching().isClosed) break
                publishScreen(session)
                delay(REPAINT_INTERVAL_MS)
            }
            // The channel closes once the shell is done writing, so this last pass
            // renders the tail — including the exit line — after every writer.
            publishScreen(session)
        }
        var exitCode: Int? = null
        var failure: String? = null
        try {
            if (session.closed) {
                Log.w(TAG, "session ${session.id} closed before startup; skipping shell")
                return
            }
            ensureBrowserCaptureHelpers()
            ensureAgentPathProfile()
            runCatching {
                paths.tmpDir.mkdirs()
                File(paths.tmpDir, session.openUrlFile).writeText("")
            }.onFailure { Log.w(TAG, "session ${session.id}: open-url init failed", it) }
            if (session.closed) {
                Log.w(TAG, "session ${session.id} closed during startup probe; skipping shell")
                return
            }
            // An agent session runs the CLI first and leaves a usable shell behind.
            // Resolve an absolute path under the non-login probe PATH: login shells
            // (`bash -lc`) reset PATH via /etc/profile, and vendor installers often
            // skip writing rc PATH lines when our proot env already included their
            // bin dir — leaving only agents that force-wrote .bashrc (e.g. Grok).
            val agentBinary = WorkspaceAgents.get(session.agentId)?.binary
            val agentCmd = agentBinary?.let { resolveAgentCommand(it) }
            if (session.closed) {
                Log.w(TAG, "session ${session.id} closed during agent probe; skipping shell")
                return
            }
            val launch = if (agentCmd != null) "$agentCmd; exec bash -l" else "exec bash -l"
            // Geometry is taken as late as possible, and from the newest measurement
            // rather than the one this session was created with: resolving the agent
            // binary runs a probe inside the rootfs, and the viewport is normally
            // measured while that is still going. Reading it any earlier bakes the
            // 80x24 default into the PTY, and an agent that paints its banner once
            // paints it at a width the grid never had — every line wraps, and it
            // stays wrapped because that output is never redrawn.
            val cols = lastColumns.get()
            val rows = lastRows.get()
            session.resize(cols, rows)
            writeWinsize(session, columns = cols, rows = rows)
            publishScreen(session)
            // Interactive login shell rooted at the project folder. No stty here: the
            // bridge already owns the winsize, and re-asserting a captured one would
            // only overwrite a resize that landed while the shell was starting.
            val handle = executor(columns = cols, rows = rows, session = session).executeStreaming(
                command = launch,
                workingDir = "/root/projects/${session.project}",
                // Runs on the PTY reader thread: keep it to parsing, and let the
                // repaint pump do the snapshotting and publishing on its own clock.
                onOutput = { buf, n ->
                    // Always scan raw stream for URLs (OSC 8/52 may be incomplete across chunks).
                    val text = buf.decodeToString(0, n, throwOnInvalidSequence = false)
                    session.withScreen { screen ->
                        screen.noteTextUrls(text)
                        screen.writeText(text)
                    }
                    repaint.trySend(Unit)
                },
            )
            session.handle = handle
            // A close that landed while the PTY was starting must not leave a live
            // bridge behind: cancel the fresh handle at once.
            if (session.closed) {
                Log.w(TAG, "session ${session.id} closed during PTY start; cancelling handle")
                runCatching { handle.cancel() }.onFailure { Log.w(TAG, "session ${session.id}: late cancel failed", it) }
                session.handle = null
                return
            }
            session.status = com.dorai.ai.workspace.WorkspaceSessionStatus.Running
            publishSessions()
            // Poll Grok's URL capture file (and our BROWSER wrapper) while the shell runs.
            urlPollJob = scope.launch { pollOpenUrlFile(session) }
            val exit = handle.awaitExit()
            exitCode = exit
            if (exit > 0) {
                session.withScreen { it.writeText("\r\n[shell exited $exit]\r\n") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shell failed", e)
            failure = e.message ?: "Shell failed"
            session.withScreen { it.writeText("\r\n${failure}\r\n") }
        } finally {
            urlPollJob?.cancel()
            // One last read in case the URL landed as the process exited.
            ingestOpenUrlFile(session)
            session.handle = null
            // Natural exit (not a close-transit): the session stays in the strip
            // so the user sees "stopped/failed" instead of a stuck active tab.
            // A closed session was already removed from the map and is headed for
            // endSession — publishing it would resurrect a ghost, so only record
            // terminal status for sessions still in the map.
            val stillInMap = synchronized(sessionsLock) { sessions.containsKey(session.id) }
            if (!session.closed && stillInMap) {
                session.exitCode = exitCode
                session.error = failure
                session.status = com.dorai.ai.workspace.deriveSessionStatus(exitCode, failure)
                Log.i(TAG, "session ${session.id} ${session.status} exit=$exitCode error=$failure")
            }
            // Capture files are only meaningful while the bridge lives; a natural
            // exit must not leave pid/winsize/open-url orphans behind.
            deleteCaptureFiles(session)
            // Closing lets the pump run its final pass, which renders whatever the
            // blocks above wrote. Nothing writes to the screen after this point.
            repaint.close()
            if (!repaintJob.isActive) publishScreen(session)
        }
    }

    /**
     * Login shells (`bash -l` / `bash -lc`) reset PATH from `/etc/profile`, which
     * drops the proot-injected vendor dirs. Vendor installers often skip writing
     * PATH into shell rc when their bin dir is already on PATH during install —
     * so Claude (`~/.local/bin`) and OpenCode (`~/.opencode/bin`) vanish while
     * Grok (always rewrites `.bashrc`) still works.
     *
     * A tiny `/etc/profile.d` snippet puts every agent bin dir back for login
     * shells without depending on vendor rc edits. Written on the host so it
     * does not need a proot round-trip.
     */
    private fun ensureAgentPathProfile() {
        val dir = File(paths.rootfsDir, "etc/profile.d")
        dir.mkdirs()
        File(dir, "workspace-path.sh").writeText(
            """
            |# Managed by Workspace — keep coding-agent CLIs on PATH for login shells.
            |export PATH="/root/.local/bin:/root/.grok/bin:/root/.opencode/bin${'$'}{PATH:+:${'$'}PATH}"
            |
            """.trimMargin(),
        )
    }

    /**
     * Absolute path (quoted) for [binary], or the bare name if resolution fails.
     * Ensures the binary is linked into a known dir before probing.
     */
    private fun resolveAgentCommand(binary: String): String {
        // Safe: [binary] is a fixed catalog name (claude/grok/opencode).
        val probe = executor()
        probe.ensureAgentBinary(binary)
        val path = probe.execute(
            "command -v $binary 2>/dev/null",
            timeoutSeconds = 20,
        ).stdout.trim().lineSequence().firstOrNull().orEmpty()
        return if (path.startsWith("/")) "'$path'" else binary
    }

    /**
     * Installs `/usr/local/bin/dorai-browser` into the rootfs so `$BROWSER` captures
     * URLs Grok tries to open when no real display/browser exists under proot.
     * A `kai-browser` alias is kept for backward compatibility with older rootfs installs.
     * The target file is per session, passed in as `DORAI_OPEN_URL_FILE` (with a
     * `KAI_OPEN_URL_FILE` fallback for older guests).
     */
    private fun ensureBrowserCaptureHelpers() {
        val script = File(paths.rootfsDir, "usr/local/bin/dorai-browser")
        script.parentFile?.mkdirs()
        script.writeText(
            """
            |#!/bin/sh
            |# Workspace: capture browser-open URLs for the in-app link bar.
            |url="${'$'}1"
            |target="${'$'}{DORAI_OPEN_URL_FILE:-${'$'}{KAI_OPEN_URL_FILE:-/tmp/dorai-open-url}}"
            |if [ -n "${'$'}url" ]; then
            |  printf '%s\n' "${'$'}url" >> "${'$'}target"
            |fi
            |exit 0
            """.trimMargin(),
        )
        // Backward-compat alias for rootfs that still reference the old name.
        runCatching {
            val alias = File(paths.rootfsDir, "usr/local/bin/kai-browser")
            alias.parentFile?.mkdirs()
            alias.writeText(script.readText())
        }.onFailure { Log.w(TAG, "browser alias write failed", it) }
        script.setExecutable(true, false)
    }

    private suspend fun pollOpenUrlFile(session: BuildSession) {
        while (true) {
            ingestOpenUrlFile(session)
            kotlinx.coroutines.delay(250)
        }
    }

    private fun ingestOpenUrlFile(session: BuildSession) {
        val file = File(paths.tmpDir, session.openUrlFile)
        if (!file.exists() || file.length() == 0L) return
        val lines = runCatching { file.readLines() }.getOrDefault(emptyList())
        if (lines.isEmpty()) return
        // Consume the file so the same URLs are not re-applied after a TTL clear.
        runCatching { file.writeText("") }
            .onFailure { Log.w(TAG, "session ${session.id}: open-url consume failed", it) }
        var added = false
        session.withScreen { screen ->
            for (line in lines) {
                val url = line.trim()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (screen.noteHyperlink(url)) {
                        added = true
                        Log.i(TAG, "captured open-url: $url")
                    }
                }
            }
        }
        if (added) {
            session.refreshSnapshot()
            publishSessions()
            scheduleHyperlinkClear(session)
        }
    }

    /**
     * When Grok shows a device code but never emits the auth URL on the PTY
     * (confirmed via term-debug.log: zero https/OSC8), surface the known sign-in
     * page plus the code so the user can complete login on the phone browser.
     */
    private fun noteDeviceCodeFromText(session: BuildSession, text: String) {
        // Only treat as auth when the login copy is on screen. This runs on every
        // repaint, so the keyword gate comes first: `contains(ignoreCase)` scans in
        // place, where the regex and a lowercased copy of the whole screen allocate.
        if (!text.contains("approve", ignoreCase = true) &&
            !text.contains("signing in", ignoreCase = true) &&
            !text.contains("browser", ignoreCase = true)
        ) {
            return
        }
        val code = DEVICE_CODE_REGEX.find(text)?.groupValues?.getOrNull(1) ?: return
        if (code == session.lastDeviceCode) return
        session.lastDeviceCode = code
        session.withScreen { screen ->
            screen.noteHyperlink("https://accounts.x.ai/sign-in")
            // Bookmark-style deep link so the code is visible next to the URL in the bar.
            screen.noteHyperlink("https://accounts.x.ai/sign-in#code=$code")
        }
        session.refreshSnapshot()
        // TTL is scheduled by publishScreen when it sees the new links.
        Log.i(TAG, "device code detected: $code — surface https://accounts.x.ai/sign-in")
    }

    /**
     * Drop the link bar a couple of minutes after the last new URL. Login flows
     * only need the links while the user completes them; leaving them forever
     * looks like they still need to sign in.
     */
    private fun scheduleHyperlinkClear(session: BuildSession) {
        session.hyperlinkClearJob?.cancel()
        session.hyperlinkClearJob = scope.launch {
            delay(HYPERLINK_TTL_MS)
            session.withScreen { it.clearHyperlinks() }
            session.refreshSnapshot()
            publishSessions()
        }
    }

    // --- state -----------------------------------------------------------

    /**
     * Publishes in two passes. The first is file stats only, so the screen learns
     * within milliseconds that Debian is installed; the second replaces the guess
     * with the authoritative probe. Doing it the other way round costs a proot
     * process per agent plus a walk of the whole rootfs before anything is shown,
     * which is what made the setup screen flash on every open.
     */
    private fun sync(error: String? = null) {
        // Debian only: an Alpine chat sandbox cannot host the agents, and Workspace
        // then installs its own Debian somewhere else.
        val ready = paths.readMarker()?.distro == LinuxDistro.DEBIAN && File(paths.prootPath).canExecute()
        _state.update {
            it.copy(
                environment = if (ready) WorkspaceEnvironmentState.Ready else WorkspaceEnvironmentState.NotInstalled,
                installedAgents = if (ready) guessAgents() else persistentSetOf(),
                projects = scanProjects(),
                // Keep the card's numbers up while they are re-measured; an
                // uninstalled system has none to keep.
                systemInfo = if (ready) it.systemInfo else null,
                lastError = error,
            )
        }
        if (!ready) return
        val agents = detectAgents()
        _state.update { it.copy(installedAgents = agents) }
        val info = readSystemInfo()
        _state.update { it.copy(systemInfo = info) }
    }

    /**
     * Host-side guess at the installed agents: the symlink [detectAgents] leaves
     * in the guest's bin dir. The link is checked without following it — its
     * target is an absolute guest path that resolves only inside proot — so this
     * costs one stat per agent where the real probe costs a process.
     */
    private fun guessAgents(): ImmutableSet<String> = WorkspaceAgents.all
        .filter {
            val link = File(paths.rootfsDir, "root/.local/bin/${it.binary}").toPath()
            Files.exists(link, LinkOption.NOFOLLOW_LINKS)
        }
        .map { it.id }
        .toImmutableSet()

    /** Read straight off the rootfs — no proot round-trip, so it is cheap enough for every refresh. */
    private fun readSystemInfo(): WorkspaceSystemInfo {
        val pretty = runCatching {
            File(paths.rootfsDir, "etc/os-release").readLines()
                .firstOrNull { it.startsWith("PRETTY_NAME=") }
                ?.substringAfter('=')
                ?.trim('"')
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
        val version = runCatching {
            File(paths.rootfsDir, "etc/debian_version").readText().trim().takeIf { it.isNotBlank() }
        }.getOrNull()
        val packages = runCatching {
            File(paths.rootfsDir, "var/lib/dpkg/status").useLines { lines ->
                lines.count { it.startsWith("Package: ") }
            }
        }.getOrDefault(0)
        val free = runCatching { StatFs(paths.root.absolutePath).availableBytes }.getOrDefault(0L)
        return WorkspaceSystemInfo(
            distribution = pretty ?: version?.let { "Debian $it" } ?: "Debian",
            architecture = DebianSpec.arch(),
            packageCount = packages,
            systemBytes = directorySize(paths.rootfsDir) + directorySize(paths.tmpDir),
            projectsBytes = directorySize(paths.projectsDir),
            freeBytes = free,
        )
    }

    private fun setStep(step: WorkspaceStep, progress: Float? = null, agentId: String? = null) {
        _state.update {
            it.copy(environment = WorkspaceEnvironmentState.Installing(step, progress, agentId), lastError = null)
        }
    }

    private fun detectAgents(): ImmutableSet<String> {
        ensureAgentPathProfile()
        val executor = executor()
        return WorkspaceAgents.all
            .filter { executor.ensureAgentBinary(it.binary) }
            .map { it.id }
            .toImmutableSet()
    }

    /**
     * `git status` for one project, read inside the guest: the project folders are
     * ordinary host files, but only the rootfs has a git binary.
     *
     * Porcelain v1 with NUL separators — file names may contain spaces or newlines,
     * which a line-oriented parse would split into bogus paths. `-b` adds the
     * branch header so one guest round-trip yields both changes and branch.
     * Failures are distinguished: missing git, not a repository, and any other
     * error each get their own [WorkspaceGitError] instead of a silent empty list.
     */
    suspend fun gitStatus(project: String): WorkspaceGitResult = withContext(Dispatchers.IO) {
        if (!isValidProjectName(project)) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.NotRepository,
                isRepository = false,
            )
        }
        val root = "/root/projects/$project"
        val result = try {
            executor().execute(
                command = "git status --porcelain=v1 -b -z --untracked-files=all",
                timeoutSeconds = 30,
                workingDir = root,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        if (!result.success) {
            val stderr = result.stderr.ifBlank { result.stdout }
            val error = when {
                stderr.contains("not a git repository") ||
                    stderr.contains("not inside a working tree") -> WorkspaceGitError.NotRepository

                stderr.contains("git: command not found") ||
                    stderr.contains("command not found") -> WorkspaceGitError.MissingGit

                else -> WorkspaceGitError.Failed
            }
            return@withContext WorkspaceGitResult(error = error, isRepository = false)
        }
        parseGitStatusPorcelain(result.stdout, root)
    }

    /**
     * Staged (`--cached`, index vs HEAD) + unstaged (worktree vs index) diffs
     * for one file. Guest-side only; the path is validated and shell-quoted.
     */
    suspend fun gitDiff(project: String, path: String): WorkspaceGitDiff = withContext(Dispatchers.IO) {
        val relative = gitRelativePath(path, project)
        if (!isValidProjectName(project) || relative == null) {
            return@withContext WorkspaceGitDiff(path = path, error = WorkspaceGitError.Failed)
        }
        val root = "/root/projects/$project"
        val quoted = shellQuote(relative)
        val unstaged = runCatching {
            executor().execute("git diff --no-color -- $quoted", timeoutSeconds = 30, workingDir = root)
        }
        val staged = runCatching {
            executor().execute("git diff --cached --no-color -- $quoted", timeoutSeconds = 30, workingDir = root)
        }
        val unstagedResult = unstaged.getOrNull()
        val stagedResult = staged.getOrNull()
        val failed = unstagedResult == null || stagedResult == null ||
            !unstagedResult.success || !stagedResult.success
        WorkspaceGitDiff(
            path = path,
            unstagedDiff = unstagedResult?.stdout.orEmpty(),
            stagedDiff = stagedResult?.stdout.orEmpty(),
            error = if (failed) WorkspaceGitError.Failed else null,
        )
    }

    /**
     * Clones a GitHub repository into `/root/projects/<project>` (depth=1, quiet).
     * Refuses to overwrite an existing destination. The URL must be a valid GitHub.com URL.
     */
    suspend fun gitClone(project: String, url: String): WorkspaceGitResult = withContext(Dispatchers.IO) {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.NotRepository,
                isRepository = false,
            )
        }
        if (!GitUrlValidator.isValidGitHubUrl(url)) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.NotRepository,
                isRepository = false,
            )
        }
        val root = "/root/projects/$project"
        val dest = File(root)
        if (dest.exists()) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        dest.mkdirs()
        val result = try {
            executor().execute(
                command = "git clone --depth=1 --quiet $url .",
                timeoutSeconds = 120,
                workingDir = root,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        if (!result.success) {
            WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        } else {
            WorkspaceGitResult(isRepository = true)
        }
    }

    /**
     * Reads `git log --oneline` for `projects/<project>`, up to [maxCount] entries.
     */
    suspend fun gitLog(project: String, maxCount: Int): List<WorkspaceGitLogEntry> = withContext(Dispatchers.IO) {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return@withContext emptyList()
        }
        val root = "/root/projects/$project"
        val result = try {
            executor().execute(
                command = "git log --oneline -$maxCount",
                timeoutSeconds = 30,
                workingDir = root,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withContext emptyList()
        }
        if (!result.success) {
            emptyList()
        } else {
            parseGitLogOneline(result.stdout as? String ?: "")
        }
    }

    /**
     * Fetches from origin for `projects/<project>` (read-only).
     */
    suspend fun gitFetch(project: String): WorkspaceGitResult = withContext(Dispatchers.IO) {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.NotRepository,
                isRepository = false,
            )
        }
        val root = "/root/projects/$project"
        val result = try {
            executor().execute(
                command = "git fetch --quiet --prune",
                timeoutSeconds = 120,
                workingDir = root,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        if (!result.success) {
            WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        } else {
            WorkspaceGitResult(isRepository = true)
        }
    }

    /**
     * Pulls from origin with --ff-only for `projects/<project>`.
     */
    suspend fun gitPullFfOnly(project: String): WorkspaceGitResult = withContext(Dispatchers.IO) {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.NotRepository,
                isRepository = false,
            )
        }
        val root = "/root/projects/$project"
        val result = try {
            executor().execute(
                command = "git pull --ff-only --quiet",
                timeoutSeconds = 120,
                workingDir = root,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        if (!result.success) {
            WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        } else {
            WorkspaceGitResult(isRepository = true)
        }
    }

    /**
     * Stages the explicit relative [files] (`git add -- <files>`) and creates a commit with
     * [message]. The message is single-line, non-blank, ≤ 200 chars. Explicit paths only —
     * never `.`/`:/`, never flags. Refused unless [files] are safe relative paths and the
     * message is valid, regardless of the write opt-in on the caller side.
     */
    suspend fun gitCommit(project: String, files: List<String>, message: String): WorkspaceGitResult = withContext(Dispatchers.IO) {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.NotRepository,
                isRepository = false,
            )
        }
        if (files.isEmpty() || files.any { !GitUrlValidator.isSafeRelativePath(it) }) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        val trimmed = message.trim()
        if (trimmed.isEmpty() || trimmed.length > 200 ||
            trimmed.any { it == '\n' || it == '\r' || it == '\u0000' }
        ) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        val root = "/root/projects/$project"
        val quotedPaths = files.joinToString(" ") { shellQuote(it) }

        val add = try {
            executor().execute(
                command = "git add -- $quotedPaths",
                timeoutSeconds = 60,
                workingDir = root,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        if (!add.success) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }

        val commit = try {
            executor().execute(
                command = "git commit -m ${shellQuote(trimmed)}",
                timeoutSeconds = 60,
                workingDir = root,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        if (!commit.success) {
            WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        } else {
            WorkspaceGitResult(isRepository = true)
        }
    }

    /**
     * `git push origin HEAD` — no force flags, no custom remote or refspec.
     * Anonymous write access does not exist: private pushes require the credential
     * broker, which is only wired for the desktop runner today (see [GitCredentialBroker]).
     */
    suspend fun gitPush(project: String): WorkspaceGitResult = withContext(Dispatchers.IO) {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.NotRepository,
                isRepository = false,
            )
        }
        val root = "/root/projects/$project"
        val result = try {
            executor().execute(
                command = "git push origin HEAD",
                timeoutSeconds = 300,
                workingDir = root,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return@withContext WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        }
        if (!result.success) {
            WorkspaceGitResult(
                error = WorkspaceGitError.Failed,
                isRepository = false,
            )
        } else {
            WorkspaceGitResult(isRepository = true)
        }
    }

    /** Stages one file (`git add -- <path>`). Never stages anything else. */
    suspend fun gitStage(project: String, path: String): Boolean = gitAction(project, path) { root, quoted ->
        "git add -- $quoted"
    }

    /** Unstages one file (`git restore --staged -- <path>`). */
    suspend fun gitUnstage(project: String, path: String): Boolean = gitAction(project, path) { root, quoted ->
        "git restore --staged -- $quoted"
    }

    /**
     * Discards staged + unstaged changes for one file, guest-side only — never
     * via the host file browser's recursive delete, which would follow symlinks
     * the codebase deliberately treats as unsafe. Untracked files fall back to
     * `git clean -f` (no `-d`, so directories are never removed recursively).
     */
    suspend fun gitDiscard(project: String, path: String): Boolean = withContext(Dispatchers.IO) {
        val relative = gitRelativePath(path, project)
        if (!isValidProjectName(project) || relative == null) return@withContext false
        val root = "/root/projects/$project"
        val quoted = shellQuote(relative)
        val restore = runCatching {
            executor().execute(
                "git restore --staged --worktree -- $quoted",
                timeoutSeconds = 30,
                workingDir = root,
            )
        }.getOrNull()
        if (restore != null && restore.success) return@withContext true
        val clean = runCatching {
            executor().execute(
                "git clean -f -- $quoted",
                timeoutSeconds = 30,
                workingDir = root,
            )
        }.getOrNull()
        clean?.success == true
    }

    private suspend fun gitAction(
        project: String,
        path: String,
        command: (root: String, quoted: String) -> String,
    ): Boolean = withContext(Dispatchers.IO) {
        val relative = gitRelativePath(path, project)
        if (!isValidProjectName(project) || relative == null) return@withContext false
        val root = "/root/projects/$project"
        runCatching {
            executor().execute(
                command = command(root, shellQuote(relative)),
                timeoutSeconds = 30,
                workingDir = root,
            ).success
        }.getOrDefault(false)
    }

    private fun scanProjects() = paths.projectsDir.listFiles()
        .orEmpty()
        .filter { it.isDirectory }
        .map { it.name }
        .sorted()
        .toImmutableList()

    private fun executor(
        columns: Int = lastColumns.get(),
        rows: Int = lastRows.get(),
        session: BuildSession? = null,
    ): WorkspaceProotExecutor {
        paths.copyLibtalloc()
        paths.ensureLayout()
        paths.ensureMountPoints()
        val openUrlPath = "/tmp/${session?.openUrlFile ?: "kai-open-url"}"
        return WorkspaceProotExecutor(
            launcher = ProotLauncher(
                prootPath = paths.prootPath,
                libDir = paths.libDir,
                rootfsPath = paths.rootfsDir.absolutePath,
                tmpPath = paths.tmpDir.absolutePath,
                // /root itself stays on the rootfs so agent binaries under the
                // vendor install dirs are executable — external storage is often
                // mounted noexec.
                binds = listOf(paths.projectsDir.absolutePath to "/root/projects"),
                extraArgs = DebianSpec.prootArgs,
                env = DebianSpec.env + agentEnv(columns, rows, openUrlPath),
            ),
            tmpPath = paths.tmpDir.absolutePath,
            columns = columns,
            rows = rows,
            winsizePath = "/tmp/${session?.winsizeFile ?: "kai-winsize"}",
            pidFileName = session?.pidFile ?: "dorai-pid",
        )
    }

    private fun agentEnv(columns: Int, rows: Int, openUrlPath: String) = mapOf(
        "USER" to "root",
        // Vendor installers: Claude → ~/.local/bin, Grok → ~/.grok/bin, OpenCode → ~/.opencode/bin.
        // Keep every agent dir on PATH; shell rc files are not sourced for non-login probes.
        "PATH" to "/root/.local/bin:/root/.grok/bin:/root/.opencode/bin:$DEFAULT_GUEST_PATH",
        "COLORTERM" to "truecolor",
        "COLUMNS" to columns.toString(),
        "LINES" to rows.toString(),
        // Grok opens the login URL via webbrowser; under proot there is no real browser.
        // GROK_TEST_OPEN_URL_FILE is an official Grok hook that writes the URL to a file.
        "GROK_TEST_OPEN_URL_FILE" to openUrlPath,
        // Same target for the dorai-browser wrapper (and its kai-browser alias), so both hooks feed one session's link bar.
        "DORAI_OPEN_URL_FILE" to openUrlPath,
        "KAI_OPEN_URL_FILE" to openUrlPath,
        // webbrowser crate falls back to ${'$'}BROWSER when xdg-open is missing.
        "BROWSER" to "/usr/local/bin/dorai-browser",
    )
}

/** Project folder names are a single path segment — reject traversal before any guest call. */
private fun isValidProjectName(project: String): Boolean = GitUrlValidator.isValidProjectName(project)

private fun WorkspaceProotExecutor.hasBinary(binary: String): Boolean = execute("command -v $binary >/dev/null 2>&1", timeoutSeconds = 20).success

/**
 * True when [binary] is reachable. Always tries to place a stable symlink in
 * `/root/.local/bin` (even when already on the probe PATH) so login shells that
 * only keep a subset of vendor dirs still find every agent by name.
 *
 * Known layouts:
 * - Claude: `~/.local/bin/claude` → `~/.local/share/claude/versions/<ver>`
 * - Grok: `~/.grok/bin/grok`
 * - OpenCode: `~/.opencode/bin/opencode`
 */
private fun WorkspaceProotExecutor.ensureAgentBinary(binary: String): Boolean {
    // Safe: [binary] is a fixed catalog name (claude/grok/opencode), not user input.
    execute(
        """
        set -e
        bin='$binary'
        dest="/root/.local/bin/${'$'}bin"
        mkdir -p /root/.local/bin
        # Prefer a real file under .local/bin; otherwise link from vendor dirs.
        if [ -x "${'$'}dest" ] && [ ! -d "${'$'}dest" ]; then
            exit 0
        fi
        for candidate in \
            "/root/.grok/bin/${'$'}bin" \
            "/root/.opencode/bin/${'$'}bin" \
            "/usr/local/bin/${'$'}bin"
        do
            if [ -x "${'$'}candidate" ] && [ ! -d "${'$'}candidate" ]; then
                ln -sfn "${'$'}candidate" "${'$'}dest"
                exit 0
            fi
        done
        # Claude install can leave only the versioned binary under share/.
        if [ "${'$'}bin" = "claude" ]; then
            ver_dir="/root/.local/share/claude/versions"
            if [ -d "${'$'}ver_dir" ]; then
                latest="${'$'}(ls -1 "${'$'}ver_dir" 2>/dev/null | sort -V | tail -1)"
                if [ -n "${'$'}latest" ] && [ -x "${'$'}ver_dir/${'$'}latest" ]; then
                    ln -sfn "${'$'}ver_dir/${'$'}latest" "${'$'}dest"
                    exit 0
                fi
            fi
        fi
        exit 0
        """.trimIndent(),
        timeoutSeconds = 30,
    )
    return hasBinary(binary)
}

/**
 * Total size of the regular files under [dir], without following symlinks — an
 * LXC rootfs contains link loops that would trap a naive recursive walk.
 */
private fun directorySize(dir: File): Long {
    if (!dir.exists()) return 0L
    var total = 0L
    runCatching {
        Files.walkFileTree(
            dir.toPath(),
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (attrs.isRegularFile) total += attrs.size()
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
            },
        )
    }
    return total
}
