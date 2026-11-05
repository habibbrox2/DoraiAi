package com.dorai.ai.theme

import androidx.compose.ui.graphics.Color
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.contrastRatio
import com.dorai.ai.ui.DarkColorScheme
import com.dorai.ai.ui.LightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetContrastTest {

    private val allExpectedPresetIds = listOf(
        ThemePresets.DARK_ID,
        ThemePresets.LIGHT_ID,
        ThemePresets.OG_SHINE_ID,
        ThemePresets.PEACH_ID,
        ThemePresets.MIDNIGHT_ID,
        ThemePresets.LILAC_ID,
        ThemePresets.DUSK_ID,
        ThemePresets.SEAFOAM_ID,
        ThemePresets.GRAPHITE_ID,
        ThemePresets.DAYBREAK_ID,
        ThemePresets.SHERBET_ID,
        ThemePresets.PAPER_ID,
        ThemePresets.CARBON_ID,
    )

    @Test
    fun `all thirteen built-in presets are registered and divided into dark and light variants`() {
        assertEquals(13, ThemePresets.all.size, "ThemePresets.all must contain exactly 13 presets")
        assertEquals(allExpectedPresetIds, ThemePresets.all.map { it.id })

        val darkPresets = ThemePresets.all.filter { it.isDark }
        val lightPresets = ThemePresets.all.filter { !it.isDark }

        assertEquals(6, darkPresets.size, "Expected exactly 6 dark presets")
        assertEquals(7, lightPresets.size, "Expected exactly 7 light presets")
    }

    @Test
    fun `every built-in preset onPrimary meets WCAG AA normal text contrast of at least 4_5 to 1 against primary`() {
        ThemePresets.all.forEach { palette ->
            val ratio = contrastRatio(palette.onPrimaryColor, palette.primaryColor)
            assertTrue(
                ratio >= 4.5,
                "${palette.id} preset onPrimary (${palette.onPrimary}) on primary (${palette.primary}) " +
                    "contrast ratio $ratio is below 4.5:1",
            )
        }
    }

    @Test
    fun `every built-in preset body text meets WCAG AA normal text contrast of at least 4_5 to 1 against background and surface`() {
        ThemePresets.all.forEach { palette ->
            val bgRatio = contrastRatio(palette.onBackgroundColor, palette.backgroundColor)
            val surfaceRatio = contrastRatio(palette.onSurfaceColor, palette.surfaceColor)

            assertTrue(
                bgRatio >= 4.5,
                "${palette.id} preset onBackground (${palette.onBackground}) on background (${palette.background}) " +
                    "contrast ratio $bgRatio is below 4.5:1",
            )
            assertTrue(
                surfaceRatio >= 4.5,
                "${palette.id} preset onSurface (${palette.onSurface}) on surface (${palette.surface}) " +
                    "contrast ratio $surfaceRatio is below 4.5:1",
            )
        }
    }

    @Test
    fun `every built-in preset secondary text meets WCAG AA normal text contrast of at least 4_5 to 1 against surface`() {
        ThemePresets.all.forEach { palette ->
            val ratio = contrastRatio(palette.onSurfaceVariantColor, palette.surfaceColor)
            assertTrue(
                ratio >= 4.5,
                "${palette.id} preset onSurfaceVariant (${palette.onSurfaceVariant}) on surface (${palette.surface}) " +
                    "contrast ratio $ratio is below 4.5:1",
            )
        }
    }

    @Test
    fun `every built-in preset primary meets WCAG large text contrast of at least 3_0 to 1 against background and surface`() {
        ThemePresets.all.forEach { palette ->
            val bgRatio = contrastRatio(palette.primaryColor, palette.backgroundColor)
            val surfaceRatio = contrastRatio(palette.primaryColor, palette.surfaceColor)

            assertTrue(
                bgRatio >= 3.0,
                "${palette.id} preset primary (${palette.primary}) on background (${palette.background}) " +
                    "contrast ratio $bgRatio is below 3.0:1 (large text / UI component requirement)",
            )
            assertTrue(
                surfaceRatio >= 3.0,
                "${palette.id} preset primary (${palette.primary}) on surface (${palette.surface}) " +
                    "contrast ratio $surfaceRatio is below 3.0:1",
            )
        }
    }

    @Test
    fun `every built-in preset primary accent border meets WCAG non-text UI component contrast of at least 3_0 to 1`() {
        // DoraiBorders.accent() and DoraiBorders.focus() use primaryColor
        ThemePresets.all.forEach { palette ->
            val bgRatio = contrastRatio(palette.primaryColor, palette.backgroundColor)
            val surfaceRatio = contrastRatio(palette.primaryColor, palette.surfaceColor)

            assertTrue(
                bgRatio >= 3.0,
                "${palette.id} preset accent border (primary ${palette.primary}) on background (${palette.background}) " +
                    "contrast ratio $bgRatio is below 3.0:1",
            )
            assertTrue(
                surfaceRatio >= 3.0,
                "${palette.id} preset accent border (primary ${palette.primary}) on surface (${palette.surface}) " +
                    "contrast ratio $surfaceRatio is below 3.0:1",
            )
        }
    }

    @Test
    fun `custom theme contrast validations assert normal text, large text and UI border compliance`() {
        val accessibleCustom = ThemePalette(
            id = "custom_test",
            displayName = "Accessible Cyberpunk",
            isDark = true,
            background = "#0E0720",
            surface = "#160C30",
            surfaceElevated = "#201242",
            surfaceRaised = "#2B1A56",
            primary = "#00F0FF",
            accent = "#BD00FF",
            onPrimary = "#0E0720",
            onBackground = "#F5F0FF",
            onSurface = "#F5F0FF",
            onSurfaceVariant = "#C5B3E6",
            outline = "#00F0FF",
            outlineVariant = "#2B1A56",
            isCustom = true,
        )

        // 1. Normal text assertions (>= 4.5:1)
        val onPrimaryRatio = contrastRatio(accessibleCustom.onPrimaryColor, accessibleCustom.primaryColor)
        val onBgRatio = contrastRatio(accessibleCustom.onBackgroundColor, accessibleCustom.backgroundColor)
        val onSurfaceRatio = contrastRatio(accessibleCustom.onSurfaceColor, accessibleCustom.surfaceColor)
        val onSurfaceVarRatio = contrastRatio(accessibleCustom.onSurfaceVariantColor, accessibleCustom.surfaceColor)

        assertTrue(onPrimaryRatio >= 4.5, "Custom theme onPrimary normal text ratio $onPrimaryRatio should be >= 4.5:1")
        assertTrue(onBgRatio >= 4.5, "Custom theme onBackground normal text ratio $onBgRatio should be >= 4.5:1")
        assertTrue(onSurfaceRatio >= 4.5, "Custom theme onSurface normal text ratio $onSurfaceRatio should be >= 4.5:1")
        assertTrue(onSurfaceVarRatio >= 4.5, "Custom theme onSurfaceVariant normal text ratio $onSurfaceVarRatio should be >= 4.5:1")

        // 2. Large text assertions (>= 3.0:1)
        val primaryBgRatio = contrastRatio(accessibleCustom.primaryColor, accessibleCustom.backgroundColor)
        val primarySurfaceRatio = contrastRatio(accessibleCustom.primaryColor, accessibleCustom.surfaceColor)
        assertTrue(primaryBgRatio >= 3.0, "Custom theme primary large text ratio $primaryBgRatio should be >= 3.0:1")
        assertTrue(primarySurfaceRatio >= 3.0, "Custom theme primary large text ratio $primarySurfaceRatio should be >= 3.0:1")

        // 3. UI borders assertions (>= 3.0:1)
        val borderBgRatio = contrastRatio(accessibleCustom.outlineColor, accessibleCustom.backgroundColor)
        val borderSurfaceRatio = contrastRatio(accessibleCustom.outlineColor, accessibleCustom.surfaceColor)
        assertTrue(borderBgRatio >= 3.0, "Custom theme UI border ratio $borderBgRatio should be >= 3.0:1")
        assertTrue(borderSurfaceRatio >= 3.0, "Custom theme UI border ratio $borderSurfaceRatio should be >= 3.0:1")
    }

    @Test
    fun `dark preset onPrimary uses dark ink with high contrast on cyan primary`() {
        val dark = ThemePresets.dark
        assertEquals("#08132B", dark.onPrimary)
        val ratio = contrastRatio(dark.onPrimaryColor, dark.primaryColor)
        assertTrue(ratio >= 9.0, "Dark preset onPrimary vs primary contrast ratio $ratio is below 9.0:1")
    }

    @Test
    fun `material color schemes provide accessible onPrimary contrast`() {
        assertEquals(Color(0xFF08132B), DarkColorScheme.onPrimary)
        assertEquals(Color(0xFFFFFFFF), LightColorScheme.onPrimary)

        val darkRatio = contrastRatio(DarkColorScheme.onPrimary, DarkColorScheme.primary)
        assertTrue(darkRatio >= 9.0, "DarkColorScheme onPrimary contrast ratio $darkRatio is below 9.0:1")

        val lightRatio = contrastRatio(LightColorScheme.onPrimary, LightColorScheme.primary)
        assertTrue(lightRatio >= 4.5, "LightColorScheme onPrimary contrast ratio $lightRatio is below 4.5:1")
    }
}
