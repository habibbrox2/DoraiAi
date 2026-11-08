package com.dorai.ai.ui

import androidx.compose.ui.graphics.Color
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.contrastRatio
import com.dorai.ai.theme.data.relativeLuminance
import com.dorai.ai.theme.engine.toColorScheme
import com.dorai.ai.theme.tokens.DoraiSemanticColors
import com.dorai.ai.ui.theme.DesignColorTokens
import com.dorai.ai.ui.theme.DesignTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    fun `every dark preset converts to a valid dark Material color scheme with consistent tokens`() {
        val darkPresets = ThemePresets.all.filter { it.isDark }
        assertEquals(6, darkPresets.size)

        darkPresets.forEach { preset ->
            val scheme = preset.toColorScheme()
            assertTrue(
                scheme.background.relativeLuminance() < 0.5,
                "${preset.id} background relative luminance should be dark (< 0.5)",
            )
            assertEquals(preset.primaryColor, scheme.primary, "${preset.id} primary")
            assertEquals(preset.onPrimaryColor, scheme.onPrimary, "${preset.id} onPrimary")
            assertEquals(preset.backgroundColor, scheme.background, "${preset.id} background")
            assertEquals(preset.onBackgroundColor, scheme.onBackground, "${preset.id} onBackground")
            assertEquals(preset.surfaceColor, scheme.surface, "${preset.id} surface")
            assertEquals(preset.onSurfaceColor, scheme.onSurface, "${preset.id} onSurface")
            assertEquals(preset.onSurfaceVariantColor, scheme.onSurfaceVariant, "${preset.id} onSurfaceVariant")
            assertEquals(preset.outlineColor, scheme.outline, "${preset.id} outline")

            // Derived tokens validation
            val tokens = DesignColorTokens.from(scheme)
            assertEquals(scheme.onSurface, tokens.textPrimary)
            assertEquals(scheme.onSurfaceVariant, tokens.textSecondary)
            assertEquals(scheme.surfaceContainerHigh, tokens.surfaceElevated)
            assertEquals(scheme.surface, tokens.cardSurface)
            assertEquals(scheme.outline, tokens.borderStrong)

            // Text contrast validation on converted scheme
            assertTrue(
                contrastRatio(scheme.onPrimary, scheme.primary) >= 4.5,
                "${preset.id} onPrimary on primary contrast ratio should be >= 4.5:1",
            )
            assertTrue(
                contrastRatio(scheme.onBackground, scheme.background) >= 4.5,
                "${preset.id} onBackground on background contrast ratio should be >= 4.5:1",
            )
            assertTrue(
                contrastRatio(scheme.onSurface, scheme.surface) >= 4.5,
                "${preset.id} onSurface on surface contrast ratio should be >= 4.5:1",
            )
        }
    }

    @Test
    fun `every light preset converts to a valid light Material color scheme with consistent tokens`() {
        val lightPresets = ThemePresets.all.filter { !it.isDark }
        assertEquals(7, lightPresets.size)

        lightPresets.forEach { preset ->
            val scheme = preset.toColorScheme()
            assertFalse(
                scheme.background.relativeLuminance() < 0.5,
                "${preset.id} background relative luminance should be light (>= 0.5)",
            )
            assertEquals(preset.primaryColor, scheme.primary, "${preset.id} primary")
            assertEquals(preset.onPrimaryColor, scheme.onPrimary, "${preset.id} onPrimary")
            assertEquals(preset.backgroundColor, scheme.background, "${preset.id} background")
            assertEquals(preset.onBackgroundColor, scheme.onBackground, "${preset.id} onBackground")
            assertEquals(preset.surfaceColor, scheme.surface, "${preset.id} surface")
            assertEquals(preset.onSurfaceColor, scheme.onSurface, "${preset.id} onSurface")
            assertEquals(preset.onSurfaceVariantColor, scheme.onSurfaceVariant, "${preset.id} onSurfaceVariant")
            assertEquals(preset.outlineColor, scheme.outline, "${preset.id} outline")

            // Derived tokens validation
            val tokens = DesignColorTokens.from(scheme)
            assertEquals(scheme.onSurface, tokens.textPrimary)
            assertEquals(scheme.onSurfaceVariant, tokens.textSecondary)
            assertEquals(scheme.surfaceContainerHigh, tokens.surfaceElevated)
            assertEquals(scheme.surface, tokens.cardSurface)
            assertEquals(scheme.outline, tokens.borderStrong)

            // Text contrast validation on converted scheme
            assertTrue(
                contrastRatio(scheme.onPrimary, scheme.primary) >= 4.5,
                "${preset.id} onPrimary on primary contrast ratio should be >= 4.5:1",
            )
            assertTrue(
                contrastRatio(scheme.onBackground, scheme.background) >= 4.5,
                "${preset.id} onBackground on background contrast ratio should be >= 4.5:1",
            )
            assertTrue(
                contrastRatio(scheme.onSurface, scheme.surface) >= 4.5,
                "${preset.id} onSurface on surface contrast ratio should be >= 4.5:1",
            )
        }
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

    @Test
    fun `legacy DesignColorTokens harmonizes with DoraiSemanticColors across all 13 presets`() {
        ThemePresets.all.forEach { preset ->
            val scheme = preset.toColorScheme()
            val legacyTokens = DesignColorTokens.from(scheme, preset)
            val semanticColors = DoraiSemanticColors.from(preset)

            assertEquals(semanticColors.primary, legacyTokens.primary, "${preset.id} primary contract")
            assertEquals(semanticColors.accent, legacyTokens.accent, "${preset.id} accent contract")
            assertEquals(semanticColors.background, legacyTokens.background, "${preset.id} background contract")
            assertEquals(semanticColors.surface, legacyTokens.surface, "${preset.id} surface contract")
            assertEquals(semanticColors.line, legacyTokens.line, "${preset.id} line contract")
            assertEquals(semanticColors.text, legacyTokens.textPrimary, "${preset.id} textPrimary contract")
            assertEquals(semanticColors.muted, legacyTokens.textSecondary, "${preset.id} textSecondary contract")
            assertEquals(semanticColors.surface2, legacyTokens.surfaceElevated, "${preset.id} surfaceElevated contract")
            assertEquals(semanticColors.surface, legacyTokens.cardSurface, "${preset.id} cardSurface contract")

            assertEquals(semanticColors.ok, legacyTokens.success, "${preset.id} success status contract")
            assertEquals(semanticColors.warn, legacyTokens.warning, "${preset.id} warning status contract")
            assertEquals(semanticColors.err, legacyTokens.error, "${preset.id} error status contract")
            assertEquals(semanticColors.info, legacyTokens.info, "${preset.id} info status contract")
        }
    }
}
