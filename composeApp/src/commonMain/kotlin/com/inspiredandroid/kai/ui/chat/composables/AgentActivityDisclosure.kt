package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.filereview.FileReviewPreview
import com.inspiredandroid.kai.filereview.FileReviewProposal
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.chat.ChatActions
import com.inspiredandroid.kai.ui.chat.ChatSpacing
import com.inspiredandroid.kai.ui.chat.activities.ActivityDetails
import com.inspiredandroid.kai.ui.chat.activities.ActivityStatus
import com.inspiredandroid.kai.ui.chat.activities.AgentActivity
import com.inspiredandroid.kai.ui.chat.activities.formatAgentRunDuration
import com.inspiredandroid.kai.ui.chat.activities.summarizeAgentRun
import com.inspiredandroid.kai.ui.theme.DesignTokens
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import kotlin.time.Clock

private val secretAssignment = Regex(
    """(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|password|secret|authorization)(["']?\s*[:=]\s*["']?)([^"'\s,}]+)""",
)
private val bearerToken = Regex("""(?i)(Bearer\s+)[A-Za-z0-9._~+/-]+=*""")

/** A turn-scoped execution trace; collapsing it never cancels runtime work. */
@Composable
internal fun AgentActivityDisclosure(
    activities: List<AgentActivity>,
    modifier: Modifier = Modifier,
    fileReviewProposals: List<FileReviewProposal> = emptyList(),
    fileReviewPreviews: Map<String, FileReviewPreview> = emptyMap(),
    actions: ChatActions? = null,
) {
    if (activities.isEmpty()) return
    val runId = activities.first().runId
    var expanded by remember(runId) {
        mutableStateOf(activities.any { it.status == ActivityStatus.PENDING || it.status == ActivityStatus.RUNNING })
    }
    val runSummary = summarizeAgentRun(activities, Clock.System.now().toEpochMilliseconds())
    val completed = runSummary.completed
    val running = runSummary.running
    val failed = runSummary.failed
    val summary = buildList {
        add("${runSummary.total} steps")
        if (runSummary.fileCount > 0) add("${runSummary.fileCount} files")
        if (runSummary.terminalCount > 0) add("${runSummary.terminalCount} terminal")
        add(formatAgentRunDuration(runSummary.durationMs))
        if (running > 0) add("$running running")
        if (completed > 0) add("$completed done")
        if (failed > 0) add("$failed failed")
    }.joinToString(" · ")
    val overallColor = when {
        failed > 0 -> DesignTokens.Error
        running > 0 -> DesignTokens.BrandCyan
        else -> DesignTokens.Success
    }

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = ChatSpacing.lg, vertical = ChatSpacing.xs),
        shape = RoundedCornerShape(14.dp),
        color = DesignTokens.SurfaceDark,
        border = BorderStroke(1.dp, DesignTokens.BorderSubtle),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { role = Role.Button }
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(overallColor))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Agent run", style = KaiTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = LocalDesignColorTokens.current.textPrimary)
                    Text(summary, style = KaiTypography.labelSmall, color = LocalDesignColorTokens.current.textSecondary)
                }
                Text(activities.size.toString(), style = KaiTypography.labelSmall.copy(fontWeight = FontWeight.Bold), color = LocalDesignColorTokens.current.textTertiary)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse activity" else "Expand activity",
                    tint = LocalDesignColorTokens.current.textTertiary,
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    activities.forEach { activity ->
                        val fileOperation = activity.details as? ActivityDetails.FileOperation
                        val proposal = fileOperation?.proposalId?.let { id ->
                            fileReviewProposals.firstOrNull { it.id == id }
                        }
                        val preview = fileOperation?.proposalId?.let(fileReviewPreviews::get)
                        AgentActivityRow(activity, proposal, preview, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentActivityRow(
    activity: AgentActivity,
    proposal: FileReviewProposal?,
    preview: FileReviewPreview?,
    actions: ChatActions?,
) {
    var detailsExpanded by remember(activity.id) {
        mutableStateOf(activity.status == ActivityStatus.RUNNING || activity.status == ActivityStatus.FAILED)
    }
    val statusColor = activityStatusColor(activity.status)
    Surface(color = DesignTokens.SurfaceElevated, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, DesignTokens.BorderSubtle)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable { detailsExpanded = !detailsExpanded }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                Column(modifier = Modifier.weight(1f)) {
                    Text(activity.title, style = KaiTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = LocalDesignColorTokens.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        activityStatusLabel(activity.status) + (activity.durationMs?.let { " · ${formatDuration(it)}" } ?: ""),
                        style = KaiTypography.labelSmall,
                        color = statusColor,
                    )
                }
                Icon(
                    imageVector = if (detailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (detailsExpanded) "Hide details" else "Show details",
                    tint = LocalDesignColorTokens.current.textTertiary,
                )
            }
            AnimatedVisibility(visible = detailsExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when (val details = activity.details) {
                        is ActivityDetails.ToolCall -> {
                            Text(details.toolDisplayName ?: details.toolName, style = KaiTypography.labelSmall, color = DesignTokens.BrandCyan)
                            details.arguments?.takeIf { it.isNotBlank() }?.let { ActivityCodePreview("Input", it) }
                            details.error?.takeIf { it.isNotBlank() }?.let { ActivityCodePreview("Error", it, DesignTokens.Error) }
                            details.result?.takeIf { it.isNotBlank() }?.let { ActivityCodePreview("Result", it) }
                        }

                        is ActivityDetails.ShellCommand -> {
                            ActivityCodePreview("Command", details.command)
                            details.workingDirectory?.takeIf { it.isNotBlank() }?.let {
                                Text("Working directory · ${safeActivityPreview(it, 240)}", style = KaiTypography.labelSmall, color = LocalDesignColorTokens.current.textTertiary)
                            }
                            details.stdout?.takeIf { it.isNotBlank() }?.let { ActivityCodePreview("Output", it) }
                            details.stderr?.takeIf { it.isNotBlank() }?.let { ActivityCodePreview("Stderr", it, DesignTokens.Error) }
                            details.exitCode?.let {
                                Text("Exit code · $it", style = KaiTypography.labelSmall, color = if (it == 0) DesignTokens.Success else DesignTokens.Error)
                            }
                        }

                        is ActivityDetails.FileOperation -> {
                            Text("${details.operation} · ${safeActivityPreview(details.path, 240)}", style = KaiTypography.labelSmall.copy(fontWeight = FontWeight.Medium), color = DesignTokens.BrandCyan)
                            FileChangeReviewCard(
                                activityId = activity.id,
                                details = details,
                                status = activity.status,
                                proposal = proposal,
                                preview = preview,
                                onAccept = actions?.acceptFileChange ?: {},
                                onReject = actions?.rejectFileChange ?: {},
                                onRollback = actions?.rollbackFileChange ?: {},
                            )
                        }

                        is ActivityDetails.Generic -> details.content?.let { ActivityCodePreview("Details", it) }

                        is ActivityDetails.Skill -> {
                            Text(details.skillName, style = KaiTypography.labelSmall, color = LocalDesignColorTokens.current.textPrimary)
                            details.description?.let { Text(safeActivityPreview(it, 500), style = KaiTypography.labelSmall, color = LocalDesignColorTokens.current.textSecondary) }
                        }

                        is ActivityDetails.Permission -> {
                            Text("${details.action} · ${details.target}", style = KaiTypography.labelSmall, color = LocalDesignColorTokens.current.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ActivityCodePreview(label: String, value: String, valueColor: Color = LocalDesignColorTokens.current.textSecondary) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = KaiTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = LocalDesignColorTokens.current.textTertiary)
        Text(
            text = safeActivityPreview(value),
            style = KaiTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = valueColor,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun safeActivityPreview(value: String, maxLength: Int = 1200): String {
    val redacted = value.replace(secretAssignment, "$1$2[REDACTED]").replace(bearerToken, "$1[REDACTED]")
    return if (redacted.length > maxLength) redacted.take(maxLength) + "…" else redacted
}

private fun activityStatusColor(status: ActivityStatus): Color = when (status) {
    ActivityStatus.PENDING -> DesignTokens.MutedText
    ActivityStatus.RUNNING -> DesignTokens.BrandCyan
    ActivityStatus.COMPLETED -> DesignTokens.Success
    ActivityStatus.FAILED, ActivityStatus.TIMEOUT -> DesignTokens.Error
    ActivityStatus.CANCELLED -> DesignTokens.TertiaryText
    ActivityStatus.DENIED -> DesignTokens.Warning
}

private fun activityStatusLabel(status: ActivityStatus): String = when (status) {
    ActivityStatus.PENDING -> "Queued"
    ActivityStatus.RUNNING -> "Running"
    ActivityStatus.COMPLETED -> "Completed"
    ActivityStatus.FAILED -> "Failed"
    ActivityStatus.CANCELLED -> "Cancelled"
    ActivityStatus.DENIED -> "Denied"
    ActivityStatus.TIMEOUT -> "Timed out"
}

private fun formatDuration(durationMs: Long): String = when {
    durationMs < 1_000 -> "${durationMs}ms"
    durationMs < 60_000 -> "${durationMs / 1_000}.${(durationMs % 1_000) / 100}s"
    else -> "${durationMs / 60_000}m ${(durationMs % 60_000) / 1_000}s"
}




