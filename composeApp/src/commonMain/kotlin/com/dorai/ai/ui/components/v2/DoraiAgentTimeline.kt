package com.dorai.ai.ui.components.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors

enum class TimelineNodeType {
    Task,
    Thinking,
    Note,
    Tool,
}

enum class TimelineNodeState {
    Running,
    Completed,
    Failed,
    Pending,
}

data class TimelineItemModel(
    val id: String,
    val type: TimelineNodeType,
    val title: String,
    val description: String? = null,
    val state: TimelineNodeState = TimelineNodeState.Completed,
    val details: String? = null,
)

/**
 * UI v2 Agent Activity Timeline:
 * - A vertical gradient rail connecting nodes.
 * - Nodes: Task (flag), Thinking (zap/bolt), Note (comment), Tool (construction).
 * - State colors: Running (info), Completed (ok), Failed (err), Pending (warn).
 */
@Composable
fun DoraiAgentTimeline(
    items: List<TimelineItemModel>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current

    Column(modifier = modifier.fillMaxWidth().padding(vertical = DoraiSpacing.SM)) {
        items.forEachIndexed { index, item ->
            TimelineRow(
                item = item,
                isLast = index == items.lastIndex,
            )
        }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItemModel,
    isLast: Boolean,
) {
    val colors = LocalDoraiColors.current
    var isExpanded by remember { mutableStateOf(false) }

    val nodeIcon: ImageVector = when (item.type) {
        TimelineNodeType.Task -> Icons.Default.Flag
        TimelineNodeType.Thinking -> Icons.Default.Bolt
        TimelineNodeType.Note -> Icons.Default.Notes
        TimelineNodeType.Tool -> Icons.Default.Construction
    }

    val stateColor = when (item.state) {
        TimelineNodeState.Running -> colors.info
        TimelineNodeState.Completed -> colors.ok
        TimelineNodeState.Failed -> colors.err
        TimelineNodeState.Pending -> colors.warn
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        // Node + Vertical Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp),
        ) {
            // Node circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.state == TimelineNodeState.Completed) stateColor else colors.background,
                    )
                    .border(2.dp, stateColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (item.state == TimelineNodeState.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.info,
                    )
                } else {
                    Icon(
                        imageVector = nodeIcon,
                        contentDescription = null,
                        tint = if (item.state == TimelineNodeState.Completed) colors.background else stateColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Vertical connecting line to next item
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(colors.info.copy(alpha = 0.5f), colors.accent.copy(alpha = 0.5f)),
                            ),
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.width(DoraiSpacing.MD))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = !item.details.isNullOrEmpty()) {
                    isExpanded = !isExpanded
                }
                .padding(bottom = if (isLast) 0.dp else DoraiSpacing.MD),
        ) {
            Text(
                text = item.title,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )

            if (!item.description.isNullOrEmpty()) {
                Text(
                    text = item.description,
                    color = colors.muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            AnimatedVisibility(visible = isExpanded && !item.details.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DoraiSpacing.SM)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .padding(DoraiSpacing.SM),
                ) {
                    Text(
                        text = item.details.orEmpty(),
                        color = colors.text,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}
