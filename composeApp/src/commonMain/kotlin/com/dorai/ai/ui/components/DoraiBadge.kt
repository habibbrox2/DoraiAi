package com.dorai.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens

/**
 * Small status indicator pill used for tags, status labels, and counts.
 *
 * @param label Display text
 * @param variant Color variant: default, success, warning, error, or accent
 * @param modifier Compose modifier
 */
@Composable
fun DoraiBadge(
    label: String,
    variant: BadgeVariant = BadgeVariant.Default,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor, borderColor) = badgeColors(variant)

    Box(
        modifier = modifier
            .clip(DoraiShapes.pill)
            .border(BorderStroke(1.dp, borderColor), DoraiShapes.pill)
            .defaultMinSize(minHeight = 22.dp)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = DoraiTypography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable private fun badgeColors(
    variant: BadgeVariant,
): Triple<Color, Color, Color> {
    val tokens = LocalDesignColorTokens.current
    return when (variant) {
        BadgeVariant.Success -> Triple(tokens.success.copy(alpha = 0.15f), tokens.success, tokens.success.copy(alpha = 0.35f))
        BadgeVariant.Warning -> Triple(tokens.warning.copy(alpha = 0.15f), tokens.warning, tokens.warning.copy(alpha = 0.35f))
        BadgeVariant.Error -> Triple(tokens.error.copy(alpha = 0.15f), tokens.error, tokens.error.copy(alpha = 0.35f))
        BadgeVariant.Accent -> Triple(tokens.primary.copy(alpha = 0.15f), tokens.primary, tokens.primary.copy(alpha = 0.35f))
        BadgeVariant.Default -> Triple(tokens.surfaceElevated, tokens.textSecondary, tokens.borderSubtle)
    }
}

/** Color variants for DoraiBadge. */
enum class BadgeVariant {
    Default,
    Success,
    Warning,
    Error,
    Accent,
}
