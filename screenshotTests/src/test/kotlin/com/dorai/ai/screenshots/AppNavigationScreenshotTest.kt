package com.dorai.ai.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.dorai.ai.Build
import com.dorai.ai.Dashboard
import com.dorai.ai.Home
import com.dorai.ai.Settings
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.engine.DoraiTheme
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.ui.dashboard.DashboardScreenContent
import com.dorai.ai.ui.dashboard.DashboardUiState
import com.dorai.ai.ui.dashboard.FactText
import com.dorai.ai.ui.dashboard.ReadinessFact
import com.dorai.ai.ui.dashboard.ServiceReadiness
import com.dorai.ai.ui.settings.SettingsScreenContent
import com.dorai.ai.ui.settings.SettingsTab
import com.dorai.ai.ui.workspace.WorkspaceActions
import com.dorai.ai.ui.workspace.WorkspaceScreenContent
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalResourceApi::class)
class AppNavigationScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9A.copy(softButtons = false),
        showSystemUi = true,
        maxPercentDifference = 0.1,
    )

    @Before
    fun setup() {
        setResourceReaderAndroidContext(paparazzi.context)
    }

    @Test
    fun dashboard_destination_dark() = paparazzi.snapDestination(Dashboard)

    @Test
    fun chat_destination_dark() = paparazzi.snapDestination(Home)

    @Test
    fun settings_destination_dark() = paparazzi.snapDestination(Settings())

    @Test
    fun settings_tools_destination_dark() = paparazzi.snapDestination(Settings.of(SettingsTab.Tools))

    @Test
    fun workspace_destination_dark() = paparazzi.snapDestination(Build("brox-kai-2.0"))

    private fun Paparazzi.snapDestination(startDestination: Any) {
        unsafeUpdateConfig(theme = "android:Theme.Material.NoActionBar")
        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        try {
            snapshot {
                CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        WithScreenshotTestKoin {
                            CompositionLocalProvider(LocalInspectionMode provides true) {
                                DoraiTheme(repository = screenshotThemeRepository(ThemePresets.dark)) {
                                    AppNavigationGraph(startDestination)
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            viewModelStoreOwner.viewModelStore.clear()
        }
    }
}

/** Uses the production serializable destinations and the same destination content contracts as App.kt. */
@Composable
private fun AppNavigationGraph(startDestination: Any) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable<Dashboard> {
            DashboardScreenContent(
                state = DashboardUiState(
                    service = ServiceReadiness(
                        serviceName = "OpenAI",
                        modelId = "GPT-6",
                        isSharedKey = false,
                    ),
                    facts = kotlinx.collections.immutable.persistentListOf(
                        ReadinessFact(
                            FactText.Raw("Workspace"),
                            FactText.Raw("Ready"),
                            OperationalStatus.Ready,
                        ),
                        ReadinessFact(
                            FactText.Raw("Sandbox"),
                            FactText.Raw("Ready"),
                            OperationalStatus.Ready,
                        ),
                    ),
                    recentProjects = kotlinx.collections.immutable.persistentListOf("brox-kai-2.0"),
                    isBuildAvailable = true,
                    isSandboxAvailable = true,
                    isDesignAvailable = true,
                ),
                onNavigateToChat = { navController.navigate(Home) },
                onOpenBuild = { project -> navController.navigate(Build(project)) },
                onOpenSettings = { tab -> navController.navigate(Settings.of(tab)) },
                onStartDesign = { navController.navigate(Home) },
                onOpenConversation = { navController.navigate(Home) },
            )
        }
        composable<Home> {
            DoraiChatScreenV2(
                messages = ScreenshotTestData.v2Messages,
                conversations = ScreenshotTestData.v2Conversations,
                selectedModeId = "build",
                onOpenConversation = { navController.navigate(Home) },
                onNavigateToSettings = { navController.navigate(Settings()) },
            )
        }
        composable<Settings> { entry ->
            val route = entry.toRoute<Settings>()
            val selectedTab = Settings.tabOf(route) ?: SettingsTab.General
            SettingsScreenContent(
                uiState = when (selectedTab) {
                    SettingsTab.Agent -> ScreenshotTestData.settingsAgent

                    SettingsTab.Appearance -> ScreenshotTestData.settingsAppearanceState

                    SettingsTab.Services -> ScreenshotTestData.freeConnected

                    SettingsTab.Tools -> ScreenshotTestData.settingsTools

                    // Not yet snapshotted — stand-in until a GitHub fixture is recorded.
                    SettingsTab.GitHub -> ScreenshotTestData.settingsTools

                    SettingsTab.Account -> ScreenshotTestData.settingsAccountState

                    SettingsTab.General -> ScreenshotTestData.settingsGeneralState
                },
                sandboxState = ScreenshotTestData.sandboxState,
                onNavigateBack = { navController.navigateUp() },
            )
        }
        composable<Build> {
            WorkspaceScreenContent(
                state = ScreenshotTestData.buildStateWithActiveProject,
                actions = navigationScreenshotWorkspaceActions,
                onExit = { navController.navigateUp() },
            )
        }
    }
}

private val navigationScreenshotWorkspaceActions = WorkspaceActions(
    toggleAgent = {},
    install = {},
    installAgent = {},
    cancel = {},
    uninstall = {},
    setLaunchAgent = {},
    openProject = {},
    createProject = {},
    deleteProject = {},
    renameProject = { _, _ -> },
    closeProject = {},
    startSession = {},
    selectSession = {},
    closeSession = {},
    submitLine = {},
    sendKey = { _, _ -> },
    sendText = { _, _ -> },
    sendMouse = {},
    resizeTerminal = { _, _ -> },
    openFile = {},
)
