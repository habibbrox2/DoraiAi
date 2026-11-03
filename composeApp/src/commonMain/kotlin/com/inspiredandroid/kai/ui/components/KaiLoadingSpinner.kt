package com.inspiredandroid.kai.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.AccentPrimary

/**
 * Cross-platform loading spinner styled for the Futuristic Midnight Glassmorphism design system.
 *
 * @param size Diameter of the spinner (default 24.dp)
 * @param strokeWidth Width of the indicator stroke (default 3.dp)
 * @param color Spinner color (default AccentPrimary)
 * @param modifier Compose modifier
 */
@Composable
fun KaiLoadingSpinner(
    size: Dp = 24.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = AccentPrimary,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(size)
                .align(androidx.compose.ui.Alignment.Center),
            strokeWidth = strokeWidth,
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}
