package com.dorai.ai.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.components.v2.DoraiDiffBlock
import com.dorai.ai.ui.components.v2.DoraiModalSheet
import com.dorai.ai.workspace.WorkspaceGitChange
import com.dorai.ai.workspace.WorkspaceGitDiff
import com.dorai.ai.workspace.WorkspaceGitError
import com.dorai.ai.workspace.WorkspaceGitStatus
import dorai.composeapp.generated.resources.Res
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
import dorai.composeapp.generated.resources.workspace_git_unstage
import dorai.composeapp.generated.resources.workspace_git_unstaged
import dorai.composeapp.generated.resources.workspace_projects_cancel_create
import org.jetbrains.compose.resources.stringResource

/**
 * UI v2 Git panel for the open project — a real worktree view fed by
 * [WorkspaceViewModel.gitState]. Lists every changed path (staged + unstaged),
 * lets the user stage/unstage/discard, and shows the selected file's diff.
 * No invented content: empty states reflect the actual `git status` result.
 */
@Composable
fun WorkspaceGitPanel(
    projectName: String?,
    branch: String?,
    loading: Boolean,
    changes: List<WorkspaceGitChange>,
    error: WorkspaceGitError?,
    isRepository: Boolean,
    gitActionInProgress: Boolean,
    gitDiff: WorkspaceGitDiff?,
    onLoadDiff: (String) -> Unit,
    onClearDiff: () -> Unit,
    onStageGitFile: (String, String) -> Unit,
    onUnstageGitFile: (String, String) -> Unit,
    onDiscardGitFile: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    var discardPath by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(DoraiSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
    ) {
        Text(
            text = stringResource(Res.string.workspace_git_changes_title, projectName ?: "—"),
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        if (branch != null) {
            Text(
                text = stringResource(Res.string.workspace_git_branch, branch),
                color = colors.muted,
                fontSize = 12.sp,
            )
        }

        when {
            projectName == null -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = DoraiSpacing.MD),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.workspace_git_no_project),
                    color = colors.muted,
                    fontSize = 13.sp,
                )
            }

            loading && changes.isEmpty() -> Text(
                text = stringResource(Res.string.workspace_git_loading),
                color = colors.muted,
                fontSize = 13.sp,
            )

            !loading && error == WorkspaceGitError.NotRepository -> Text(
                text = stringResource(Res.string.workspace_git_not_repository),
                color = colors.warn,
                fontSize = 13.sp,
            )

            !loading && error == WorkspaceGitError.MissingGit -> Text(
                text = stringResource(Res.string.workspace_git_missing_git),
                color = colors.warn,
                fontSize = 13.sp,
            )

            !loading && error == WorkspaceGitError.Failed -> Text(
                text = stringResource(Res.string.workspace_git_failed),
                color = colors.err,
                fontSize = 13.sp,
            )

            changes.isEmpty() && isRepository -> Text(
                text = stringResource(Res.string.workspace_git_no_changes),
                color = colors.ok,
                fontSize = 13.sp,
            )

            else -> changes.forEach { change ->
                GitChangeRow(
                    change = change,
                    gitActionInProgress = gitActionInProgress,
                    onToggleStage = {
                        val project = projectName
                        if (change.staged) {
                            onUnstageGitFile(project, change.path)
                        } else {
                            onStageGitFile(project, change.path)
                        }
                    },
                    onOpenDiff = { onLoadDiff(change.path) },
                    onDiscard = { discardPath = change.path },
                )
            }
        }
    }

    // Diff sheet for the selected change
    if (gitDiff != null) {
        DoraiModalSheet(onDismissRequest = onClearDiff) {
            val sheetColors = LocalDoraiColors.current
            val combined = listOf(gitDiff.stagedDiff, gitDiff.unstagedDiff)
                .filter { it.isNotBlank() }
                .joinToString("\n")
            Column(modifier = Modifier.fillMaxWidth().padding(DoraiSpacing.LG)) {
                Text(
                    text = stringResource(Res.string.workspace_git_diff_title, gitDiff.path.substringAfterLast('/')),
                    color = sheetColors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(DoraiSpacing.MD))
                if (combined.isNotBlank()) {
                    DoraiDiffBlock(diff = combined)
                } else {
                    Text(
                        text = stringResource(Res.string.workspace_git_diff_error),
                        color = sheetColors.err,
                        fontSize = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.height(DoraiSpacing.MD))
                TextButton(onClick = onClearDiff, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.workspace_projects_cancel_create), color = sheetColors.primary)
                }
            }
        }
    }

    // Discard confirmation
    discardPath?.let { path ->
        AlertDialog(
            onDismissRequest = { discardPath = null },
            title = { Text(stringResource(Res.string.workspace_git_discard_title)) },
            text = { Text(stringResource(Res.string.workspace_git_discard_message, path)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        projectName?.let { project -> onDiscardGitFile(project, path) }
                        discardPath = null
                    },
                ) {
                    Text(stringResource(Res.string.workspace_git_discard), color = colors.err)
                }
            },
            dismissButton = {
                TextButton(onClick = { discardPath = null }) {
                    Text(
                        text = stringResource(Res.string.workspace_projects_cancel_create),
                        color = colors.muted,
                    )
                }
            },
        )
    }
}

@Composable
private fun GitChangeRow(
    change: WorkspaceGitChange,
    gitActionInProgress: Boolean,
    onToggleStage: () -> Unit,
    onOpenDiff: () -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = LocalDoraiColors.current
    val (badgeColor, badgeText) = when (change.status) {
        WorkspaceGitStatus.Added -> colors.ok to "A"
        WorkspaceGitStatus.Modified -> colors.warn to "M"
        WorkspaceGitStatus.Deleted -> colors.err to "D"
        WorkspaceGitStatus.Renamed -> colors.info to "R"
        WorkspaceGitStatus.Untracked -> colors.muted to "U"
        WorkspaceGitStatus.Conflicted -> colors.err to "!"
    }
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onOpenDiff, role = Role.Button)
            .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badgeText,
                color = colors.background,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = change.path.substringAfterLast('/'),
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = change.path.replace(Regex("^/root/projects/[^/]+/"), ""),
                color = colors.muted,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(if (change.staged) Res.string.workspace_git_staged else Res.string.workspace_git_unstaged),
                color = colors.muted,
                fontSize = 10.5.sp,
            )
        }
        IconButton(onClick = onToggleStage, enabled = !gitActionInProgress) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(if (change.staged) Res.string.workspace_git_unstage else Res.string.workspace_git_stage),
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onOpenDiff) {
            Icon(
                Icons.Default.Code,
                contentDescription = stringResource(Res.string.workspace_git_diff),
                tint = colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onDiscard, enabled = !gitActionInProgress) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(Res.string.workspace_git_discard),
                tint = colors.err,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
