package com.dorai.ai.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.KaiBorders
import com.dorai.ai.ui.KaiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens

/**
 * Terminal-style status pill (Plan 3, Phase 1). Small colored dot plus a label so
 * session tabs, the workspace header, and the setup flow all announce status the
 * same way. The dot is decorative; the text carries meaning for screen readers.
 */
@Composable
fun WorkspaceStatusPill(
    label: String,
    kind: WorkspaceStatusKind,
    modifier: Modifier = Modifier,
) {
    val colors = rememberWorkspaceStatusColors()
    val color = colors.forKind(kind)
    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Status: $label"
            }
            .clip(com.dorai.ai.ui.KaiShapes.pill)
            .background(color.copy(alpha = 0.16f), com.dorai.ai.ui.KaiShapes.pill)
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                com.dorai.ai.ui.KaiShapes.pill,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WorkspaceSpacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
            Text(
                text = label,
                style = KaiTypography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * Step progress block (Plan 3, Phase 1). Shows the current install step, an
 * optional determinate progress bar, and the running/success status so setup and
 * agent installation progress read in plain language.
 */
@Composable
fun BuildProgressBlock(
    title: String,
    kind: WorkspaceStatusKind,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
) {
    val colors = rememberWorkspaceStatusColors()
    val color = colors.forKind(kind)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(com.dorai.ai.ui.KaiShapes.card)
            .background(com.dorai.ai.ui.theme.DesignTokens.SurfaceElevated, com.dorai.ai.ui.KaiShapes.card)
            .border(androidx.compose.foundation.BorderStroke(1.dp, com.dorai.ai.ui.theme.DesignTokens.BorderSubtle), com.dorai.ai.ui.KaiShapes.card),
    ) {
        Column(
            modifier = Modifier.padding(WorkspaceSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(WorkspaceSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WorkspaceSpacing.sm),
            ) {
                if (kind == WorkspaceStatusKind.Running) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(16.dp)
                            .height(16.dp),
                        color = color,
                        trackColor = WorkspaceSurfaces.progressTrack,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color, CircleShape),
                    )
                }
                Text(
                    text = title,
                    style = KaiTypography.titleSmall,
                    color = LocalDesignColorTokens.current.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                WorkspaceStatusPill(label = kind.name.lowercase().replaceFirstChar { it.uppercase() }, kind = kind)
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = KaiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = color,
                    trackColor = WorkspaceSurfaces.progressTrack,
                )
            }
        }
    }
}
