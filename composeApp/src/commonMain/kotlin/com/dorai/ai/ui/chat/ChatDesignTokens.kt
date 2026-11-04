package com.dorai.ai.ui.chat

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.BorderSoft
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.SuccessGreen
import com.dorai.ai.ui.WarningAmber

/**
 * Shared design tokens for the DORAI AI chat workspace.
 * Updated for Futuristic Midnight Glassmorphism design system.
 */
object ChatSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp

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
    val streaming = AccentPrimary
    val toolActivity = AccentPrimary.copy(alpha = 0.8f)
    val fallback = Color(0xFFB388FF)
    val error = DangerAlert
    val retry = WarningAmber
    val success = SuccessGreen
    val glow = AccentPrimary
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
    fun border(): Color = BorderSoft

    @Composable
    fun assistantMessage(): Color = CardFill

    @Composable
    fun composer(): Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
}
