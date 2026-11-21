package com.dorai.ai.screenshots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.engine.DoraiTheme
import com.dorai.ai.ui.components.v2.ErrorLogItem
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Bengali-mode error-state snapshot: `ErrorLogItem`'s own labels (status a11y, "Details",
 * "Copy details", "Retry") are localized, so this captures the Bengali rendering of the
 * error card in a dark theme.
 *
 * Note: `composeApp.Res` accessors are `internal`, so localized short messages live at the
 * production call sites (HeartbeatSection, McpSection); this snapshot covers the error
 * card's localized chrome in Bengali mode.
 */
class ErrorStatesLocalizationScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9A.copy(softButtons = false),
    )

    private lateinit var originalLocale: Locale

    @Before
    @OptIn(ExperimentalResourceApi::class)
    fun setup() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale("bn"))
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_9A.copy(softButtons = false, locale = "bn"),
        )
        setResourceReaderAndroidContext(paparazzi.context)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun errorStatesBengali() {
        paparazzi.snapshot(name = "error_states_bn") {
            Box(Modifier.fillMaxSize()) {
                WithScreenshotTestKoin {
                    CompositionLocalProvider(LocalInspectionMode provides true) {
                        DoraiTheme(repository = screenshotThemeRepository(ThemePresets.dark)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                ErrorLogItem(
                                    title = "Heartbeat",
                                    timeText = "21:53",
                                    shortMessage = "Connection timed out",
                                    detailsText = "Connection to api.openai.com timed out after 30s",
                                    onRetry = {},
                                )
                                ErrorLogItem(
                                    title = "Heartbeat",
                                    timeText = "21:40",
                                    shortMessage = "Model endpoint not found (HTTP 404)",
                                )
                                ErrorLogItem(
                                    title = "Heartbeat",
                                    timeText = "21:20",
                                    shortMessage = "Unknown error",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
