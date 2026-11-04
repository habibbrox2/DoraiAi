package com.dorai.ai.screenshots

import androidx.compose.runtime.Composable
import com.dorai.ai.data.AppSettings
import com.dorai.ai.theme.ThemeViewModel
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.russhwolf.settings.MapSettings
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val screenshotTestModule = module {
    single { AppSettings(MapSettings()) }
    single { ThemeSettingsRepository(MapSettings()) }
    viewModel { ThemeViewModel(get<ThemeSettingsRepository>()) }
}

/**
 * Provides the small dependency graph required by screenshot-rendered chat UI.
 * Keep this isolated from the production app module so Paparazzi never starts
 * platform services or touches persistent user settings.
 */
@Composable
internal fun WithScreenshotTestKoin(content: @Composable () -> Unit) {
    KoinApplication(application = { modules(screenshotTestModule) }) {
        content()
    }
}
