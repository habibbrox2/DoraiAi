package com.inspiredandroid.kai.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Brox Kai v2.0 — 2027 Premium AI Agent Workspace Design Tokens.
 *
 * Implements the design specifications defined in docs/ui-upgrade-plan-7-premium-chat.md
 * and aligns with the Brox Kai Brand & Logo Pack.
 */
object DesignTokens {
    // ─── Core Surfaces (Deep Obsidian / OLED) ───
    val Background: Color = Color(0xFF090A0F)
    val BackgroundOled: Color = Color(0xFF000000)
    val TerminalBackground: Color = Color(0xFF0B0D11)
    val Surface: Color = Color(0x0AFFFFFF) // rgba(255, 255, 255, 0.04)
    val SurfaceDark: Color = Color(0xFF0F1117)
    val SurfaceElevated: Color = Color(0xFF171A23)
    val SurfaceRaised: Color = Color(0xFF1E2330)

    // ─── Borders ───
    val BorderSubtle: Color = Color(0x14FFFFFF) // rgba(255, 255, 255, 0.08)
    val BorderSoft: Color = Color(0x26FFFFFF) // rgba(255, 255, 255, 0.15)
    val BorderStrong: Color = Color(0x40FFFFFF) // rgba(255, 255, 255, 0.25)
    val BorderFocus: Color = Color(0x6600D7E8)

    // ─── Brand Colors (Brox Kai v2.0 Official Logo Pack) ───
    val BrandNavy: Color = Color(0xFF0B2B5B)
    val BrandCyan: Color = Color(0xFF00D1E8)
    val BrandBlue: Color = Color(0xFF168BDB)
    val BrandWhite: Color = Color(0xFFFFFFFF)

    // ─── Accents ───
    val PrimaryCyan: Color = Color(0xFF00D7E8)
    val AgentViolet: Color = Color(0xFF8A63F6)
    val AccentPrimary: Color = PrimaryCyan
    val AccentAgent: Color = AgentViolet

    // ─── Semantic Typography Colors ───
    val PrimaryText: Color = Color(0xFFF0F3F8)
    val SecondaryText: Color = Color(0xFFCBD5E1)
    val TertiaryText: Color = Color(0xFF94A3B8)
    val MutedText: Color = Color(0xFF64748B)

    // ─── Status Colors ───
    val Success: Color = Color(0xFF10B981)
    val Warning: Color = Color(0xFFF59E0B)
    val Error: Color = Color(0xFFEF4444)
    val Info: Color = Color(0xFF3B82F6)

    // ─── Spacing Dimensions ───
    val SpacingXxs: Dp = 2.dp
    val SpacingXs: Dp = 4.dp
    val SpacingSm: Dp = 8.dp
    val SpacingMd: Dp = 12.dp
    val SpacingLg: Dp = 16.dp
    val SpacingXl: Dp = 20.dp
    val SpacingXxl: Dp = 24.dp
    val SpacingXxxl: Dp = 32.dp

    // ─── Corner Radii ───
    val RadiusCard: Dp = 16.dp
    val RadiusHud: Dp = 24.dp
    val RadiusPill: Dp = 999.dp
    val RadiusSheet: Dp = 20.dp
}

/**
 * CompositionLocal providing access to [DesignTokens] throughout the composable tree.
 */
val LocalDesignTokens = staticCompositionLocalOf { DesignTokens }

/**
 * Immutable color roles derived from the active Material color scheme.
 *
 * Layout metrics and brand/status constants remain in [DesignTokens]; this palette
 * represents only theme-dependent foreground, surface, and border roles.
 */
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
) {
    companion object {
        fun from(colorScheme: ColorScheme): DesignColorTokens {
            val isLight = colorScheme.background.luminance() > 0.5f
            return DesignColorTokens(
                textPrimary = colorScheme.onSurface,
                textSecondary = colorScheme.onSurfaceVariant,
                textTertiary = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                textMuted = if (isLight) Color(0xFF94A3B8) else Color(0xFF64748B),
                surfaceElevated = colorScheme.surfaceContainerHigh,
                surfaceRaised = colorScheme.surfaceContainerHighest,
                cardFill = colorScheme.surfaceContainerHigh,
                cardSurface = colorScheme.surface,
                borderSubtle = colorScheme.outlineVariant.copy(alpha = 0.55f),
                borderSoft = colorScheme.outlineVariant,
                borderStrong = colorScheme.outline,
            )
        }
    }
}

/** Theme-scoped color roles; unlike [LocalDesignTokens], this follows the active scheme. */
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
    )
}
