package com.inspiredandroid.kai.ui

import androidx.compose.ui.graphics.Color
import com.inspiredandroid.kai.ui.theme.DesignColorTokens
import com.inspiredandroid.kai.ui.theme.DesignTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ThemeColorSchemeTest {

    @Test
    fun `light color roles are derived from the supplied Material scheme`() {
        val tokens = DesignColorTokens.from(LightColorScheme)

        assertEquals(LightColorScheme.onSurface, tokens.textPrimary)
        assertEquals(LightColorScheme.onSurfaceVariant, tokens.textSecondary)
        assertEquals(LightColorScheme.surfaceContainerHigh, tokens.surfaceElevated)
        assertEquals(LightColorScheme.surface, tokens.cardSurface)
        assertEquals(LightColorScheme.outline, tokens.borderStrong)
        assertEquals(Color(0xFF0F172A), tokens.textPrimary)
    }

    @Test
    fun `dark color roles are derived from the supplied Material scheme`() {
        val tokens = DesignColorTokens.from(DarkColorScheme)

        assertEquals(DarkColorScheme.onSurface, tokens.textPrimary)
        assertEquals(DarkColorScheme.onSurfaceVariant, tokens.textSecondary)
        assertEquals(DarkColorScheme.surfaceContainerHigh, tokens.surfaceElevated)
        assertEquals(DarkColorScheme.surface, tokens.cardSurface)
        assertEquals(DarkColorScheme.outline, tokens.borderStrong)
        assertEquals(Color(0xFFF0F3F8), tokens.textPrimary)
    }

    @Test
    fun `2027 premium workspace design tokens match specification`() {
        // Core Obsidian / OLED surfaces
        assertEquals(Color(0xFF090A0F), DesignTokens.Background)
        assertEquals(Color(0xFF000000), DesignTokens.BackgroundOled)
        assertEquals(Color(0xFF0B0D11), DesignTokens.TerminalBackground)
        assertEquals(Color(0x0AFFFFFF), DesignTokens.Surface)
        assertEquals(Color(0xFF171A23), DesignTokens.SurfaceElevated)
        assertEquals(Color(0xFF1E2330), DesignTokens.SurfaceRaised)

        // Borders
        assertEquals(Color(0x14FFFFFF), DesignTokens.BorderSubtle)
        assertEquals(Color(0x26FFFFFF), DesignTokens.BorderSoft)
        assertEquals(Color(0x40FFFFFF), DesignTokens.BorderStrong)

        // Brox Kai Logo Pack Brand Colors
        assertEquals(Color(0xFF0B2B5B), DesignTokens.BrandNavy)
        assertEquals(Color(0xFF00D1E8), DesignTokens.BrandCyan)
        assertEquals(Color(0xFF168BDB), DesignTokens.BrandBlue)

        // Accents
        assertEquals(Color(0xFF00D7E8), DesignTokens.PrimaryCyan)
        assertEquals(Color(0xFF8A63F6), DesignTokens.AgentViolet)
        assertEquals(DesignTokens.PrimaryCyan, DesignTokens.AccentPrimary)
        assertEquals(DesignTokens.AgentViolet, DesignTokens.AccentAgent)

        // Status Colors
        assertEquals(Color(0xFF10B981), DesignTokens.Success)
        assertEquals(Color(0xFFF59E0B), DesignTokens.Warning)
        assertEquals(Color(0xFFEF4444), DesignTokens.Error)
        assertEquals(Color(0xFF3B82F6), DesignTokens.Info)
    }

    @Test
    fun `local design tokens has default instance`() {
        assertNotNull(com.inspiredandroid.kai.ui.theme.LocalDesignTokens)
    }
}
