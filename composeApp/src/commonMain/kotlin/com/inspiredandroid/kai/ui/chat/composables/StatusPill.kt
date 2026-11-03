package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.chat.ChatShapes
import com.inspiredandroid.kai.ui.chat.ChatSpacing
import com.inspiredandroid.kai.ui.chat.ChatStatusColors

/**
 * A compact status pill used by the timeline to surface streaming, tool execution,
 * fallback, retry and error states as distinct visual elements.
 *
 * Phase 1 of the chat UI upgrade plan: reusable presentation component, no
 * business logic.
 */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentDescription: String? = null,
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), ChatShapes.pill)
            .border(1.dp, color.copy(alpha = 0.32f), ChatShapes.pill)
            .padding(horizontal = ChatSpacing.sm, vertical = ChatSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChatSpacing.xs),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            style = KaiTypography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Convenience presets so callers do not repeat color wiring. */
object StatusPillDefaults {
    @Composable
    fun streaming(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) = StatusPill(text = text, color = ChatStatusColors.streaming, modifier = modifier, icon = icon)

    @Composable
    fun tool(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) = StatusPill(text = text, color = ChatStatusColors.toolActivity, modifier = modifier, icon = icon)

    @Composable
    fun fallback(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) = StatusPill(text = text, color = ChatStatusColors.fallback, modifier = modifier, icon = icon)

    @Composable
    fun error(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) = StatusPill(text = text, color = ChatStatusColors.error, modifier = modifier, icon = icon)

    @Composable
    fun retry(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) = StatusPill(text = text, color = ChatStatusColors.retry, modifier = modifier, icon = icon)
}
