package com.dorai.ai.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.WORKSPACE_FILES
import com.dorai.ai.WorkspaceController
import com.dorai.ai.ui.sandbox.EditorState
import com.dorai.ai.ui.sandbox.SandboxFileBrowserViewModel
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.workspace_editor_binary
import dorai.composeapp.generated.resources.workspace_editor_empty
import dorai.composeapp.generated.resources.workspace_editor_too_large
import dorai.composeapp.generated.resources.workspace_editor_unreadable
import dorai.composeapp.generated.resources.workspace_explorer_loading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val PROJECTS_GUEST_DIR = "/root/projects"
private const val WORKSPACE_FILES_EXPLORER_KEY = "WorkspaceFileExplorer"
private const val WORKSPACE_EDITOR_KEY = "WorkspaceEditor"

/**
 * Production-connected wrapper for UI v2 Workspace, binding real data sources:
 * - the guest filesystem browser ([SandboxFileBrowserViewModel]) powers the left-side
 *   file explorer and the Editor pane,
 * - [WorkspaceViewModel.gitState] powers the Git panel and the open project's status,
 * - project last-updated times come from the host-side projects directory.
 * Nothing here is a placeholder.
 */
@Composable
fun DoraiWorkspaceScreenConnected(
    modifier: Modifier = Modifier,
    viewModel: WorkspaceViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gitState by viewModel.gitState.collectAsStateWithLifecycle()
    val gitDiff by viewModel.gitDiff.collectAsStateWithLifecycle()
    val gitActionInProgress by viewModel.gitActionInProgress.collectAsStateWithLifecycle()
    val openProjectName = uiState.openProject

    // Real guest-fs browser instance dedicated to the workspace explorer (drawer key pattern).
    val filesViewModel: SandboxFileBrowserViewModel = koinViewModel(
        qualifier = WORKSPACE_FILES,
        key = WORKSPACE_FILES_EXPLORER_KEY,
    )
    val filesState by filesViewModel.state.collectAsStateWithLifecycle()

    // Dedicated instance for the Editor pane, so browsing and editing never fight
    // over the same open-file handle.
    val editorViewModel: SandboxFileBrowserViewModel = koinViewModel(
        qualifier = WORKSPACE_FILES,
        key = WORKSPACE_EDITOR_KEY,
    )
    val editorState by editorViewModel.state.collectAsStateWithLifecycle()

    // Re-enter the project's guest directory and re-read git status whenever the open project changes.
    LaunchedEffect(openProjectName) {
        val project = openProjectName ?: return@LaunchedEffect
        filesViewModel.start("$PROJECTS_GUEST_DIR/$project")
        editorViewModel.start("$PROJECTS_GUEST_DIR/$project")
        viewModel.refreshGitStatus(project)
    }

    // Real project last-modified times from the host-side projects directory (one read, no proot calls).
    val workspaceController: WorkspaceController = koinInject()
    var projectMtimes by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    LaunchedEffect(uiState.build.projects) {
        projectMtimes = withContext(Dispatchers.Default) {
            runCatching { workspaceController.files.listDirectory(PROJECTS_GUEST_DIR) }
                .getOrDefault(emptyList())
                .associate { it.name to it.lastModifiedMs }
        }
    }

    // Git badges keyed by absolute path, so explorer and git panel never disagree.
    val gitStatusByPath = remember(gitState.changes) {
        gitState.changes.associate { it.path to it.status }
    }
    val explorerFiles = remember(filesState.entries, gitStatusByPath) {
        filesState.entries.map { entry ->
            ProjectFile(
                path = entry.path,
                name = entry.name,
                isDirectory = entry.isDirectory,
                gitStatus = gitStatusByPath[entry.path],
            )
        }
    }

    val projects = remember(uiState.build.projects, openProjectName, gitState, projectMtimes) {
        uiState.build.projects.map { name ->
            val isOpen = name == openProjectName
            WorkspaceProjectItem(
                name = name,
                path = "$PROJECTS_GUEST_DIR/$name",
                branch = gitState.branch.takeIf { isOpen && gitState.project == name },
                lastUpdatedText = projectMtimes[name]
                    ?.takeIf { it > 0L }
                    ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() },
                changesCount = gitState.changes.size.takeIf { isOpen && gitState.project == name },
            )
        }
    }
    val activeProject = projects.firstOrNull { it.name == openProjectName }
    val setupComplete = uiState.build.isReady

    DoraiWorkspaceScreen(
        isSetupCompleted = setupComplete,
        activeProject = activeProject,
        projects = projects,
        isSandboxRunning = setupComplete,
        onStartSandbox = { viewModel.install() },
        onCompleteSetup = { viewModel.install() },
        onOpenProject = { proj -> viewModel.openProject(proj.name) },
        onCloseProject = { viewModel.closeProject() },
        onCreateProject = { name -> viewModel.createProject(name) },
        terminalContent = if (uiState.build.activeSession != null) {
            {
                Box(modifier = Modifier.fillMaxSize()) {
                    WorkspaceTerminalContent(
                        session = uiState.build.activeSession!!,
                        onSubmitLine = viewModel::submitLine,
                        onKey = viewModel::sendKey,
                        onText = viewModel::sendText,
                        onMouse = viewModel::sendMouse,
                        onResize = viewModel::resizeTerminal,
                    )
                }
            }
        } else {
            null
        },
        editorContent = {
            EditorPanel(
                editor = editorState.editor,
                canUndo = editorState.canUndoEditor,
                isSaving = editorState.editorSaving,
                onOpenInEditor = editorViewModel::openInEditor,
                onUpdateContent = editorViewModel::updateEditorContent,
                onSave = editorViewModel::save,
                onUndo = editorViewModel::undoEditor,
            )
        },
        gitContent = {
            WorkspaceGitPanel(
                projectName = openProjectName,
                branch = gitState.branch.takeIf { gitState.project == openProjectName },
                loading = gitState.loading,
                changes = gitState.changes,
                error = gitState.error,
                isRepository = gitState.isRepository,
                gitActionInProgress = gitActionInProgress,
                gitDiff = gitDiff,
                onLoadDiff = { path -> openProjectName?.let { project -> viewModel.loadGitDiff(project, path) } },
                onClearDiff = viewModel::clearGitDiff,
                onStageGitFile = viewModel::stageGitFile,
                onUnstageGitFile = viewModel::unstageGitFile,
                onDiscardGitFile = viewModel::discardGitFile,
            )
        },
        fileExplorerContent = { onOpenFile ->
            WorkspaceProjectsExplorer(
                currentProject = openProjectName,
                projects = uiState.build.projects,
                files = explorerFiles,
                onOpenProject = { name -> viewModel.openProject(name) },
                onCreateProject = { name -> viewModel.createProject(name) },
                onOpenFile = { path ->
                    val entry = filesState.entries.firstOrNull { it.path == path }
                    if (entry?.isDirectory == true) {
                        filesViewModel.navigateTo(path)
                    } else {
                        filesViewModel.openInEditor(path)
                        editorViewModel.openInEditor(path)
                        onOpenFile(path)
                    }
                },
                isLoading = filesState.loading,
                error = filesState.error,
                onRefresh = { filesViewModel.start(filesState.currentPath) },
            )
        },
        modifier = modifier,
    )
}

/** Inline editor pane backed by the real guest file system. */
@Composable
private fun EditorPanel(
    editor: EditorState?,
    canUndo: Boolean,
    isSaving: Boolean,
    onOpenInEditor: (String) -> Unit,
    onUpdateContent: (String) -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
) {
    when (editor) {
        is EditorState.Loaded -> {
            val lines = remember(editor.current) { highlightSource(editor.current) }
            WorkspaceCodeEditor(
                filePath = editor.path,
                lines = lines,
                onDiffToggled = {},
                onRun = {},
                onUndo = onUndo,
                content = editor.current,
                onContentChange = onUpdateContent,
                editable = true,
                isDirty = editor.dirty,
                onSave = onSave,
                canUndo = canUndo,
                isSaving = isSaving,
                modifier = Modifier.fillMaxSize(),
            )
        }

        EditorState.Loading -> CenteredMessage(stringResource(Res.string.workspace_explorer_loading))

        is EditorState.Binary -> CenteredMessage(stringResource(Res.string.workspace_editor_binary, editor.path.substringAfterLast('/')))

        is EditorState.TooLarge -> CenteredMessage(stringResource(Res.string.workspace_editor_too_large, editor.sizeBytes))

        is EditorState.Unreadable -> CenteredMessage(stringResource(Res.string.workspace_editor_unreadable, editor.path.substringAfterLast('/')))

        null -> CenteredMessage(stringResource(Res.string.workspace_editor_empty))
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = com.dorai.ai.theme.tokens.LocalDoraiColors.current.muted, fontSize = 13.sp)
    }
}
