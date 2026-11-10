package com.dorai.ai.screenshots

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.dorai.ai.data.Service
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.engine.DoraiTheme
import com.dorai.ai.ui.adaptive.DoraiAdaptiveScaffold
import com.dorai.ai.ui.adaptive.DoraiDestination
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.adaptive.calculateDoraiWindowWidthClass
import com.dorai.ai.ui.chat.DoraiChatScreen
import com.dorai.ai.ui.components.v2.DoraiComponentsPreviewContent
import com.dorai.ai.ui.home.DoraiHomeScreen
import com.dorai.ai.ui.home.HomeChatSummary
import com.dorai.ai.ui.home.HomeProjectSummary
import com.dorai.ai.ui.home.HomeUiState
import com.dorai.ai.ui.settings.ConfiguredServiceEntry
import com.dorai.ai.ui.settings.ConnectionStatus
import com.dorai.ai.ui.settings.DoraiSettingsScreen
import com.dorai.ai.ui.settings.SettingsActions
import com.dorai.ai.ui.settings.SettingsCategory
import com.dorai.ai.ui.settings.SettingsModel
import com.dorai.ai.ui.settings.SettingsTab
import com.dorai.ai.ui.settings.SettingsUiState
import com.dorai.ai.ui.workspace.DoraiWorkspaceScreen
import com.dorai.ai.ui.workspace.ProjectFile
import com.dorai.ai.ui.workspace.WorkspaceProjectItem
import com.dorai.ai.ui.workspace.WorkspaceProjectsExplorer
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi screenshot test suite for all DORAI AI UI v2 screens and components.
 * Captures both phone (Compact, Pixel 9a) and tablet (Expanded/Large, Pixel Tablet)
 * layouts using the prototype's signature OG Shine theme and full adaptive scaffold.
 */
@OptIn(ExperimentalResourceApi::class)
class DoraiUiV2ScreenshotTest {

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

    // -------------------------------------------------------------------------
    // 1. Home / Dashboard Screen
    // -------------------------------------------------------------------------

    @Test
    fun home_compact_ogshine() = snap(
        destination = DoraiDestination.Home,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiHomeScreen(
            state = sampleHomeState(),
            onNavigateToChat = {},
            onNavigateToWorkspace = {},
            onOpenConversation = {},
            onOpenModels = {},
        )
    }

    @Test
    fun home_compact_light() = snap(
        destination = DoraiDestination.Home,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.light,
    ) {
        DoraiHomeScreen(
            state = sampleHomeState(),
            onNavigateToChat = {},
            onNavigateToWorkspace = {},
            onOpenConversation = {},
            onOpenModels = {},
        )
    }

    @Test
    fun home_tablet_ogshine() = snap(
        destination = DoraiDestination.Home,
        device = DeviceConfig.PIXEL_TABLET,
        palette = ThemePresets.ogShine,
    ) {
        DoraiHomeScreen(
            state = sampleHomeState(),
            onNavigateToChat = {},
            onNavigateToWorkspace = {},
            onOpenConversation = {},
            onOpenModels = {},
        )
    }

    // -------------------------------------------------------------------------
    // 2. Chat Screen
    // -------------------------------------------------------------------------

    @Test
    fun chat_compact_empty_state_ogshine() = snap(
        destination = DoraiDestination.Chat,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiChatScreen(
            messages = emptyList(),
            conversations = ScreenshotTestData.v2Conversations,
            currentModelName = "Google Gemini",
            isModelLocal = false,
            selectedModeId = "ask",
            onModeSelected = {},
            onSendMessage = {},
            onNewChat = {},
            onOpenConversation = {},
        )
    }

    @Test
    fun chat_compact_conversation_ogshine() = snap(
        destination = DoraiDestination.Chat,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiChatScreen(
            messages = ScreenshotTestData.v2Messages,
            conversations = ScreenshotTestData.v2Conversations,
            currentModelName = "Google Gemini",
            isModelLocal = false,
            selectedModeId = "build",
            onModeSelected = {},
            onSendMessage = {},
            onNewChat = {},
            onOpenConversation = {},
        )
    }

    @Test
    fun chat_compact_conversation_fontscale_14() = snap(
        destination = DoraiDestination.Chat,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
        fontScale = 1.4f,
    ) {
        DoraiChatScreen(
            messages = ScreenshotTestData.v2Messages,
            conversations = ScreenshotTestData.v2Conversations,
            currentModelName = "Google Gemini",
            isModelLocal = false,
            selectedModeId = "build",
            onModeSelected = {},
            onSendMessage = {},
            onNewChat = {},
            onOpenConversation = {},
        )
    }

    @Test
    fun chat_tablet_conversation_ogshine() = snap(
        destination = DoraiDestination.Chat,
        device = DeviceConfig.PIXEL_TABLET,
        palette = ThemePresets.ogShine,
    ) {
        DoraiChatScreen(
            messages = ScreenshotTestData.v2Messages,
            conversations = ScreenshotTestData.v2Conversations,
            currentModelName = "Google Gemini",
            isModelLocal = false,
            selectedModeId = "build",
            onModeSelected = {},
            onSendMessage = {},
            onNewChat = {},
            onOpenConversation = {},
        )
    }

    // -------------------------------------------------------------------------
    // 3. Workspace Screen
    // -------------------------------------------------------------------------

    @Test
    fun workspace_setup_compact_ogshine() = snap(
        destination = DoraiDestination.Workspace,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiWorkspaceScreen(
            isSetupCompleted = false,
            activeProject = null,
            projects = emptyList(),
            isSandboxRunning = false,
            onStartSandbox = {},
            onCompleteSetup = {},
            onOpenProject = {},
            onCloseProject = {},
        )
    }

    @Test
    fun workspace_projects_compact_ogshine() = snap(
        destination = DoraiDestination.Workspace,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiWorkspaceScreen(
            isSetupCompleted = true,
            activeProject = null,
            projects = sampleProjects(),
            isSandboxRunning = true,
            onStartSandbox = {},
            onCompleteSetup = {},
            onOpenProject = {},
            onCloseProject = {},
        )
    }

    @Test
    fun workspace_active_project_compact_ogshine() = snap(
        destination = DoraiDestination.Workspace,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
        waitingApprovalsCount = 2,
    ) {
        DoraiWorkspaceScreen(
            isSetupCompleted = true,
            activeProject = sampleProjects().first(),
            projects = sampleProjects(),
            isSandboxRunning = true,
            onStartSandbox = {},
            onCompleteSetup = {},
            onOpenProject = {},
            onCloseProject = {},
            fileExplorerContent = { _ -> SampleExplorerContent() },
        )
    }

    @Test
    fun workspace_active_project_tablet_ogshine() = snap(
        destination = DoraiDestination.Workspace,
        device = DeviceConfig.PIXEL_TABLET,
        palette = ThemePresets.ogShine,
        waitingApprovalsCount = 2,
    ) {
        DoraiWorkspaceScreen(
            isSetupCompleted = true,
            activeProject = sampleProjects().first(),
            projects = sampleProjects(),
            isSandboxRunning = true,
            onStartSandbox = {},
            onCompleteSetup = {},
            onOpenProject = {},
            onCloseProject = {},
            fileExplorerContent = { _ -> SampleExplorerContent() },
        )
    }

    // -------------------------------------------------------------------------
    // 4. Settings Screen (All 9 Categories on Compact Phone + Tablet)
    // -------------------------------------------------------------------------

    @Test
    fun settings_general_compact_ogshine() = snap(
        destination = DoraiDestination.Settings,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiSettingsScreen(
            activeCategory = SettingsCategory.General,
            onCategoryChanged = {},
            selectedThemeId = "ogshine",
            onThemeSelected = {},
            currentThemeStyle = ThemeStyle.Flat,
            onThemeStyleSelected = {},
        )
    }

    @Test
    fun settings_appearance_compact_ogshine() = snap(
        destination = DoraiDestination.Settings,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiSettingsScreen(
            activeCategory = SettingsCategory.Appearance,
            onCategoryChanged = {},
            selectedThemeId = "ogshine",
            onThemeSelected = {},
            currentThemeStyle = ThemeStyle.Flat,
            onThemeStyleSelected = {},
        )
    }

    @Test
    fun settings_agent_compact_ogshine() = snap(
        destination = DoraiDestination.Settings,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiSettingsScreen(
            activeCategory = SettingsCategory.Agent,
            onCategoryChanged = {},
            selectedThemeId = "ogshine",
            onThemeSelected = {},
            currentThemeStyle = ThemeStyle.Flat,
            onThemeStyleSelected = {},
        )
    }

    @Test
    fun settings_providers_custom_gateway_expanded_ogshine() = snap(
        destination = DoraiDestination.Settings,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiSettingsScreen(
            activeCategory = SettingsCategory.Providers,
            onCategoryChanged = {},
            selectedThemeId = "ogshine",
            onThemeSelected = {},
            currentThemeStyle = ThemeStyle.Flat,
            onThemeStyleSelected = {},
            uiState = customGatewayState(),
            actions = SettingsActions.NoOp,
        )
    }

    @Test
    fun settings_providers_compact_ogshine() = snap(
        destination = DoraiDestination.Settings,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiSettingsScreen(
            activeCategory = SettingsCategory.Providers,
            onCategoryChanged = {},
            selectedThemeId = "ogshine",
            onThemeSelected = {},
            currentThemeStyle = ThemeStyle.Flat,
            onThemeStyleSelected = {},
        )
    }

    @Test
    fun settings_tools_compact_ogshine() = snap(
        destination = DoraiDestination.Settings,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiSettingsScreen(
            activeCategory = SettingsCategory.Tools,
            onCategoryChanged = {},
            selectedThemeId = "ogshine",
            onThemeSelected = {},
            currentThemeStyle = ThemeStyle.Flat,
            onThemeStyleSelected = {},
        )
    }

    @Test
    fun settings_mcp_compact_ogshine() = snap(
        destination = DoraiDestination.Settings,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiSettingsScreen(
            activeCategory = SettingsCategory.Mcp,
            onCategoryChanged = {},
            selectedThemeId = "ogshine",
            onThemeSelected = {},
            currentThemeStyle = ThemeStyle.Flat,
            onThemeStyleSelected = {},
        )
    }

    @Test
    fun settings_account_compact_ogshine() = snap(
        destination = DoraiDestination.Settings,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiSettingsScreen(
            activeCategory = SettingsCategory.Account,
            onCategoryChanged = {},
            selectedThemeId = "ogshine",
            onThemeSelected = {},
            currentThemeStyle = ThemeStyle.Flat,
            onThemeStyleSelected = {},
        )
    }

    @Test
    fun settings_tablet_ogshine() = snap(
        destination = DoraiDestination.Settings,
        device = DeviceConfig.PIXEL_TABLET,
        palette = ThemePresets.ogShine,
    ) {
        DoraiSettingsScreen(
            activeCategory = SettingsCategory.General,
            onCategoryChanged = {},
            selectedThemeId = "ogshine",
            onThemeSelected = {},
            currentThemeStyle = ThemeStyle.Flat,
            onThemeStyleSelected = {},
        )
    }

    // -------------------------------------------------------------------------
    // 5. Shared Components Showcase
    // -------------------------------------------------------------------------

    @Test
    fun components_showcase_ogshine() = snap(
        destination = null,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.ogShine,
    ) {
        DoraiComponentsPreviewContent()
    }

    @Test
    fun components_showcase_light() = snap(
        destination = null,
        device = DeviceConfig.PIXEL_9A,
        palette = ThemePresets.light,
    ) {
        DoraiComponentsPreviewContent()
    }

    // -------------------------------------------------------------------------
    // Helper snapshot renderer
    // -------------------------------------------------------------------------

    private fun snap(
        destination: DoraiDestination? = null,
        device: DeviceConfig = DeviceConfig.PIXEL_9A,
        palette: ThemePalette = ThemePresets.ogShine,
        waitingApprovalsCount: Int = 2,
        fontScale: Float = 1.0f,
        content: @Composable () -> Unit,
    ) {
        val theme = if (palette.isDark) {
            "android:Theme.Material.NoActionBar"
        } else {
            "android:Theme.Material.Light.NoActionBar"
        }

        paparazzi.unsafeUpdateConfig(
            deviceConfig = device.copy(softButtons = false),
            theme = theme,
        )

        paparazzi.snapshot {
            WithScreenshotTestKoin {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    DoraiTheme(repository = screenshotThemeRepository(palette)) {
                        // Optional font-scale override (font-scale stress tests) without touching device density.
                        val baseDensity = LocalDensity.current
                        CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale)) {
                            if (destination != null) {
                                DoraiAdaptiveScaffold(
                                    selectedDestination = destination,
                                    onDestinationSelected = {},
                                    waitingApprovalsCount = waitingApprovalsCount,
                                ) {
                                    content()
                                }
                            } else {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val widthClass = calculateDoraiWindowWidthClass(maxWidth)
                                    CompositionLocalProvider(LocalDoraiWindowWidthClass provides widthClass) {
                                        content()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Sample Test Data
    // -------------------------------------------------------------------------

    private fun sampleHomeState() = HomeUiState(
        isWorkspaceSetupDone = true,
        activeProject = HomeProjectSummary(
            name = "dorai-notes",
            branch = "main",
            changesCount = 3,
        ),
        runningCount = 1,
        waitingApprovalCount = 2,
        currentModelName = "Google Gemini",
        isModelLocal = false,
        isSandboxRunning = true,
        storageUsageText = "1.2 / 4 GB",
        recentChats = listOf(
            HomeChatSummary(
                id = "1",
                title = "ডার্ক-মোড সুইচ",
                snippet = "সেটিংস স্ক্রিনে ডার্ক-মোড সুইচ যোগ করো।",
            ),
            HomeChatSummary(
                id = "2",
                title = "কোন ডেটাবেস নেব",
                snippet = "লোকাল স্টোরেজে কোন ডেটাবেস ব্যবহার করব?",
            ),
            HomeChatSummary(
                id = "3",
                title = "ব্যর্থ টেস্ট ঠিক করা",
                snippet = "টেস্ট ফেইল করছে, দেখো তো।",
            ),
        ),
    )

    private fun customGatewayState() = SettingsUiState(
        currentTab = SettingsTab.Services,
        configuredServices = persistentListOf(
            ConfiguredServiceEntry(
                instanceId = "custom-gateway",
                service = Service.OpenAICompatible,
                connectionStatus = ConnectionStatus.Connected,
                apiKey = "sk-••••••••••••••••••••••",
                baseUrl = "https://litellm.example.com/v1",
                selectedModel = SettingsModel(id = "gpt-4o-mini", subtitle = "gpt-4o-mini", displayName = "gpt-4o-mini", isSelected = true),
                models = persistentListOf(
                    SettingsModel(id = "gpt-4o-mini", subtitle = "gpt-4o-mini"),
                    SettingsModel(id = "gpt-4o", subtitle = "gpt-4o"),
                ),
                customName = "My LiteLLM proxy",
            ),
        ),
        expandedServiceId = "custom-gateway",
    )

    @Composable
    private fun SampleExplorerContent() {
        WorkspaceProjectsExplorer(
            currentProject = "dorai-notes",
            projects = sampleProjects().map { it.name },
            files = listOf(
                ProjectFile(path = "/root/projects/dorai-notes", name = "dorai-notes", isDirectory = true, gitStatus = null),
                ProjectFile(path = "/root/projects/dorai-notes/src", name = "src", isDirectory = true, gitStatus = com.dorai.ai.workspace.WorkspaceGitStatus.Untracked),
                ProjectFile(path = "/root/projects/dorai-notes/src/Main.kt", name = "Main.kt", isDirectory = false, gitStatus = com.dorai.ai.workspace.WorkspaceGitStatus.Modified),
                ProjectFile(path = "/root/projects/dorai-notes/README.md", name = "README.md", isDirectory = false, gitStatus = null),
            ),
            onOpenProject = {},
            onCreateProject = {},
            onOpenFile = {},
            onRefresh = {},
        )
    }

    private fun sampleProjects() = listOf(
        WorkspaceProjectItem(
            name = "dorai-notes",
            path = "~/projects/dorai-notes",
            branch = "main",
            lastUpdatedText = "২ ঘণ্টা আগে",
            changesCount = 3,
        ),
        WorkspaceProjectItem(
            name = "weather-app",
            path = "~/projects/weather-app",
            branch = "dev",
            lastUpdatedText = "গতকাল",
            changesCount = 0,
        ),
        WorkspaceProjectItem(
            name = "landing-page",
            path = "~/projects/landing-page",
            branch = "main",
            lastUpdatedText = "গত সপ্তাহ",
            changesCount = 0,
        ),
    )
}
