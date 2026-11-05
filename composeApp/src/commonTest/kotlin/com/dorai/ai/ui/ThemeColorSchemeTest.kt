package com.dorai.ai.ui

import androidx.compose.ui.graphics.Color
import com.dorai.ai.ui.theme.DesignColorTokens
import com.dorai.ai.ui.theme.DesignTokens
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
        assertEquals(Color(0xFF000000), tokens.textPrimary)
        assertEquals(Color(0xFFFFFFFF), LightColorScheme.onPrimary)
    }

    @Test
    fun `dark color roles are derived from the supplied Material scheme`() {
        val tokens = DesignColorTokens.from(DarkColorScheme)

        assertEquals(DarkColorScheme.onSurface, tokens.textPrimary)
        assertEquals(DarkColorScheme.onSurfaceVariant, tokens.textSecondary)
        assertEquals(DarkColorScheme.surfaceContainerHigh, tokens.surfaceElevated)
        assertEquals(DarkColorScheme.surface, tokens.cardSurface)
        assertEquals(DarkColorScheme.outline, tokens.borderStrong)
        assertEquals(Color(0xFFF1F8FF), tokens.textPrimary)
        assertEquals(Color(0xFF08132B), DarkColorScheme.onPrimary)
    }

    @Test
    fun `AURORA brand palette uses shared cyan blue violet identity`() {
        assertEquals(Color(0xFF050A1C), DesignTokens.Background)
        assertEquals(Color(0xFF000000), DesignTokens.BackgroundOled)
        assertEquals(Color(0xFF08132B), DesignTokens.SurfaceDark)
        assertEquals(Color(0xFF0D1C38), DesignTokens.SurfaceElevated)
        assertEquals(Color(0xFF13284A), DesignTokens.SurfaceRaised)
        assertEquals(Color(0xFF08132B), DesignTokens.BrandNavy)
        assertEquals(Color(0xFF25E2F5), DesignTokens.BrandCyan)
        assertEquals(Color(0xFF377CFF), DesignTokens.BrandBlue)
        assertEquals(Color(0xFF6F5BFF), DesignTokens.BrandViolet)
        assertEquals(Color(0xFF25CDE2), DesignTokens.PrimaryCyan)
        assertEquals(Color(0xFF8B78FF), DesignTokens.AgentViolet)
        assertEquals(DesignTokens.PrimaryCyan, DesignTokens.AccentPrimary)
        assertEquals(DesignTokens.AgentViolet, DesignTokens.AccentAgent)
    }

    @Test
    fun `local design tokens has default instance`() {
        assertNotNull(com.dorai.ai.ui.theme.LocalDesignTokens)
    }
}
