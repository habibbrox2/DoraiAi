package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.prefersReducedMotion
import com.inspiredandroid.kai.ui.theme.DesignTokens
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

/** Presentation state only; chat runtime state continues to be owned by ChatUiState. */
internal enum class AgentPresentationState {
    Idle,
    Generating,
    Executing,
    Speaking,
}

/**
 * Layered obsidian background inspired by the supplied premium-dark reference.
 * It deliberately uses Compose drawing primitives so all supported targets get
 * the same graceful fallback without a platform shader or downloaded asset.
 */
internal fun Modifier.premiumChatBackdrop(): Modifier = drawWithCache {
    val cyan = Color(0xFF00D7E8)
    val violet = Color(0xFF8A63F6)
    val topGlow = Brush.radialGradient(
        colors = listOf(cyan.copy(alpha = 0.20f), Color.Transparent),
        center = Offset(size.width * 0.14f, size.height * 0.06f),
        radius = size.maxDimension * 0.72f,
    )
    val lowerGlow = Brush.radialGradient(
        colors = listOf(violet.copy(alpha = 0.20f), Color.Transparent),
        center = Offset(size.width * 0.93f, size.height * 0.63f),
        radius = size.maxDimension * 0.70f,
    )
    val base = Brush.verticalGradient(
        colors = listOf(Color(0xFF020918), Color(0xFF06112A), Color(0xFF010612)),
    )
    onDrawBehind {
        drawRect(base)
        drawRect(topGlow)
        drawRect(lowerGlow)
        drawAmbientRibbon(cyan, size.height * 0.28f, 0.10f)
        drawAmbientRibbon(violet, size.height * 0.50f, 0.08f)
    }
}

private fun DrawScope.drawAmbientRibbon(color: Color, top: Float, alpha: Float) {
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = size.maxDimension * 0.82f,
        center = Offset(-size.width * 0.22f, top),
        style = Stroke(width = size.minDimension * 0.012f),
    )
}

@Composable
internal fun AgentOrb(
    state: AgentPresentationState,
    modifier: Modifier = Modifier,
) {
    val pulse = if (prefersReducedMotion()) {
        0.96f
    } else {
        val transition = rememberInfiniteTransition(label = "agentOrb")
        val animatedPulse by transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (state == AgentPresentationState.Idle) 2_400 else 900,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "agentOrbPulse",
        )
        animatedPulse
    }
    val glow = when (state) {
        AgentPresentationState.Executing -> DesignTokens.Warning
        AgentPresentationState.Generating -> DesignTokens.AgentViolet
        AgentPresentationState.Speaking -> DesignTokens.PrimaryCyan
        AgentPresentationState.Idle -> DesignTokens.BrandCyan
    }

    Box(
        modifier = modifier.size(144.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size((128 * pulse).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            glow.copy(alpha = 0.46f),
                            DesignTokens.AgentViolet.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .size(98.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    ambientColor = glow.copy(alpha = 0.55f),
                    spotColor = DesignTokens.AgentViolet.copy(alpha = 0.6f),
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            DesignTokens.BrandCyan.copy(alpha = 0.94f),
                            Color(0xFF174E9A),
                            DesignTokens.AgentViolet.copy(alpha = 0.94f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Brox Kai Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(62.dp),
            )
        }
    }
}
