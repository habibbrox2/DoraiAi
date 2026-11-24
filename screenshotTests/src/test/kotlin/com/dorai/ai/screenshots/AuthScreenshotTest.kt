package com.dorai.ai.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.dorai.ai.data.AppSettings
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.engine.DoraiTheme
import com.dorai.ai.ui.settings.AuthViewModel
import com.dorai.ai.ui.settings.LoginScreen
import com.dorai.ai.ui.settings.ProfileScreen
import com.russhwolf.settings.MapSettings
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/** Golden coverage for the optional auth screens in both supported UI languages. */
class AuthScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_9A.copy(softButtons = false))

    private lateinit var originalLocale: Locale

    @Before
    @OptIn(ExperimentalResourceApi::class)
    fun setup() {
        originalLocale = Locale.getDefault()
        setResourceReaderAndroidContext(paparazzi.context)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    private fun useLocale(tag: String) {
        Locale.setDefault(Locale(tag))
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_9A.copy(softButtons = false, locale = tag),
        )
    }

    private fun snapshot(name: String, signedIn: Boolean, displayName: String = "") {
        paparazzi.snapshot(name = name) {
            WithScreenshotTestKoin {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    DoraiTheme(repository = screenshotThemeRepository(ThemePresets.ogShine)) {
                        val settings = remember { AppSettings(MapSettings()) }
                        val viewModel = remember {
                            if (signedIn) {
                                settings.setUserLoggedIn(true)
                                settings.setUserEmail("user@example.com")
                                settings.setUserDisplayName(displayName)
                            }
                            AuthViewModel(settings)
                        }
                        Box(Modifier.fillMaxSize()) {
                            if (signedIn) {
                                ProfileScreen(viewModel, onSignOut = {})
                            } else {
                                LoginScreen(viewModel, onLoginSuccess = {}, onNavigateToProfile = {})
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun loginScreenEnglish() = snapshot(name = "auth_login_en", signedIn = false)

    @Test
    fun loginScreenBengali() {
        useLocale("bn")
        snapshot(name = "auth_login_bn", signedIn = false)
    }

    @Test
    fun profileScreenBengali() {
        useLocale("bn")
        snapshot(name = "auth_profile_bn", signedIn = true, displayName = "আশা রহমান")
    }
}
