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
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.dorai.ai.theme.engine.doraiSurfaceStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.DoraiAdaptiveDefaults
import com.dorai.ai.ui.adaptive.DoraiWindowWidthClass
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.components.v2.DoraiDiffBlock
import com.dorai.ai.ui.components.v2.DoraiPillTabs
import com.dorai.ai.ui.components.v2.DoraiShellBlock
import com.dorai.ai.ui.components.v2.DoraiStatusBadge
import com.dorai.ai.ui.components.v2.DoraiToolCard
import com.dorai.ai.ui.components.v2.ToolExecutionModel
import com.dorai.ai.ui.components.v2.ToolExecutionState
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.nav_workspace
import dorai.composeapp.generated.resources.workspace_commit_action
import dorai.composeapp.generated.resources.workspace_commit_message_hint
import dorai.composeapp.generated.resources.workspace_files_changed_count
import dorai.composeapp.generated.resources.workspace_import_project
import dorai.composeapp.generated.resources.workspace_new_project
import dorai.composeapp.generated.resources.workspace_ready_desc
import dorai.composeapp.generated.resources.workspace_ready_title
import dorai.composeapp.generated.resources.workspace_sandbox_stopped_title
import dorai.composeapp.generated.resources.workspace_setup_desc
import dorai.composeapp.generated.resources.workspace_setup_step_1
import dorai.composeapp.generated.resources.workspace_setup_step_2
import dorai.composeapp.generated.resources.workspace_setup_step_3
import dorai.composeapp.generated.resources.workspace_start_sandbox_action
import dorai.composeapp.generated.resources.workspace_v2_setup_title
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
    val branch: String,
    val lastUpdatedText: String,
    val changesCount: Int,
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
 *   - Compact: Single tab at a time (Agent, Editor, Terminal, Git).
 *   - Medium/Expanded: Center tabs (Editor, Terminal, Git) + Right persistent Agent panel (320dp).
 *   - Large: Left File Tree (230dp) + Center tabs (Editor, Terminal, Git) + Right Agent panel (380dp).
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
                    text = "Finish Setup & Launch",
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
    var newProjectName by remember { mutableStateOf("") }

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
                    text = if (isSandboxRunning) "Sandbox Running" else "Sandbox Stopped",
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
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.workspace_new_project))
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.text),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(stringResource(Res.string.workspace_import_project))
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
                    DoraiStatusBadge(
                        text = "${proj.changesCount} changes",
                        color = if (proj.changesCount > 0) colors.warn else colors.ok,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${proj.branch} · ${proj.path}",
                    color = colors.muted,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
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
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current
    var activeTab by remember { mutableStateOf(WorkspaceProjectTab.Editor) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Project Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(1.dp, colors.line)
                .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCloseProject) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.text)
            }
            Text(
                text = project.name,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(DoraiSpacing.SM))
            DoraiStatusBadge(text = project.branch, color = colors.info)

            Spacer(modifier = Modifier.weight(1f))

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

        // Active layout:
        if (widthClass.isCompact) {
            // Compact: 1 tab at a time with top tabs including Agent
            DoraiPillTabs(
                items = WorkspaceProjectTab.entries,
                selectedItem = activeTab,
                onItemSelected = { activeTab = it },
                labelProvider = { it.name },
                modifier = Modifier.padding(DoraiSpacing.SM),
            )
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    WorkspaceProjectTab.Agent -> AgentTabContent()
                    WorkspaceProjectTab.Editor -> if (editorContent != null) editorContent() else EditorTabContent()
                    WorkspaceProjectTab.Terminal -> if (terminalContent != null) terminalContent() else TerminalTabContent(isSandboxRunning, onStartSandbox)
                    WorkspaceProjectTab.Git -> if (gitContent != null) gitContent() else GitTabContent(project)
                }
            }
        } else {
            // Medium+ & Large: 3-pane on Large or 2-pane on Medium
            Row(modifier = Modifier.weight(1f)) {
                // Large File Tree
                if (widthClass == DoraiWindowWidthClass.Large) {
                    Box(
                        modifier = Modifier
                            .width(DoraiAdaptiveDefaults.FileTreeWidthLarge)
                            .fillMaxHeight()
                            .background(colors.surface)
                            .border(1.dp, colors.line)
                            .padding(DoraiSpacing.MD),
                    ) {
                        FileTreeColumn()
                    }
                }

                // Center Pane (Editor / Terminal / Git)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    when (activeTab) {
                        WorkspaceProjectTab.Agent, WorkspaceProjectTab.Editor -> if (editorContent != null) editorContent() else EditorTabContent()
                        WorkspaceProjectTab.Terminal -> if (terminalContent != null) terminalContent() else TerminalTabContent(isSandboxRunning, onStartSandbox)
                        WorkspaceProjectTab.Git -> if (gitContent != null) gitContent() else GitTabContent(project)
                    }
                }

                // Right Agent Panel (320dp Medium/Expanded, 380dp Large)
                Box(
                    modifier = Modifier
                        .width(widthClass.workspaceAgentPanelWidth)
                        .fillMaxHeight()
                        .background(colors.surface)
                        .border(1.dp, colors.line),
                ) {
                    AgentTabContent()
                }
            }
        }
    }
}

@Composable
private fun AgentTabContent() {
    val colors = LocalDoraiColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DoraiSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SmartToy, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Agent Activity", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        DoraiToolCard(
            model = ToolExecutionModel(
                toolName = "git_status",
                argsSummary = "--short --branch",
                state = ToolExecutionState.Completed,
                outputText = "## feature/ui-v2-migration\nM composeApp/src/commonMain/kotlin/com/dorai/ai/App.kt",
            ),
        )

        DoraiToolCard(
            model = ToolExecutionModel(
                toolName = "edit_file",
                argsSummary = "DoraiWorkspaceScreen.kt",
                state = ToolExecutionState.PendingApproval,
                diffText = "+ fun DoraiWorkspaceScreen()",
            ),
            onAllowOnce = {},
            onAllowSession = {},
            onDeny = {},
        )
    }
}

@Composable
private fun EditorTabContent() {
    val colors = LocalDoraiColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.codeBackground)
            .padding(DoraiSpacing.MD),
    ) {
        Text(
            text = "1  package com.dorai.ai\n2  \n3  // Dorai AI Workspace Code Editor\n4  fun main() {\n5      println(\"Ready\")\n6  }",
            fontFamily = FontFamily.Monospace,
            color = colors.text,
            fontSize = 13.sp,
            lineHeight = 20.sp,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.codeBackground)
                .padding(DoraiSpacing.MD),
        ) {
            DoraiShellBlock(
                command = "ls -la",
                output = "drwxr-xr-x  6 user user  4096 Nov 8 00:00 .\n-rw-r--r--  1 user user  1248 Nov 8 00:00 build.gradle.kts",
                error = null,
            )
        }
    }
}

@Composable
private fun GitTabContent(project: WorkspaceProjectItem) {
    val colors = LocalDoraiColors.current
    var commitMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DoraiSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
    ) {
        Text("Source Control (${project.changesCount} changes)", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        DoraiDiffBlock(
            diff = "--- a/App.kt\n+++ b/App.kt\n@@ -200,3 +200,4 @@\n+DoraiWorkspaceScreen()",
        )

        OutlinedTextField(
            value = commitMessage,
            onValueChange = { commitMessage = it },
            placeholder = { Text(stringResource(Res.string.workspace_commit_message_hint), color = colors.muted, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedBorderColor = colors.info,
                unfocusedBorderColor = colors.line,
            ),
            shape = RoundedCornerShape(10.dp),
        )

        Button(
            onClick = {},
            enabled = commitMessage.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.workspace_commit_action))
        }
    }
}

@Composable
private fun FileTreeColumn() {
    val colors = LocalDoraiColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Files", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("▾ composeApp", color = colors.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Text("   ▾ src/commonMain", color = colors.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Text("      App.kt", color = colors.ok, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Text("      DoraiTheme.kt", color = colors.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}
