package com.inspiredandroid.kai.ui.build

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inspiredandroid.kai.KAI_BUILD_FILES
import com.inspiredandroid.kai.PlatformBackHandler
import com.inspiredandroid.kai.build.BuildAgents
import com.inspiredandroid.kai.build.terminal.TerminalKey
import com.inspiredandroid.kai.build.terminal.TerminalModifiers
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.sandbox.SandboxFilesContent
import com.inspiredandroid.kai.ui.TextPrimary
import com.inspiredandroid.kai.ui.TextSecondary
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.kai_build_exit_content_description
import kai.composeapp.generated.resources.kai_build_new_project_content_description
import kai.composeapp.generated.resources.kai_build_title
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Where Debian mounts the project folders — the browser's root inside a project. */
private const val PROJECTS_GUEST_DIR = "/root/projects"

/**
 * Keeps Kai Build's browser out of the chat sandbox's ViewModel slot: same class,
 * same store owner, so without a distinct key they would share one instance.
 */
private const val KAI_BUILD_FILES_KEY = "kaiBuildFiles"

@Immutable
data class KaiBuildActions(
    val toggleAgent: (String) -> Unit,
    val install: () -> Unit,
    val installAgent: (String) -> Unit,
    val cancel: () -> Unit,
    val uninstall: () -> Unit,
    val setLaunchAgent: (String?) -> Unit,
    val openProject: (String) -> Unit,
    val createProject: (String) -> Unit,
    val deleteProject: (String) -> Unit,
    val renameProject: (name: String, newName: String) -> Unit,
    val closeProject: () -> Unit,
    val startSession: (String?) -> Unit,
    val selectSession: (String) -> Unit,
    val closeSession: (String) -> Unit,
    val submitLine: (String) -> Unit,
    val sendKey: (TerminalKey, TerminalModifiers) -> Unit,
    val sendText: (String, TerminalModifiers) -> Unit,
    val sendMouse: (String) -> Unit,
    val resizeTerminal: (columns: Int, rows: Int) -> Unit,
)

/**
 * Full-screen Kai Build surface. Entered from the empty chat state and left via
 * the top-bar close button or system back — same shape as Interactive UI mode,
 * so it never becomes a navigation destination of its own.
 *
 * One screen, three states: set up Linux, pick a project, work in its terminal.
 */
@Composable
fun KaiBuildScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    initialProject: String? = null,
    viewModel: KaiBuildViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        KaiBuildActions(
            toggleAgent = viewModel::toggleAgent,
            install = viewModel::install,
            installAgent = viewModel::installAgent,
            cancel = viewModel::cancel,
            uninstall = viewModel::uninstall,
            setLaunchAgent = viewModel::setLaunchAgent,
            openProject = viewModel::openProject,
            createProject = viewModel::createProject,
            deleteProject = viewModel::deleteProject,
            renameProject = viewModel::renameProject,
            closeProject = viewModel::closeProject,
            startSession = viewModel::startSession,
            selectSession = viewModel::selectSession,
            closeSession = viewModel::closeSession,
            submitLine = viewModel::submitLine,
            sendKey = viewModel::sendKey,
            sendText = viewModel::sendText,
            sendMouse = viewModel::sendMouse,
            resizeTerminal = viewModel::resizeTerminal,
        )
    }

    // A dashboard resume link (Plan 6) opens the project once its environment
    // is ready; a stale link (project or Debian gone) is dropped instead of
    // showing a broken terminal.
    var pendingProject by rememberSaveable(initialProject) { mutableStateOf(initialProject) }
    LaunchedEffect(state.build) {
        val pending = pendingProject ?: return@LaunchedEffect
        if (state.build.isReady) {
            if (pending in state.build.projects) {
                viewModel.openProject(pending)
            }
            pendingProject = null
        }
    }

    KaiBuildScreenContent(state = state, actions = actions, onExit = onExit, modifier = modifier)
}

@Composable
fun KaiBuildScreenContent(
    state: KaiBuildUiState,
    actions: KaiBuildActions,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val project = state.openProject
    var showCreateProject by rememberSaveable { mutableStateOf(false) }
    // Files is a view of the project, not a session — it resets when the project does.
    var filesOpen by rememberSaveable(project) { mutableStateOf(false) }
    PlatformBackHandler(enabled = true) {
        when {
            filesOpen -> filesOpen = false
            project != null -> actions.closeProject()
            else -> onExit()
        }
    }

    val installedAgents = remember(state.build.installedAgents) {
        BuildAgents.all.filter { it.id in state.build.installedAgents }.toImmutableList()
    }

    // Surface, not just a background modifier: it also provides the matching
    // content color, which is what keeps the bare icons legible in dark mode.
    Box(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = com.inspiredandroid.kai.ui.build.BuildSpacing.sm),
        ) {
            if (project != null) {
                val sessions = remember(state.build.sessions, project) {
                    state.build.sessions.filter { it.project == project }.toImmutableList()
                }
                BuildSessionBar(
                    sessions = sessions,
                    activeSessionId = state.build.activeSessionId,
                    installedAgents = installedAgents,
                    filesSelected = filesOpen,
                    project = project,
                    onBack = actions.closeProject,
                    onSelectSession = { id ->
                        filesOpen = false
                        actions.selectSession(id)
                    },
                    onCloseSession = actions.closeSession,
                    onSelectFiles = { filesOpen = true },
                    onNewSession = { agentId ->
                        filesOpen = false
                        actions.startSession(agentId)
                    },
                )
                // Fall back to the project's first tab: the active id can briefly point
                // elsewhere while a session is being closed.
                val session = sessions.firstOrNull { it.id == state.build.activeSessionId }
                    ?: sessions.firstOrNull()
                when {
                    filesOpen -> {
                        SandboxFilesContent(
                            // This surface is full-screen, so nothing else keeps the
                            // soft keyboard off the editor's text field.
                            modifier = Modifier.fillMaxSize().imePadding(),
                            // Opens on the project, but browses the whole Debian tree
                            // the way the chat sandbox does — agent config and /etc are
                            // a breadcrumb away rather than shell-only.
                            initialPath = "$PROJECTS_GUEST_DIR/$project",
                            viewModel = koinViewModel(
                                qualifier = KAI_BUILD_FILES,
                                key = KAI_BUILD_FILES_KEY,
                            ),
                        )
                    }

                    session != null -> BuildTerminalContent(
                        session = session,
                        onSubmitLine = actions.submitLine,
                        onKey = actions.sendKey,
                        onText = actions.sendText,
                        onMouse = actions.sendMouse,
                        onResize = actions.resizeTerminal,
                    )

                    else -> {
                        // No live session yet (race during close / project opened without
                        // a started session): kick one off instead of leaving the area blank.
                        LaunchedEffect(project) {
                            actions.startSession(null)
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Starting session…",
                                style = KaiTypography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            } else {
                // Centralized width policy (Plan 3, Phase 6): the hub centers with a
                // readable max width on expanded windows and stays full-bleed below.
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    val tier = buildWorkspaceTier(maxWidth)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .widthIn(max = tier.contentMaxWidth)
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    ) {
                        KaiBuildTopBar(
                            title = stringResource(Res.string.kai_build_title),
                            onExit = onExit,
                            actions = {
                                if (state.build.isReady) {
                                    IconButton(
                                        onClick = { showCreateProject = true },
                                        modifier = Modifier.handCursor(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(
                                                Res.string.kai_build_new_project_content_description,
                                            ),
                                        )
                                    }
                                }
                            },
                        )

                        if (state.build.isReady) {
                            BuildProjectsContent(
                                state = state.build,
                                launchAgentId = state.launchAgentId,
                                installedAgents = installedAgents,
                                onSelectLaunchAgent = actions.setLaunchAgent,
                                onOpenProject = actions.openProject,
                                onDeleteProject = actions.deleteProject,
                                onRenameProject = actions.renameProject,
                                onInstallAgent = actions.installAgent,
                                onUninstall = actions.uninstall,
                            )
                        } else {
                            BuildSetupContent(
                                state = state.build,
                                selectedAgents = state.selectedAgents,
                                onToggleAgent = actions.toggleAgent,
                                onInstall = actions.install,
                                onCancel = actions.cancel,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateProject) {
        CreateProjectDialog(
            onDismiss = { showCreateProject = false },
            onCreate = { name ->
                showCreateProject = false
                actions.createProject(name)
            },
        )
    }
}

@Composable
private fun KaiBuildTopBar(
    title: String,
    onExit: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(com.inspiredandroid.kai.ui.KaiShapes.card)
            .background(MaterialTheme.colorScheme.surface, com.inspiredandroid.kai.ui.KaiShapes.card)
            .border(com.inspiredandroid.kai.ui.KaiBorders.soft, com.inspiredandroid.kai.ui.KaiShapes.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = com.inspiredandroid.kai.ui.build.BuildSpacing.sm, vertical = com.inspiredandroid.kai.ui.build.BuildSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit, modifier = Modifier.handCursor()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.kai_build_exit_content_description),
                    tint = com.inspiredandroid.kai.ui.TextSecondary,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = com.inspiredandroid.kai.ui.build.BuildSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "WORKSPACE",
                    style = KaiTypography.labelSmall,
                    color = com.inspiredandroid.kai.ui.AccentPrimary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
                Text(
                    text = title,
                    style = KaiTypography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            actions()
        }
    }
}
