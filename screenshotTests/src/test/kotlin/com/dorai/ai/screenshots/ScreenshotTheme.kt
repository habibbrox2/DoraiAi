package com.dorai.ai.screenshots

import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.russhwolf.settings.MapSettings

/** Creates isolated, deterministic theme state for a single screenshot render. */
internal fun screenshotThemeRepository(
    palette: ThemePalette,
    matchDevice: Boolean = false,
): ThemeSettingsRepository {
    val repository = ThemeSettingsRepository(MapSettings())
    if (!palette.isCustom && ThemePresets.find(palette.id) != null) {
        repository.setSelectedThemeId(palette.id)
    } else {
        repository.saveCustomTheme(palette.copy(isCustom = true))
    }
    repository.setMatchDevice(matchDevice)
    return repository
}
