package com.dorai.ai.screenshots

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.adaptive.calculateDoraiWindowWidthClass
import com.dorai.ai.ui.settings.SandboxSettingsCard
import com.dorai.ai.ui.settings.SandboxUiState
import com.dorai.ai.ui.workspace.DoraiWorkspaceScreen
import com.dorai.ai.ui.workspace.WorkspaceProjectItem

/**
 * Snapshot wrapper for the UI v2 Workspace screen. Provides the adaptive
 * [LocalDoraiWindowWidthClass] the screen reads, plus the Agent + Sandbox sections
 * (the Linux Sandbox card moved here from Settings → Sandbox).
 */
@Composable
internal fun DoraiWorkspaceScreenSnapshot(
    sandboxState: SandboxUiState,
    projects: List<WorkspaceProjectItem> = ScreenshotTestData.workspaceV2Projects,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthClass = calculateDoraiWindowWidthClass(maxWidth)
        CompositionLocalProvider(LocalDoraiWindowWidthClass provides widthClass) {
            DoraiWorkspaceScreen(
                isSetupCompleted = true,
                activeProject = null,
                projects = projects,
                isSandboxReady = true,
                onStartSandbox = {},
                onCompleteSetup = {},
                onOpenProject = {},
                onCloseProject = {},
                installedAgentIds = setOf("claude-code", "opencode"),
                launchAgentId = "claude-code",
                onLaunchAgentSelected = {},
                onInstallAgent = {},
                sandboxSlot = {
                    SandboxSettingsCard(
                        sandboxState = sandboxState,
                        onToggleSandbox = {},
                        onSelectDistro = {},
                        onSetupSandbox = {},
                        onCancelSandbox = {},
                        onResetSandbox = {},
                        onInstallPackages = {},
                        onMigrateHome = {},
                    )
                },
            )
        }
    }
}
