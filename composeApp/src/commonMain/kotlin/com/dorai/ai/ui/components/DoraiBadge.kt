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
import com.dorai.ai.ui.theme.DesignTokens
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
            .border(BorderStroke(1.dp, borderColor), DoraiShapes.pill)
            .defaultMinSize(minHeight = 22.dp)
            .clip(DoraiShapes.pill)
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

private @Composable fun badgeColors(
    variant: BadgeVariant,
): Triple<Color, Color, Color> = when (variant) {
    BadgeVariant.Success -> Triple(DesignTokens.Success.copy(alpha = 0.15f), DesignTokens.Success, DesignTokens.Success.copy(alpha = 0.35f))
    BadgeVariant.Warning -> Triple(DesignTokens.Warning.copy(alpha = 0.15f), DesignTokens.Warning, DesignTokens.Warning.copy(alpha = 0.35f))
    BadgeVariant.Error -> Triple(DesignTokens.Error.copy(alpha = 0.15f), DesignTokens.Error, DesignTokens.Error.copy(alpha = 0.35f))
    BadgeVariant.Accent -> Triple(DesignTokens.PrimaryCyan.copy(alpha = 0.15f), DesignTokens.PrimaryCyan, DesignTokens.PrimaryCyan.copy(alpha = 0.35f))
    BadgeVariant.Default -> Triple(LocalDesignColorTokens.current.surfaceElevated, LocalDesignColorTokens.current.textSecondary, LocalDesignColorTokens.current.borderSubtle)
}

/** Color variants for DoraiBadge. */
enum class BadgeVariant {
    Default,
    Success,
    Warning,
    Error,
    Accent,
}
