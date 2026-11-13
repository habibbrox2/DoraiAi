package com.dorai.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.vectorResource

/**
 * Circular avatar with optional initials fallback and status indicator dot.
 *
 * @param initials Optional 1-2 character initials shown when no image is provided
 * @param size Diameter of the avatar (default 40.dp)
 * @param borderColor Border color (default BorderSoft)
 * @param statusColor Optional status dot color (null = no dot)
 * @param modifier Compose modifier
 */
@Composable
fun DoraiAvatar(
    initials: String? = null,
    size: Dp = 40.dp,
    borderColor: Color? = null,
    statusColor: Color? = null,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignColorTokens.current
    val resolvedBorder = borderColor ?: colors.borderSoft
    val avatarDescription = contentDescription
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(BorderStroke(1.dp, resolvedBorder), CircleShape)
            .background(colors.surfaceElevated, CircleShape)
            .semantics {
                if (avatarDescription == null) {
                    hideFromAccessibility()
                } else {
                    this.contentDescription = avatarDescription
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (initials != null && initials.isNotEmpty()) {
            Text(
                text = initials.take(2),
                style = DoraiTypography.labelMedium,
                color = colors.primary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // Fallback: simple dot when no initials provided
            Box(
                modifier = Modifier
                    .size(size * 0.5f)
                    .clip(CircleShape)
                    .background(colors.textSecondary),
            )
        }

        // Status indicator dot (bottom-right)
        if (statusColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.25f)
                    .clip(CircleShape)
                    .background(statusColor)
                    .border(BorderStroke(1.dp, colors.borderSoft), CircleShape),
            )
        }
    }
}
