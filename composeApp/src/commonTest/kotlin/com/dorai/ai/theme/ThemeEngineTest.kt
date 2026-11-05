package com.dorai.ai.theme

import androidx.compose.ui.graphics.Color
import com.dorai.ai.theme.data.DefaultDarkText
import com.dorai.ai.theme.data.DefaultLightText
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.data.contrastRatio
import com.dorai.ai.theme.data.isValidHexColor
import com.dorai.ai.theme.data.parseHexColor
import com.dorai.ai.theme.data.readableTextColorFor
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

    @Test
    fun `contrast ratio calculation correctly detects accessible and low contrast pairs`() {
        val accessibleRatio = contrastRatio(
            parseHexColor("#25CDE2")!!,
            parseHexColor("#08132B")!!,
        )
        assertTrue(accessibleRatio >= 9.0)

        val lowContrastRatio = contrastRatio(
            parseHexColor("#1A1A1A")!!,
            parseHexColor("#111111")!!,
        )
        assertTrue(lowContrastRatio < 3.0)
    }

    @Test
    fun `contrast ratio calculation correctly validates custom palettes`() {
        val accessibleCustom = ThemePalette(
            id = "custom_accessible",
            displayName = "Accessible Custom",
            isDark = true,
            background = "#050A1C",
            surface = "#08132B",
            surfaceElevated = "#0D1C38",
            surfaceRaised = "#13284A",
            primary = "#25CDE2",
            accent = "#8B78FF",
            onPrimary = "#08132B",
            onBackground = "#F1F8FF",
            onSurface = "#F1F8FF",
            onSurfaceVariant = "#C4D3E8",
            outline = "#2B3A55",
            outlineVariant = "#1AFFFFFF",
            isCustom = true,
        )
        val onPrimaryRatio = contrastRatio(accessibleCustom.onPrimaryColor, accessibleCustom.primaryColor)
        val onBgRatio = contrastRatio(accessibleCustom.onBackgroundColor, accessibleCustom.backgroundColor)
        assertTrue(onPrimaryRatio >= 4.5, "onPrimary contrast $onPrimaryRatio should be >= 4.5:1")
        assertTrue(onBgRatio >= 4.5, "onBackground contrast $onBgRatio should be >= 4.5:1")

        // Inaccessible custom palette where onPrimary and onBackground fail WCAG AA contrast
        val poorContrastCustom = accessibleCustom.copy(
            id = "custom_poor",
            primary = "#1E293B",
            onPrimary = "#0F172A", // Dark on dark -> fails < 3.0:1
            background = "#888888",
            onBackground = "#777777", // Gray on gray -> fails < 3.0:1
        )
        val poorPrimaryRatio = contrastRatio(poorContrastCustom.onPrimaryColor, poorContrastCustom.primaryColor)
        val poorBgRatio = contrastRatio(poorContrastCustom.onBackgroundColor, poorContrastCustom.backgroundColor)
        assertTrue(poorPrimaryRatio < 3.0, "Poor onPrimary contrast $poorPrimaryRatio should be < 3.0:1")
        assertTrue(poorBgRatio < 3.0, "Poor onBackground contrast $poorBgRatio should be < 3.0:1")
    }

    @Test
    fun `readableTextColorFor selects highest contrast text color for custom palettes`() {
        val darkBg = parseHexColor("#050A1C")!!
        val lightBg = parseHexColor("#FFFFFF")!!
        val textColorForDark = readableTextColorFor(darkBg)
        val textColorForLight = readableTextColorFor(lightBg)

        assertEquals(DefaultLightText, textColorForDark)
        assertEquals(DefaultDarkText, textColorForLight)

        val contrastOnDark = contrastRatio(textColorForDark, darkBg)
        val contrastOnLight = contrastRatio(textColorForLight, lightBg)
        assertTrue(contrastOnDark >= 4.5, "Contrast on dark $contrastOnDark should be >= 4.5:1")
        assertTrue(contrastOnLight >= 4.5, "Contrast on light $contrastOnLight should be >= 4.5:1")
    }

    @Test
    fun `ThemeViewModel creates accessible custom palette with compliant contrast`() {
        val repo = repository()
        val viewModel = ThemeViewModel(repo)
        val saved = viewModel.saveCustomTheme(
            displayName = "Cyan Cyberpunk",
            backgroundHex = "#120824",
            surfaceHex = "#1A0D33",
            primaryHex = "#00F0FF",
        )
        assertTrue(saved)
        val customTheme = repo.state.value.customThemes.single()
        val onPrimaryRatio = contrastRatio(customTheme.onPrimaryColor, customTheme.primaryColor)
        val onBgRatio = contrastRatio(customTheme.onBackgroundColor, customTheme.backgroundColor)
        assertTrue(onPrimaryRatio >= 4.5, "Derived onPrimary contrast $onPrimaryRatio should be >= 4.5:1")
        assertTrue(onBgRatio >= 4.5, "Derived onBackground contrast $onBgRatio should be >= 4.5:1")
    }
}
