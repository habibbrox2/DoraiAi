package com.dorai.ai.theme.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
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
        ThemeStyle.Flat ->
            this
                .clip(shape)
                .background(containerColor, shape)
                .border(1.dp, palette.outlineVariantColor, shape)

        ThemeStyle.ThreeD ->
            this
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

/**
 * Pressable 3D "edge" for buttons / cards / composer / chips
 * (prototype: `html[data-style=d3] .btn{box-shadow:0 4px 0 …,0 8px 14px …}`
 * and `:active{transform:translateY(3px)}`).
 *
 * - [ThemeStyle.Flat]: no-op — same size, no shadow.
 * - [ThemeStyle.ThreeD]: a hard ~4dp bottom edge + soft 8dp ambient shadow.
 *   When [pressed] is true the content shifts down 3dp (via `graphicsLayer`,
 *   so layout size never changes) and the hard edge shrinks to 1dp.
 *
 * Usage: apply BEFORE the surface paint so the press translation moves the
 * fill (and the content) but not the edge:
 * `Modifier.doraiEdgeStyle(pressed = pressed).doraiSurfaceStyle()`.
 * Wire [pressed] from `collectIsPressedAsState()` on your interaction source.
 * Pass [edgeColor] = primary (for primary buttons) to mirror the prototype's
 * primary/accent edges; defaults to the palette outline. Pass [cornerRadius]
 * matching your surface shape (e.g. `48.dp` for pills, `16.dp` for cards).
 */
@Composable
fun Modifier.doraiEdgeStyle(
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    edgeColor: Color = LocalDoraiPalette.current.outlineColor,
    pressed: Boolean = false,
    enabled: Boolean = true,
): Modifier {
    if (LocalDoraiThemeStyle.current == ThemeStyle.Flat) return this
    val edgeHeight = when {
        !enabled -> 2.dp
        pressed -> 1.dp
        else -> 4.dp
    }
    val edgeAlpha = if (enabled) 0.85f else 0.40f
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pressPx = with(density) { 3.dp.toPx() }
    return this
        .shadow(
            elevation = 8.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.20f),
            spotColor = Color.Black.copy(alpha = 0.22f),
        )
        .drawBehind {
            val edgePx = edgeHeight.toPx()
            drawRoundRect(
                color = edgeColor.copy(alpha = edgeAlpha),
                topLeft = androidx.compose.ui.geometry.Offset(0f, edgePx),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
            )
        }
        .graphicsLayer {
            translationY = if (pressed && enabled) pressPx else 0f
        }
}
