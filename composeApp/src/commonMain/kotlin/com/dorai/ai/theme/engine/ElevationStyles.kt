package com.dorai.ai.theme.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.data.ThemeStyle

/**
 * 2027 premium surface treatment.
 *
 * - [ThemeStyle.Flat]: the surface tier in [containerColor] with a 1dp hairline outline.
 * - [ThemeStyle.ThreeD]: neumorphic depth — dual-toned outer shadow, a vertical light gradient
 *   across the fill and a faint white top-edge highlight, tuned to read on Web (Wasm), Desktop,
 *   Android and iOS.
 *
 * Reads [LocalDoraiPalette] for tier colors, so it stays coherent under Match Device.
 */
@Composable
fun Modifier.doraiSurfaceStyle(
    style: ThemeStyle = LocalDoraiThemeStyle.current,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = LocalDoraiPalette.current.surfaceElevatedColor,
    highlight: Color = Color.White.copy(alpha = 0.15f),
): Modifier {
    val palette = LocalDoraiPalette.current
    return when (style) {
        ThemeStyle.Flat -> this
            .clip(shape)
            .background(containerColor, shape)
            .border(1.dp, palette.outlineVariantColor, shape)

        ThemeStyle.ThreeD -> this
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black.copy(alpha = 0.45f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.surfaceRaisedColor, containerColor),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY,
                ),
                shape,
            )
            .border(1.dp, highlight, shape)
    }
}
