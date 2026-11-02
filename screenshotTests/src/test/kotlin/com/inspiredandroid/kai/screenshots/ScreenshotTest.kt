@file:OptIn(ExperimentalVoiceApi::class)

package com.inspiredandroid.kai.screenshots

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.inspiredandroid.kai.ui.DarkColorScheme
import com.inspiredandroid.kai.ui.LightColorScheme
import com.inspiredandroid.kai.ui.Theme
import com.inspiredandroid.kai.ui.build.BuildStatusKind
import com.inspiredandroid.kai.ui.build.BuildStatusPill
import com.inspiredandroid.kai.ui.build.KaiBuildActions
import com.inspiredandroid.kai.ui.build.KaiBuildScreenContent
import com.inspiredandroid.kai.ui.chat.ChatScreenContent
import com.inspiredandroid.kai.ui.chat.composables.AgentActivityTimeline
import com.inspiredandroid.kai.ui.chat.composables.ChatComposer
import com.inspiredandroid.kai.ui.chat.composables.ShellExecutionCard
import com.inspiredandroid.kai.ui.chat.composables.ToolConfirmationCard
import com.inspiredandroid.kai.ui.chat.composables.ToolExecutionCard
import com.inspiredandroid.kai.ui.dynamicui.LocalPreviewImages
import com.inspiredandroid.kai.ui.settings.SettingsScreenContent
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

    private val noOpBuildActions = KaiBuildActions(
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
    )

    private fun loadPreviewImage(key: String, resourcePath: String) {
        val bitmap = BitmapFactory.decodeStream(javaClass.getResourceAsStream(resourcePath)) ?: return
        previewImages[key] = bitmap.asImageBitmap()
    }

    fun Paparazzi.snap(
        colorScheme: ColorScheme,
        content: @Composable () -> Unit,
    ) {
        val theme = if (colorScheme == LightColorScheme) {
            "android:Theme.Material.Light.NoActionBar"
        } else {
            "android:Theme.Material.NoActionBar"
        }
        unsafeUpdateConfig(theme = theme)

        snapshot {
            Box(modifier = Modifier.fillMaxSize()) {
                WithScreenshotTestKoin {
                    CompositionLocalProvider(
                        LocalInspectionMode provides true,
                        LocalPreviewImages provides previewImages,
                    ) {
                        Theme(colorScheme = colorScheme) {
                            content()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun chatEmptyState_premiumDark() {
        paparazzi.snap(DarkColorScheme) {
            ChatScreenContent(
                uiState = ScreenshotTestData.chatEmptyState,
                FakeTextToSpeechInstance(),
                isSandboxAvailable = true,
            )
        }
    }

    @Test
    fun chatWithMessages_dark() {
        paparazzi.snap(DarkColorScheme) {
            ChatScreenContent(
                uiState = ScreenshotTestData.chatWithMessages,
                FakeTextToSpeechInstance(),
            )
        }
    }

    @Test
    fun chatWithDynamicUi_light() {
        paparazzi.snap(LightColorScheme) {
            ChatScreenContent(
                uiState = ScreenshotTestData.chatWithDynamicUi,
                FakeTextToSpeechInstance(),
            )
        }
    }

    @Test
    fun settingsAgent_dark() {
        paparazzi.snap(DarkColorScheme) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsAgent,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsFree_dark() {
        paparazzi.snap(DarkColorScheme) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.freeConnected,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsTools_light() {
        paparazzi.snap(LightColorScheme) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsTools,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsSandbox_dark() {
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsGeneralState,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsAppearance_dark() {
        paparazzi.snap(DarkColorScheme) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsAppearanceState,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsAccount_dark() {
        paparazzi.snap(DarkColorScheme) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsAccountState,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    @Test
    fun settingsData_dark() {
        paparazzi.snap(DarkColorScheme) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsDataState,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    // --- Phase C: Settings light theme ---

    @Test
    fun settingsAgent_light() {
        paparazzi.snap(LightColorScheme) {
            SettingsScreenContent(
                uiState = ScreenshotTestData.settingsAgent,
                sandboxState = ScreenshotTestData.sandboxState,
            )
        }
    }

    // --- Phase C: Dynamic UI states ---

    @Test
    fun chatWithDynamicUi_dark() {
        paparazzi.snap(DarkColorScheme) {
            ChatScreenContent(
                uiState = ScreenshotTestData.chatWithDynamicUi,
                FakeTextToSpeechInstance(),
            )
        }
    }

    // --- Phase C: Build Workspace states ---

    @Test
    fun buildSetupNotInstalled_dark() {
        paparazzi.snap(DarkColorScheme) {
            KaiBuildScreenContent(
                state = ScreenshotTestData.buildStateNotInstalled,
                actions = noOpBuildActions,
                onExit = {},
            )
        }
    }

    @Test
    fun buildSetupInstalling_dark() {
        paparazzi.snap(DarkColorScheme) {
            KaiBuildScreenContent(
                state = ScreenshotTestData.buildStateInstalling,
                actions = noOpBuildActions,
                onExit = {},
            )
        }
    }

    @Test
    fun buildProjectsReady_dark() {
        paparazzi.snap(DarkColorScheme) {
            KaiBuildScreenContent(
                state = ScreenshotTestData.buildStateReadyWithProjects,
                actions = noOpBuildActions,
                onExit = {},
            )
        }
    }

    @Test
    fun buildWithActiveProject_dark() {
        paparazzi.snap(DarkColorScheme) {
            KaiBuildScreenContent(
                state = ScreenshotTestData.buildStateWithActiveProject,
                actions = noOpBuildActions,
                onExit = {},
            )
        }
    }

    @Test
    fun buildStatusPills_light() {
        paparazzi.snap(LightColorScheme) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                BuildStatusPill(label = "Idle", kind = BuildStatusKind.Idle)
                BuildStatusPill(label = "Running", kind = BuildStatusKind.Running)
                BuildStatusPill(label = "Success", kind = BuildStatusKind.Success)
                BuildStatusPill(label = "Warning", kind = BuildStatusKind.Warning)
                BuildStatusPill(label = "Failed", kind = BuildStatusKind.Failed)
            }
        }
    }

    // --- Phase C: Agent Activity states ---

    @Test
    fun agentActivityTimelineMixed_dark() {
        paparazzi.snap(DarkColorScheme) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesMixed,
            )
        }
    }

    @Test
    fun agentActivityTimelineToolCall_dark() {
        paparazzi.snap(DarkColorScheme) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesToolCall,
            )
        }
    }

    @Test
    fun agentActivityTimelineShellCommand_dark() {
        paparazzi.snap(DarkColorScheme) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesShellCommand,
            )
        }
    }

    @Test
    fun agentActivityTimelineFileOperation_dark() {
        paparazzi.snap(DarkColorScheme) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesFileOperation,
            )
        }
    }

    @Test
    fun agentActivityTimelinePermission_dark() {
        paparazzi.snap(DarkColorScheme) {
            AgentActivityTimeline(
                activities = ScreenshotTestData.agentActivitiesPermission,
            )
        }
    }

    // --- Phase C: Composer states ---

    @Test
    fun composerEmpty_light() {
        paparazzi.snap(LightColorScheme) {
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
        paparazzi.snap(LightColorScheme) {
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
        paparazzi.snap(DarkColorScheme) {
            ChatComposer(
                uiState = ScreenshotTestData.chatEmptyState.copy(isLoading = true),
                questionInputText = ScreenshotTestData.composerEmptyText,
                onTextStateChange = {},
                isSandboxOpen = false,
            )
        }
    }
}
