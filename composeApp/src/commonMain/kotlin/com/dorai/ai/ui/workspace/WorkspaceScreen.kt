package com.dorai.ai.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.PlatformBackHandler
import com.dorai.ai.WORKSPACE_FILES
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.sandbox.EditorState
import com.dorai.ai.ui.sandbox.SandboxFileBrowserViewModel
import com.dorai.ai.ui.sandbox.SandboxFilesContent
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.ui.workspace.WorkspaceSpacing
import com.dorai.ai.workspace.WorkspaceAgents
import com.dorai.ai.workspace.WorkspaceGitStatus
import com.dorai.ai.workspace.WorkspaceSessionStatus
import com.dorai.ai.workspace.activeSessionForProject
import com.dorai.ai.workspace.terminal.TerminalKey
import com.dorai.ai.workspace.terminal.TerminalModifiers
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.image_viewer_close
import dorai.composeapp.generated.resources.sandbox_files_dialog_cancel
import dorai.composeapp.generated.resources.sandbox_files_save_failed
import dorai.composeapp.generated.resources.workspace_default_session_label
import dorai.composeapp.generated.resources.workspace_editor_binary
import dorai.composeapp.generated.resources.workspace_editor_empty
import dorai.composeapp.generated.resources.workspace_editor_too_large
import dorai.composeapp.generated.resources.workspace_editor_unreadable
import dorai.composeapp.generated.resources.workspace_exit_content_description
import dorai.composeapp.generated.resources.workspace_files_tab
import dorai.composeapp.generated.resources.workspace_git_branch
import dorai.composeapp.generated.resources.workspace_git_changes_title
import dorai.composeapp.generated.resources.workspace_git_diff
import dorai.composeapp.generated.resources.workspace_git_diff_error
import dorai.composeapp.generated.resources.workspace_git_diff_title
import dorai.composeapp.generated.resources.workspace_git_discard
import dorai.composeapp.generated.resources.workspace_git_discard_message
import dorai.composeapp.generated.resources.workspace_git_discard_title
import dorai.composeapp.generated.resources.workspace_git_failed
import dorai.composeapp.generated.resources.workspace_git_loading
import dorai.composeapp.generated.resources.workspace_git_missing_git
import dorai.composeapp.generated.resources.workspace_git_no_changes
import dorai.composeapp.generated.resources.workspace_git_no_project
import dorai.composeapp.generated.resources.workspace_git_not_repository
import dorai.composeapp.generated.resources.workspace_git_stage
import dorai.composeapp.generated.resources.workspace_git_staged
import dorai.composeapp.generated.resources.workspace_git_status_added
import dorai.composeapp.generated.resources.workspace_git_status_conflicted
import dorai.composeapp.generated.resources.workspace_git_status_deleted
import dorai.composeapp.generated.resources.workspace_git_status_modified
import dorai.composeapp.generated.resources.workspace_git_status_renamed
import dorai.composeapp.generated.resources.workspace_git_status_untracked
import dorai.composeapp.generated.resources.workspace_git_unstage
import dorai.composeapp.generated.resources.workspace_git_unstaged
import dorai.composeapp.generated.resources.workspace_loading_file
import dorai.composeapp.generated.resources.workspace_new_project_content_description
import dorai.composeapp.generated.resources.workspace_no_active_session
import dorai.composeapp.generated.resources.workspace_open_drawer_content_description
import dorai.composeapp.generated.resources.workspace_open_project_terminal
import dorai.composeapp.generated.resources.workspace_save_dismiss
import dorai.composeapp.generated.resources.workspace_starting_session
import dorai.composeapp.generated.resources.workspace_tab_agent
import dorai.composeapp.generated.resources.workspace_tab_editor
import dorai.composeapp.generated.resources.workspace_tab_git
import dorai.composeapp.generated.resources.workspace_tab_terminal
import dorai.composeapp.generated.resources.workspace_title
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Where Debian mounts the project folders â€” the browser's root inside a project. */
private const val PROJECTS_GUEST_DIR = "/root/projects"

/** Keeps Workspace's browser out of the chat sandbox's ViewModel slot. */
private const val WORKSPACE_FILES_KEY = "WorkspaceFiles"

/** Separate browser instance for the projects drawer explorer. */
private const val WORKSPACE_FILES_DRAWER_KEY = "WorkspaceDrawerFiles"

/** One changed file shown in the Git tab's changes list. */
@Immutable
data class GitFileChange(
    val path: String,
    val status: WorkspaceGitStatus,
    /** True when the change is staged (index column of the porcelain XY pair). */
    val staged: Boolean = false,
    /** True when the change is unstaged (worktree column). Untracked paths have neither. */
    val unstaged: Boolean = false,
)

@Immutable
data class WorkspaceActions(
    val toggleAgent: (String) -> Unit,
    val install: () -> Unit,
    val installAgent: (String) -> Unit,
    val cancel: () -> Unit,
    val uninstall: () -> Unit,
    val setLaunchAgent: (String?) -> Unit,
    val openProject: (String) -> Unit,
    val createProject: (String) -> Unit,
    val deleteProject: (String) -> Unit,
    val renameProject: (name: String, newName: String) -> Unit,
    val closeProject: () -> Unit,
    val startSession: (String?) -> Unit,
    val selectSession: (String) -> Unit,
    val closeSession: (String) -> Unit,
    val submitLine: (String) -> Unit,
    val sendKey: (TerminalKey, TerminalModifiers) -> Unit,
    val sendText: (String, TerminalModifiers) -> Unit,
    val sendMouse: (String) -> Unit,
    val resizeTerminal: (columns: Int, rows: Int) -> Unit,
    val openFile: (String) -> Unit,
)

/** The four bottom-navigation tabs of the Workspace. */
enum class WorkspaceTab(
    val id: String,
    val label: org.jetbrains.compose.resources.StringResource,
    val icon: ImageVector,
) {
    Agent("agent", Res.string.workspace_tab_agent, Icons.AutoMirrored.Filled.Comment),
    Editor("editor", Res.string.workspace_tab_editor, Icons.Default.Code),
    Terminal("terminal", Res.string.workspace_tab_terminal, Icons.Default.Terminal),
    Git("git", Res.string.workspace_tab_git, Icons.Default.History),
}

/** Stateless Workspace surface content for screenshot tests. Takes [state] and
 * [actions] directly so it can be rendered without a ViewModel or Koin.
 */
@Composable
fun WorkspaceScreenContent(
    state: WorkspaceUiState,
    actions: WorkspaceActions,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: WorkspaceTab = WorkspaceTab.Agent,
    initialEditorState: EditorState? = null,
    /** Screenshot/preview surface renders whatever git state it is handed. */
    gitState: WorkspaceGitState = WorkspaceGitState(),
    updateEditorContent: (String) -> Unit = { },
    saveEditorContent: () -> Unit = { },
    undoEditorContent: () -> Unit = { },
    canUndoEditor: Boolean = false,
    editorSaving: Boolean = false,
    saveMessage: org.jetbrains.compose.resources.StringResource? = null,
    onConsumeSaveMessage: () -> Unit = { },
    gitDiff: com.dorai.ai.workspace.WorkspaceGitDiff? = null,
    onLoadGitDiff: (String, String) -> Unit = { _, _ -> },
    onClearGitDiff: () -> Unit = { },
    onStageGitFile: (String, String) -> Unit = { _, _ -> },
    onUnstageGitFile: (String, String) -> Unit = { _, _ -> },
    onDiscardGitFile: (String, String) -> Unit = { _, _ -> },
    gitActionInProgress: Boolean = false,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var tab by remember(initialTab) { mutableStateOf(initialTab) }
    var pendingProject by remember { mutableStateOf<String?>(null) }
    var filesOpen by remember { mutableStateOf(false) }
    var selectedFile by remember(initialEditorState) {
        mutableStateOf((initialEditorState as? EditorState.Loaded)?.path)
    }

    val drawerFiles = remember(selectedFile) {
        state.build.projects.map { name ->
            ProjectFile(
                path = "$PROJECTS_GUEST_DIR/$name",
                name = name,
                isDirectory = true,
                isCurrentFile = false,
            )
        }
    }

    LaunchedEffect(state.build) {
        val pending = pendingProject ?: return@LaunchedEffect
        if (state.build.isReady) {
            if (pending in state.build.projects) {
                actions.openProject(pending)
            }
            pendingProject = null
        }
    }

    val editorLines = (initialEditorState as? EditorState.Loaded)?.let { highlightSource(it.current) }.orEmpty()

    PlatformBackHandler(enabled = true) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            filesOpen -> filesOpen = false
            state.openProject != null -> actions.closeProject()
            else -> onExit()
        }
    }

    // Drawer content: Projects & File Explorer
    val drawerContent = @Composable {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            WorkspaceProjectsExplorer(
                currentProject = state.openProject,
                projects = state.build.projects,
                files = drawerFiles,
                onOpenProject = { name ->
                    scope.launch { drawerState.close() }
                    filesOpen = false
                    actions.openProject(name)
                },
                onCreateProject = { name ->
                    actions.createProject(name)
                    scope.launch { drawerState.close() }
                },
                onOpenFile = { path ->
                    scope.launch { drawerState.close() }
                    selectedFile = path
                    tab = WorkspaceTab.Editor
                    filesOpen = false
                },
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = drawerContent,
            modifier = Modifier.fillMaxHeight(),
        ) {
            if (state.build.isReady) {
                WorkspaceTabContent(
                    tab = tab,
                    onTabChanged = { tab = it },
                    state = state,
                    actions = actions,
                    filesOpen = filesOpen,
                    onFilesToggle = { filesOpen = it },
                    selectedFile = selectedFile,
                    editorState = initialEditorState,
                    editorLines = editorLines,
                    gitState = gitState,
                    onOpenEditorFile = { path ->
                        selectedFile = path
                        tab = WorkspaceTab.Editor
                    },
                    onDrawerToggle = { scope.launch { drawerState.open() } },
                    onExit = onExit,
                    updateEditorContent = updateEditorContent,
                    saveEditorContent = saveEditorContent,
                    undoEditorContent = undoEditorContent,
                    canUndoEditor = canUndoEditor,
                    editorSaving = editorSaving,
                    saveMessage = saveMessage,
                    onConsumeSaveMessage = onConsumeSaveMessage,
                    gitDiff = gitDiff,
                    onLoadGitDiff = onLoadGitDiff,
                    onClearGitDiff = onClearGitDiff,
                    onStageGitFile = onStageGitFile,
                    onUnstageGitFile = onUnstageGitFile,
                    onDiscardGitFile = onDiscardGitFile,
                    gitActionInProgress = gitActionInProgress,
                )
            } else {
                WorkspaceSetupContent(
                    state = state.build,
                    selectedAgents = state.selectedAgents,
                    onToggleAgent = actions.toggleAgent,
                    onInstall = actions.install,
                    onCancel = actions.cancel,
                )
            }
        }
    }
}

/** Full-screen Workspace surface. Entered from the empty chat state and left via
 * the top-bar close button or system back.
 */
@Composable
fun WorkspaceScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    initialProject: String? = null,
    viewModel: WorkspaceViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gitState by viewModel.gitState.collectAsStateWithLifecycle()
    val gitDiff by viewModel.gitDiff.collectAsStateWithLifecycle()
    val gitActionInProgress by viewModel.gitActionInProgress.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        WorkspaceActions(
            toggleAgent = viewModel::toggleAgent,
            install = viewModel::install,
            installAgent = viewModel::installAgent,
            cancel = viewModel::cancel,
            uninstall = viewModel::uninstall,
            setLaunchAgent = viewModel::setLaunchAgent,
            openProject = viewModel::openProject,
            createProject = viewModel::createProject,
            deleteProject = viewModel::deleteProject,
            renameProject = viewModel::renameProject,
            closeProject = viewModel::closeProject,
            startSession = viewModel::startSession,
            selectSession = viewModel::selectSession,
            closeSession = viewModel::closeSession,
            submitLine = viewModel::submitLine,
            sendKey = viewModel::sendKey,
            sendText = viewModel::sendText,
            sendMouse = viewModel::sendMouse,
            resizeTerminal = viewModel::resizeTerminal,
            openFile = { path -> },
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(WorkspaceTab.Agent) }
    var pendingProject by rememberSaveable(initialProject) { mutableStateOf(initialProject) }
    var filesOpen by rememberSaveable { mutableStateOf(false) }
    var selectedFile by rememberSaveable { mutableStateOf<String?>(null) }

    // Drawer explorer keeps its own browser instance so paging through the tree
    // never fights the Agent tab's file browser over the same directory.
    val drawerFilesViewModel: SandboxFileBrowserViewModel = koinViewModel(
        qualifier = WORKSPACE_FILES,
        key = WORKSPACE_FILES_DRAWER_KEY,
    )
    val drawerFilesState by drawerFilesViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.openProject) {
        val project = state.openProject ?: return@LaunchedEffect
        drawerFilesViewModel.start("$PROJECTS_GUEST_DIR/$project")
        viewModel.refreshGitStatus(project)
    }

    // Badges come from the same `git status` the Git tab shows, keyed by path, so the
    // explorer and the changes list never disagree.
    val drawerFiles = remember(drawerFilesState.entries, selectedFile, gitState.changes) {
        val statusByPath = gitState.changes.associate { it.path to it.status }
        drawerFilesState.entries.map { entry ->
            ProjectFile(
                path = entry.path,
                name = entry.name,
                isDirectory = entry.isDirectory,
                gitStatus = statusByPath[entry.path],
                isCurrentFile = entry.path == selectedFile,
            )
        }
    }

    LaunchedEffect(state.build) {
        val pending = pendingProject ?: return@LaunchedEffect
        if (state.build.isReady) {
            if (pending in state.build.projects) {
                viewModel.openProject(pending)
            }
            pendingProject = null
        }
    }

    val editorLines = remember(drawerFilesState.editor) {
        (drawerFilesState.editor as? EditorState.Loaded)?.let { highlightSource(it.current) }.orEmpty()
    }

    PlatformBackHandler(enabled = true) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            filesOpen -> filesOpen = false
            state.openProject != null -> actions.closeProject()
            else -> onExit()
        }
    }

    // Drawer content: Projects & File Explorer
    val drawerContent = @Composable {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            WorkspaceProjectsExplorer(
                currentProject = state.openProject,
                projects = state.build.projects,
                files = drawerFiles,
                onOpenProject = { name ->
                    scope.launch { drawerState.close() }
                    filesOpen = false
                    actions.openProject(name)
                },
                onCreateProject = { name ->
                    actions.createProject(name)
                    scope.launch { drawerState.close() }
                },
                onOpenFile = { path ->
                    val entry = drawerFilesState.entries.firstOrNull { it.path == path }
                    if (entry?.isDirectory == true) {
                        drawerFilesViewModel.navigateTo(path)
                    } else {
                        scope.launch { drawerState.close() }
                        drawerFilesViewModel.openInEditor(path)
                        selectedFile = path
                        tab = WorkspaceTab.Editor
                        filesOpen = false
                    }
                },
                isLoading = drawerFilesState.loading,
                error = drawerFilesState.error,
                onRefresh = {
                    // Silent re-list of the visible directory; keeps any open
                    // editor buffer (unlike navigateTo, which clears it).
                    drawerFilesViewModel.start(drawerFilesState.currentPath)
                },
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = drawerContent,
            modifier = Modifier.fillMaxHeight(),
        ) {
            if (state.build.isReady) {
                WorkspaceTabContent(
                    tab = tab,
                    onTabChanged = { next ->
                        tab = next
                        // The agent edits through a shell, so a visit to the Git tab asks
                        // the guest again rather than reusing the open-project listing.
                        if (next == WorkspaceTab.Git) state.openProject?.let(viewModel::refreshGitStatus)
                    },
                    state = state,
                    actions = actions,
                    filesOpen = filesOpen,
                    onFilesToggle = { filesOpen = it },
                    selectedFile = selectedFile,
                    editorState = drawerFilesState.editor,
                    editorLines = editorLines,
                    gitState = gitState,
                    onOpenEditorFile = { path ->
                        drawerFilesViewModel.openInEditor(path)
                        selectedFile = path
                        tab = WorkspaceTab.Editor
                    },
                    onDrawerToggle = { scope.launch { drawerState.open() } },
                    onExit = onExit,
                    updateEditorContent = { content ->
                        drawerFilesViewModel.updateEditorContent(content)
                    },
                    saveEditorContent = {
                        drawerFilesViewModel.save()
                    },
                    undoEditorContent = { drawerFilesViewModel.undoEditor() },
                    canUndoEditor = drawerFilesState.canUndoEditor,
                    editorSaving = drawerFilesState.editorSaving,
                    saveMessage = drawerFilesState.snackbarMessage,
                    onConsumeSaveMessage = { drawerFilesViewModel.consumeSnackbar() },
                    gitDiff = gitDiff,
                    onLoadGitDiff = viewModel::loadGitDiff,
                    onClearGitDiff = viewModel::clearGitDiff,
                    onStageGitFile = viewModel::stageGitFile,
                    onUnstageGitFile = viewModel::unstageGitFile,
                    onDiscardGitFile = viewModel::discardGitFile,
                    gitActionInProgress = gitActionInProgress,
                )
            } else {
                WorkspaceSetupContent(
                    state = state.build,
                    selectedAgents = state.selectedAgents,
                    onToggleAgent = actions.toggleAgent,
                    onInstall = actions.install,
                    onCancel = actions.cancel,
                )
            }
        }
    }
}

/** Tabbed workspace content: top bar + tab body + bottom navigation. */
@Composable
private fun WorkspaceTabContent(
    tab: WorkspaceTab,
    onTabChanged: (WorkspaceTab) -> Unit,
    state: com.dorai.ai.ui.workspace.WorkspaceUiState,
    actions: WorkspaceActions,
    filesOpen: Boolean,
    onFilesToggle: (Boolean) -> Unit,
    selectedFile: String?,
    editorState: EditorState?,
    editorLines: List<CodeLine>,
    gitState: WorkspaceGitState,
    onOpenEditorFile: (String) -> Unit,
    onDrawerToggle: () -> Unit,
    onExit: () -> Unit,
    updateEditorContent: (String) -> Unit,
    saveEditorContent: () -> Unit,
    undoEditorContent: () -> Unit = { },
    canUndoEditor: Boolean = false,
    editorSaving: Boolean = false,
    saveMessage: org.jetbrains.compose.resources.StringResource? = null,
    onConsumeSaveMessage: () -> Unit = { },
    gitDiff: com.dorai.ai.workspace.WorkspaceGitDiff? = null,
    onLoadGitDiff: (String, String) -> Unit = { _, _ -> },
    onClearGitDiff: () -> Unit = { },
    onStageGitFile: (String, String) -> Unit = { _, _ -> },
    onUnstageGitFile: (String, String) -> Unit = { _, _ -> },
    onDiscardGitFile: (String, String) -> Unit = { _, _ -> },
    gitActionInProgress: Boolean = false,
) {
    val project = state.openProject
    val installedAgents = remember(state.build.installedAgents) {
        WorkspaceAgents.all.filter { it.id in state.build.installedAgents }.toImmutableList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // â”€â”€â”€ Top bar â”€â”€â”€
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDrawerToggle) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(Res.string.workspace_open_drawer_content_description),
                        tint = LocalDesignColorTokens.current.textSecondary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = WorkspaceSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = project ?: stringResource(Res.string.workspace_title),
                        style = DoraiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = state.build.activeSessionId?.take(7) ?: stringResource(Res.string.workspace_default_session_label),
                        style = DoraiTypography.labelSmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (project != null && !filesOpen) {
                        IconButton(onClick = { onFilesToggle(true) }) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = stringResource(Res.string.workspace_files_tab),
                                tint = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.workspace_exit_content_description),
                            tint = LocalDesignColorTokens.current.textSecondary,
                        )
                    }
                }
            }
        }

        // â”€â”€â”€ Tab body â”€â”€â”€
        when (tab) {
            WorkspaceTab.Agent -> {
                if (project != null) {
                    val sessions = remember(state.build.sessions, project) {
                        state.build.sessions.filter { it.project == project }.toImmutableList()
                    }
                    // The global active id may point at another project's session;
                    // resolve against this project's sessions only.
                    val session = activeSessionForProject(state.build.sessions, state.build.activeSessionId, project)
                    WorkspaceSessionBar(
                        sessions = sessions,
                        activeSessionId = session?.id,
                        installedAgents = installedAgents,
                        filesSelected = filesOpen,
                        project = project,
                        onBack = actions.closeProject,
                        onSelectSession = { id ->
                            onFilesToggle(false)
                            actions.selectSession(id)
                        },
                        onCloseSession = actions.closeSession,
                        onSelectFiles = { onFilesToggle(true) },
                        onNewSession = { agentId ->
                            onFilesToggle(false)
                            actions.startSession(agentId)
                        },
                    )
                    when {
                        filesOpen -> {
                            SandboxFilesContent(
                                modifier = Modifier.fillMaxSize().imePadding(),
                                initialPath = "$PROJECTS_GUEST_DIR/$project",
                                viewModel = koinViewModel(
                                    qualifier = WORKSPACE_FILES,
                                    key = WORKSPACE_FILES_KEY,
                                ),
                            )
                        }

                        session != null -> WorkspaceTerminalContent(
                            session = session,
                            onSubmitLine = actions.submitLine,
                            onKey = actions.sendKey,
                            onText = actions.sendText,
                            onMouse = actions.sendMouse,
                            onResize = actions.resizeTerminal,
                            onRestart = {
                                actions.startSession(session.agentId)
                                actions.closeSession(session.id)
                            },
                        )

                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(
                                        if (sessions.any { it.status == WorkspaceSessionStatus.Starting }) {
                                            Res.string.workspace_starting_session
                                        } else {
                                            Res.string.workspace_no_active_session
                                        },
                                    ),
                                    style = DoraiTypography.bodyMedium,
                                    color = LocalDesignColorTokens.current.textSecondary,
                                )
                            }
                        }
                    }
                } else {
                    // No project open - show projects picker
                    WorkspaceProjectsContent(
                        state = state.build,
                        launchAgentId = state.launchAgentId,
                        installedAgents = installedAgents,
                        onSelectLaunchAgent = actions.setLaunchAgent,
                        onOpenProject = actions.openProject,
                        onDeleteProject = actions.deleteProject,
                        onRenameProject = actions.renameProject,
                        onInstallAgent = actions.installAgent,
                        onUninstall = actions.uninstall,
                    )
                }
            }

            WorkspaceTab.Editor -> {
                when (val editor = editorState) {
                    is EditorState.Loaded -> {
                        var editable by remember(editor.path) { mutableStateOf(true) }
                        var showSearch by remember(editor.path) { mutableStateOf(false) }
                        var searchQuery by remember(editor.path) { mutableStateOf("") }
                        var searchIndex by remember(editor.path) { mutableStateOf(0) }
                        val matchCount = remember(editor.current, searchQuery) {
                            countSearchMatches(editor.current, searchQuery)
                        }
                        val safeIndex = if (matchCount == 0) 0 else searchIndex % matchCount
                        val language = remember(editor.path) {
                            when (editor.path.substringAfterLast('.', "").lowercase()) {
                                "kt", "kts" -> "Kotlin"
                                "java" -> "Java"
                                "md" -> "Markdown"
                                "py" -> "Python"
                                "js", "ts", "json" -> "JavaScript"
                                "gradle", "xml" -> "XML"
                                else -> "Text"
                            }
                        }
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Save feedback: success/failure from the file browser.
                            if (saveMessage != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(saveMessage),
                                        style = DoraiTypography.labelMedium,
                                        color = if (saveMessage == Res.string.sandbox_files_save_failed) LocalDesignColorTokens.current.error else LocalDesignColorTokens.current.success,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = onConsumeSaveMessage) {
                                        Text(text = stringResource(Res.string.workspace_save_dismiss), style = DoraiTypography.labelMedium)
                                    }
                                }
                            }
                            WorkspaceCodeEditor(
                                filePath = editor.path,
                                lines = editorLines,
                                content = editor.current,
                                onContentChange = updateEditorContent,
                                onDiffToggled = { },
                                onRun = { },
                                onUndo = undoEditorContent,
                                onSearchChanged = { query ->
                                    searchQuery = query
                                    searchIndex = 0
                                },
                                editable = editable && !editor.readOnly,
                                onEditableChange = { editable = it },
                                isDirty = editor.dirty,
                                onSave = saveEditorContent,
                                onSearchToggle = { showSearch = !showSearch },
                                onCloseSearch = { showSearch = false },
                                showSearch = showSearch,
                                searchQuery = searchQuery,
                                language = language,
                                readOnly = editor.readOnly,
                                canUndo = canUndoEditor,
                                isSaving = editorSaving,
                                matchCount = matchCount,
                                currentMatch = safeIndex,
                                onSearchPrevious = {
                                    if (matchCount > 0) searchIndex = (safeIndex - 1 + matchCount) % matchCount
                                },
                                onSearchNext = {
                                    if (matchCount > 0) searchIndex = (safeIndex + 1) % matchCount
                                },
                                showDiffRun = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    EditorState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.workspace_loading_file),
                                style = DoraiTypography.bodyMedium,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }

                    is EditorState.Binary -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.workspace_editor_binary, editor.path),
                                style = DoraiTypography.bodyMedium,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }

                    is EditorState.TooLarge -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.workspace_editor_too_large, editor.sizeBytes),
                                style = DoraiTypography.bodyMedium,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }

                    is EditorState.Unreadable -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.workspace_editor_unreadable, editor.path),
                                style = DoraiTypography.bodyMedium,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }

                    null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.workspace_editor_empty),
                                style = DoraiTypography.bodyMedium,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }
                }
            }

            WorkspaceTab.Terminal -> {
                if (project != null) {
                    val session = activeSessionForProject(state.build.sessions, state.build.activeSessionId, project)
                    if (session != null) {
                        WorkspaceTerminalContent(
                            session = session,
                            onSubmitLine = actions.submitLine,
                            onKey = actions.sendKey,
                            onText = actions.sendText,
                            onMouse = actions.sendMouse,
                            onResize = actions.resizeTerminal,
                            onRestart = {
                                actions.startSession(session.agentId)
                                actions.closeSession(session.id)
                            },
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.workspace_no_active_session),
                                style = DoraiTypography.bodyMedium,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.workspace_open_project_terminal),
                            style = DoraiTypography.bodyMedium,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                    }
                }
            }

            WorkspaceTab.Git -> {
                WorkspaceGitContent(
                    project = project,
                    gitState = gitState,
                    onOpenEditorFile = onOpenEditorFile,
                    gitDiff = gitDiff,
                    onLoadGitDiff = onLoadGitDiff,
                    onClearGitDiff = onClearGitDiff,
                    onStageGitFile = onStageGitFile,
                    onUnstageGitFile = onUnstageGitFile,
                    onDiscardGitFile = onDiscardGitFile,
                    gitActionInProgress = gitActionInProgress,
                )
            }
        }

        // â”€â”€â”€ Bottom navigation â”€â”€â”€
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            WorkspaceTab.entries.forEach { t ->
                NavigationBarItem(
                    selected = tab == t,
                    onClick = { onTabChanged(t) },
                    icon = {
                        Icon(
                            imageVector = t.icon,
                            contentDescription = null,
                            tint = if (tab == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(t.label),
                            style = DoraiTypography.labelSmall,
                            color = if (tab == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}

/** Git & Source Control tab content: lists changed files and opens them in the editor. */
@Composable
private fun WorkspaceGitContent(
    project: String?,
    gitState: WorkspaceGitState,
    onOpenEditorFile: (String) -> Unit,
    gitDiff: com.dorai.ai.workspace.WorkspaceGitDiff? = null,
    onLoadGitDiff: (String, String) -> Unit = { _, _ -> },
    onClearGitDiff: () -> Unit = { },
    onStageGitFile: (String, String) -> Unit = { _, _ -> },
    onUnstageGitFile: (String, String) -> Unit = { _, _ -> },
    onDiscardGitFile: (String, String) -> Unit = { _, _ -> },
    gitActionInProgress: Boolean = false,
) {
    var pendingDiscardPath by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (project == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.workspace_git_no_project),
                    style = DoraiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
        } else {
            Text(
                text = stringResource(Res.string.workspace_git_changes_title, project),
                style = DoraiTypography.titleMedium,
                color = LocalDesignColorTokens.current.textPrimary,
                modifier = Modifier.padding(16.dp),
            )
            if (gitState.branch != null) {
                Text(
                    text = stringResource(Res.string.workspace_git_branch, gitState.branch),
                    style = DoraiTypography.labelMedium,
                    color = LocalDesignColorTokens.current.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            when {
                gitState.loading && gitState.changes.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.workspace_git_loading),
                        style = DoraiTypography.bodyMedium,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                }

                !gitState.loading && gitState.error == com.dorai.ai.workspace.WorkspaceGitError.NotRepository -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.workspace_git_not_repository),
                        style = DoraiTypography.bodyMedium,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                }

                !gitState.loading && gitState.error == com.dorai.ai.workspace.WorkspaceGitError.MissingGit -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.workspace_git_missing_git),
                        style = DoraiTypography.bodyMedium,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                }

                !gitState.loading && gitState.error == com.dorai.ai.workspace.WorkspaceGitError.Failed -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.workspace_git_failed),
                        style = DoraiTypography.bodyMedium,
                        color = LocalDesignColorTokens.current.error,
                    )
                }

                else -> WorkspaceGitChangesList(
                    project = project,
                    changes = gitState.changes.map {
                        GitFileChange(it.path, it.status, staged = it.staged, unstaged = it.unstaged)
                    },
                    onOpenFile = onOpenEditorFile,
                    onStageFile = onStageGitFile,
                    onUnstageFile = onUnstageGitFile,
                    onDiscardFile = { pendingDiscardPath = it },
                    onShowDiff = onLoadGitDiff,
                    actionInProgress = gitActionInProgress,
                )
            }
        }
    }

    if (gitDiff != null) {
        WorkspaceGitDiffDialog(
            diff = gitDiff,
            onDismiss = onClearGitDiff,
        )
    }

    val discardPath = pendingDiscardPath
    if (discardPath != null && project != null) {
        AlertDialog(
            onDismissRequest = { pendingDiscardPath = null },
            title = {
                Text(text = stringResource(Res.string.workspace_git_discard_title))
            },
            text = {
                Text(text = stringResource(Res.string.workspace_git_discard_message, discardPath))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDiscardGitFile(project, discardPath)
                        pendingDiscardPath = null
                    },
                    enabled = !gitActionInProgress,
                ) {
                    Text(text = stringResource(Res.string.workspace_git_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDiscardPath = null }) {
                    Text(text = stringResource(Res.string.sandbox_files_dialog_cancel))
                }
            },
        )
    }
}

/** Dialog showing the staged + unstaged unified diffs for one file. */
@Composable
private fun WorkspaceGitDiffDialog(
    diff: com.dorai.ai.workspace.WorkspaceGitDiff,
    onDismiss: () -> Unit,
) {
    val colorTokens = LocalDesignColorTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.workspace_git_diff_title, diff.path))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(WorkspaceSpacing.sm),
            ) {
                if (diff.error != null) {
                    Text(
                        text = stringResource(Res.string.workspace_git_diff_error),
                        style = DoraiTypography.bodyMedium,
                        color = LocalDesignColorTokens.current.error,
                    )
                } else {
                    if (diff.stagedDiff.isNotEmpty()) {
                        Text(
                            text = stringResource(Res.string.workspace_git_staged),
                            style = DoraiTypography.labelMedium,
                            color = colorTokens.textSecondary,
                        )
                        Text(
                            text = diff.stagedDiff,
                            style = DoraiTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = colorTokens.textPrimary,
                        )
                    }
                    if (diff.unstagedDiff.isNotEmpty()) {
                        Text(
                            text = stringResource(Res.string.workspace_git_unstaged),
                            style = DoraiTypography.labelMedium,
                            color = colorTokens.textSecondary,
                        )
                        Text(
                            text = diff.unstagedDiff,
                            style = DoraiTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = colorTokens.textPrimary,
                        )
                    }
                    if (diff.stagedDiff.isEmpty() && diff.unstagedDiff.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.workspace_git_no_changes),
                            style = DoraiTypography.bodyMedium,
                            color = colorTokens.textSecondary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.image_viewer_close))
            }
        },
    )
}

/** List of changed files with git status badges; rows open in the Editor tab, action buttons stage/discard/diff. */
@Composable
private fun WorkspaceGitChangesList(
    project: String,
    changes: List<GitFileChange>,
    onOpenFile: (String) -> Unit,
    onStageFile: (String, String) -> Unit,
    onUnstageFile: (String, String) -> Unit,
    onDiscardFile: (String) -> Unit,
    onShowDiff: (String, String) -> Unit,
    actionInProgress: Boolean,
) {
    val colorTokens = LocalDesignColorTokens.current

    if (changes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.workspace_git_no_changes),
                style = DoraiTypography.bodyMedium,
                color = colorTokens.textSecondary,
            )
        }
        return@WorkspaceGitChangesList
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = changes, key = { it.path }) { change ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onOpenFile(change.path) }, role = Role.Button),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.FileCopy,
                        contentDescription = null,
                        tint = colorTokens.textMuted,
                        modifier = Modifier.size(18.dp),
                    )

                    Column(
                        modifier = Modifier.weight(1f).padding(start = WorkspaceSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = change.path.substringAfterLast("/"),
                            style = DoraiTypography.bodyMedium,
                            color = colorTokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = change.path,
                            style = DoraiTypography.labelSmall,
                            color = colorTokens.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    val (badgeColor, badgeText) = when (change.status) {
                        WorkspaceGitStatus.Added -> Pair(colorTokens.success, stringResource(Res.string.workspace_git_status_added))
                        WorkspaceGitStatus.Modified -> Pair(colorTokens.warning, stringResource(Res.string.workspace_git_status_modified))
                        WorkspaceGitStatus.Deleted -> Pair(colorTokens.error, stringResource(Res.string.workspace_git_status_deleted))
                        WorkspaceGitStatus.Renamed -> Pair(colorTokens.accent, stringResource(Res.string.workspace_git_status_renamed))
                        WorkspaceGitStatus.Untracked -> Pair(colorTokens.textMuted, stringResource(Res.string.workspace_git_status_untracked))
                        WorkspaceGitStatus.Conflicted -> Pair(colorTokens.error, stringResource(Res.string.workspace_git_status_conflicted))
                    }
                    Box(
                        modifier = Modifier
                            .heightIn(min = 22.dp)
                            .defaultMinSize(minWidth = 22.dp)
                            .background(badgeColor)
                            .clip(RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = badgeText,
                            style = DoraiTypography.labelSmall,
                            color = colorTokens.background,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // Staged/unstaged context + per-file actions.
                val stagedLabel = when {
                    change.staged && change.unstaged -> stringResource(Res.string.workspace_git_staged) + ", " + stringResource(Res.string.workspace_git_unstaged)
                    change.staged -> stringResource(Res.string.workspace_git_staged)
                    change.unstaged -> stringResource(Res.string.workspace_git_unstaged)
                    else -> stringResource(Res.string.workspace_git_unstaged)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = stagedLabel,
                        style = DoraiTypography.labelSmall,
                        color = colorTokens.textSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (change.staged) {
                        TextButton(
                            onClick = { onUnstageFile(project, change.path) },
                            enabled = !actionInProgress,
                        ) {
                            Text(text = stringResource(Res.string.workspace_git_unstage))
                        }
                    } else {
                        TextButton(
                            onClick = { onStageFile(project, change.path) },
                            enabled = !actionInProgress,
                        ) {
                            Text(text = stringResource(Res.string.workspace_git_stage))
                        }
                    }
                    TextButton(
                        onClick = { onDiscardFile(change.path) },
                        enabled = !actionInProgress,
                    ) {
                        Text(text = stringResource(Res.string.workspace_git_discard))
                    }
                    TextButton(
                        onClick = { onShowDiff(project, change.path) },
                        enabled = !actionInProgress,
                    ) {
                        Text(text = stringResource(Res.string.workspace_git_diff))
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(horizontal = WorkspaceSpacing.sm),
                color = colorTokens.borderSubtle,
            )
        }
    }
}
