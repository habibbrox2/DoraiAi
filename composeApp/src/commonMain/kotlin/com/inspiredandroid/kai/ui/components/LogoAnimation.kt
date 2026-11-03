package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LogoAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.minDimension / 2f * 0.82f
            val innerRadius = this.size.minDimension / 2f * 0.48f
            val strokeWidth = size.value * 0.08f

            drawCircle(
                color = Color(0xFF3B82F6),
                center = center,
                radius = outerRadius,
                style = Stroke(width = strokeWidth),
            )
            drawCircle(
                color = Color(0xFF06B6D4),
                center = center,
                radius = innerRadius,
                style = Stroke(width = strokeWidth * 0.8f),
            )
            drawCircle(
                color = Color(0xFFFFFFFF),
                center = center,
                radius = innerRadius * 0.25f,
            )
        }
    }
}
