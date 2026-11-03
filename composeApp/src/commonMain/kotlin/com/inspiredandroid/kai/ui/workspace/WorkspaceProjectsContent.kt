package com.inspiredandroid.kai.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.formatFileSize
import com.inspiredandroid.kai.ui.AccentPrimary
import com.inspiredandroid.kai.ui.BorderSoft
import com.inspiredandroid.kai.ui.CardFill
import com.inspiredandroid.kai.ui.DangerAlert
import com.inspiredandroid.kai.ui.KaiShapes
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.components.KaiChip
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.settings.SettingsCard
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import com.inspiredandroid.kai.workspace.WorkspaceAgent
import com.inspiredandroid.kai.workspace.WorkspaceAgents
import com.inspiredandroid.kai.workspace.WorkspaceState
import com.inspiredandroid.kai.workspace.WorkspaceSystemInfo
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.sandbox_files_action_delete
import kai.composeapp.generated.resources.sandbox_files_action_more
import kai.composeapp.generated.resources.sandbox_files_action_rename
import kai.composeapp.generated.resources.sandbox_files_delete_confirm
import kai.composeapp.generated.resources.sandbox_files_delete_message_directory
import kai.composeapp.generated.resources.sandbox_files_delete_title
import kai.composeapp.generated.resources.sandbox_files_rename_confirm
import kai.composeapp.generated.resources.sandbox_files_rename_error_collision
import kai.composeapp.generated.resources.sandbox_files_rename_error_invalid
import kai.composeapp.generated.resources.sandbox_files_rename_label
import kai.composeapp.generated.resources.sandbox_files_rename_title
import kai.composeapp.generated.resources.workspace_new_project_title
import kai.composeapp.generated.resources.workspace_open_with
import kai.composeapp.generated.resources.workspace_projects_cancel_create
import kai.composeapp.generated.resources.workspace_projects_cancel_delete
import kai.composeapp.generated.resources.workspace_projects_cancel_rename
import kai.composeapp.generated.resources.workspace_projects_cancel_uninstall
import kai.composeapp.generated.resources.workspace_projects_create
import kai.composeapp.generated.resources.workspace_projects_empty
import kai.composeapp.generated.resources.workspace_projects_new_placeholder
import kai.composeapp.generated.resources.workspace_projects_session_open
import kai.composeapp.generated.resources.workspace_projects_sessions_open
import kai.composeapp.generated.resources.workspace_session_shell
import kai.composeapp.generated.resources.workspace_system_disk_free
import kai.composeapp.generated.resources.workspace_system_disk_projects
import kai.composeapp.generated.resources.workspace_system_disk_system
import kai.composeapp.generated.resources.workspace_system_packages
import kai.composeapp.generated.resources.workspace_system_title
import kai.composeapp.generated.resources.workspace_uninstall
import kai.composeapp.generated.resources.workspace_uninstall_message
import kai.composeapp.generated.resources.workspace_uninstall_title
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

/**
 * Landing surface once Debian is ready: pick what a project opens with, open one
 * — new projects come from the plus button in the top bar. The Linux system
 * itself (size, facts, extra agents, uninstall) sits in one card below.
 */
@Composable
internal fun WorkspaceProjectsContent(
    state: WorkspaceState,
    launchAgentId: String?,
    installedAgents: ImmutableList<WorkspaceAgent>,
    onSelectLaunchAgent: (String?) -> Unit,
    onOpenProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onRenameProject: (name: String, newName: String) -> Unit,
    onInstallAgent: (String) -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUninstall by remember { mutableStateOf(false) }
    // The project each dialog is about; null while it is closed.
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val missingAgents = remember(state.installedAgents) {
        WorkspaceAgents.all.filterNot { it.id in state.installedAgents }
    }
    val sessionCounts = remember(state.sessions) {
        state.sessions.groupingBy { it.project }.eachCount()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BuildEnvironmentHeader(state = state)
        }

        if (installedAgents.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.workspace_open_with),
                        style = KaiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                    )
                    KaiChip(
                        selected = launchAgentId == null,
                        onClick = { onSelectLaunchAgent(null) },
                    ) {
                        Text(
                            text = stringResource(Res.string.workspace_session_shell),
                            style = KaiTypography.labelMedium,
                        )
                    }
                    installedAgents.forEach { agent ->
                        KaiChip(
                            selected = launchAgentId == agent.id,
                            onClick = { onSelectLaunchAgent(agent.id) },
                        ) {
                            Text(text = agent.title, style = KaiTypography.labelMedium)
                        }
                    }
                }
            }
        }

        if (state.projects.isEmpty()) {
            item {
                WorkspaceSectionHeader(
                    title = "Projects",
                    subtitle = stringResource(Res.string.workspace_projects_empty),
                )
            }
        } else {
            item {
                WorkspaceSectionHeader(title = "Projects")
            }
        }

        items(state.projects, key = { it }) { project ->
            val cardColors = CardDefaults.cardColors(
                containerColor = com.inspiredandroid.kai.ui.theme.DesignTokens.SurfaceElevated,
            )
            val cardBorder = androidx.compose.foundation.BorderStroke(1.dp, com.inspiredandroid.kai.ui.theme.DesignTokens.BorderSubtle)

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenProject(project) },
                colors = cardColors,
                border = cardBorder,
                shape = KaiShapes.card,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Project folder",
                        tint = AccentPrimary,
                    )
                    Text(
                        text = project,
                        style = KaiTypography.titleMedium,
                        color = LocalDesignColorTokens.current.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    // Shells left behind here: the list is the only place they can be
                    // found from, now that stepping out of a project keeps them.
                    val open = sessionCounts[project] ?: 0
                    if (open > 0) {
                        Text(
                            text = openSessionsLabel(open),
                            style = KaiTypography.labelMedium,
                            color = AccentPrimary,
                        )
                    }
                    ProjectRowMenu(
                        onRename = { renaming = project },
                        onDelete = { deleting = project },
                    )
                }
            }
        }

        state.lastError?.let { error ->
            item {
                Text(
                    text = error,
                    style = KaiTypography.bodySmall,
                    color = DangerAlert,
                )
            }
        }

        item {
            BuildSystemCard(
                info = state.systemInfo,
                onUninstall = { showUninstall = true },
            )
        }

        if (missingAgents.isNotEmpty()) {
            item {
                WorkspaceAgentManagerCard(
                    missingAgents = missingAgents,
                    onInstallAgent = onInstallAgent,
                )
            }
        }
    }

    renaming?.let { project ->
        RenameProjectDialog(
            project = project,
            projects = state.projects,
            onDismiss = { renaming = null },
            onRename = { newName ->
                renaming = null
                onRenameProject(project, newName)
            },
        )
    }

    deleting?.let { project ->
        DeleteProjectDialog(
            project = project,
            openSessions = sessionCounts[project] ?: 0,
            onDismiss = { deleting = null },
            onDelete = {
                deleting = null
                onDeleteProject(project)
            },
        )
    }

    if (showUninstall) {
        AlertDialog(
            onDismissRequest = { showUninstall = false },
            title = { Text(stringResource(Res.string.workspace_uninstall_title)) },
            text = { Text(stringResource(Res.string.workspace_uninstall_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUninstall = false
                        onUninstall()
                    },
                    modifier = Modifier.handCursor(),
                ) { Text(stringResource(Res.string.workspace_uninstall)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUninstall = false },
                    modifier = Modifier.handCursor(),
                ) { Text(stringResource(Res.string.workspace_projects_cancel_uninstall)) }
            },
        )
    }
}

/** How many shells the project has running, said in words. */
@Composable
private fun openSessionsLabel(open: Int): String = if (open == 1) {
    stringResource(Res.string.workspace_projects_session_open)
} else {
    stringResource(Res.string.workspace_projects_sessions_open, open)
}

/**
 * Rename and delete for one project. Kept behind an overflow so the row's own tap
 * stays the thing it looks like — opening the project.
 */
@Composable
private fun ProjectRowMenu(
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.handCursor(),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.sandbox_files_action_more),
                tint = LocalDesignColorTokens.current.textSecondary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = KaiShapes.card,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.sandbox_files_action_rename)) },
                onClick = {
                    expanded = false
                    onRename()
                },
                modifier = Modifier.handCursor(),
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.sandbox_files_action_delete)) },
                onClick = {
                    expanded = false
                    onDelete()
                },
                modifier = Modifier.handCursor(),
            )
        }
    }
}

/**
 * New name for an existing project folder. The list is right here, so a name that
 * is already taken is caught before the rename is attempted rather than reported
 * as a silent no-op.
 */
@Composable
private fun RenameProjectDialog(
    project: String,
    projects: ImmutableList<String>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by rememberSaveable(project) { mutableStateOf(project) }
    val trimmed = name.trim()
    val error = when {
        trimmed.isEmpty() || trimmed.contains('/') -> Res.string.sandbox_files_rename_error_invalid
        trimmed != project && trimmed in projects -> Res.string.sandbox_files_rename_error_collision
        else -> null
    }
    val rename = { if (error == null) onRename(trimmed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sandbox_files_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.sandbox_files_rename_label)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { res -> { Text(stringResource(res)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { rename() }),
            )
        },
        confirmButton = {
            TextButton(
                onClick = rename,
                enabled = error == null,
                modifier = Modifier.handCursor(),
            ) { Text(stringResource(Res.string.sandbox_files_rename_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.handCursor()) {
                Text(stringResource(Res.string.workspace_projects_cancel_rename))
            }
        },
    )
}

/** Deleting takes the folder's contents with it, and any shell still open in it. */
@Composable
private fun DeleteProjectDialog(
    project: String,
    openSessions: Int,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sandbox_files_delete_title, project)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.sandbox_files_delete_message_directory))
                if (openSessions > 0) {
                    Text(
                        text = openSessionsLabel(openSessions),
                        style = KaiTypography.bodySmall,
                        color = DangerAlert,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete, modifier = Modifier.handCursor()) {
                Text(
                    text = stringResource(Res.string.sandbox_files_delete_confirm),
                    color = DangerAlert,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.handCursor()) {
                Text(stringResource(Res.string.workspace_projects_cancel_delete))
            }
        },
    )
}

/** What the Linux install is and what it costs, plus the uninstall action. */
@Composable
private fun BuildSystemCard(
    info: WorkspaceSystemInfo?,
    onUninstall: () -> Unit,
) {
    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        WorkspaceSectionHeader(title = stringResource(Res.string.workspace_system_title))
        if (info != null) {
            Text(
                text = "${info.distribution} · ${info.architecture}",
                style = KaiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
            if (info.packageCount > 0) {
                Text(
                    text = stringResource(Res.string.workspace_system_packages, info.packageCount),
                    style = KaiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            SystemInfoRow(
                label = stringResource(Res.string.workspace_system_disk_system),
                value = formatFileSize(info.systemBytes),
            )
            SystemInfoRow(
                label = stringResource(Res.string.workspace_system_disk_projects),
                value = formatFileSize(info.projectsBytes),
            )
            SystemInfoRow(
                label = stringResource(Res.string.workspace_system_disk_free),
                value = formatFileSize(info.freeBytes),
            )
        }

        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onUninstall,
            modifier = Modifier.handCursor(),
        ) {
            Text(
                text = stringResource(Res.string.workspace_uninstall),
                color = DangerAlert,
            )
        }
    }
}

/**
 * Explicit Agent Manager card (Plan 3, Phase 2): the agents you can install are
 * their own surface rather than an afterthought below the system facts, so it is
 * clear Build can grow beyond a plain shell.
 */
@Composable
private fun WorkspaceAgentManagerCard(
    missingAgents: List<WorkspaceAgent>,
    onInstallAgent: (String) -> Unit,
) {
    if (missingAgents.isEmpty()) return
    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        WorkspaceSectionHeader(
            title = "Agents",
            subtitle = "Install an agent to open projects with it instead of a plain shell.",
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            missingAgents.forEach { agent ->
                KaiChip(onClick = { onInstallAgent(agent.id) }) {
                    Text(text = agent.title, style = KaiTypography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = KaiTypography.bodyMedium,
            color = LocalDesignColorTokens.current.textSecondary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = KaiTypography.bodyMedium,
            color = LocalDesignColorTokens.current.textPrimary,
            textAlign = TextAlign.End,
        )
    }
}

/** Reached from the plus button in the top bar; creating opens the project right away. */
@Composable
internal fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val create = { if (name.isNotBlank()) onCreate(name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.workspace_new_project_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.workspace_projects_new_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { create() }),
            )
        },
        confirmButton = {
            TextButton(
                onClick = create,
                enabled = name.isNotBlank(),
                modifier = Modifier.handCursor(),
            ) { Text(stringResource(Res.string.workspace_projects_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.handCursor()) {
                Text(stringResource(Res.string.workspace_projects_cancel_create))
            }
        },
    )
}
