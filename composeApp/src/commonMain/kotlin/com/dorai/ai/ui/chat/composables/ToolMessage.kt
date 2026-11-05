package com.dorai.ai.ui.chat.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.waiting_brewing
import dorai.composeapp.generated.resources.waiting_content_description
import dorai.composeapp.generated.resources.waiting_thinking
import dorai.composeapp.generated.resources.waiting_working
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

private fun toolSummaryText(executingTools: ImmutableList<Pair<String, String>>): String? {
    if (executingTools.isEmpty()) return null
    return executingTools.joinToString(", ") { (_, summary) -> summary }
}

@Composable
internal fun AnimatedTypingDots(
    modifier: Modifier = Modifier,
    dotColor: Color = AccentPrimary,
) {
    if (prefersReducedMotion()) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(dotColor))
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "typingDots")
    val dot1Alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot1",
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot2",
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot3",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(dotColor.copy(alpha = dot1Alpha)))
        Box(Modifier.size(5.dp).clip(CircleShape).background(dotColor.copy(alpha = dot2Alpha)))
        Box(Modifier.size(5.dp).clip(CircleShape).background(dotColor.copy(alpha = dot3Alpha)))
    }
}

@Composable
internal fun AnimatedLoadingBar(
    modifier: Modifier = Modifier,
    color: Color = AccentPrimary,
) {
    if (prefersReducedMotion()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color.copy(alpha = 0.55f)),
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "loadingBar")
    val offsetFraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "barOffset",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(Color(0xFF1E293B)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = offsetFraction * 240f
                }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0f),
                            color,
                            color.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
internal fun WaitingResponseRow(
    executingTools: ImmutableList<Pair<String, String>>,
    isStatusOnly: Boolean = false,
    statusText: String? = null,
) {
    val summary = statusText ?: toolSummaryText(executingTools)
    val waitingCd = stringResource(Res.string.waiting_content_description)

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clipToBounds(),
    ) {
        Column(
            modifier = Modifier
                .clip(DoraiShapes.row)
                .shadow(
                    elevation = 3.dp,
                    shape = DoraiShapes.row,
                    ambientColor = AccentPrimary.copy(alpha = 0.35f),
                    spotColor = AccentPrimary.copy(alpha = 0.35f),
                )
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.30f)),
                    RoundedCornerShape(12.dp),
                )
                .semantics { contentDescription = waitingCd },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnimatedTypingDots(dotColor = AccentPrimary)
                Text(
                    text = "Thinking",
                    style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentPrimary,
                    fontFamily = FontFamily.Monospace,
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        fontFamily = FontFamily.Monospace,
                        style = DoraiTypography.labelSmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AnimatedLoadingBar(color = AccentPrimary)
        }
    }
}

@Composable
internal fun PulsingStatusIndicator(
    toolSummary: String?,
    dotSize: Dp,
    dotColor: Color,
    textColor: Color,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    isStatusOnly: Boolean = false,
) {
    val reducedMotion = prefersReducedMotion()

    val infiniteTransition = if (reducedMotion) null else rememberInfiniteTransition(label = "toolStatusPulse")
    val pulseScale: Float
    val pulseAlpha: Float
    if (infiniteTransition == null) {
        pulseScale = 1.0f
        pulseAlpha = 1.0f
    } else {
        pulseScale = infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        ).value
        pulseAlpha = infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        ).value
    }
    val waitingTexts = remember {
        listOf(
            Res.string.waiting_thinking,
            Res.string.waiting_working,
            Res.string.waiting_brewing,
        )
    }
    val index: Int = if (reducedMotion) {
        0
    } else {
        val progress = rememberInfiniteTransition(label = "waitingTextIndex").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, delayMillis = 0, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "textIndex",
        ).value
        (progress * waitingTexts.size).toInt().coerceIn(0, waitingTexts.size - 1)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .background(dotColor, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        if (isStatusOnly && toolSummary != null) {
            Text(
                text = toolSummary,
                color = textColor,
                style = textStyle,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (fadeIn(tween(300)) togetherWith fadeOut(tween(300)))
                        .using(SizeTransform(clip = false) { _, _ -> tween(300) })
                },
            ) { targetIndex ->
                Text(
                    text = stringResource(waitingTexts[targetIndex]),
                    color = textColor,
                    style = textStyle,
                )
            }
            if (toolSummary != null) {
                Text(
                    text = " · $toolSummary",
                    modifier = Modifier.weight(1f, fill = false),
                    color = textColor,
                    style = textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
