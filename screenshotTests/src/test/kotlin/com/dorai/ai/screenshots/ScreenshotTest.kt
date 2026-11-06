@file:OptIn(ExperimentalVoiceApi::class)

package com.dorai.ai.screenshots

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.dorai.ai.theme.ThemeViewModel
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.dorai.ai.theme.engine.DoraiTheme
import com.dorai.ai.theme.ui.DoraiAppearanceSettings
import com.dorai.ai.ui.chat.ChatScreenContent
import com.dorai.ai.ui.chat.composables.AgentActivityTimeline
import com.dorai.ai.ui.chat.composables.ChatComposer
import com.dorai.ai.ui.chat.composables.ShellExecutionCard
import com.dorai.ai.ui.chat.composables.ToolConfirmationCard
import com.dorai.ai.ui.chat.composables.ToolExecutionCard
import com.dorai.ai.ui.dynamicui.LocalPreviewImages
import com.dorai.ai.ui.settings.SettingsScreenContent
import com.dorai.ai.ui.workspace.WorkspaceActions
import com.dorai.ai.ui.workspace.WorkspaceScreenContent
import com.dorai.ai.ui.workspace.WorkspaceStatusKind
import com.dorai.ai.ui.workspace.WorkspaceStatusPill
import nl.marc_apps.tts.experimental.ExperimentalVoiceApi
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalResourceApi::class)
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9A.copy(softButtons = false),
        showSystemUi = true,
        maxPercentDifference = 0.1,
    )

    private val previewImages = mutableMapOf<String, ImageBitmap>()

    @Before
    fun setup() {
        setResourceReaderAndroidContext(paparazzi.context)
        loadPreviewImage("resource://orc_survival.png", "/orc_survival.png")
        loadPreviewImage("resource://cacio_e_pepe.png", "/cacio_e_pepe.png")
    }

    private val noOpBuildActions = WorkspaceActions(
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

    private fun loadPreviewImage(key: String, resourcePath: String) {
        val bitmap = BitmapFactory.decodeStream(javaClass.getResourceAsStream(resourcePath)) ?: return
        previewImages[key] = bitmap.asImageBitmap()
    }

    fun Paparazzi.snap(
        palette: ThemePalette,
        repository: ThemeSettingsRepository = screenshotThemeRepository(palette),
        content: @Composable () -> Unit,
    ) {
        val theme = if (palette.isDark) {
            "android:Theme.Material.NoActionBar"
        } else {
            "android:Theme.Material.Light.NoActionBar"
        }
        unsafeUpdateConfig(theme = theme)

        snapshot {
            Box(modifier = Modifier.fillMaxSize()) {
                WithScreenshotTestKoin {
                    CompositionLocalProvider(
                        LocalInspectionMode provides true,
                        LocalPreviewImages provides previewImages,
                    ) {
                        DoraiTheme(repository = repository) {
                            content()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun chatEmptyState_premiumDark() {
        paparazzi.snap(ThemePresets.dark) {
            ChatScreenContent(
                uiState = ScreenshotTestData.chatEmptyState,
                FakeTextToSpeechInstance(),
                isSandboxAvailable = true,
            )
        }
    }

    @Test
    fun chatWithMessages_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ChatScreenContent(
                uiState = ScreenshotTestData.chatWithMessages,
                FakeTextToSpeechInstance(),
            )
        }
    }

    @Test
    fun chatWithDynamicUi_light() {
        paparazzi.snap(ThemePresets.light) {
            ChatScreenContent(
                uiState = ScreenshotTestData.chatWithDynamicUi,
                FakeTextToSpeechInstance(),
            )
        }
    }

    @Test
    fun settingsAgent_dark() {
        paparazzi.snap(ThemePresets.dark) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsAgent,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsFree_dark() {
        paparazzi.snap(ThemePresets.dark) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.freeConnected,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsTools_light() {
        paparazzi.snap(ThemePresets.light) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsTools,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsSandbox_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ChatScreenContent(
                uiState = ScreenshotTestData.chatEmptyState,
                isSandboxAvailable = true,
                initialSandboxOpen = true,
                previewSandboxState = ScreenshotTestData.sandboxState,
                previewSandboxLines = ScreenshotTestData.sandboxTerminalLines,
            )
        }
    }

    // --- Phase C: Tool execution states ---

    @Test
    fun toolExecutionRunning_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ToolExecutionCard(
                toolName = ScreenshotTestData.toolExecutionRunningParams.toolName,
                arguments = ScreenshotTestData.toolExecutionRunningParams.arguments,
                result = ScreenshotTestData.toolExecutionRunningParams.result,
                isRunning = ScreenshotTestData.toolExecutionRunningParams.isRunning,
                isError = ScreenshotTestData.toolExecutionRunningParams.isError,
                initiallyExpanded = ScreenshotTestData.toolExecutionRunningParams.initiallyExpanded,
            )
        }
    }

    @Test
    fun toolExecutionCompleted_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ToolExecutionCard(
                toolName = ScreenshotTestData.toolExecutionCompletedParams.toolName,
                arguments = ScreenshotTestData.toolExecutionCompletedParams.arguments,
                result = ScreenshotTestData.toolExecutionCompletedParams.result,
                isRunning = ScreenshotTestData.toolExecutionCompletedParams.isRunning,
                isError = ScreenshotTestData.toolExecutionCompletedParams.isError,
                initiallyExpanded = ScreenshotTestData.toolExecutionCompletedParams.initiallyExpanded,
            )
        }
    }

    @Test
    fun toolExecutionFailed_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ToolExecutionCard(
                toolName = ScreenshotTestData.toolExecutionFailedParams.toolName,
                arguments = ScreenshotTestData.toolExecutionFailedParams.arguments,
                result = ScreenshotTestData.toolExecutionFailedParams.result,
                isRunning = ScreenshotTestData.toolExecutionFailedParams.isRunning,
                isError = ScreenshotTestData.toolExecutionFailedParams.isError,
                initiallyExpanded = ScreenshotTestData.toolExecutionFailedParams.initiallyExpanded,
            )
        }
    }

    @Test
    fun toolExecutionExpanded_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ToolExecutionCard(
                toolName = ScreenshotTestData.toolExecutionExpandedParams.toolName,
                arguments = ScreenshotTestData.toolExecutionExpandedParams.arguments,
                result = ScreenshotTestData.toolExecutionExpandedParams.result,
                isRunning = ScreenshotTestData.toolExecutionExpandedParams.isRunning,
                isError = ScreenshotTestData.toolExecutionExpandedParams.isError,
                initiallyExpanded = ScreenshotTestData.toolExecutionExpandedParams.initiallyExpanded,
            )
        }
    }

    @Test
    fun toolExecutionLongOutput_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ToolExecutionCard(
                toolName = ScreenshotTestData.toolExecutionLongOutputParams.toolName,
                arguments = ScreenshotTestData.toolExecutionLongOutputParams.arguments,
                result = ScreenshotTestData.toolExecutionLongOutputParams.result,
                isRunning = ScreenshotTestData.toolExecutionLongOutputParams.isRunning,
                isError = ScreenshotTestData.toolExecutionLongOutputParams.isError,
                initiallyExpanded = ScreenshotTestData.toolExecutionLongOutputParams.initiallyExpanded,
            )
        }
    }

    // --- Phase C: Tool approval states ---

    @Test
    fun toolApproval_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ToolConfirmationCard(
                toolName = ScreenshotTestData.toolApprovalParams.toolName,
                toolCallId = ScreenshotTestData.toolApprovalParams.toolCallId,
                arguments = ScreenshotTestData.toolApprovalParams.arguments,
                onDeny = {},
                onAllowOnce = {},
                onAllowSession = {},
                onAlwaysAllow = {},
            )
        }
    }

    // --- Phase C: Shell execution states ---

    @Test
    fun shellExecutionRunning_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ShellExecutionCard(
                command = ScreenshotTestData.shellExecutionRunningParams.command,
                workingDirectory = ScreenshotTestData.shellExecutionRunningParams.workingDirectory,
                stdout = ScreenshotTestData.shellExecutionRunningParams.stdout,
                stderr = ScreenshotTestData.shellExecutionRunningParams.stderr,
                exitCode = ScreenshotTestData.shellExecutionRunningParams.exitCode,
                isRunning = ScreenshotTestData.shellExecutionRunningParams.isRunning,
            )
        }
    }

    @Test
    fun shellExecutionCompleted_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ShellExecutionCard(
                command = ScreenshotTestData.shellExecutionCompletedParams.command,
                workingDirectory = ScreenshotTestData.shellExecutionCompletedParams.workingDirectory,
                stdout = ScreenshotTestData.shellExecutionCompletedParams.stdout,
                stderr = ScreenshotTestData.shellExecutionCompletedParams.stderr,
                exitCode = ScreenshotTestData.shellExecutionCompletedParams.exitCode,
                isRunning = ScreenshotTestData.shellExecutionCompletedParams.isRunning,
            )
        }
    }

    @Test
    fun shellExecutionFailed_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ShellExecutionCard(
                command = ScreenshotTestData.shellExecutionFailedParams.command,
                workingDirectory = ScreenshotTestData.shellExecutionFailedParams.workingDirectory,
                stdout = ScreenshotTestData.shellExecutionFailedParams.stdout,
                stderr = ScreenshotTestData.shellExecutionFailedParams.stderr,
                exitCode = ScreenshotTestData.shellExecutionFailedParams.exitCode,
                isRunning = ScreenshotTestData.shellExecutionFailedParams.isRunning,
            )
        }
    }

    // --- Phase C: Settings tab states ---

    @Test
    fun settingsGeneral_dark() {
        paparazzi.snap(ThemePresets.dark) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsGeneralState,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsAppearance_dark() {
        paparazzi.snap(ThemePresets.dark) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsAppearanceState,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsAccount_dark() {
        paparazzi.snap(ThemePresets.dark) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsAccountState,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun appearancePresets_dark() {
        val repository = screenshotThemeRepository(ThemePresets.dark)
        paparazzi.snap(ThemePresets.dark, repository) {
            DoraiAppearanceSettings(viewModel = ThemeViewModel(repository))
        }
    }

    @Test
    fun appearancePresets_light() {
        val repository = screenshotThemeRepository(ThemePresets.light)
        paparazzi.snap(ThemePresets.light, repository) {
            DoraiAppearanceSettings(viewModel = ThemeViewModel(repository))
        }
    }
    // --- Phase C: Settings light theme ---

    @Test
    fun settingsAgent_light() {
        paparazzi.snap(ThemePresets.light) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsAgent,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    // --- Phase C: Dynamic UI states ---

    @Test
    fun chatWithDynamicUi_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ChatScreenContent(
                uiState = ScreenshotTestData.chatWithDynamicUi,
                FakeTextToSpeechInstance(),
            )
        }
    }

    // --- Phase C: Build Workspace states ---

    @Test
    fun buildSetupNotInstalled_dark() {
        paparazzi.snap(ThemePresets.dark) {
            WorkspaceScreenContent(
                state = ScreenshotTestData.buildStateNotInstalled,
                actions = noOpBuildActions,
                onExit = {},
            )
        }
    }

    @Test
    fun buildSetupInstalling_dark() {
        paparazzi.snap(ThemePresets.dark) {
            WorkspaceScreenContent(
                state = ScreenshotTestData.buildStateInstalling,
                actions = noOpBuildActions,
                onExit = {},
            )
        }
    }

    @Test
    fun buildProjectsReady_dark() {
        paparazzi.snap(ThemePresets.dark) {
            WorkspaceScreenContent(
                state = ScreenshotTestData.buildStateReadyWithProjects,
                actions = noOpBuildActions,
                onExit = {},
            )
        }
    }

    @Test
    fun buildWithActiveProject_dark() {
        paparazzi.snap(ThemePresets.dark) {
            WorkspaceScreenContent(
                state = ScreenshotTestData.buildStateWithActiveProject,
                actions = noOpBuildActions,
                onExit = {},
            )
        }
    }

    @Test
    fun buildStatusPills_light() {
        paparazzi.snap(ThemePresets.light) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                WorkspaceStatusPill(label = "Idle", kind = WorkspaceStatusKind.Idle)
                WorkspaceStatusPill(label = "Running", kind = WorkspaceStatusKind.Running)
                WorkspaceStatusPill(label = "Success", kind = WorkspaceStatusKind.Success)
                WorkspaceStatusPill(label = "Warning", kind = WorkspaceStatusKind.Warning)
                WorkspaceStatusPill(label = "Failed", kind = WorkspaceStatusKind.Failed)
            }
        }
    }

    // --- Phase C: Agent Activity states ---

    @Test
    fun agentActivityTimelineMixed_dark() {
        paparazzi.snap(ThemePresets.dark) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesMixed,
            )
        }
    }

    @Test
    fun agentActivityTimelineToolCall_dark() {
        paparazzi.snap(ThemePresets.dark) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesToolCall,
            )
        }
    }

    @Test
    fun agentActivityTimelineShellCommand_dark() {
        paparazzi.snap(ThemePresets.dark) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesShellCommand,
            )
        }
    }

    @Test
    fun agentActivityTimelineFileOperation_dark() {
        paparazzi.snap(ThemePresets.dark) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesFileOperation,
            )
        }
    }

    @Test
    fun agentActivityTimelinePermission_dark() {
        paparazzi.snap(ThemePresets.dark) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesPermission,
            )
        }
    }

    // --- Phase C: Composer states ---

    @Test
    fun composerEmpty_light() {
        paparazzi.snap(ThemePresets.light) {
            ChatComposer(
                uiState = ScreenshotTestData.chatEmptyState,
                questionInputText = ScreenshotTestData.composerEmptyText,
                onTextStateChange = {},
                isSandboxOpen = false,
            )
        }
    }

    @Test
    fun composerWithDraft_light() {
        paparazzi.snap(ThemePresets.light) {
            ChatComposer(
                uiState = ScreenshotTestData.chatEmptyState.copy(
                    availableModels = ScreenshotTestData.composerAvailableModels,
                ),
                questionInputText = ScreenshotTestData.composerDraftText,
                onTextStateChange = {},
                isSandboxOpen = false,
            )
        }
    }

    @Test
    fun composerLoading_dark() {
        paparazzi.snap(ThemePresets.dark) {
            ChatComposer(
                uiState = ScreenshotTestData.chatEmptyState.copy(isLoading = true),
                questionInputText = ScreenshotTestData.composerEmptyText,
                onTextStateChange = {},
                isSandboxOpen = false,
            )
        }
    }
}
