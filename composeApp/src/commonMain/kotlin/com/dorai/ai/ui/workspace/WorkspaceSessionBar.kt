package com.dorai.ai.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.workspace.WorkspaceAgent
import com.dorai.ai.workspace.WorkspaceAgents
import com.dorai.ai.workspace.WorkspaceSessionStatus
import com.dorai.ai.workspace.WorkspaceTerminalSession
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.workspace_back_content_description
import dorai.composeapp.generated.resources.workspace_files_tab
import dorai.composeapp.generated.resources.workspace_session_close_content_description
import dorai.composeapp.generated.resources.workspace_session_new_content_description
import dorai.composeapp.generated.resources.workspace_session_shell
import dorai.composeapp.generated.resources.workspace_session_status_failed
import dorai.composeapp.generated.resources.workspace_session_status_running
import dorai.composeapp.generated.resources.workspace_session_status_starting
import dorai.composeapp.generated.resources.workspace_session_status_stopped
import dorai.composeapp.generated.resources.workspace_session_status_stopping
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

/**
 * The terminal screen's only chrome: back out of the project, switch between the
 * project's live shells, and start another one. Replaces a separate title bar so
 * the cell grid keeps the vertical space.
 */
@Composable
internal fun WorkspaceSessionBar(
    sessions: ImmutableList<WorkspaceTerminalSession>,
    activeSessionId: String?,
    installedAgents: ImmutableList<WorkspaceAgent>,
    filesSelected: Boolean,
    project: String,
    onBack: () -> Unit,
    onSelectSession: (String) -> Unit,
    onCloseSession: (String) -> Unit,
    onSelectFiles: () -> Unit,
    onNewSession: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showNewMenu by remember { mutableStateOf(false) }

    // Focus management for session tabs
    val tabCount = sessions.size + 1 // sessions + files tab
    val tabFocusRequesters = remember { List(tabCount) { FocusRequester() } }
    var focusedTabIndex by remember { mutableStateOf(0) }

    // Determine initial focused index based on selection
    LaunchedEffect(filesSelected, activeSessionId, sessions) {
        if (filesSelected) {
            focusedTabIndex = sessions.size // files tab is last
        } else {
            focusedTabIndex = sessions.indexOfFirst { it.id == activeSessionId }.coerceAtLeast(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(com.dorai.ai.ui.DoraiShapes.card)
            .background(MaterialTheme.colorScheme.surface, com.dorai.ai.ui.DoraiShapes.card)
            .border(com.dorai.ai.ui.DoraiBorders.soft(), com.dorai.ai.ui.DoraiShapes.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = com.dorai.ai.ui.workspace.WorkspaceSpacing.sm, vertical = com.dorai.ai.ui.workspace.WorkspaceSpacing.xs)
                .onKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionLeft -> {
                            focusedTabIndex = (focusedTabIndex - 1).coerceIn(0, tabCount - 1)
                            if (focusedTabIndex < sessions.size) {
                                onSelectSession(sessions[focusedTabIndex].id)
                            } else {
                                onSelectFiles()
                            }
                            true
                        }

                        Key.DirectionRight -> {
                            focusedTabIndex = (focusedTabIndex + 1).coerceIn(0, tabCount - 1)
                            if (focusedTabIndex < sessions.size) {
                                onSelectSession(sessions[focusedTabIndex].id)
                            } else {
                                onSelectFiles()
                            }
                            true
                        }

                        Key.Enter, Key.NumPadEnter -> {
                            if (focusedTabIndex < sessions.size) {
                                onSelectSession(sessions[focusedTabIndex].id)
                            } else {
                                onSelectFiles()
                            }
                            true
                        }

                        else -> false
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).handCursor()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.workspace_back_content_description),
                    tint = LocalDesignColorTokens.current.textSecondary,
                )
            }

            // Project-aware header: name the active project so the session strip reads
            // as a workspace, not a bare list of shells (Plan 3, Phase 3).
            Text(
                text = project,
                style = com.dorai.ai.ui.DoraiTypography.titleSmall,
                color = LocalDesignColorTokens.current.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Cap the header so a long project name can't squeeze the session tabs
                // out of the strip on narrow windows (Plan 3, Phase 6).
                modifier = Modifier.padding(end = com.dorai.ai.ui.workspace.WorkspaceSpacing.sm).widthIn(max = 200.dp),
            )

            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                sessions.forEachIndexed { index, session ->
                    SessionTab(
                        session = session,
                        numbered = sessions.size > 1,
                        selected = !filesSelected && session.id == activeSessionId,
                        onSelect = { onSelectSession(session.id) },
                        onClose = { onCloseSession(session.id) },
                        focusRequester = tabFocusRequesters[index],
                        isFocused = focusedTabIndex == index,
                        onFocus = { focusedTabIndex = index },
                    )
                }
                // Pinned: the project's files are always there, unlike a session.
                TabPill(
                    label = stringResource(Res.string.workspace_files_tab),
                    selected = filesSelected,
                    onSelect = onSelectFiles,
                    focusRequester = tabFocusRequesters[sessions.size],
                    isFocused = focusedTabIndex == sessions.size,
                    onFocus = { focusedTabIndex = sessions.size },
                )
            }

            // Build/session status next to the title instead of only inside terminal
            // text (Plan 3, Phase 3, item 6). Derived from the session lifecycle.
            val activeSession = sessions.firstOrNull { it.id == activeSessionId }
            if (activeSession != null) {
                val (label, kind) = when (activeSession.status) {
                    WorkspaceSessionStatus.Starting ->
                        stringResource(Res.string.workspace_session_status_starting) to WorkspaceStatusKind.Running

                    WorkspaceSessionStatus.Running ->
                        stringResource(Res.string.workspace_session_status_running) to WorkspaceStatusKind.Running

                    WorkspaceSessionStatus.Stopping ->
                        stringResource(Res.string.workspace_session_status_stopping) to WorkspaceStatusKind.Warning

                    WorkspaceSessionStatus.Stopped ->
                        stringResource(Res.string.workspace_session_status_stopped) to WorkspaceStatusKind.Idle

                    WorkspaceSessionStatus.Failed ->
                        stringResource(Res.string.workspace_session_status_failed) to WorkspaceStatusKind.Failed
                }
                WorkspaceStatusPill(label = label, kind = kind)
            }

            Box {
                IconButton(
                    onClick = { showNewMenu = true },
                    modifier = Modifier.size(48.dp).handCursor(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.workspace_session_new_content_description),
                        tint = com.dorai.ai.ui.AccentPrimary,
                    )
                }
                DropdownMenu(expanded = showNewMenu, onDismissRequest = { showNewMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.workspace_session_shell)) },
                        onClick = {
                            showNewMenu = false
                            onNewSession(null)
                        },
                        modifier = Modifier.handCursor(),
                    )
                    installedAgents.forEach { agent ->
                        DropdownMenuItem(
                            text = { Text(agent.title) },
                            onClick = {
                                showNewMenu = false
                                onNewSession(agent.id)
                            },
                            modifier = Modifier.handCursor(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionTab(
    session: WorkspaceTerminalSession,
    numbered: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
    isFocused: Boolean,
    onFocus: () -> Unit,
) {
    val base = WorkspaceAgents.get(session.agentId)?.title ?: stringResource(Res.string.workspace_session_shell)
    val label = if (numbered) "$base ${session.number}" else base
    val contentColor = when {
        // A finished shell keeps its output readable but reads as inactive.
        session.status.isTerminal -> if (selected) MaterialTheme.colorScheme.background.copy(alpha = 0.75f) else LocalDesignColorTokens.current.textTertiary.copy(alpha = 0.7f)

        selected -> MaterialTheme.colorScheme.background

        else -> LocalDesignColorTokens.current.textSecondary
    }
    TabPill(
        label = label,
        selected = selected,
        onSelect = onSelect,
        contentColor = contentColor,
        // Status is only carried by color for sighted users; make it audible
        // (Plan 3, Phase 6).
        description = when (session.status) {
            WorkspaceSessionStatus.Starting -> "$label, ${stringResource(Res.string.workspace_session_status_starting)}"
            WorkspaceSessionStatus.Running -> "$label, ${stringResource(Res.string.workspace_session_status_running)}"
            WorkspaceSessionStatus.Stopping -> "$label, ${stringResource(Res.string.workspace_session_status_stopping)}"
            WorkspaceSessionStatus.Stopped -> "$label, ${stringResource(Res.string.workspace_session_status_stopped)}"
            WorkspaceSessionStatus.Failed -> "$label, ${stringResource(Res.string.workspace_session_status_failed)}"
        },
        focusRequester = focusRequester,
        isFocused = isFocused,
        onFocus = onFocus,
        trailing = {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp).handCursor()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(
                        Res.string.workspace_session_close_content_description,
                    ),
                    tint = contentColor,
                    modifier = Modifier.size(14.dp),
                )
            }
        },
    )
}

/** The strip's pill shape. [trailing] is shown only while selected — that is where a tab's close button goes. */
@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    contentColor: Color = if (selected) {
        MaterialTheme.colorScheme.background
    } else {
        LocalDesignColorTokens.current.textSecondary
    },
    description: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    isFocused: Boolean = false,
    onFocus: () -> Unit = {},
) {
    val shape = com.dorai.ai.ui.DoraiShapes.pill
    // `selected` the parameter shadows the semantics property of the same name,
    // so capture it first and assign through `this.selected`.
    val isSelected = selected

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = when {
            selected -> com.dorai.ai.ui.AccentPrimary
            isFocused -> com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        contentColor = contentColor,
    )

    val border = when {
        selected -> com.dorai.ai.ui.DoraiBorders.accent()
        isFocused -> com.dorai.ai.ui.DoraiBorders.focus()
        else -> androidx.compose.foundation.BorderStroke(1.dp, com.dorai.ai.ui.BorderSubtle.copy(alpha = 0.5f))
    }

    Button(
        onClick = onSelect,
        modifier = Modifier
            .clip(shape)
            .handCursor()
            .focusRequester(focusRequester)
            .onFocusChanged { focused ->
                if (focused.isFocused) onFocus()
            }
            .onKeyEvent { event ->
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        onSelect()
                        true
                    }

                    else -> false
                }
            }
            .semantics {
                role = Role.Tab
                this.selected = isSelected
                if (description != null) contentDescription = description
            },
        colors = buttonColors,
        shape = shape,
        border = border,
        contentPadding = PaddingValues(
            start = 14.dp,
            end = if (selected && trailing != null) 4.dp else 14.dp,
            top = 8.dp,
            bottom = 8.dp,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = com.dorai.ai.ui.DoraiTypography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
            )
            if (selected && trailing != null) trailing()
        }
    }
}
