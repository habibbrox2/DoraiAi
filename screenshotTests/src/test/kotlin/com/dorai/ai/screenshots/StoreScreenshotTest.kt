package com.dorai.ai.screenshots

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.engine.DoraiTheme
import com.dorai.ai.ui.dynamicui.LocalPreviewImages
import com.dorai.ai.ui.settings.SettingsScreenContent
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class StoreScreenshotTest(
    private val locale: String,
    private val playStoreLocale: String,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun locales() = StoreLocales.all
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9A.copy(softButtons = false),
        showSystemUi = true,
        useDeviceResolution = true,
    )

    private lateinit var originalLocale: Locale
    private val previewImages = mutableMapOf<String, ImageBitmap>()

    @OptIn(ExperimentalResourceApi::class)
    @Before
    fun setup() {
        originalLocale = Locale.getDefault()
        val newLocale = if (locale.contains("-")) {
            val parts = locale.split("-")
            Locale(parts[0], parts[1])
        } else {
            Locale(locale)
        }
        Locale.setDefault(newLocale)

        val deviceLocale = if (locale.contains("-")) {
            val parts = locale.split("-")
            "${parts[0]}-r${parts[1]}"
        } else {
            locale
        }
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_9A.copy(
                softButtons = false,
                locale = deviceLocale,
            ),
        )
        setResourceReaderAndroidContext(paparazzi.context)
        loadPreviewImage("resource://orc_survival.png", "/orc_survival.png")
        loadPreviewImage("resource://cacio_e_pepe.png", "/cacio_e_pepe.png")
    }

    private fun loadPreviewImage(key: String, resourcePath: String) {
        val bitmap = BitmapFactory.decodeStream(javaClass.getResourceAsStream(resourcePath)) ?: return
        previewImages[key] = bitmap.asImageBitmap()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    private fun snap(
        name: String,
        palette: ThemePalette,
        content: @Composable () -> Unit,
    ) {
        val theme = if (palette.isDark) {
            "android:Theme.Material.NoActionBar"
        } else {
            "android:Theme.Material.Light.NoActionBar"
        }
        paparazzi.unsafeUpdateConfig(theme = theme)
        paparazzi.snapshot(name = "store_${playStoreLocale}_$name") {
            Box(modifier = Modifier.fillMaxSize()) {
                WithScreenshotTestKoin {
                    CompositionLocalProvider(
                        LocalInspectionMode provides true,
                        LocalPreviewImages provides previewImages,
                    ) {
                        DoraiTheme(repository = screenshotThemeRepository(palette)) {
                            content()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun chatEmptyState() {
        snap("01", ThemePresets.light) {
            DoraiChatScreenV2(
                conversations = ScreenshotTestData.v2Conversations,
            )
        }
    }

    @Test
    fun chatWithMessages() {
        snap("02", ThemePresets.dark) {
            DoraiChatScreenV2(
                messages = ScreenshotTestData.localizedV2Messages(locale),
                conversations = ScreenshotTestData.v2Conversations,
                selectedModeId = "build",
            )
        }
    }

    @Test
    fun chatWithDynamicUi() {
        snap("03", ThemePresets.light) {
            DoraiChatScreenV2(
                messages = ScreenshotTestData.localizedV2DynamicUiMessages(locale),
                conversations = ScreenshotTestData.v2Conversations,
                selectedModeId = "build",
            )
        }
    }

    @Test
    fun settingsFree() {
        snap("04", ThemePresets.dark) {
            SettingsScreenContent(uiState = ScreenshotTestData.freeConnected)
        }
    }

    @Test
    fun settingsTools() {
        snap("05", ThemePresets.light) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsTools,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsAgent() {
        snap("06", ThemePresets.dark) {
            SettingsScreenContent(uiState = ScreenshotTestData.settingsAgent)
        }
    }

    @Test
    fun settingsSandbox() {
        snap("07", ThemePresets.dark) {
            DoraiWorkspaceScreenSnapshot(
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }
}
