package com.dorai.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.dorai.ai.data.AppSettings
import com.dorai.ai.data.DataRepository
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.dorai.ai.theme.data.resolve
import com.dorai.ai.updater.AndroidUpdateAutoUpdate
import com.dorai.ai.updater.AndroidUpdateSettingsSection
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import nl.marc_apps.tts.TextToSpeechEngine
import nl.marc_apps.tts.TextToSpeechFactory
import nl.marc_apps.tts.TextToSpeechInstance
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val languageCode = newBase.applicationContext
            .getSharedPreferences("dorai_locale", Context.MODE_PRIVATE)
            .getString("app_language", null)
        val wrapped = if (languageCode != null) {
            // Cold start in the app-selected language. Compose Resources resolves against the
            // context configuration, so wrapping with createConfigurationContext is enough for
            // the whole UI (attachBaseContext runs before any inflation/composition).
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(java.util.Locale.forLanguageTag(languageCode))
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DoraiLocaleActivityHolder.activity = this
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        handleDeepLinkIntent(intent)

        val themeSettingsRepository: ThemeSettingsRepository = get()
        val appSettings: AppSettings = get()
        if (BuildConfig.FLAVOR == "foss") {
            AndroidUpdateAutoUpdate.start(this, appSettings)
        }
        setContent {
            val themeState by themeSettingsRepository.state.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = themeState.resolve(systemInDark).isDark
            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                )
            }
            val navController = rememberNavController()
            // Defer TTS initialization until after the first frame
            var ttsReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { ttsReady = true }
            val textToSpeech = if (ttsReady) {
                rememberSystemTextToSpeechOrNull()
            } else {
                null
            }
            App(
                navController = navController,
                textToSpeech = textToSpeech,
                isKoinStarted = true,
                onAppOpens = { appOpens ->
                    if (appOpens % 5 == 0) {
                        requestReview(this@MainActivity)
                    }
                },
                androidUpdateContent = if (BuildConfig.FLAVOR == "foss") {
                    { AndroidUpdateSettingsSection(appSettings = appSettings) }
                } else {
                    null
                },
            )
        }
    }

    override fun onDestroy() {
        if (DoraiLocaleActivityHolder.activity === this) {
            DoraiLocaleActivityHolder.activity = null
        }
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        // Re-assert the daemon every time the activity is brought to the foreground.
        // `onCreate`-only is not enough: aggressive OEM battery managers (MIUI,
        // EMUI/Huawei) sometimes kill the foreground service while the activity
        // is still alive in the background — without this, the user has to fully
        // close and reopen the app for scheduling to resume. `startForegroundService`
        // is idempotent when the service is already up.
        autoStartDaemon()
    }

    private fun autoStartDaemon() {
        val daemonController: DaemonController = get()
        if (daemonController is AndroidDaemonController && daemonController.shouldAutoStart()) {
            daemonController.start()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_HEARTBEAT, false) == true) {
            val dataRepository: DataRepository = get()
            dataRepository.requestOpenHeartbeat()
            // Drop the extra so a configuration change (screen rotation) doesn't re-trigger
            // the deep-link after ChatViewModel has already consumed it.
            intent.removeExtra(EXTRA_OPEN_HEARTBEAT)
        }
        if (intent?.action == Intent.ACTION_ASSIST) {
            val dataRepository: DataRepository = get()
            dataRepository.requestOpenAssist()
            // Clear the action so a configuration change doesn't re-trigger a fresh
            // chat after ChatViewModel has already consumed the request.
            intent.action = null
        }
        if (intent?.action == Intent.ACTION_SEND) {
            val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
                ?.toString()
                ?.trim()
                .orEmpty()
            if (sharedText.isNotEmpty()) {
                val dataRepository: DataRepository = get()
                dataRepository.requestOpenShare(sharedText)
            }
            // Drop the action and extra so rotation doesn't re-apply the share after
            // ChatViewModel has already consumed the request.
            intent.action = null
            intent.removeExtra(Intent.EXTRA_TEXT)
        }
    }
}

/**
 * Speaks with the engine the user selected in the system text-to-speech settings, so a
 * third-party engine (Sherpa, RHVoice, eSpeak, ...) is honoured instead of being overridden.
 * Google's engine is only used when the system default fails to initialise, which happens on
 * devices where no default engine is configured.
 */
@Composable
private fun rememberSystemTextToSpeechOrNull(): TextToSpeechInstance? {
    val context = LocalContext.current.applicationContext
    var textToSpeech by remember { mutableStateOf<TextToSpeechInstance?>(null) }

    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeechFactory(context, TextToSpeechEngine.SystemDefault).createOrNull()
            ?: TextToSpeechFactory(context, TextToSpeechEngine.Google).createOrNull()
    }

    DisposableEffect(Unit) {
        onDispose {
            textToSpeech?.close()
            textToSpeech = null
        }
    }

    return textToSpeech
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(navController = rememberNavController())
}
