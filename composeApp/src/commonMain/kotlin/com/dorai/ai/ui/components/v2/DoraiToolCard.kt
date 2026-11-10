package com.dorai.ai.ui.components.v2

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.engine.doraiSurfaceStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.rememberCopyToClipboard
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_copy
import dorai.composeapp.generated.resources.a11y_copy_details
import dorai.composeapp.generated.resources.tool_action_ask_fix
import dorai.composeapp.generated.resources.tool_action_retry
import dorai.composeapp.generated.resources.tool_status_completed
import dorai.composeapp.generated.resources.tool_status_failed
import dorai.composeapp.generated.resources.tool_status_running
import dorai.composeapp.generated.resources.tool_status_waiting_approval
import org.jetbrains.compose.resources.stringResource

enum class ToolExecutionState {
    Running,
    Completed,
    Failed,
    PendingApproval,
}

data class ToolExecutionModel(
    val toolName: String,
    val argsSummary: String = "",
    val state: ToolExecutionState = ToolExecutionState.Running,
    val durationText: String? = null,
    val commandText: String? = null,
    val outputText: String? = null,
    val errorText: String? = null,
    val diffText: String? = null,
    val toolCallId: String = "",
    val exitCode: Int? = null,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
)

/**
 * Maps tool name to a semantic icon.
 */
fun getToolCategoryIcon(toolName: String): ImageVector {
    val name = toolName.lowercase()
    return when {
        name.contains("shell") || name.contains("terminal") || name.contains("bash") ||
            name.contains("sh") || name.contains("cmd") || name.contains("exec") -> Icons.Default.Terminal

        name.contains("edit") || name.contains("patch") || name.contains("modify") ||
            name.contains("write") -> Icons.Default.Edit

        name.contains("read") || name.contains("cat") || name.contains("view") ||
            name.contains("file") || name.contains("fs") -> Icons.AutoMirrored.Filled.InsertDriveFile

        name.contains("search") || name.contains("grep") || name.contains("find") ||
            name.contains("glob") -> Icons.Default.Search

        name.contains("git") -> Icons.Default.Code

        name.contains("mcp") || name.contains("plugin") -> Icons.Default.Extension

        else -> Icons.Default.Build
    }
}

/**
 * Formats tool name into a compact, human-readable identifier.
 */
fun formatToolDisplayName(toolName: String): String = when (toolName) {
    "execute_shell_command" -> "bash"
    "read_file" -> "read_file"
    "edit_file", "propose_file_change" -> "edit_file"
    "search", "web_search" -> "search"
    else -> toolName.removePrefix("tool_")
}

/**
 * Production-grade Agent Tool Execution Card:
 * - Compact tool header with semantic icon, tool name, arguments preview, and metadata.
 * - Semantic status badge (Running with indicator, Completed, Failed, Pending Approval).
 * - Execution metadata: duration and explicit exit code badge.
 * - Expandable command block with copy action.
 * - Collapsible stdout output with line-count badge to prevent vertical bloat.
 * - Error-focused panel with reddish highlight, stderr output, copy error, and retry actions.
 * - Unified diff view for file modifications.
 * - Blocking approval actions for security-sensitive executions.
 */
@Composable
fun DoraiToolCard(
    model: ToolExecutionModel,
    modifier: Modifier = Modifier,
    onAllowOnce: (() -> Unit)? = null,
    onAllowSession: (() -> Unit)? = null,
    onDeny: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onAskFix: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val copyToClipboard = rememberCopyToClipboard()

    // Default expanded state: Completed is collapsed by default to save space; Failed, Running, and PendingApproval expand.
    var isExpanded by remember(model.state) {
        mutableStateOf(model.state != ToolExecutionState.Completed)
    }

    val borderColor = when (model.state) {
        ToolExecutionState.Running -> colors.info.copy(alpha = 0.5f)
        ToolExecutionState.Completed -> colors.line
        ToolExecutionState.Failed -> colors.err
        ToolExecutionState.PendingApproval -> colors.warn
    }

    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surface)
            .border(1.dp, borderColor, cardShape)
            .doraiSurfaceStyle(shape = cardShape)
            .padding(DoraiSpacing.SM),
    ) {
        // Compact Tool Header Row (min-height 44dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tool Icon Badge (28x28 dp container)
            val iconTint = when (model.state) {
                ToolExecutionState.Running -> colors.info
                ToolExecutionState.Completed -> colors.ok
                ToolExecutionState.Failed -> colors.err
                ToolExecutionState.PendingApproval -> colors.warn
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (model.state == ToolExecutionState.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = colors.info,
                    )
                } else {
                    Icon(
                        imageVector = getToolCategoryIcon(model.toolName),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(DoraiSpacing.SM))

            // Tool Name (compact bold monospace)
            Text(
                text = formatToolDisplayName(model.toolName),
                color = colors.text,
                fontSize = 13.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )

            // Arguments / Command Preview
            val previewText = model.argsSummary.ifBlank { model.commandText.orEmpty() }
            if (previewText.isNotBlank()) {
                Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                Text(
                    text = previewText,
                    color = colors.muted,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Exit Code Badge (Explicit Result Metadata)
            if (model.exitCode != null) {
                val exitColor = if (model.exitCode == 0) colors.ok else colors.err
                Text(
                    text = "exit ${model.exitCode}",
                    color = exitColor,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(exitColor.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
                Spacer(modifier = Modifier.width(DoraiSpacing.XS))
            }

            // Duration Metadata
            if (model.durationText != null) {
                Text(
                    text = model.durationText,
                    color = colors.muted,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.surface2)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
                Spacer(modifier = Modifier.width(DoraiSpacing.XS))
            }

            // Semantic Status Badge
            when (model.state) {
                ToolExecutionState.Running -> {
                    DoraiStatusBadge(
                        text = stringResource(Res.string.tool_status_running),
                        color = colors.info,
                    )
                }

                ToolExecutionState.Completed -> {
                    DoraiStatusBadge(
                        text = stringResource(Res.string.tool_status_completed),
                        color = colors.ok,
                    )
                }

                ToolExecutionState.Failed -> {
                    DoraiStatusBadge(
                        text = stringResource(Res.string.tool_status_failed),
                        color = colors.err,
                    )
                }

                ToolExecutionState.PendingApproval -> {
                    DoraiStatusBadge(
                        text = stringResource(Res.string.tool_status_waiting_approval),
                        color = colors.warn,
                    )
                }
            }

            Spacer(modifier = Modifier.width(DoraiSpacing.XS))

            // Expand / Collapse Chevron
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }

        // Expanded Body
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DoraiSpacing.SM),
            ) {
                // 1. Command Block (Expandable code block with Copy button)
                if (!model.commandText.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.codeBackground)
                            .padding(DoraiSpacing.SM),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "COMMAND",
                                    color = colors.muted.copy(alpha = 0.7f),
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                IconButton(
                                    onClick = { copyToClipboard(model.commandText) },
                                    modifier = Modifier.size(22.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(Res.string.a11y_copy),
                                        tint = colors.muted,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    text = "$ ${model.commandText}",
                                    color = colors.info,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }
                }

                // 2. Error-Focused Panel
                if (model.state == ToolExecutionState.Failed || !model.errorText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.err.copy(alpha = 0.08f))
                            .border(1.dp, colors.err.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(DoraiSpacing.SM),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = colors.err,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val errorTitle = listOfNotNull(
                                        "EXECUTION ERROR",
                                        model.exitCode?.let { "EXIT $it" },
                                    ).joinToString(" · ")
                                    Text(
                                        text = errorTitle,
                                        color = colors.err,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                val errorContent = model.errorText ?: "Command failed with exit code ${model.exitCode}"
                                IconButton(
                                    onClick = { copyToClipboard(errorContent) },
                                    modifier = Modifier.size(22.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(Res.string.a11y_copy_details),
                                        tint = colors.err.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = model.errorText ?: "Command failed with exit code ${model.exitCode}",
                                color = colors.err,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                            )

                            // Error Action Buttons: Retry + Ask agent to fix
                            if (onRetry != null || onAskFix != null) {
                                Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
                                ) {
                                    if (onRetry != null) {
                                        Button(
                                            onClick = onRetry,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.surface2,
                                                contentColor = colors.text,
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(Res.string.tool_action_retry),
                                                fontSize = 12.5.sp,
                                            )
                                        }
                                    }
                                    if (onAskFix != null) {
                                        Button(
                                            onClick = onAskFix,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.primary,
                                                contentColor = colors.onPrimary,
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.tool_action_ask_fix),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Collapsible Output (stdout)
                if (!model.outputText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                    val outputLines = remember(model.outputText) { model.outputText.lines() }
                    var isOutputExpanded by remember(model.toolCallId) {
                        mutableStateOf(model.state == ToolExecutionState.Running || outputLines.size <= 4)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.codeBackground)
                            .padding(DoraiSpacing.SM),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "OUTPUT",
                                        color = colors.muted.copy(alpha = 0.7f),
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${outputLines.size} lines",
                                        color = colors.muted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.surface2)
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (outputLines.size > 4) {
                                        Text(
                                            text = if (isOutputExpanded) "Collapse" else "Expand",
                                            color = colors.info,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .clickable { isOutputExpanded = !isOutputExpanded }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(
                                        onClick = { copyToClipboard(model.outputText) },
                                        modifier = Modifier.size(22.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(Res.string.a11y_copy),
                                            tint = colors.muted,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            if (!isOutputExpanded && outputLines.size > 4) {
                                val preview = outputLines.take(3).joinToString("\n")
                                Text(
                                    text = preview,
                                    color = colors.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                )
                                Text(
                                    text = "… (${outputLines.size - 3} more lines · click to expand)",
                                    color = colors.info.copy(alpha = 0.85f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier
                                        .clickable { isOutputExpanded = true }
                                        .padding(top = 2.dp),
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState()),
                                ) {
                                    Text(
                                        text = model.outputText,
                                        color = colors.text,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Diff Block
                if (!model.diffText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                    DoraiDiffBlock(diff = model.diffText)
                }

                // 5. Pending Approval Action Row
                if (model.state == ToolExecutionState.PendingApproval) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.MD))
                    DoraiApprovalActions(
                        onAllowOnce = onAllowOnce,
                        onAllowSession = onAllowSession,
                        onDeny = onDeny,
                    )
                }
            }
        }
    }
}
