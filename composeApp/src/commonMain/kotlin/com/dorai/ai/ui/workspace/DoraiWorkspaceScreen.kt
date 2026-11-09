package com.dorai.ai.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.engine.doraiEdgeStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.DoraiAdaptiveDefaults
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.components.v2.DoraiPillTabs
import com.dorai.ai.ui.components.v2.DoraiStatusBadge
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.workspace_agent_empty
import dorai.composeapp.generated.resources.workspace_back_content_description
import dorai.composeapp.generated.resources.workspace_changes_count
import dorai.composeapp.generated.resources.workspace_editor_empty
import dorai.composeapp.generated.resources.workspace_files_toggle
import dorai.composeapp.generated.resources.workspace_finish_setup
import dorai.composeapp.generated.resources.workspace_git_unavailable
import dorai.composeapp.generated.resources.workspace_last_updated
import dorai.composeapp.generated.resources.workspace_new_project
import dorai.composeapp.generated.resources.workspace_ready_desc
import dorai.composeapp.generated.resources.workspace_ready_title
import dorai.composeapp.generated.resources.workspace_sandbox_running_badge
import dorai.composeapp.generated.resources.workspace_sandbox_stopped_badge
import dorai.composeapp.generated.resources.workspace_sandbox_stopped_title
import dorai.composeapp.generated.resources.workspace_setup_desc
import dorai.composeapp.generated.resources.workspace_setup_step_1
import dorai.composeapp.generated.resources.workspace_setup_step_2
import dorai.composeapp.generated.resources.workspace_setup_step_3
import dorai.composeapp.generated.resources.workspace_start_sandbox_action
import dorai.composeapp.generated.resources.workspace_terminal_unavailable
import dorai.composeapp.generated.resources.workspace_v2_setup_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

enum class WorkspaceProjectTab {
    Agent,
    Editor,
    Terminal,
    Git,
}

data class WorkspaceProjectItem(
    val name: String,
    val path: String,
    /** Null when the branch is unknown (project not opened yet / not a repo). */
    val branch: String? = null,
    /** Null when no host-side modification time is available. */
    val lastUpdatedText: String? = null,
    /** Null when the change count is unknown (project not opened yet). */
    val changesCount: Int? = null,
)

data class GitChangeItem(
    val path: String,
    val statusLetter: String, // "M", "A", "D"
)

/**
 * UI v2 Workspace screen:
 * - Setup state (first-run 3 steps).
 * - Project List / Ready state.
 * - Active Project:
 *   - The file explorer (worktree files list) is shown by default whenever a project opens.
 *   - Compact: file explorer opens as a left slide-in pane (open by default); tabs switch below.
 *   - Medium/Expanded/Large: persistent left file panel (230dp) + center tabs + right Agent panel.
 * - All project data shown is real (guest filesystem / git status); unknown values are omitted,
 *   never faked.
 */
@Composable
fun DoraiWorkspaceScreen(
    isSetupCompleted: Boolean,
    activeProject: WorkspaceProjectItem?,
    projects: List<WorkspaceProjectItem>,
    isSandboxRunning: Boolean,
    onStartSandbox: () -> Unit,
    onCompleteSetup: () -> Unit,
    onOpenProject: (WorkspaceProjectItem) -> Unit,
    onCloseProject: () -> Unit,
    modifier: Modifier = Modifier,
    terminalContent: (@Composable () -> Unit)? = null,
    editorContent: (@Composable () -> Unit)? = null,
    gitContent: (@Composable () -> Unit)? = null,
    /** Slot rendering the real guest file tree. [openFile] must be called with a file path so the center pane switches to the Editor tab. */
    fileExplorerContent: (@Composable (openFile: (String) -> Unit) -> Unit)? = null,
    onCreateProject: ((String) -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (!isSetupCompleted) {
            WorkspaceSetupView(onCompleteSetup = onCompleteSetup)
        } else if (activeProject == null) {
            WorkspaceProjectsView(
                projects = projects,
                isSandboxRunning = isSandboxRunning,
                onOpenProject = onOpenProject,
                onStartSandbox = onStartSandbox,
                onCreateProject = onCreateProject,
            )
        } else {
            ActiveProjectView(
                project = activeProject,
                isSandboxRunning = isSandboxRunning,
                onCloseProject = onCloseProject,
                onStartSandbox = onStartSandbox,
                terminalContent = terminalContent,
                editorContent = editorContent,
                gitContent = gitContent,
                fileExplorerContent = fileExplorerContent,
            )
        }
    }
}

@Composable
private fun WorkspaceSetupView(onCompleteSetup: () -> Unit) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(DoraiSpacing.LG),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.line, shape)
                .padding(DoraiSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
        ) {
            Text(
                text = stringResource(Res.string.workspace_v2_setup_title),
                color = colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.workspace_setup_desc),
                color = colors.muted,
                fontSize = 14.sp,
            )

            Spacer(modifier = Modifier.height(DoraiSpacing.SM))

            SetupStepRow(number = "1", text = stringResource(Res.string.workspace_setup_step_1), isDone = true)
            SetupStepRow(number = "2", text = stringResource(Res.string.workspace_setup_step_2), isDone = true)
            SetupStepRow(number = "3", text = stringResource(Res.string.workspace_setup_step_3), isDone = false)

            Spacer(modifier = Modifier.height(DoraiSpacing.MD))

            Button(
                onClick = onCompleteSetup,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .doraiEdgeStyle(shape = RoundedCornerShape(12.dp), cornerRadius = 12.dp, edgeColor = colors.primary),
            ) {
                Text(
                    text = stringResource(Res.string.workspace_finish_setup),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SetupStepRow(number: String, text: String, isDone: Boolean) {
    val colors = LocalDoraiColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isDone) colors.ok else colors.surface2)
                .border(1.dp, if (isDone) colors.ok else colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.background, modifier = Modifier.size(14.dp))
            } else {
                Text(number, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(DoraiSpacing.MD))
        Text(text, color = colors.text, fontSize = 14.sp)
    }
}

@Composable
private fun WorkspaceProjectsView(
    projects: List<WorkspaceProjectItem>,
    isSandboxRunning: Boolean,
    onOpenProject: (WorkspaceProjectItem) -> Unit,
    onStartSandbox: () -> Unit,
    onCreateProject: ((String) -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val scrollState = rememberScrollState()
    var showNewProjectDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DoraiSpacing.MD)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
    ) {
        // Workspace ready status banner
        val cardShape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.line, cardShape)
                .padding(DoraiSpacing.MD),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.workspace_ready_title),
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                DoraiStatusBadge(
                    text = stringResource(if (isSandboxRunning) Res.string.workspace_sandbox_running_badge else Res.string.workspace_sandbox_stopped_badge),
                    color = if (isSandboxRunning) colors.ok else colors.muted,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.workspace_ready_desc),
                color = colors.muted,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(DoraiSpacing.MD))
            Row(horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM)) {
                if (onCreateProject != null) {
                    Button(
                        onClick = { showNewProjectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.workspace_new_project))
                    }
                }
            }
        }

        // Project Cards
        projects.forEach { proj ->
            val projShape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(projShape)
                    .background(colors.surface)
                    .border(1.dp, colors.line, projShape)
                    .clickable { onOpenProject(proj) }
                    .padding(DoraiSpacing.MD),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = proj.name,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    // Show the badge only when we actually know the count (open project).
                    proj.changesCount?.let { count ->
                        DoraiStatusBadge(
                            text = stringResource(Res.string.workspace_changes_count, count),
                            color = if (count > 0) colors.warn else colors.ok,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Branch (when known) + real guest path.
                val subtitle = listOfNotNull(proj.branch, proj.path).joinToString(" · ")
                Text(
                    text = subtitle,
                    color = colors.muted,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                )
                proj.lastUpdatedText?.let { updated ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.workspace_last_updated, updated),
                        color = colors.muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        if (showNewProjectDialog) {
            CreateProjectDialog(
                projects = projects.map { it.name },
                onDismiss = { showNewProjectDialog = false },
                onCreate = { name ->
                    showNewProjectDialog = false
                    onCreateProject?.invoke(name)
                },
            )
        }
    }
}

@Composable
private fun ActiveProjectView(
    project: WorkspaceProjectItem,
    isSandboxRunning: Boolean,
    onCloseProject: () -> Unit,
    onStartSandbox: () -> Unit,
    terminalContent: (@Composable () -> Unit)? = null,
    editorContent: (@Composable () -> Unit)? = null,
    gitContent: (@Composable () -> Unit)? = null,
    fileExplorerContent: (@Composable (openFile: (String) -> Unit) -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current
    // The file explorer opens by default whenever a project opens; on Compact it is
    // a left slide-in pane (Drawer), on Medium+ a permanent left panel.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(WorkspaceProjectTab.Editor) }

    // The explorer calls this after opening a file: switch the center pane to the
    // Editor tab (and close the drawer on compact).
    val openFileInEditor: (String) -> Unit = {
        activeTab = WorkspaceProjectTab.Editor
        if (widthClass.isCompact) {
            scope.launch { drawerState.close() }
        }
    }
    val showFilesToggle = widthClass.isCompact && fileExplorerContent != null

    // Top bar shared across width classes
    val topBar: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(1.dp, colors.line)
                .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCloseProject) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.workspace_back_content_description),
                    tint = colors.text,
                )
            }
            Text(
                text = project.name,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            project.branch?.let { branch ->
                Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                DoraiStatusBadge(text = branch, color = colors.info)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showFilesToggle) {
                IconButton(onClick = {
                    scope.launch {
                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                    }
                }) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = stringResource(Res.string.workspace_files_toggle),
                        tint = colors.text,
                    )
                }
            }

            // Center tabs on medium & up
            if (!widthClass.isCompact) {
                DoraiPillTabs(
                    items = listOf(WorkspaceProjectTab.Editor, WorkspaceProjectTab.Terminal, WorkspaceProjectTab.Git),
                    selectedItem = activeTab,
                    onItemSelected = { activeTab = it },
                    labelProvider = { it.name },
                )
            }
        }
    }

    val centerContent: @Composable () -> Unit = {
        when (activeTab) {
            WorkspaceProjectTab.Agent -> AgentPanelEmpty()
            WorkspaceProjectTab.Editor -> if (editorContent != null) editorContent() else EditorTabEmpty()
            WorkspaceProjectTab.Terminal -> if (terminalContent != null) terminalContent() else TerminalTabContent(isSandboxRunning, onStartSandbox)
            WorkspaceProjectTab.Git -> if (gitContent != null) gitContent() else GitContentUnavailable()
        }
    }

    if (widthClass.isCompact) {
        // Compact: top bar + (optional) slide-in file explorer, open by default + tabs
        if (fileExplorerContent == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                topBar()
                DoraiPillTabs(
                    items = WorkspaceProjectTab.entries,
                    selectedItem = activeTab,
                    onItemSelected = { activeTab = it },
                    labelProvider = { it.name },
                    modifier = Modifier.padding(DoraiSpacing.SM),
                )
                Box(modifier = Modifier.weight(1f)) {
                    centerContent()
                }
            }
            return
        }
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .background(colors.surface)
                            .padding(DoraiSpacing.SM),
                    ) {
                        fileExplorerContent?.invoke(openFileInEditor)
                    }
                },
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DoraiPillTabs(
                        items = WorkspaceProjectTab.entries,
                        selectedItem = activeTab,
                        onItemSelected = { activeTab = it },
                        labelProvider = { it.name },
                        modifier = Modifier.padding(DoraiSpacing.SM),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        centerContent()
                    }
                }
            }
        }
    } else {
        // Medium/Expanded/Large: permanent left file panel + center + agent rail
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            Row(modifier = Modifier.weight(1f)) {
                // Left File Explorer (worktree files list), shown by default on project open
                if (fileExplorerContent != null) {
                    Box(
                        modifier = Modifier
                            .width(DoraiAdaptiveDefaults.FileTreeWidthLarge)
                            .fillMaxHeight()
                            .background(colors.surface)
                            .border(1.dp, colors.line),
                    ) {
                        fileExplorerContent.invoke(openFileInEditor)
                    }
                }

                // Center Pane (Editor / Terminal / Git)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    centerContent()
                }

                // Right Agent Panel (320dp Medium/Expanded, 380dp Large)
                Box(
                    modifier = Modifier
                        .width(widthClass.workspaceAgentPanelWidth)
                        .fillMaxHeight()
                        .background(colors.surface)
                        .border(1.dp, colors.line),
                ) {
                    AgentPanelEmpty()
                }
            }
        }
    }
}

@Composable
private fun AgentPanelEmpty() {
    val colors = LocalDoraiColors.current
    Box(
        modifier = Modifier.fillMaxSize().padding(DoraiSpacing.MD),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(DoraiSpacing.SM))
            Text(
                text = stringResource(Res.string.workspace_agent_empty),
                color = colors.muted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun EditorTabEmpty() {
    val colors = LocalDoraiColors.current
    Box(
        modifier = Modifier.fillMaxSize().background(colors.codeBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.workspace_editor_empty),
            color = colors.muted,
            fontSize = 13.5.sp,
        )
    }
}

@Composable
private fun GitContentUnavailable() {
    val colors = LocalDoraiColors.current
    Box(
        modifier = Modifier.fillMaxSize().padding(DoraiSpacing.MD),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.workspace_git_unavailable),
            color = colors.muted,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun TerminalTabContent(
    isSandboxRunning: Boolean,
    onStartSandbox: () -> Unit,
) {
    val colors = LocalDoraiColors.current
    if (!isSandboxRunning) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.workspace_sandbox_stopped_title),
                    color = colors.muted,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(DoraiSpacing.MD))
                Button(
                    onClick = onStartSandbox,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.workspace_start_sandbox_action))
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize().padding(DoraiSpacing.MD),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.workspace_terminal_unavailable),
                color = colors.muted,
                fontSize = 13.sp,
            )
        }
    }
}
