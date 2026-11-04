package com.dorai.ai.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
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
import com.dorai.ai.workspace.terminal.TerminalKey
import com.dorai.ai.workspace.terminal.TerminalModifiers
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.workspace_exit_content_description
import dorai.composeapp.generated.resources.workspace_git_changes_title
import dorai.composeapp.generated.resources.workspace_git_no_changes
import dorai.composeapp.generated.resources.workspace_git_no_project
import dorai.composeapp.generated.resources.workspace_git_status_added
import dorai.composeapp.generated.resources.workspace_git_status_deleted
import dorai.composeapp.generated.resources.workspace_git_status_modified
import dorai.composeapp.generated.resources.workspace_new_project_content_description
import dorai.composeapp.generated.resources.workspace_title
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Where Debian mounts the project folders — the browser's root inside a project. */
private const val PROJECTS_GUEST_DIR = "/root/projects"

/** Keeps Workspace's browser out of the chat sandbox's ViewModel slot. */
private const val WORKSPACE_FILES_KEY = "WorkspaceFiles"

/** Separate browser instance for the projects drawer explorer. */
private const val WORKSPACE_FILES_DRAWER_KEY = "WorkspaceDrawerFiles"

/** One changed file shown in the Git tab's changes list. */
@Immutable
data class GitFileChange(
    val path: String,
    val status: GitStatus,
    val touchedByAgent: Boolean = false,
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
enum class WorkspaceTab(val id: String, val label: String, val icon: ImageVector) {
    Agent("agent", "Agent", Icons.Default.Comment),
    Editor("editor", "Editor", Icons.Default.Code),
    Terminal("terminal", "Terminal", Icons.Default.Terminal),
    Git("git", "Git", Icons.Default.History),
}

/**
 * Stateless Workspace surface content for screenshot tests. Takes [state] and
 * [actions] directly so it can be rendered without a ViewModel or Koin.
 */
@Composable
fun WorkspaceScreenContent(
    state: WorkspaceUiState,
    actions: WorkspaceActions,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(WorkspaceTab.Agent) }
    var pendingProject by remember { mutableStateOf<String?>(null) }
    var filesOpen by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<String?>(null) }

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

    val editorLines = emptyList<CodeLine>()

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
                onFilterChanged = { },
                selectedFilter = ProjectFilter.All,
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
                    editorState = null,
                    editorLines = editorLines,
                    onOpenEditorFile = { path ->
                        selectedFile = path
                        tab = WorkspaceTab.Editor
                    },
                    onDrawerToggle = { scope.launch { drawerState.open() } },
                    onExit = onExit,
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

/**
 * Full-screen Workspace surface. Entered from the empty chat state and left via
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
    }

    val drawerFiles = remember(drawerFilesState.entries, selectedFile) {
        drawerFilesState.entries.map { entry ->
            ProjectFile(
                path = entry.path,
                name = entry.name,
                isDirectory = entry.isDirectory,
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
                onFilterChanged = { },
                selectedFilter = ProjectFilter.All,
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
                    editorState = drawerFilesState.editor,
                    editorLines = editorLines,
                    onOpenEditorFile = { path ->
                        drawerFilesViewModel.openInEditor(path)
                        selectedFile = path
                        tab = WorkspaceTab.Editor
                    },
                    onDrawerToggle = { scope.launch { drawerState.open() } },
                    onExit = onExit,
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
    onOpenEditorFile: (String) -> Unit,
    onDrawerToggle: () -> Unit,
    onExit: () -> Unit,
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
        // ─── Top bar ───
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
                        contentDescription = "Open drawer",
                        tint = LocalDesignColorTokens.current.textSecondary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = WorkspaceSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = project ?: "Workspace",
                        style = DoraiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = state.build.activeSessionId?.take(7) ?: "main",
                        style = DoraiTypography.labelSmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                        maxLines = 1,
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
                                contentDescription = null,
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

        // ─── Tab body ───
        when (tab) {
            WorkspaceTab.Agent -> {
                if (project != null) {
                    val sessions = remember(state.build.sessions, project) {
                        state.build.sessions.filter { it.project == project }.toImmutableList()
                    }
                    WorkspaceSessionBar(
                        sessions = sessions,
                        activeSessionId = state.build.activeSessionId,
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
                    val session = sessions.firstOrNull { it.id == state.build.activeSessionId }
                        ?: sessions.firstOrNull()
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
                        )

                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Starting session…",
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
                val editorPath = selectedFile ?: "${PROJECTS_GUEST_DIR}/main.kt"
                when (val editor = editorState) {
                    is EditorState.Loaded -> {
                        WorkspaceCodeEditor(
                            filePath = editor.path,
                            lines = editorLines,
                            content = editor.current,
                            onContentChange = { },
                            onDiffToggled = { },
                            onRun = { },
                            onUndo = { },
                            onSearchChanged = { },
                            editable = true,
                            onEditableChange = { },
                            isDirty = editor.dirty,
                            onSave = { },
                            onSearchToggle = { },
                        )
                    }

                    EditorState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Loading file…",
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
                                text = "Binary file: ${editor.path}",
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
                                text = "File too large (${editor.sizeBytes} bytes)",
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
                                text = "Cannot read file: ${editor.path}",
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
                                text = "Open a file to edit",
                                style = DoraiTypography.bodyMedium,
                                color = LocalDesignColorTokens.current.textSecondary,
                            )
                        }
                    }
                }
            }

            WorkspaceTab.Terminal -> {
                if (project != null) {
                    val sessions = remember(state.build.sessions, project) {
                        state.build.sessions.filter { it.project == project }.toImmutableList()
                    }
                    val session = sessions.firstOrNull { it.id == state.build.activeSessionId }
                        ?: sessions.firstOrNull()
                    if (session != null) {
                        WorkspaceTerminalContent(
                            session = session,
                            onSubmitLine = actions.submitLine,
                            onKey = actions.sendKey,
                            onText = actions.sendText,
                            onMouse = actions.sendMouse,
                            onResize = actions.resizeTerminal,
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No active session",
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
                            text = "Open a project to use Terminal",
                            style = DoraiTypography.bodyMedium,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                    }
                }
            }

            WorkspaceTab.Git -> {
                WorkspaceGitContent(
                    project = project,
                    onOpenEditorFile = onOpenEditorFile,
                )
            }
        }

        // ─── Bottom navigation ───
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
                            text = t.label,
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
    onOpenEditorFile: (String) -> Unit,
) {
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
            WorkspaceGitChangesList(
                project = project,
                changes = gitChangesFor(project),
                onOpenFile = onOpenEditorFile,
            )
        }
    }
}

/** Sample of changed files for the open project. In a real build this comes from
 * the sandbox's git status; here it demonstrates the changes-list UI pattern. */
private fun gitChangesFor(project: String): List<GitFileChange> = listOf(
    GitFileChange(path = "${PROJECTS_GUEST_DIR}/$project/src/Main.kt", status = GitStatus.Modified, touchedByAgent = true),
    GitFileChange(path = "${PROJECTS_GUEST_DIR}/$project/src/Utils.kt", status = GitStatus.Added),
    GitFileChange(path = "${PROJECTS_GUEST_DIR}/$project/build.gradle.kts", status = GitStatus.Modified),
    GitFileChange(path = "${PROJECTS_GUEST_DIR}/$project/old/Temp.kt", status = GitStatus.Deleted, touchedByAgent = true),
)

/** List of changed files with git status badges; each row opens the file in the Editor tab. */
@Composable
private fun WorkspaceGitChangesList(
    project: String,
    changes: List<GitFileChange>,
    onOpenFile: (String) -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs)
                    .clickable(onClick = { onOpenFile(change.path) }),
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
                    )
                    Text(
                        text = change.path,
                        style = DoraiTypography.labelSmall,
                        color = colorTokens.textSecondary,
                        maxLines = 1,
                    )
                }

                val (badgeColor, badgeText) = when (change.status) {
                    GitStatus.Added -> Pair(DesignTokens.Success, stringResource(Res.string.workspace_git_status_added))
                    GitStatus.Modified -> Pair(DesignTokens.Warning, stringResource(Res.string.workspace_git_status_modified))
                    GitStatus.Deleted -> Pair(DesignTokens.Error, stringResource(Res.string.workspace_git_status_deleted))
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(badgeColor)
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badgeText.take(1).uppercase(),
                        style = DoraiTypography.labelSmall.copy(fontSize = 9.sp),
                        color = Color(0xFF0F1419),
                    )
                }

                if (change.touchedByAgent) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = WorkspaceSpacing.xs)
                            .background(Color(0xFF1B2A44))
                            .clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Agent",
                            style = DoraiTypography.labelSmall.copy(fontSize = 9.sp),
                            color = DesignTokens.AccentPrimary,
                        )
                    }
                }
            }

            Divider(
                modifier = Modifier.fillMaxWidth().padding(horizontal = WorkspaceSpacing.sm),
                color = colorTokens.borderSubtle,
            )
        }
    }
}
