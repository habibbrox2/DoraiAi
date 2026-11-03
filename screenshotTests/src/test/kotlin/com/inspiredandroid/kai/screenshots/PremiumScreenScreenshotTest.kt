@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.inspiredandroid.kai.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.inspiredandroid.kai.ui.DarkColorScheme
import com.inspiredandroid.kai.ui.Theme
import com.inspiredandroid.kai.ui.chat.composables.ToolConfirmationCard
import com.inspiredandroid.kai.ui.chat.composables.ToolExecutionCard
import com.inspiredandroid.kai.ui.components.OperationalStatus
import com.inspiredandroid.kai.ui.dashboard.DashboardScreenContent
import com.inspiredandroid.kai.ui.dashboard.DashboardUiState
import com.inspiredandroid.kai.ui.dashboard.ReadinessFact
import com.inspiredandroid.kai.ui.dashboard.ServiceReadiness
import com.inspiredandroid.kai.ui.theme.DesignTokens
import com.inspiredandroid.kai.ui.workspace.WorkspaceActions
import com.inspiredandroid.kai.ui.workspace.WorkspaceScreenContent
import com.inspiredandroid.kai.ui.workspace.WorkspaceUiState
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PremiumScreenScreenshotTest {

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

    private fun Paparazzi.snap(content: @Composable () -> Unit) {
        unsafeUpdateConfig(theme = "android:Theme.Material.NoActionBar")
        snapshot {
            Box(modifier = Modifier.fillMaxSize()) {
                WithScreenshotTestKoin {
                    CompositionLocalProvider(LocalInspectionMode provides true) {
                        Theme(colorScheme = DarkColorScheme) {
                            content()
                        }
                    }
                }
            }
        }
    }

    private fun Paparazzi.snapTool(content: @Composable () -> Unit) {
        unsafeUpdateConfig(theme = "android:Theme.Material.NoActionBar")
        snapshot {
            Box(modifier = Modifier.fillMaxSize().background(DesignTokens.Background)) {
                WithScreenshotTestKoin {
                    CompositionLocalProvider(LocalInspectionMode provides true) {
                        Theme(colorScheme = DarkColorScheme) {
                            content()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun dashboard_overview_dark() {
        paparazzi.snap {
            DashboardScreenContent(
                state = DashboardUiState(
                    service = ServiceReadiness(
                        serviceName = "OpenAI",
                        modelId = "GPT-6",
                        isSharedKey = false,
                    ),
                    facts = persistentListOf(
                        ReadinessFact("Workspace", "Ready", OperationalStatus.Ready),
                        ReadinessFact("Sandbox", "Ready", OperationalStatus.Ready),
                    ),
                    recentProjects = persistentListOf("brox-kai-2.0"),
                    isBuildAvailable = true,
                    isSandboxAvailable = true,
                    isDesignAvailable = true,
                ),
                onNavigateToChat = {},
                onOpenBuild = {},
                onOpenSettings = {},
                onStartDesign = {},
                onOpenConversation = {},
            )
        }
    }

    @Test
    fun Workspace_setup_dark() {
        paparazzi.snap {
            WorkspaceScreenContent(
                state = WorkspaceUiState(),
                actions = WorkspaceActions(
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
                ),
                onExit = {},
            )
        }
    }

    @Test
    fun toolExecution_running_dark() {
        paparazzi.snapTool {
            ToolExecutionCard(
                toolName = "web_search",
                arguments = """{"query":"premium AI agent workspace"}""",
                result = null,
                isRunning = true,
                initiallyExpanded = true,
            )
        }
    }

    @Test
    fun toolExecution_failed_dark() {
        paparazzi.snapTool {
            ToolExecutionCard(
                toolName = "execute_shell_command",
                arguments = """{"command":"./gradlew test"}""",
                result = "Process exited with code 1",
                isError = true,
                initiallyExpanded = true,
            )
        }
    }

    @Test
    fun toolConfirmation_pending_dark() {
        paparazzi.snapTool {
            ToolConfirmationCard(
                toolName = "execute_shell_command",
                toolCallId = "screenshot-tool-call",
                arguments = """{"command":"./gradlew test"}""",
                onDeny = {},
                onAllowOnce = {},
                onAllowSession = {},
                onAlwaysAllow = {},
            )
        }
    }
}
