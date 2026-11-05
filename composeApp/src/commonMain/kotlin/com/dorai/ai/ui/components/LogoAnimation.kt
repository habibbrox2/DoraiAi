package com.dorai.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.theme.DesignTokens

/**
 * Theme-safe AURORA DORAI mark.
 *
 * The same transparent D/play glyph works on both light and dark surfaces;
 * only its restrained cyan-to-blue gradient is used, never a baked-in background.
 */
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
            val w = this.size.width
            val h = this.size.height
            val sx = w / 512f
            val sy = h / 512f
            fun p(x: Float, y: Float) = Offset(x * sx, y * sy)

            val glyph = Path().apply {
                fillType = PathFillType.EvenOdd
                moveTo(p(104f, 72f).x, p(104f, 72f).y)
                lineTo(p(226f, 72f).x, p(226f, 72f).y)
                cubicTo(p(374f, 72f).x, p(374f, 72f).y, p(440f, 147f).x, p(440f, 147f).y, p(440f, 256f).x, p(440f, 256f).y)
                cubicTo(p(440f, 365f).x, p(440f, 365f).y, p(374f, 440f).x, p(374f, 440f).y, p(226f, 440f).x, p(226f, 440f).y)
                lineTo(p(104f, 440f).x, p(104f, 440f).y)
                close()
                moveTo(p(184f, 154f).x, p(184f, 154f).y)
                lineTo(p(225f, 154f).x, p(225f, 154f).y)
                cubicTo(p(323f, 154f).x, p(323f, 154f).y, p(358f, 188f).x, p(358f, 188f).y, p(358f, 256f).x, p(358f, 256f).y)
                cubicTo(p(358f, 324f).x, p(358f, 324f).y, p(323f, 358f).x, p(323f, 358f).y, p(225f, 358f).x, p(225f, 358f).y)
                lineTo(p(184f, 358f).x, p(184f, 358f).y)
                close()
            }
            val gradient = Brush.linearGradient(
                colors = listOf(DesignTokens.BrandCyan, DesignTokens.BrandBlue, DesignTokens.BrandViolet),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            )
            drawPath(glyph, gradient)
            val play = Path().apply {
                moveTo(p(222f, 194f).x, p(222f, 194f).y)
                lineTo(p(334f, 256f).x, p(334f, 256f).y)
                lineTo(p(222f, 318f).x, p(222f, 318f).y)
                close()
            }
            drawPath(play, Brush.linearGradient(listOf(DesignTokens.BrandBlue, DesignTokens.BrandViolet)))
            drawCircle(
                color = DesignTokens.BrandCyan.copy(alpha = 0.18f),
                radius = w * 0.47f,
                style = Stroke(width = w * 0.012f),
                center = Offset(w / 2f, h / 2f),
            )
        }
    }
}
