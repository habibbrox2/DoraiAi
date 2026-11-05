package com.dorai.ai.theme

import androidx.compose.ui.graphics.Color
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.contrastRatio
import com.dorai.ai.ui.DarkColorScheme
import com.dorai.ai.ui.LightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetContrastTest {

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
    fun `every built-in preset primary meets WCAG non-text UI component contrast of at least 3_0 to 1 against background`() {
        ThemePresets.all.forEach { palette ->
            val ratio = contrastRatio(palette.primaryColor, palette.backgroundColor)
            assertTrue(
                ratio >= 3.0,
                "${palette.id} preset primary (${palette.primary}) on background (${palette.background}) " +
                    "contrast ratio $ratio is below 3.0:1",
            )
        }
    }

    @Test
    fun `every built-in preset body text meets WCAG AA contrast of at least 4_5 to 1 against background and surface`() {
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
