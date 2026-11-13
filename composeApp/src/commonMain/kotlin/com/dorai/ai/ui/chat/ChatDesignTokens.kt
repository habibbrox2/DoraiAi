package com.dorai.ai.ui.chat

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens

/**
 * Shared design tokens for the DORAI AI chat workspace.
 * Updated for Futuristic Midnight Glassmorphism design system.
 */
object ChatSpacing {
    val xxs: Dp = DesignTokens.SpacingXxs
    val xs: Dp = DesignTokens.SpacingXs
    val sm: Dp = DesignTokens.SpacingSm
    val md: Dp = DesignTokens.SpacingMd
    val lg: Dp = DesignTokens.SpacingLg
    val xl: Dp = DesignTokens.SpacingXl
    val xxl: Dp = DesignTokens.SpacingXxl
    val xxxl: Dp = DesignTokens.SpacingXxxl

    /** Width of the centered conversation column on expanded windows. */
    val conversationColumnMax: Dp = ChatAdaptivePolicy.conversationColumnMax

    /** Minimum touch target enforced across the chat UI. */
    val minTouchTarget: Dp = 48.dp
}

object ChatShapes {
    /** User message bubble. */
    val userBubble = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 8.dp,
        bottomStart = 20.dp,
        bottomEnd = 20.dp,
    )

    /** Assistant message surface. */
    val assistantSurface = RoundedCornerShape(16.dp)

    /** Status card (streaming / tool / fallback / error). */
    val statusCard = RoundedCornerShape(12.dp)

    /** Quick-start card on the empty state. */
    val quickStartCard = RoundedCornerShape(16.dp)

    /** Composer container. */
    val composer = RoundedCornerShape(14.dp)

    /** Compact pill (status pills, chips). */
    val pill = RoundedCornerShape(999.dp)

    /** Panel / sheet heading container. */
    val panel = RoundedCornerShape(20.dp)
}

/**
 * Status colors used by timeline cards and status pills. Updated for cyan accent theme.
 */
object ChatStatusColors {
    val streaming = DesignTokens.PrimaryCyan
    val toolActivity = DesignTokens.PrimaryCyan.copy(alpha = 0.8f)
    val fallback = DesignTokens.AgentViolet
    val error = DesignTokens.Error
    val retry = DesignTokens.Warning
    val success = DesignTokens.Success
    val glow = DesignTokens.PrimaryCyan
}

/**
 * Surface resolvers. Updated for glassmorphism design system.
 */
object ChatSurfaces {
    @Composable
    fun page(): Color = MaterialTheme.colorScheme.background

    @Composable
    fun elevated(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

    @Composable
    fun border(): Color = LocalDesignColorTokens.current.borderSoft

    @Composable
    fun assistantMessage(): Color = LocalDesignColorTokens.current.cardFill

    @Composable
    fun composer(): Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
}
