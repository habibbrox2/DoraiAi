@file:OptIn(ExperimentalMaterial3Api::class)

package com.dorai.ai

import androidx.compose.foundation.background
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.dorai.ai.data.AppSettings
import com.dorai.ai.tools.AppPermission
import com.dorai.ai.tools.PermissionController
import com.dorai.ai.tools.SetupPermissionHandler
import com.dorai.ai.theme.engine.DoraiTheme
import com.dorai.ai.ui.chat.ChatScreen
import com.dorai.ai.ui.chat.ChatViewModel
import com.dorai.ai.ui.components.FullScreenImageHost
import com.dorai.ai.ui.dashboard.DashboardScreen
import com.dorai.ai.ui.rememberSandboxAwareUriHandler
import com.dorai.ai.ui.settings.SettingsScreen
import com.dorai.ai.ui.settings.SettingsTab
import com.dorai.ai.ui.workspace.WorkspaceScreen
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.experimental.ExperimentalVoiceApi
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

@Serializable
@SerialName("dashboard")
object Dashboard

@Serializable
@SerialName("home")
object Home

/**
 * Settings destination.
 *
 * The tab travels as its [SettingsTab] *name* instead of the enum itself: typed routes only
 * support a fixed set of NavTypes, and an enum NavType exists on Android only. On desktop, iOS
 * and wasm the enum resolves to `UNKNOWN` and the app crashes while the route pattern is built
 * ("Route settings could not find any NavType for argument tab of type SettingsTab?").
 */
@Serializable
@SerialName("settings")
data class Settings(val tab: String? = null) {
    companion object {
        /** Route to the given settings [tab], or to the default tab when [tab] is null. */
        fun of(tab: SettingsTab?): Settings = Settings(tab?.name)

        /** The settings tab this route points at, or null when it points at the default tab. */
        fun tabOf(route: Settings): SettingsTab? = route.tab?.let { value -> SettingsTab.entries.find { it.name == value } }
    }
}

/** Workspace as a real destination (Plan 6), resumed into [project] when set. */
@Serializable
@SerialName("build")
data class Build(val project: String = "")

/**
 * Shared app entry.
 *
 * [lightColorScheme] / [darkColorScheme] are accepted for source compatibility with existing
 * call sites but are no longer applied at the root — the 2027 premium theme engine
 * ([DoraiTheme]) resolves the active palette from the persisted preset/custom selection.
 */
@Composable
fun App(
    navController: NavHostController,
    lightColorScheme: ColorScheme = com.dorai.ai.ui.LightColorScheme,
    darkColorScheme: ColorScheme = com.dorai.ai.ui.DarkColorScheme,
    textToSpeech: TextToSpeechInstance? = null,
    isKoinStarted: Boolean = false,
    onAppOpens: ((Int) -> Unit)? = null,
    androidUpdateContent: (@Composable () -> Unit)? = null,
) {
    setSingletonImageLoaderFactory { context: PlatformContext ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
                add(SvgDecoder.Factory())
            }
            .build()
    }

    // Reuse global Koin if already started (Android Application class),
    // otherwise create a new instance (iOS, Desktop, Wasm).
    if (isKoinStarted) {
        AppContent(navController, textToSpeech, onAppOpens, androidUpdateContent)
    } else {
        KoinApplication(
            configuration = koinConfiguration {
                modules(appModule)
            },
        ) {
            AppContent(navController, textToSpeech, onAppOpens, androidUpdateContent)
        }
    }
}

@Composable
private fun AppContent(
    navController: NavHostController,
    textToSpeech: TextToSpeechInstance?,
    onAppOpens: ((Int) -> Unit)?,
    androidUpdateContent: (@Composable () -> Unit)?,
) {
    val appSettings = koinInject<AppSettings>()

    // Track app opens after Koin is initialized
    onAppOpens?.let { callback ->
        LaunchedEffect(Unit) {
            callback(appSettings.trackAppOpen())
        }
    }

    // Set up permission handlers
    AppPermission.entries.forEach { permission ->
        SetupPermissionHandler(koinInject<PermissionController>(permissionQualifier(permission)))
    }

    // Keep the voice the engine already defaults to — that is the one the user picked in the
    // system speech settings. Only pick another voice when the default one cannot speak the
    // system language.
    @OptIn(ExperimentalVoiceApi::class)
    LaunchedEffect(textToSpeech) {
        val tts = textToSpeech ?: return@LaunchedEffect
        val systemLanguage = Locale.current.language
        if (tts.language.startsWith(systemLanguage)) return@LaunchedEffect
        val matchingVoice = tts.voices
            .firstOrNull { it.languageTag.startsWith(systemLanguage) }
        if (matchingVoice != null) {
            tts.currentVoice = matchingVoice
        }
    }

    val uiScale by appSettings.uiScaleFlow.collectAsStateWithLifecycle()
    val defaultDensity = LocalDensity.current
    val scaledDensity = remember(defaultDensity, uiScale) {
        Density(defaultDensity.density * uiScale, defaultDensity.fontScale)
    }

    val appLanguage by appSettings.appLanguageFlow.collectAsStateWithLifecycle()

    LaunchedEffect(appLanguage) {
        try {
            applyAppLanguage(appLanguage.code)
        } catch (_: Exception) {
            // best-effort; ignore on platforms without Locale support
        }
    }

    val sandboxController = koinInject<SandboxController>()
    val sandboxAwareUriHandler = rememberSandboxAwareUriHandler(sandboxController)

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalUriHandler provides sandboxAwareUriHandler,
    ) {
        DoraiTheme {
            FullScreenImageHost {
                val chatViewModel: ChatViewModel = koinViewModel()
                val chatUiState by chatViewModel.state.collectAsStateWithLifecycle()

                NavHost(
                    navController,
                    startDestination = Home,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                ) {
                    composable<Dashboard> {
                        PlatformBackHandler(true) {
                            navController.navigate(Home) {
                                popUpTo(Home)
                                launchSingleTop = true
                            }
                        }
                        DashboardScreen(
                            chatUiState = chatUiState,
                            isBuildAvailable = currentPlatform is Platform.Mobile.Android,
                            isSandboxAvailable = currentPlatform is Platform.Mobile.Android,
                            isDesignAvailable = true,
                            onNavigateToChat = {
                                navController.navigate(Home) {
                                    popUpTo(Home)
                                    launchSingleTop = true
                                }
                            },
                            onOpenBuild = { project ->
                                navController.navigate(Build(project)) {
                                    popUpTo(Home)
                                    launchSingleTop = true
                                }
                            },
                            onOpenSettings = { tab ->
                                navController.navigate(Settings.of(tab)) {
                                    popUpTo(Home)
                                    launchSingleTop = true
                                }
                            },
                            onStartDesign = { prompt ->
                                chatViewModel.startDesignChat(prompt)
                                navController.navigate(Home) {
                                    popUpTo(Home)
                                    launchSingleTop = true
                                }
                            },
                            onOpenConversation = { id ->
                                chatViewModel.loadConversation(id)
                                navController.navigate(Home) {
                                    popUpTo(Home)
                                    launchSingleTop = true
                                }
                            },
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                        )
                    }
                    composable<Home> {
                        ChatScreen(
                            viewModel = chatViewModel,
                            textToSpeech = textToSpeech,
                            onNavigateToDashboard = {
                                navController.navigate(Dashboard) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToSettings = {
                                navController.navigate(Settings()) {
                                    launchSingleTop = true
                                }
                            },
                            isSandboxAvailable = currentPlatform is Platform.Mobile.Android,
                            isWorkspaceAvailable = currentPlatform is Platform.Mobile.Android,
                        )
                    }
                    composable<Settings> { entry ->
                        PlatformBackHandler(true) {
                            chatViewModel.refreshSettings()
                            navController.navigateUp()
                        }
                        val initialTab = Settings.tabOf(entry.toRoute<Settings>())
                        DisposableEffect(Unit) {
                            onDispose {
                                chatViewModel.refreshSettings()
                            }
                        }
                        SettingsScreen(
                            initialSettingsTab = initialTab,
                            onNavigateBack = {
                                chatViewModel.refreshSettings()
                                navController.navigateUp()
                            },
                            androidUpdateContent = androidUpdateContent,
                        )
                    }
                    composable<Build> { entry ->
                        PlatformBackHandler(true) {
                            navController.navigateUp()
                        }
                        val project = entry.toRoute<Build>().project
                        WorkspaceScreen(
                            initialProject = project,
                            onExit = {
                                navController.navigateUp()
                            },
                        )
                    }
                }
            }
        }
    }
}
