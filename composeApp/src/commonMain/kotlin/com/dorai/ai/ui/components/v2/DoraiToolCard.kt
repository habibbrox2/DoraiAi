package com.dorai.ai.ui.components.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.engine.doraiSurfaceStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.tool_action_allow_once
import dorai.composeapp.generated.resources.tool_action_allow_session
import dorai.composeapp.generated.resources.tool_action_ask_fix
import dorai.composeapp.generated.resources.tool_action_deny
import dorai.composeapp.generated.resources.tool_action_retry
import dorai.composeapp.generated.resources.tool_approval_prompt
import dorai.composeapp.generated.resources.tool_error_label
import dorai.composeapp.generated.resources.tool_output_label
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
)

/**
 * UI v2 Tool Card implementing the 4 interaction states:
 * - [ToolExecutionState.Running]: Spinner icon, Running badge, expanded by default.
 * - [ToolExecutionState.Completed]: Check icon, duration, collapsed by default with tap-to-expand.
 * - [ToolExecutionState.Failed]: Red X icon, Failed badge, red border, error output, Retry + Ask agent to fix.
 * - [ToolExecutionState.PendingApproval]: Shield icon, warning border, command/diff preview, blocking approval buttons.
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
    var isExpanded by remember(model.state) {
        mutableStateOf(model.state != ToolExecutionState.Completed)
    }

    val borderColor = when (model.state) {
        ToolExecutionState.Running -> colors.line
        ToolExecutionState.Completed -> colors.line
        ToolExecutionState.Failed -> colors.err
        ToolExecutionState.PendingApproval -> colors.warn
    }

    val cardShape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surface)
            .border(1.dp, borderColor, cardShape)
            .doraiSurfaceStyle(shape = cardShape)
            .padding(DoraiSpacing.MD),
    ) {
        // Header Row (min-height 48dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (model.state) {
                ToolExecutionState.Running -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.info,
                    )
                }

                ToolExecutionState.Completed -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(Res.string.tool_status_completed),
                        tint = colors.ok,
                        modifier = Modifier.size(18.dp),
                    )
                }

                ToolExecutionState.Failed -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.tool_status_failed),
                        tint = colors.err,
                        modifier = Modifier.size(18.dp),
                    )
                }

                ToolExecutionState.PendingApproval -> {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = stringResource(Res.string.tool_status_waiting_approval),
                        tint = colors.warn,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(DoraiSpacing.SM))

            Text(
                text = model.toolName,
                color = colors.text,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.width(DoraiSpacing.SM))

            Text(
                text = model.argsSummary,
                color = colors.muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(DoraiSpacing.SM))

            // State Badge / Duration
            when (model.state) {
                ToolExecutionState.Running -> {
                    Text(
                        text = stringResource(Res.string.tool_status_running),
                        color = colors.info,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                ToolExecutionState.Completed -> {
                    Text(
                        text = model.durationText ?: stringResource(Res.string.tool_status_completed),
                        color = colors.muted,
                        fontSize = 12.sp,
                    )
                }

                ToolExecutionState.Failed -> {
                    Text(
                        text = stringResource(Res.string.tool_status_failed),
                        color = colors.err,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                ToolExecutionState.PendingApproval -> {
                    Text(
                        text = stringResource(Res.string.tool_status_waiting_approval),
                        color = colors.warn,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Expanded Body
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = DoraiSpacing.MD)) {
                // Command or Shell output if present
                if (!model.commandText.isNullOrEmpty() || !model.outputText.isNullOrEmpty() || !model.errorText.isNullOrEmpty()) {
                    DoraiShellBlock(
                        command = model.commandText,
                        output = model.outputText,
                        error = model.errorText,
                    )
                }

                // Diff block if present
                if (!model.diffText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                    DoraiDiffBlock(diff = model.diffText)
                }

                // Failure action row
                if (model.state == ToolExecutionState.Failed) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.MD))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
                    ) {
                        if (onRetry != null) {
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surface2, contentColor = colors.text),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(Res.string.tool_action_retry))
                            }
                        }
                        if (onAskFix != null) {
                            Button(
                                onClick = onAskFix,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(Res.string.tool_action_ask_fix))
                            }
                        }
                    }
                }

                // Pending Approval action row
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
