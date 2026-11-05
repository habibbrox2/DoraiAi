package com.dorai.ai.theme.data

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Multiplatform (multiplatform-settings) persistence for the 2027 premium theme engine.
 *
 * The four engine keys — `selected_theme_id`, `is_system_theme`, `theme_style` and
 * `custom_themes_json` — are the source of truth. The legacy `theme_mode` key
 * (`System` / `Light` / `Dark`) is written in tandem so Android system-bar styling and any
 * remaining legacy consumers keep behaving; on first launch the repository migrates an
 * existing `theme_mode` into the new keys instead of discarding it.
 */
class ThemeSettingsRepository(private val settings: Settings) {

    private val _state = MutableStateFlow(loadInitial())

    /** Live engine state. Collected by the theme root and the Appearance screen. */
    val state: StateFlow<ThemeState> = _state.asStateFlow()

    init {
        migrateLegacyThemeModeIfNeeded()
        // Persist the migrated / default state so subsequent launches skip migration.
        persist()
        syncLegacyThemeMode()
    }

    fun setSelectedThemeId(id: String) {
        val known = _state.value.availablePresets.any { it.id == id }
        if (!known) return
        settings.putString(KEY_SELECTED_THEME_ID, id)
        _state.update { it.copy(selectedThemeId = id) }
        syncLegacyThemeMode()
    }

    fun setMatchDevice(enabled: Boolean) {
        settings.putBoolean(KEY_IS_SYSTEM_THEME, enabled)
        _state.update { it.copy(isSystemTheme = enabled) }
        syncLegacyThemeMode()
    }

    fun setThemeStyle(style: ThemeStyle) {
        settings.putString(KEY_THEME_STYLE, style.name)
        _state.update { it.copy(style = style.name) }
    }

    /**
     * Insert or replace a custom theme (matched by id), make it the active selection and turn
     * Match Device off so the choice is visible immediately. [palette] is forced to
     * [ThemePalette.isCustom] = true.
     */
    fun saveCustomTheme(palette: ThemePalette) {
        val custom = palette.copy(isCustom = true)
        val updated = _state.value.customThemes.filterNot { it.id == custom.id } + custom
        settings.putString(KEY_CUSTOM_THEMES_JSON, encodeCustomThemes(updated))
        settings.putString(KEY_SELECTED_THEME_ID, custom.id)
        settings.putBoolean(KEY_IS_SYSTEM_THEME, false)
        _state.update {
            it.copy(customThemes = updated, selectedThemeId = custom.id, isSystemTheme = false)
        }
        syncLegacyThemeMode()
    }

    /** Remove a custom theme. If it was selected, the engine falls back to the Dark preset. */
    fun deleteCustomTheme(id: String) {
        val updated = _state.value.customThemes.filterNot { it.id == id }
        if (updated.size == _state.value.customThemes.size) return
        val selection = if (_state.value.selectedThemeId == id) ThemePresets.DARK_ID else _state.value.selectedThemeId
        settings.putString(KEY_CUSTOM_THEMES_JSON, encodeCustomThemes(updated))
        settings.putString(KEY_SELECTED_THEME_ID, selection)
        _state.update { it.copy(customThemes = updated, selectedThemeId = selection) }
        syncLegacyThemeMode()
    }

    private fun loadInitial(): ThemeState {
        val styleRaw = settings.getStringOrNull(KEY_THEME_STYLE)
        return ThemeState(
            selectedThemeId = settings.getStringOrNull(KEY_SELECTED_THEME_ID) ?: ThemePresets.DARK_ID,
            isSystemTheme = settings.getBoolean(KEY_IS_SYSTEM_THEME, true),
            style = ThemeStyle.fromStorage(styleRaw).name,
            customThemes = decodeCustomThemes(settings.getStringOrNull(KEY_CUSTOM_THEMES_JSON)),
        )
    }

    /** One-time migration: honor an existing legacy `theme_mode` instead of ignoring it. */
    private fun migrateLegacyThemeModeIfNeeded() {
        if (settings.getStringOrNull(KEY_SELECTED_THEME_ID) != null) return
        when (settings.getStringOrNull(KEY_THEME_MODE)) {
            "Light" -> {
                settings.putString(KEY_SELECTED_THEME_ID, ThemePresets.LIGHT_ID)
                settings.putBoolean(KEY_IS_SYSTEM_THEME, false)
                _state.update { it.copy(selectedThemeId = ThemePresets.LIGHT_ID, isSystemTheme = false) }
            }

            "Dark" -> {
                settings.putString(KEY_SELECTED_THEME_ID, ThemePresets.DARK_ID)
                settings.putBoolean(KEY_IS_SYSTEM_THEME, false)
                _state.update { it.copy(selectedThemeId = ThemePresets.DARK_ID, isSystemTheme = false) }
            }
            // "System" (or anything unreadable) keeps the premium default: Dark base + Match Device on.
        }
    }

    /** Keep the legacy `theme_mode` key coherent so system-bar logic and legacy readers stay correct. */
    private fun syncLegacyThemeMode() {
        val mode = when {
            _state.value.isSystemTheme -> "System"
            _state.value.basePalette().isDark -> "Dark"
            else -> "Light"
        }
        settings.putString(KEY_THEME_MODE, mode)
    }

    private fun persist() {
        val current = _state.value
        settings.putString(KEY_SELECTED_THEME_ID, current.selectedThemeId)
        settings.putBoolean(KEY_IS_SYSTEM_THEME, current.isSystemTheme)
        settings.putString(KEY_THEME_STYLE, current.style)
        settings.putString(KEY_CUSTOM_THEMES_JSON, encodeCustomThemes(current.customThemes))
    }

    private fun encodeCustomThemes(themes: List<ThemePalette>): String = json.encodeToString(ListSerializer(ThemePalette.serializer()), themes)

    private fun decodeCustomThemes(raw: String?): List<ThemePalette> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(ThemePalette.serializer()), raw) }
            .getOrDefault(emptyList())
    }

    companion object {
        const val KEY_SELECTED_THEME_ID = "selected_theme_id"
        const val KEY_IS_SYSTEM_THEME = "is_system_theme"
        const val KEY_THEME_STYLE = "theme_style"
        const val KEY_CUSTOM_THEMES_JSON = "custom_themes_json"
        const val KEY_THEME_MODE = "theme_mode"

        private val json = Json { ignoreUnknownKeys = true }
    }
}
