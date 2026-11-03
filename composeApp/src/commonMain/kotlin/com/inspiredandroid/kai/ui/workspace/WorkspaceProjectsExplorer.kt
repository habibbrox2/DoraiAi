package com.inspiredandroid.kai.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.components.KaiChip
import com.inspiredandroid.kai.ui.theme.DesignTokens
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.workspace_projects_cancel_create
import kai.composeapp.generated.resources.workspace_projects_create
import kai.composeapp.generated.resources.workspace_projects_new_placeholder
import org.jetbrains.compose.resources.stringResource

/** Filter chip state for the project explorer. */
enum class ProjectFilter {
    All,
    Modified,
    AgentTouched,
}

/** A file entry in the explorer tree. */
@Immutable
data class ProjectFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val gitStatus: GitStatus? = null,
    val touchedByAgent: Boolean = false,
    val isCurrentFile: Boolean = false,
)

enum class GitStatus {
    Added,
    Modified,
    Deleted,
}

/** Projects & File Explorer drawer content. */
@Composable
fun WorkspaceProjectsExplorer(
    currentProject: String?,
    projects: List<String>,
    files: List<ProjectFile>,
    onOpenProject: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onFilterChanged: (ProjectFilter) -> Unit,
    selectedFilter: ProjectFilter = ProjectFilter.All,
    modifier: Modifier = Modifier,
) {
    val colorTokens = LocalDesignColorTokens.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxHeight(),
    ) {
        // ─── Project header ───
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(DesignTokens.SurfaceElevated)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = DesignTokens.AccentPrimary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = WorkspaceSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = currentProject ?: "No project",
                        style = KaiTypography.titleMedium,
                        color = colorTokens.textPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = if (projects.isEmpty()) "Create a project to get started" else "${projects.size} project${if (projects.size != 1) "s" else ""}",
                        style = KaiTypography.labelSmall,
                        color = colorTokens.textSecondary,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = colorTokens.textSecondary,
                    )
                }
            }
        }

        // ─── File search ───
        TextField(
            value = "",
            onValueChange = { },
            placeholder = { Text(text = "Search files", style = KaiTypography.bodyMedium) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WorkspaceSpacing.sm),
            singleLine = true,
        )

        // ─── Filter chips ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProjectFilterChip(
                label = "All files",
                selected = selectedFilter == ProjectFilter.All,
                onClick = { onFilterChanged(ProjectFilter.All) },
            )
            ProjectFilterChip(
                label = "Modified 3",
                selected = selectedFilter == ProjectFilter.Modified,
                onClick = { onFilterChanged(ProjectFilter.Modified) },
            )
            ProjectFilterChip(
                label = "Agent touched 2",
                selected = selectedFilter == ProjectFilter.AgentTouched,
                onClick = { onFilterChanged(ProjectFilter.AgentTouched) },
            )
        }

        // ─── File tree ───
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = WorkspaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (files.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = WorkspaceSpacing.lg),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = colorTokens.textMuted,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Text(
                            text = "No files yet",
                            style = KaiTypography.bodyMedium,
                            color = colorTokens.textMuted,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            } else {
                items(files, key = { it.path }) { file ->
                    ProjectFileRow(
                        file = file,
                        onClick = { onOpenFile(file.path) },
                    )
                }
            }
        }

        // ─── Footer ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(Res.string.workspace_projects_create),
                    style = KaiTypography.labelMedium,
                )
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(text = stringResource(Res.string.workspace_projects_new_placeholder)) },
                text = {
                    TextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCreateDialog = false
                            val name = newProjectName.trim()
                            newProjectName = ""
                            if (name.isNotEmpty()) onCreateProject(name)
                        },
                    ) {
                        Text(text = stringResource(Res.string.workspace_projects_create))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text(text = stringResource(Res.string.workspace_projects_cancel_create))
                    }
                },
            )
        }
    }
}

/** Filter chip for project explorer. */
@Composable
private fun ProjectFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colorTokens = LocalDesignColorTokens.current
    KaiChip(
        selected = selected,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = KaiTypography.labelMedium,
            color = if (selected) Color(0xFF0B1A33) else colorTokens.textPrimary,
        )
    }
}

/** Single file row in the explorer tree. */
@Composable
private fun ProjectFileRow(
    file: ProjectFile,
    onClick: () -> Unit,
) {
    val colorTokens = LocalDesignColorTokens.current
    val bgColor = if (file.isCurrentFile) Color(0xFF1B2A44) else Color(0xFF0F1419)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .background(bgColor)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Indent based on depth
        val depth = file.path.count { it == '/' }
        Spacer(
            modifier = Modifier.width((depth * 16).dp),
        )

        // Icon
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.FileCopy,
            contentDescription = null,
            tint = if (file.isDirectory) colorTokens.textSecondary else colorTokens.textMuted,
            modifier = Modifier.size(18.dp),
        )

        // Name
        Text(
            text = file.name,
            style = KaiTypography.bodyMedium,
            color = colorTokens.textPrimary,
            modifier = Modifier.weight(1f).padding(start = WorkspaceSpacing.xs),
        )

        // Git status badge
        file.gitStatus?.let { status ->
            val (badgeColor, badgeText) = when (status) {
                GitStatus.Added -> Pair(DesignTokens.Success, "A")
                GitStatus.Modified -> Pair(DesignTokens.Warning, "M")
                GitStatus.Deleted -> Pair(DesignTokens.Error, "D")
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(badgeColor)
                    .clip(RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badgeText,
                    style = KaiTypography.labelSmall,
                    color = Color(0xFF0F1419),
                )
            }
        }

        // Agent tag
        if (file.touchedByAgent) {
            Box(
                modifier = Modifier
                    .padding(horizontal = WorkspaceSpacing.xs)
                    .background(Color(0xFF1B2A44))
                    .clip(RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Agent",
                    style = KaiTypography.labelSmall.copy(fontSize = 9.sp),
                    color = DesignTokens.AccentPrimary,
                )
            }
        }
    }
}
