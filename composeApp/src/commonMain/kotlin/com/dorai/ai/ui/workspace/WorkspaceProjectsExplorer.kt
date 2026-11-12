package com.dorai.ai.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.DoraiChip
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.workspace.WorkspaceGitStatus
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.workspace_explorer_clear_search
import dorai.composeapp.generated.resources.workspace_explorer_create_hint
import dorai.composeapp.generated.resources.workspace_explorer_error
import dorai.composeapp.generated.resources.workspace_explorer_filter_all
import dorai.composeapp.generated.resources.workspace_explorer_filter_files
import dorai.composeapp.generated.resources.workspace_explorer_filter_folders
import dorai.composeapp.generated.resources.workspace_explorer_filter_modified
import dorai.composeapp.generated.resources.workspace_explorer_loading
import dorai.composeapp.generated.resources.workspace_explorer_no_files
import dorai.composeapp.generated.resources.workspace_explorer_no_matches
import dorai.composeapp.generated.resources.workspace_explorer_no_project
import dorai.composeapp.generated.resources.workspace_explorer_projects_one
import dorai.composeapp.generated.resources.workspace_explorer_projects_other
import dorai.composeapp.generated.resources.workspace_explorer_refresh
import dorai.composeapp.generated.resources.workspace_explorer_retry
import dorai.composeapp.generated.resources.workspace_explorer_search_placeholder
import dorai.composeapp.generated.resources.workspace_new_project_content_description
import dorai.composeapp.generated.resources.workspace_projects_create
import org.jetbrains.compose.resources.stringResource

/** Filter chip state for the project explorer. */
enum class ProjectFilter {
    All,
    Files,
    Folders,
    Modified,
}

/** A file entry in the explorer tree. */
@Immutable
data class ProjectFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val gitStatus: WorkspaceGitStatus? = null,
    val isCurrentFile: Boolean = false,
)

/**
 * Pure in-memory filter for the explorer list. Runs on the already-loaded
 * listing only — no filesystem access, so large directories never block the UI.
 * Exported for tests.
 */
fun filterProjectFiles(
    files: List<ProjectFile>,
    query: String,
    filter: ProjectFilter,
): List<ProjectFile> {
    val trimmed = query.trim()
    return files.filter { file ->
        val matchesQuery = trimmed.isEmpty() ||
            file.name.contains(trimmed, ignoreCase = true) ||
            file.path.contains(trimmed, ignoreCase = true)
        val matchesFilter = when (filter) {
            ProjectFilter.All -> true
            ProjectFilter.Files -> !file.isDirectory
            ProjectFilter.Folders -> file.isDirectory
            ProjectFilter.Modified -> file.gitStatus != null
        }
        matchesQuery && matchesFilter
    }
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
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onRefresh: (() -> Unit)? = null,
) {
    val colorTokens = LocalDesignColorTokens.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ProjectFilter.All) }

    val modifiedCount = files.count { it.gitStatus != null }
    val visibleFiles = filterProjectFiles(files, searchQuery, selectedFilter)

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
                        .background(LocalDesignColorTokens.current.surfaceElevated)
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
                        text = currentProject ?: stringResource(Res.string.workspace_explorer_no_project),
                        style = DoraiTypography.titleMedium,
                        color = colorTokens.textPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = if (projects.isEmpty()) {
                            stringResource(Res.string.workspace_explorer_create_hint)
                        } else if (projects.size == 1) {
                            stringResource(Res.string.workspace_explorer_projects_one)
                        } else {
                            stringResource(Res.string.workspace_explorer_projects_other, projects.size)
                        },
                        style = DoraiTypography.labelSmall,
                        color = colorTokens.textSecondary,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(Res.string.workspace_new_project_content_description),
                        tint = colorTokens.textSecondary,
                    )
                }
            }
        }

        // ─── File search ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WorkspaceSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = stringResource(Res.string.workspace_explorer_search_placeholder), style = DoraiTypography.bodyMedium) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(Res.string.workspace_explorer_clear_search),
                            )
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(Res.string.workspace_explorer_refresh),
                        tint = colorTokens.textSecondary,
                    )
                }
            }
        }

        // ─── Filter chips ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProjectFilterChip(
                label = stringResource(Res.string.workspace_explorer_filter_all),
                selected = selectedFilter == ProjectFilter.All,
                onClick = { selectedFilter = ProjectFilter.All },
            )
            ProjectFilterChip(
                label = stringResource(Res.string.workspace_explorer_filter_files),
                selected = selectedFilter == ProjectFilter.Files,
                onClick = { selectedFilter = ProjectFilter.Files },
            )
            ProjectFilterChip(
                label = stringResource(Res.string.workspace_explorer_filter_folders),
                selected = selectedFilter == ProjectFilter.Folders,
                onClick = { selectedFilter = ProjectFilter.Folders },
            )
            ProjectFilterChip(
                label = stringResource(Res.string.workspace_explorer_filter_modified, modifiedCount),
                selected = selectedFilter == ProjectFilter.Modified,
                onClick = { selectedFilter = ProjectFilter.Modified },
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
            when {
                isLoading && files.isEmpty() -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = WorkspaceSpacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(WorkspaceSpacing.sm),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(Res.string.workspace_explorer_loading),
                                style = DoraiTypography.bodyMedium,
                                color = colorTokens.textMuted,
                            )
                        }
                    }
                }

                error != null && files.isEmpty() -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = WorkspaceSpacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(WorkspaceSpacing.sm),
                        ) {
                            Text(
                                text = stringResource(Res.string.workspace_explorer_error),
                                style = DoraiTypography.bodyMedium,
                                color = DesignTokens.Error,
                            )
                            if (onRefresh != null) {
                                TextButton(onClick = onRefresh) {
                                    Text(
                                        text = stringResource(Res.string.workspace_explorer_retry),
                                        style = DoraiTypography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                visibleFiles.isEmpty() -> {
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
                                text = if (files.isEmpty()) {
                                    stringResource(Res.string.workspace_explorer_no_files)
                                } else {
                                    stringResource(Res.string.workspace_explorer_no_matches)
                                },
                                style = DoraiTypography.bodyMedium,
                                color = colorTokens.textMuted,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                }

                else -> {
                    items(visibleFiles, key = { it.path }) { file ->
                        ProjectFileRow(
                            file = file,
                            onClick = { onOpenFile(file.path) },
                        )
                    }
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
                    style = DoraiTypography.labelMedium,
                )
            }
        }

        if (showCreateDialog) {
            CreateProjectDialog(
                projects = projects,
                onDismiss = { showCreateDialog = false },
                onCreate = { name ->
                    showCreateDialog = false
                    onCreateProject(name)
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
    DoraiChip(
        selected = selected,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = DoraiTypography.labelMedium,
            color = if (selected) DesignTokens.PrimaryCyan else colorTokens.textPrimary,
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
    val bgColor = if (file.isCurrentFile) DesignTokens.SurfaceRaised else DesignTokens.BackgroundOled

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .background(bgColor)
            .clip(DoraiShapes.sm)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flat list rendered for the currently-opened project — no inter-tree indentation.
        // (An absolute-path depth here pushed every row's content off-screen when the
        // project was nested deep in the guest filesystem.)
        Spacer(modifier = Modifier.width(8.dp))

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
            style = DoraiTypography.bodyMedium,
            color = colorTokens.textPrimary,
            modifier = Modifier.weight(1f).padding(start = WorkspaceSpacing.xs),
        )

        // Git status badge
        file.gitStatus?.let { status ->
            val (badgeColor, badgeText) = when (status) {
                WorkspaceGitStatus.Added -> Pair(DesignTokens.Success, "A")
                WorkspaceGitStatus.Modified -> Pair(DesignTokens.Warning, "M")
                WorkspaceGitStatus.Deleted -> Pair(DesignTokens.Error, "D")
                WorkspaceGitStatus.Renamed -> Pair(DesignTokens.AccentPrimary, "R")
                WorkspaceGitStatus.Untracked -> Pair(colorTokens.textMuted, "U")
                WorkspaceGitStatus.Conflicted -> Pair(DesignTokens.Error, "!")
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
                    style = DoraiTypography.labelSmall,
                    color = DesignTokens.BrandInk,
                )
            }
        }
    }
}
