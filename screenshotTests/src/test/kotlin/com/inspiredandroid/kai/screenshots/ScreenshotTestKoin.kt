package com.inspiredandroid.kai.screenshots

import androidx.compose.runtime.Composable
import com.inspiredandroid.kai.data.AppSettings
import com.russhwolf.settings.MapSettings
import org.koin.compose.KoinApplication
import org.koin.dsl.module

private val screenshotTestModule = module {
    single { AppSettings(MapSettings()) }
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
