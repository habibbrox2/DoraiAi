package com.dorai.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens

/**
 * Chip with full control over appearance — no hidden minimum-interactive-size
 * padding from Material's FilterChip. At least 48.dp tall at the default font
 * scale, and taller when the label needs the room.
 */
@Composable
fun DoraiChip(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val primary = LocalDesignColorTokens.current.primary
    val containerColor = when {
        !enabled -> LocalDesignColorTokens.current.surfaceElevated.copy(alpha = 0.5f)
        selected -> primary.copy(alpha = 0.15f)
        else -> LocalDesignColorTokens.current.surfaceElevated
    }
    val contentColor = when {
        !enabled -> LocalDesignColorTokens.current.textSecondary.copy(alpha = 0.38f)
        selected -> primary
        else -> LocalDesignColorTokens.current.textSecondary
    }
    val borderColor = when {
        !enabled -> LocalDesignColorTokens.current.borderSubtle.copy(alpha = 0.38f)
        selected -> primary.copy(alpha = 0.45f)
        else -> LocalDesignColorTokens.current.borderSubtle
    }
    val shape = DoraiShapes.xs
    val border = BorderStroke(1.dp, borderColor)
    val sizeModifier = modifier.defaultMinSize(minHeight = 48.dp)

    val cardModifier = Modifier
        .clip(shape)
        .background(containerColor, shape)
        .border(border, shape)

    val chipContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp)
                .padding(horizontal = ChatSpacing.lg, vertical = ChatSpacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }

    if (onClick != null) {
        Box(
            modifier = sizeModifier
                .handCursor()
                .then(cardModifier)
                .selectable(selected = selected, enabled = enabled, onClick = onClick, role = Role.Checkbox),
            contentAlignment = Alignment.Center,
        ) {
            chipContent()
        }
    } else {
        Box(
            modifier = sizeModifier.then(cardModifier),
            contentAlignment = Alignment.Center,
        ) {
            chipContent()
        }
    }
}
