package com.dorai.ai.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Production-connected wrapper for UI v2 Workspace, binding [WorkspaceViewModel].
 */
@Composable
fun DoraiWorkspaceScreenConnected(
    modifier: Modifier = Modifier,
    viewModel: WorkspaceViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gitState by viewModel.gitState.collectAsStateWithLifecycle()
    val gitDiff by viewModel.gitDiff.collectAsStateWithLifecycle()

    val projects = remember(uiState.build.projects) {
        uiState.build.projects.map { name ->
            WorkspaceProjectItem(
                name = name,
                path = "/root/projects/$name",
                branch = "main",
                lastUpdatedText = "Ready",
                changesCount = 0,
            )
        }
    }

    var selectedProjectItem by remember(uiState.openProject) {
        mutableStateOf(
            uiState.openProject?.let { openName ->
                WorkspaceProjectItem(
                    name = openName,
                    path = "/root/projects/$openName",
                    branch = gitState.branch ?: "main",
                    lastUpdatedText = "Active",
                    changesCount = gitState.changes.size,
                )
            },
        )
    }

    val activeSession = uiState.build.activeSession

    DoraiWorkspaceScreen(
        isSetupCompleted = uiState.build.isReady,
        activeProject = selectedProjectItem,
        projects = projects,
        isSandboxRunning = uiState.build.isReady,
        onStartSandbox = { viewModel.install() },
        onCompleteSetup = { viewModel.install() },
        onOpenProject = { proj ->
            selectedProjectItem = proj
            viewModel.openProject(proj.name)
        },
        onCloseProject = {
            selectedProjectItem = null
            viewModel.closeProject()
        },
        onCreateProject = { name -> viewModel.createProject(name) },
        terminalContent = if (activeSession != null) {
            {
                Box(modifier = Modifier.fillMaxSize()) {
                    WorkspaceTerminalContent(
                        session = activeSession,
                        onSubmitLine = viewModel::submitLine,
                        onKey = viewModel::sendKey,
                        onText = viewModel::sendText,
                        onMouse = viewModel::sendMouse,
                        onResize = viewModel::resizeTerminal,
                    )
                }
            }
        } else {
            null
        },
        modifier = modifier,
    )
}
