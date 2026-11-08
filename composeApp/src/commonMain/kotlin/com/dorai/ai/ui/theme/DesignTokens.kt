package com.dorai.ai.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.tokens.DoraiSemanticColors

/**
 * DORAI AI AURORA design tokens.
 * Brand hues remain consistent; surface and semantic roles adapt to the active theme.
 */
object DesignTokens {
    // AURORA surfaces
    val Background: Color = Color(0xFF050A1C)
    val BackgroundOled: Color = Color(0xFF000000)
    val TerminalBackground: Color = Color(0xFF070D20)
    val Surface: Color = Color(0x0AFFFFFF)
    val SurfaceDark: Color = Color(0xFF08132B)
    val SurfaceElevated: Color = Color(0xFF0D1C38)
    val SurfaceRaised: Color = Color(0xFF13284A)

    // Theme-aware borders
    val BorderSubtle: Color = Color(0x1AFFFFFF)
    val BorderSoft: Color = Color(0x2BFFFFFF)
    val BorderStrong: Color = Color(0x4DFFFFFF)
    val BorderFocus: Color = Color(0x9925E2F5)

    // AURORA brand palette
    val BrandNavy: Color = Color(0xFF08132B)
    val BrandCyan: Color = Color(0xFF25E2F5)
    val BrandBlue: Color = Color(0xFF377CFF)
    val BrandViolet: Color = Color(0xFF6F5BFF)
    val BrandWhite: Color = Color(0xFFF1F8FF)
    val BrandInk: Color = Color(0xFF10213D)

    // Accents
    val PrimaryCyan: Color = Color(0xFF25CDE2)
    val AgentViolet: Color = Color(0xFF8B78FF)
    val AccentPrimary: Color = PrimaryCyan
    val AccentAgent: Color = AgentViolet

    // Semantic typography colors (dark defaults)
    val PrimaryText: Color = Color(0xFFF1F8FF)
    val SecondaryText: Color = Color(0xFFC4D3E8)
    val TertiaryText: Color = Color(0xFF91ABCD)
    val MutedText: Color = Color(0xFF7186A5)

    // Status colors
    val Success: Color = Color(0xFF10B981)
    val Warning: Color = Color(0xFFF59E0B)
    val Error: Color = Color(0xFFEF4444)
    val Info: Color = Color(0xFF377CFF)

    // Spacing (single source of truth shared by chat, settings, and workspace)
    val SpacingXxs: Dp = 2.dp
    val SpacingXs: Dp = 4.dp
    val SpacingSm: Dp = 8.dp
    val SpacingMd: Dp = 12.dp
    val SpacingLg: Dp = 16.dp
    val SpacingXl: Dp = 20.dp
    val SpacingXxl: Dp = 24.dp
    val SpacingXxxl: Dp = 32.dp

    /** Minimum touch target enforced across chat, settings, and workspace surfaces. */
    val MinTouchTarget: Dp = 48.dp

    // Radii
    val RadiusCard: Dp = 16.dp
    val RadiusHud: Dp = 24.dp
    val RadiusPill: Dp = 999.dp
    val RadiusSheet: Dp = 20.dp
}

val LocalDesignTokens = staticCompositionLocalOf { DesignTokens }

data class DesignColorTokens(
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val surfaceElevated: Color,
    val surfaceRaised: Color,
    val cardFill: Color,
    val cardSurface: Color,
    val borderSubtle: Color,
    val borderSoft: Color,
    val borderStrong: Color,
    val primary: Color = DesignTokens.PrimaryCyan,
    val accent: Color = DesignTokens.AgentViolet,
    val background: Color = DesignTokens.Background,
    val surface: Color = DesignTokens.SurfaceDark,
    val line: Color = DesignTokens.BorderSoft,
    val success: Color = DesignTokens.Success,
    val warning: Color = DesignTokens.Warning,
    val error: Color = DesignTokens.Error,
    val info: Color = DesignTokens.Info,
) {
    companion object {
        fun from(
            colorScheme: ColorScheme,
            palette: ThemePalette? = null,
        ): DesignColorTokens {
            val isLight = colorScheme.background.luminance() > 0.5f

            val textTertiary = when {
                palette != null && palette.id == ThemePresets.DARK_ID -> Color(0xFF91ABCD)
                palette != null && palette.id == ThemePresets.LIGHT_ID -> Color(0xFF52647D)
                palette != null -> lerp(palette.onSurfaceVariantColor, palette.surfaceColor, if (palette.isDark) 0.25f else 0.20f)
                isLight -> Color(0xFF52647D)
                else -> Color(0xFF91ABCD)
            }

            val textMuted = when {
                palette != null && palette.id == ThemePresets.DARK_ID -> Color(0xFF7186A5)
                palette != null && palette.id == ThemePresets.LIGHT_ID -> Color(0xFF718096)
                palette != null -> lerp(palette.onSurfaceVariantColor, palette.surfaceColor, if (palette.isDark) 0.40f else 0.35f)
                isLight -> Color(0xFF718096)
                else -> Color(0xFF7186A5)
            }

            val (statusSuccess, statusWarning, statusError, statusInfo) = if (palette != null) {
                val sem = DoraiSemanticColors.from(palette)
                listOf(sem.ok, sem.warn, sem.err, sem.info)
            } else {
                if (isLight) {
                    listOf(DoraiSemanticColors.LightOk, DoraiSemanticColors.LightWarn, colorScheme.error, DoraiSemanticColors.LightInfo)
                } else {
                    listOf(DoraiSemanticColors.DarkOk, DoraiSemanticColors.DarkWarn, colorScheme.error, DoraiSemanticColors.DarkInfo)
                }
            }

            return DesignColorTokens(
                textPrimary = colorScheme.onSurface,
                textSecondary = colorScheme.onSurfaceVariant,
                textTertiary = textTertiary,
                textMuted = textMuted,
                surfaceElevated = colorScheme.surfaceContainerHigh,
                surfaceRaised = colorScheme.surfaceContainerHighest,
                cardFill = colorScheme.surfaceContainerHigh,
                cardSurface = colorScheme.surface,
                borderSubtle = colorScheme.outlineVariant.copy(alpha = 0.55f),
                borderSoft = colorScheme.outlineVariant,
                borderStrong = colorScheme.outline,
                primary = palette?.primaryColor ?: colorScheme.primary,
                accent = palette?.accentColor ?: colorScheme.secondary,
                background = palette?.backgroundColor ?: colorScheme.background,
                surface = palette?.surfaceColor ?: colorScheme.surface,
                line = palette?.outlineColor ?: colorScheme.outline,
                success = statusSuccess,
                warning = statusWarning,
                error = statusError,
                info = statusInfo,
            )
        }
    }
}

val LocalDesignColorTokens = staticCompositionLocalOf {
    DesignColorTokens(
        textPrimary = DesignTokens.PrimaryText,
        textSecondary = DesignTokens.SecondaryText,
        textTertiary = DesignTokens.TertiaryText,
        textMuted = DesignTokens.MutedText,
        surfaceElevated = DesignTokens.SurfaceElevated,
        surfaceRaised = DesignTokens.SurfaceRaised,
        cardFill = DesignTokens.SurfaceElevated,
        cardSurface = DesignTokens.SurfaceDark,
        borderSubtle = DesignTokens.BorderSubtle,
        borderSoft = DesignTokens.BorderSoft,
        borderStrong = DesignTokens.BorderStrong,
        primary = DesignTokens.PrimaryCyan,
        accent = DesignTokens.AgentViolet,
        background = DesignTokens.Background,
        surface = DesignTokens.SurfaceDark,
        line = DesignTokens.BorderSoft,
        success = DesignTokens.Success,
        warning = DesignTokens.Warning,
        error = DesignTokens.Error,
        info = DesignTokens.Info,
    )
}
