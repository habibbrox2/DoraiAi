package com.dorai.ai.theme

import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.data.isValidHexColor
import com.dorai.ai.theme.data.parseHexColor
import com.dorai.ai.theme.data.resolve
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThemeEngineTest {

    private fun repository(settings: MapSettings = MapSettings()) = ThemeSettingsRepository(settings)

    @Test
    fun `defaults to dark preset with Match Device on and flat style`() {
        val state = repository().state.value
        assertEquals(ThemePresets.DARK_ID, state.selectedThemeId)
        assertTrue(state.isSystemTheme)
        assertEquals(ThemeStyle.Flat, state.activeStyle)
        assertTrue(state.customThemes.isEmpty())
    }

    @Test
    fun `legacy theme_mode migrates - Light`() {
        val settings = MapSettings()
        settings.putString("theme_mode", "Light")
        val state = repository(settings).state.value
        assertEquals(ThemePresets.LIGHT_ID, state.selectedThemeId)
        assertFalse(state.isSystemTheme)
        // tandem sync keeps the legacy key coherent
        assertEquals("Light", settings.getString("theme_mode", ""))
    }

    @Test
    fun `legacy theme_mode migrates - Dark`() {
        val settings = MapSettings()
        settings.putString("theme_mode", "Dark")
        val state = repository(settings).state.value
        assertEquals(ThemePresets.DARK_ID, state.selectedThemeId)
        assertFalse(state.isSystemTheme)
        assertEquals("Dark", settings.getString("theme_mode", ""))
    }

    @Test
    fun `legacy theme_mode migrates - System keeps Match Device on`() {
        val settings = MapSettings()
        settings.putString("theme_mode", "System")
        val state = repository(settings).state.value
        assertTrue(state.isSystemTheme)
        assertEquals("System", settings.getString("theme_mode", ""))
    }

    @Test
    fun `match device maps to dark and light base while retaining a same-class accent`() {
        val settings = MapSettings()
        val repo = repository(settings)
        repo.setSelectedThemeId(ThemePresets.MIDNIGHT_ID) // dark accent class
        repo.setMatchDevice(true)

        // Dark device: the base is Dark, carrying Midnight's accent (same luminance class).
        val darkResolved = repo.state.value.resolve(systemInDark = true)
        assertEquals(ThemePresets.DARK_ID, darkResolved.id)
        assertEquals(ThemePresets.find(ThemePresets.MIDNIGHT_ID)!!.primary, darkResolved.primary)

        // Light device: mixed classes -> the light base keeps its own accent (contrast guard).
        val lightResolved = repo.state.value.resolve(systemInDark = false)
        assertEquals(ThemePresets.LIGHT_ID, lightResolved.id)
        assertEquals(ThemePresets.light.primary, lightResolved.primary)
    }

    @Test
    fun `match device retains a light accent when device is light`() {
        val settings = MapSettings()
        val repo = repository(settings)
        repo.setSelectedThemeId(ThemePresets.SEAFOAM_ID) // light accent class
        repo.setMatchDevice(true)

        val resolved = repo.state.value.resolve(systemInDark = false)
        assertEquals(ThemePresets.LIGHT_ID, resolved.id)
        assertEquals(
            ThemePresets.find(ThemePresets.SEAFOAM_ID)!!.primary,
            resolved.primary,
        )
    }

    @Test
    fun `custom theme is saved, selected and round-trips through JSON`() {
        val settings = MapSettings()
        val repo = repository(settings)
        val custom = ThemePresets.dark.copy(
            id = "custom_test1",
            displayName = "Test",
            isCustom = true,
            primary = "#FF2E9A",
        )
        repo.saveCustomTheme(custom)

        val reloaded = repository(settings).state.value
        assertEquals(listOf("custom_test1"), reloaded.customThemes.map { it.id })
        assertEquals("custom_test1", reloaded.selectedThemeId)
        assertFalse(reloaded.isSystemTheme)
        assertEquals("#FF2E9A", reloaded.customThemes.single().primary)
    }

    @Test
    fun `deleting the selected custom theme falls back to dark`() {
        val settings = MapSettings()
        val repo = repository(settings)
        repo.saveCustomTheme(
            ThemePresets.dark.copy(id = "custom_gone", displayName = "Gone", isCustom = true),
        )
        assertEquals("custom_gone", repo.state.value.selectedThemeId)
        repo.deleteCustomTheme("custom_gone")
        assertEquals(ThemePresets.DARK_ID, repo.state.value.selectedThemeId)
        assertTrue(repo.state.value.customThemes.isEmpty())
    }

    @Test
    fun `unknown selected id resolves to dark preset`() {
        val state = com.dorai.ai.theme.data.ThemeState(
            selectedThemeId = "does_not_exist",
            isSystemTheme = false,
        )
        assertEquals(ThemePresets.DARK_ID, state.basePalette().id)
    }

    @Test
    fun `hex validation accepts shorthand and rejects garbage`() {
        assertTrue(isValidHexColor("#1E1E2E"))
        assertTrue(isValidHexColor("#fff"))
        assertTrue(isValidHexColor("#FFAABBCC"))
        assertFalse(isValidHexColor("#GG0000"))
        assertFalse(isValidHexColor("1E1E2E")) // missing '#'
        assertFalse(isValidHexColor(""))
        assertNotNull(parseHexColor("#1E1E2E"))
        assertNotEquals(null, parseHexColor("#1E1E2E"))
    }

    @Test
    fun `style setter persists and parses back`() {
        val repo = repository()
        repo.setThemeStyle(ThemeStyle.ThreeD)
        assertEquals(ThemeStyle.ThreeD, repo.state.value.activeStyle)
        val reloaded = repository()
        reloaded.setThemeStyle(ThemeStyle.ThreeD)
        assertEquals(ThemeStyle.ThreeD, reloaded.state.value.activeStyle)
    }

    @Test
    fun `all thirteen built-in presets are present with valid hex colors`() {
        val ids = ThemePresets.all.map { it.id }
        assertEquals(
            listOf(
                "dark", "light", "og_shine", "peach", "midnight", "lilac", "dusk", "seafoam",
                "graphite", "daybreak", "sherbet", "paper", "carbon",
            ),
            ids,
        )
        ThemePresets.all.forEach { palette ->
            assertTrue(isValidHexColor(palette.background), "${palette.id} background")
            assertTrue(isValidHexColor(palette.surface), "${palette.id} surface")
            assertTrue(isValidHexColor(palette.primary), "${palette.id} primary")
            assertNotNull(ThemePresets.find(palette.id))
        }
    }
}
