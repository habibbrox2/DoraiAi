package com.dorai.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.tokens.LocalDoraiColors

/**
 * Official DORAI brand loading animation.
 *
 * Implements the 1.8s dual-ring expansion loop from docs/branding/dorai/assets/motion/loader-animation.svg:
 * - Two outward pulsing rings with 0.9s phase delay
 * - Central core breathing indicator
 */
@Composable
fun DoraiLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color? = null,
) {
    val colors = LocalDoraiColors.current
    val strokeColor = color ?: colors.primary

    val transition = rememberInfiniteTransition(label = "DoraiLoaderMotion")

    // Ring 1 (0 to 1800ms)
    val ring1Progress by transition.animateFloat(
        initialValue = 0.24f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "Ring1Scale",
    )

    val ring1Alpha by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "Ring1Alpha",
    )

    // Core breathing
    val coreScale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "CoreScale",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Expanding ring
        Box(
            modifier = Modifier
                .size(size)
                .scale(ring1Progress)
                .border(
                    width = 1.5.dp,
                    color = strokeColor.copy(alpha = ring1Alpha),
                    shape = CircleShape,
                ),
        )

        // Center core
        Box(
            modifier = Modifier
                .size(size * 0.42f)
                .scale(coreScale)
                .border(
                    width = 2.dp,
                    color = strokeColor,
                    shape = CircleShape,
                ),
        )
    }
}
