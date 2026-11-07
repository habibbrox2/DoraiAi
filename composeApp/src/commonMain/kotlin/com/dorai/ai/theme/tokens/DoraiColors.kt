package com.dorai.ai.theme.tokens

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets

/**
 * UI v2 semantic color tokens. v2 screens read every color from this single
 * place — no raw hex in screens. One token set per [ThemePalette], so the
 * 13 presets keep their identity colors and only derived status/scrim/code
 * colors adapt to the light/dark luminance class.
 *
 * Mapping (CSS name in `docs/design/dorai-ui-v2-prototype.html` → source):
 * - `background` → `ThemePalette.background`
 * - `surface` → `ThemePalette.surface`
 * - `surface2` → `ThemePalette.surfaceElevated` (hover/raised tier)
 * - `line` → `ThemePalette.outline` (hairline border)
 * - `text` → `ThemePalette.onSurface` (== onBackground for every preset)
 * - `muted` → `ThemePalette.onSurfaceVariant`
 * - `primary`/`onPrimary`/`accent` → `ThemePalette.primary/onPrimary/accent`
 * - `ok`/`warn`/`err`/`info` → luminance-split status colors (see below)
 * - `codeBackground` → darkened `background` (dark) / `surfaceElevated` (light)
 * - `scrim` → `Color.Black` 60% (dark) / indigo 40% (light)
 *
 * Status colors: the palette's stored `success/warning/error/info` defaults
 * (`#10B981/#F59E0B/#EF4444/#377CFF`) fail WCAG AA on light surfaces, so
 * they are NOT used directly. The prototype's luminance-split values are
 * used instead — verified >= 4.5:1 on both `background` and `surface` of
 * all 13 presets:
 * - dark: ok `#34D399`, warn `#FBBF24`, err `#FB7185`, info `#22D3EE`
 * - light: ok `#0B7A52`, warn `#8F5A00`, err `#C0262D`, info `#1D5FBF`
 */
data class DoraiSemanticColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val ok: Color,
    val warn: Color,
    val err: Color,
    val info: Color,
    val codeBackground: Color,
    val scrim: Color,
) {
    companion object {
        // Dark-class status set (prototype --ok/--warn/--err/--info when d=1).
        val DarkOk: Color = Color(0xFF34D399)
        val DarkWarn: Color = Color(0xFFFBBF24)
        val DarkErr: Color = Color(0xFFFB7185)
        val DarkInfo: Color = Color(0xFF22D3EE)

        // Light-class status set (prototype --ok/--warn/--err/--info when d=0).
        val LightOk: Color = Color(0xFF0B7A52)
        val LightWarn: Color = Color(0xFF8F5A00)
        val LightErr: Color = Color(0xFFC0262D)
        val LightInfo: Color = Color(0xFF1D5FBF)

        /** Derive the semantic token set for one preset. */
        fun from(palette: ThemePalette): DoraiSemanticColors {
            val dark = palette.isDark
            return DoraiSemanticColors(
                background = palette.backgroundColor,
                surface = palette.surfaceColor,
                surface2 = palette.surfaceElevatedColor,
                line = palette.outlineColor,
                text = palette.onSurfaceColor,
                muted = palette.onSurfaceVariantColor,
                primary = palette.primaryColor,
                onPrimary = palette.onPrimaryColor,
                accent = palette.accentColor,
                ok = if (dark) DarkOk else LightOk,
                warn = if (dark) DarkWarn else LightWarn,
                err = if (dark) DarkErr else LightErr,
                info = if (dark) DarkInfo else LightInfo,
                codeBackground = if (dark) {
                    lerp(palette.backgroundColor, Color.Black, 0.35f)
                } else {
                    palette.surfaceElevatedColor
                },
                scrim = if (dark) Color.Black.copy(alpha = 0.60f) else Color(0xFF141428).copy(alpha = 0.40f),
            )
        }
    }
}

/** Preferred: `palette.toSemanticColors()`. */
fun ThemePalette.toSemanticColors(): DoraiSemanticColors = DoraiSemanticColors.from(this)

/** v2 color tokens in composition. Provided by `DoraiTheme`; default matches the OG Shine brand preset. */
val LocalDoraiColors = staticCompositionLocalOf { DoraiSemanticColors.from(ThemePresets.ogShine) }
