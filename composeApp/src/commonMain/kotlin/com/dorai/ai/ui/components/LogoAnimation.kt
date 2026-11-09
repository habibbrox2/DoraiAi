package com.dorai.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.theme.tokens.LocalDoraiColors
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

/**
 * Official DORAI brand logo mark and animation.
 *
 * Implements the official Hexa-Core motion spec from docs/branding/dorai/assets/motion/logo-animation.svg:
 * - 4.2s breathe cycle (scale 1.0 -> 1.045)
 * - Soft cyan/blue radial bloom halo behind the mark
 */
@Composable
fun LogoAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    animate: Boolean = true,
    contentDescription: String? = "DORAI AI Logo",
) {
    val isInspection = LocalInspectionMode.current
    val shouldAnimate = animate && !isInspection && !prefersReducedMotion()
    val colors = LocalDoraiColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "DoraiLogoMotion")
    val scale by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.045f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "LogoBreathe",
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val haloAlpha by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.38f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "HaloGlow",
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (shouldAnimate) {
            Box(
                modifier = Modifier
                    .size(size * 1.25f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = haloAlpha),
                                Color(0xFF377CFF).copy(alpha = haloAlpha * 0.5f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
        }

        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
                .scale(scale),
        )
    }
}
