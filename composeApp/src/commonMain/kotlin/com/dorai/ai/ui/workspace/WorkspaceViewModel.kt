package com.dorai.ai.ui.workspace

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dorai.ai.WorkspaceController
import com.dorai.ai.data.DataRepository
import com.dorai.ai.workspace.WorkspaceGitChange
import com.dorai.ai.workspace.WorkspaceGitDiff
import com.dorai.ai.workspace.WorkspaceGitError
import com.dorai.ai.workspace.WorkspaceGitResult
import com.dorai.ai.workspace.WorkspaceState
import com.dorai.ai.workspace.terminal.TerminalKey
import com.dorai.ai.workspace.terminal.TerminalKeyEncoder
import com.dorai.ai.workspace.terminal.TerminalModifiers
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class WorkspaceUiState(
    val build: WorkspaceState = WorkspaceState(),
    /** Agents ticked on the setup screen, before Debian exists. */
    val selectedAgents: ImmutableSet<String> = persistentSetOf(),
    /** Agent a project opens with — null means a plain shell. */
    val launchAgentId: String? = null,
    /** Non-null while the user is inside a project's terminal. */
    val openProject: String? = null,
)

/**
 * Source-control view of the open project. [project] is the folder [changes] belong
 * to; it changes as soon as another project is opened, so a slow `git status` can
 * never paint the previous project's files over the new one.
 */
@Immutable
data class WorkspaceGitState(
    val project: String? = null,
    val loading: Boolean = false,
    val changes: List<WorkspaceGitChange> = emptyList(),
    /** True once a status has come back for this project, even if the repo is clean. */
    val loaded: Boolean = false,
    /** Current branch from `git status -b`; null when detached, unborn, or unread. */
    val branch: String? = null,
    /** Why the last read failed; null means it succeeded (clean or dirty). */
    val error: WorkspaceGitError? = null,
    /** False when the folder is not a repository — distinct from "clean". */
    val isRepository: Boolean = true,
)

class WorkspaceViewModel(
    private val controller: WorkspaceController,
    private val dataRepository: DataRepository,
) : ViewModel() {

    private val selectedAgents = MutableStateFlow<ImmutableSet<String>>(persistentSetOf())
    private val launchAgentId = MutableStateFlow(dataRepository.getWorkspaceLaunchAgent())
    private val openProject = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WorkspaceUiState> = combine(
        controller.state,
        selectedAgents,
        launchAgentId,
        openProject,
    ) { build, selected, launchAgent, project ->
        WorkspaceUiState(
            build = build,
            selectedAgents = selected,
            launchAgentId = launchAgent.installedIn(build),
            openProject = project,
        )
        // viewModelScope is Main.immediate, so without this the merge runs on the
        // main thread once per terminal repaint.
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkspaceUiState(
            build = controller.state.value,
            launchAgentId = launchAgentId.value.installedIn(controller.state.value),
        ),
    )

    private val _gitState = MutableStateFlow(WorkspaceGitState())
    val gitState: StateFlow<WorkspaceGitState> = _gitState.asStateFlow()

    private val _gitDiff = MutableStateFlow<WorkspaceGitDiff?>(null)
    val gitDiff: StateFlow<WorkspaceGitDiff?> = _gitDiff.asStateFlow()

    private val _gitActionInProgress = MutableStateFlow(false)
    val gitActionInProgress: StateFlow<Boolean> = _gitActionInProgress.asStateFlow()

    init {
        controller.refresh()
    }

    /**
     * Re-reads `git status` for [project]. Called when the Git tab is opened — the
     * agent edits files through a shell, so any cached answer goes stale at once.
     */
    fun refreshGitStatus(project: String) {
        if (_gitState.value.project != project) {
            _gitState.value = WorkspaceGitState(project = project, loading = true)
        } else {
            _gitState.update { it.copy(loading = true) }
        }
        viewModelScope.launch {
            // The status runs a guest process; keep it off the main dispatcher.
            val result = withContext(Dispatchers.Default) {
                try {
                    controller.gitStatus(project)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = false)
                }
            }
            _gitState.update {
                if (it.project != project) {
                    it
                } else {
                    it.copy(
                        changes = result.changes,
                        branch = result.branch,
                        error = result.error,
                        isRepository = result.isRepository,
                        loading = false,
                        loaded = true,
                    )
                }
            }
        }
    }

    /** Loads staged + unstaged diffs for one changed file. */
    fun loadGitDiff(project: String, path: String) {
        viewModelScope.launch {
            val diff = withContext(Dispatchers.Default) {
                try {
                    controller.gitDiff(project, path)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    WorkspaceGitDiff(path = path, error = WorkspaceGitError.Failed)
                }
            }
            // Only the latest requested file's diff is shown — a slow read for a
            // previous file must not repaint a newer selection.
            if (_gitDiff.value?.path != path) {
                _gitDiff.value = diff
            } else {
                _gitDiff.value = diff
            }
        }
    }

    /** Clears the currently shown diff (dialog dismissed). */
    fun clearGitDiff() {
        _gitDiff.value = null
    }

    /** Stages one file, then refreshes the status list. */
    fun stageGitFile(project: String, path: String) {
        runGitAction(project) { controller.gitStage(project, path) }
    }

    /** Unstages one file, then refreshes the status list. */
    fun unstageGitFile(project: String, path: String) {
        runGitAction(project) { controller.gitUnstage(project, path) }
    }

    /** Discards one file's changes, then refreshes the status list. */
    fun discardGitFile(project: String, path: String) {
        runGitAction(project) { controller.gitDiscard(project, path) }
    }

    private fun runGitAction(project: String, action: suspend () -> Boolean) {
        if (_gitActionInProgress.value) return
        _gitActionInProgress.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { action() }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // A failed action leaves the tree untouched; the refreshed
                // status below is still the correct UI state.
            } finally {
                _gitActionInProgress.value = false
            }
            refreshGitStatus(project)
        }
    }

    /**
     * Drops a remembered agent that the environment no longer has — the choice
     * outlives an uninstall, and starting a session on a binary that is gone
     * would only print "command not found".
     */
    private fun String?.installedIn(build: WorkspaceState): String? = this?.takeIf { it in build.installedAgents }

    fun toggleAgent(id: String) {
        val current = selectedAgents.value
        selectedAgents.value = if (id in current) {
            (current - id).toImmutableSet()
        } else {
            (current + id).toImmutableSet()
        }
    }

    fun install() = controller.install(selectedAgents.value)

    /** Installs a single agent into an environment that is already set up. */
    fun installAgent(id: String) {
        selectedAgents.value = (selectedAgents.value + id).toImmutableSet()
        controller.install(setOf(id))
    }

    fun cancel() = controller.cancel()
    fun uninstall() = controller.uninstall()

    /**
     * Picks what the next opened project starts with; null is a plain shell.
     * Remembered across app runs — the row is a preference, not a per-visit pick.
     */
    fun setLaunchAgent(agentId: String?) {
        launchAgentId.value = agentId
        dataRepository.setWorkspaceLaunchAgent(agentId)
    }

    /** Opens the project on the shells it already had, or starts its first one. */
    fun openProject(name: String) {
        openProject.value = name
        if (!controller.resumeProject(name)) {
            controller.startSession(name, launchAgentId.value.installedIn(controller.state.value))
        }
    }

    /** Steps back to the list; the project's shells keep running until closed. */
    fun closeProject() {
        openProject.value?.let(controller::leaveProject)
        openProject.value = null
    }

    fun createProject(name: String) {
        controller.createProject(name)?.let(::openProject)
    }

    /** Both are offered from the list only, so neither can pull the ground from under an open project. */
    fun deleteProject(name: String) = controller.deleteProject(name)

    fun renameProject(name: String, newName: String) {
        controller.renameProject(name, newName)
    }

    /** Opens another session in the project the user is already in. */
    fun startSession(agentId: String?) {
        val project = openProject.value ?: return
        controller.startSession(project, agentId)
    }

    fun selectSession(id: String) = controller.selectSession(id)

    /** Closes one tab; closing the last one of the project steps back to the list. */
    fun closeSession(id: String) {
        controller.closeSession(id)
        val project = openProject.value ?: return
        if (controller.state.value.sessions.none { it.project == project }) {
            openProject.value = null
        }
    }

    /**
     * Replaces a stopped/failed session with a fresh shell in its place.
     * Starts the replacement first so the project never looks empty: closing
     * the old tab first would step the user back to the project list.
     */
    fun restartSession(id: String) {
        val project = openProject.value ?: return
        val old = controller.state.value.sessions.firstOrNull { it.id == id && it.project == project } ?: return
        controller.startSession(project, old.agentId)
        controller.closeSession(id)
        if (controller.state.value.sessions.none { it.project == project }) {
            openProject.value = null
        }
    }

    /**
     * Ends every session and releases the environment. Not called on ViewModel
     * clear — the manager is process-scoped and leaving the Workspace screen
     * must keep sessions alive for resume.
     */
    fun closeEnvironment() = controller.close()

    /** Submits a finished input line (caller includes the trailing carriage return). */
    fun submitLine(text: String) {
        if (openProject.value == null) return
        controller.writeToTerminal(text)
    }

    /** Sends one key press from the key row (arrows, Esc, Ctrl+C, Shift+Tab, …). */
    fun sendKey(key: TerminalKey, modifiers: TerminalModifiers) {
        if (openProject.value == null) return
        val encoded = TerminalKeyEncoder.encode(
            key = key,
            modifiers = modifiers,
            applicationCursorKeys = controller.state.value.activeSession?.terminal?.applicationCursorKeys ?: false,
        )
        controller.writeToTerminal(encoded)
    }

    /** Sends characters typed on the soft or hardware keyboard, one press at a time. */
    fun sendText(text: String, modifiers: TerminalModifiers) {
        if (openProject.value == null || text.isEmpty()) return
        controller.writeToTerminal(TerminalKeyEncoder.encodeText(text, modifiers))
    }

    /**
     * Sends a mouse report for a touch on the cell grid. Already encoded by the
     * grid, which is the only place that knows which cell was under the finger.
     */
    fun sendMouse(report: String) {
        if (openProject.value == null || report.isEmpty()) return
        controller.writeToTerminal(report)
    }

    fun resizeTerminal(columns: Int, rows: Int) = controller.resizeTerminal(columns, rows)
}
