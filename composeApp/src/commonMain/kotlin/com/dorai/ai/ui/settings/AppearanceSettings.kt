package com.dorai.ai.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dorai.ai.theme.ThemeViewModel
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.dorai.ai.theme.ui.DoraiAppearanceSettings
import org.koin.compose.koinInject

/**
 * Appearance tab for the 2027 premium theme engine (presets, Match Device, Flat/3D surface
 * style, custom themes). The legacy System/Light/Dark switch is replaced by the engine's own
 * state; `theme_mode` is still written in tandem by [com.dorai.ai.theme.data.ThemeSettingsRepository]
 * for Android system-bar styling.
 *
 * The view model is constructed directly (rather than via `koinViewModel`) so the tab renders
 * without a [androidx.lifecycle.ViewModelStoreOwner] — required by Paparazzi screenshot tests
 * and harmless in production, since the repository is still the Koin singleton.
 */
@Composable
internal fun AppearanceContent(
    uiState: SettingsUiState,
    actions: SettingsActions,
) {
    val repository = koinInject<ThemeSettingsRepository>()
    val viewModel = remember { ThemeViewModel(repository) }
    DoraiAppearanceSettings(viewModel = viewModel)
}
