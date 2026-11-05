package com.dorai.ai.screenshots

import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotThemeRepositoryTest {

    @Test
    fun darkPresetIsExplicitAndIndependentOfSystemTheme() {
        val repository = screenshotThemeRepository(ThemePresets.dark)

        assertEquals(ThemePresets.DARK_ID, repository.state.value.selectedThemeId)
        assertFalse(repository.state.value.isSystemTheme)
        assertEquals(ThemePresets.dark.id, repository.state.value.resolve(systemInDark = false).id)
    }

    @Test
    fun lightPresetIsExplicitAndIndependentOfSystemTheme() {
        val repository = screenshotThemeRepository(ThemePresets.light)

        assertEquals(ThemePresets.LIGHT_ID, repository.state.value.selectedThemeId)
        assertFalse(repository.state.value.isSystemTheme)
        assertEquals(ThemePresets.light.id, repository.state.value.resolve(systemInDark = true).id)
    }

    @Test
    fun midnightPresetCanBeSelectedDeterministically() {
        val repository = screenshotThemeRepository(ThemePresets.midnight)

        assertEquals(ThemePresets.MIDNIGHT_ID, repository.state.value.selectedThemeId)
        assertFalse(repository.state.value.isSystemTheme)
        assertEquals(ThemePresets.midnight.id, repository.state.value.resolve(systemInDark = false).id)
    }

    @Test
    fun customPaletteIsPersistedAndSelected() {
        val custom = ThemePresets.midnight.copy(
            id = "custom_screenshot_test",
            displayName = "Screenshot Custom",
            primary = "#FF5A36",
            isCustom = true,
        )
        val repository = screenshotThemeRepository(custom)

        assertEquals(custom.id, repository.state.value.selectedThemeId)
        assertFalse(repository.state.value.isSystemTheme)
        assertTrue(repository.state.value.customThemes.any { it.id == custom.id })
        assertEquals(custom.id, repository.state.value.resolve(systemInDark = false).id)
    }

    @Test
    fun matchDeviceModeUsesExplicitSystemThemeInput() {
        val repository = screenshotThemeRepository(ThemePresets.dark, matchDevice = true)

        assertTrue(repository.state.value.isSystemTheme)
        assertEquals(ThemePresets.light.id, repository.state.value.resolve(systemInDark = false).id)
        assertEquals(ThemePresets.dark.id, repository.state.value.resolve(systemInDark = true).id)
    }
}
