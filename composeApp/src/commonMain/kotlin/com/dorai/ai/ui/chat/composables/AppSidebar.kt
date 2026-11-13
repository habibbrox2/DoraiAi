package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.chat.ChatUiState
import com.dorai.ai.ui.chat.ConversationSummary
import com.dorai.ai.ui.components.DoraiSearchField
import com.dorai.ai.ui.components.LogoAnimation
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignTokens
import com.dorai.ai.ui.workspace.CreateProjectDialog
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.action_cancel
import dorai.composeapp.generated.resources.action_delete
import dorai.composeapp.generated.resources.action_rename
import dorai.composeapp.generated.resources.action_save
import dorai.composeapp.generated.resources.chat_history_heartbeat_label
import dorai.composeapp.generated.resources.chat_today
import dorai.composeapp.generated.resources.chat_untitled
import dorai.composeapp.generated.resources.chat_yesterday
import dorai.composeapp.generated.resources.dashboard_title
import dorai.composeapp.generated.resources.ic_settings
import dorai.composeapp.generated.resources.sidebar_nav_dashboard
import dorai.composeapp.generated.resources.sidebar_nav_interactive
import dorai.composeapp.generated.resources.sidebar_nav_settings
import dorai.composeapp.generated.resources.sidebar_nav_workspace
import dorai.composeapp.generated.resources.sidebar_new_chat
import dorai.composeapp.generated.resources.sidebar_new_project
import dorai.composeapp.generated.resources.sidebar_no_chats
import dorai.composeapp.generated.resources.sidebar_no_matches
import dorai.composeapp.generated.resources.sidebar_older
import dorai.composeapp.generated.resources.sidebar_options
import dorai.composeapp.generated.resources.sidebar_pin
import dorai.composeapp.generated.resources.sidebar_pinned_group
import dorai.composeapp.generated.resources.sidebar_previous_30_days
import dorai.composeapp.generated.resources.sidebar_previous_7_days
import dorai.composeapp.generated.resources.sidebar_projects
import dorai.composeapp.generated.resources.sidebar_projects_empty
import dorai.composeapp.generated.resources.sidebar_projects_setup
import dorai.composeapp.generated.resources.sidebar_recent_chats
import dorai.composeapp.generated.resources.sidebar_rename_chat
import dorai.composeapp.generated.resources.sidebar_search_chats
import dorai.composeapp.generated.resources.sidebar_tagline
import dorai.composeapp.generated.resources.sidebar_unpin
import dorai.composeapp.generated.resources.sidebar_version
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 2027 Premium Navigation Sidebar.
 *
 * New Chat + search on top, a time-grouped conversation list in the middle
 * (Pinned / Today / Yesterday / Previous 7 Days / Previous 30 Days / Older),
 * and app-level destinations plus the DORAI AI brand mark at the bottom.
 */
@Composable
fun AppSidebar(
    uiState: ChatUiState,
    filteredConversations: ImmutableList<ConversationSummary>,
    onNewChat: () -> Unit,
    onConversationSelected: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onTogglePinConversation: (String) -> Unit,
    onNavigateToDashboard: (() -> Unit)?,
    onNavigateToSettings: () -> Unit,
    /** Interactive UI mode is on: the link below reads as an active toggle. */
    isInteractiveMode: Boolean = false,
    onToggleInteractiveMode: (() -> Unit)?,
    onOpenWorkspace: (() -> Unit)?,
    /**
     * Workspace projects for the Projects section. Null hides the section entirely
     * (NoOp platforms); non-null with [workspaceIsReady] = false shows the setup row.
     */
    workspaceProjects: ImmutableList<String>? = null,
    workspaceIsReady: Boolean = false,
    onCreateProject: ((String) -> Unit)? = null,
    onOpenProject: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDesignTokens.current
    var query by remember { mutableStateOf("") }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    val visibleConversations = remember(filteredConversations, query) {
        if (query.isBlank()) {
            filteredConversations
        } else {
            filteredConversations.filter { it.title.contains(query, ignoreCase = true) }.toImmutableList()
        }
    }

    // Time-based grouping. Pinned conversations always float to the top group.
    val groups = remember(visibleConversations) { groupConversations(visibleConversations) }
    // (group header strings are rendered as stringResources at the row below)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.SurfaceDark)
            .border(BorderStroke(1.dp, tokens.BorderSubtle))
            .padding(ChatSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChatSpacing.xs),
    ) {
        // --- Header: brand mark + search -------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LogoAnimation(size = 32.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.dashboard_title),
                    style = DoraiTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.PrimaryText,
                )
                Text(
                    text = stringResource(Res.string.sidebar_tagline),
                    style = DoraiTypography.labelSmall,
                    color = tokens.MutedText,
                )
            }
        }

        DoraiSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(Res.string.sidebar_search_chats),
            modifier = Modifier.fillMaxWidth(),
        )

        // Elevated 2027 "New Chat" primary action button
        Surface(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .handCursor(),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, tokens.PrimaryCyan.copy(alpha = 0.45f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf<Color>(
                                tokens.PrimaryCyan.copy(alpha = 0.16f),
                                DesignTokens.BrandBlue.copy(alpha = 0.08f),
                            ),
                        ),
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(tokens.PrimaryCyan.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = tokens.PrimaryCyan,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.sidebar_new_chat),
                        style = DoraiTypography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.PrimaryText,
                    )
                }
            }
        }

        // Secondary primary action: create a project inline when ready, else navigate to setup.
        onOpenWorkspace?.let { openWorkspace ->
            val canCreateInline = workspaceProjects != null && workspaceIsReady && onCreateProject != null
            SidebarActionButton(
                icon = Icons.Default.Folder,
                label = stringResource(Res.string.sidebar_new_project),
                onClick = if (canCreateInline) {
                    { showCreateProjectDialog = true }
                } else {
                    openWorkspace
                },
            )
        }

        workspaceProjects?.let { projects ->
            SidebarProjectsSection(
                projects = projects,
                isReady = workspaceIsReady,
                onOpenProject = onOpenProject,
                onOpenWorkspace = onOpenWorkspace,
            )
        }

        // --- Chat history ----------------------------------------------
        Text(
            text = stringResource(Res.string.sidebar_recent_chats).uppercase(),
            style = DoraiTypography.labelSmall.copy(letterSpacing = 1.sp),
            fontWeight = FontWeight.Bold,
            color = tokens.MutedText.copy(alpha = 0.7f),
            modifier = Modifier.padding(
                start = ChatSpacing.xs,
                top = ChatSpacing.xs,
                bottom = ChatSpacing.xxs,
            ),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ChatSpacing.xxs),
        ) {
            groups.forEachIndexed { groupIndex, (labelRes, conversations) ->
                if (conversations.isEmpty()) return@forEachIndexed
                item(key = "header_$groupIndex") {
                    Text(
                        text = stringResource(labelRes).uppercase(),
                        style = DoraiTypography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = tokens.MutedText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(
                            start = ChatSpacing.xs,
                            top = ChatSpacing.sm,
                            bottom = ChatSpacing.xxs,
                        ),
                    )
                }
                items(conversations, key = { it.id }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        isSelected = conversation.id == uiState.currentConversationId,
                        onSelect = { onConversationSelected(conversation.id) },
                        onDelete = { onDeleteConversation(conversation.id) },
                        onRename = { newTitle -> onRenameConversation(conversation.id, newTitle) },
                        onTogglePin = { onTogglePinConversation(conversation.id) },
                    )
                }
            }
            if (visibleConversations.isEmpty()) {
                item {
                    Text(
                        text = if (query.isBlank()) {
                            stringResource(Res.string.sidebar_no_chats)
                        } else {
                            stringResource(Res.string.sidebar_no_matches)
                        },
                        style = DoraiTypography.bodySmall,
                        color = tokens.MutedText,
                        modifier = Modifier.padding(vertical = ChatSpacing.md, horizontal = ChatSpacing.xs),
                    )
                }
            }
        }

        HorizontalDivider(color = tokens.BorderSubtle)

        // --- Bottom navigation -----------------------------------------
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            onNavigateToDashboard?.let {
                SidebarLink(
                    icon = Icons.Default.Dashboard,
                    label = stringResource(Res.string.sidebar_nav_dashboard),
                    onClick = it,
                )
            }
            onToggleInteractiveMode?.let { toggle ->
                SidebarLink(
                    icon = Icons.Default.Widgets,
                    label = stringResource(Res.string.sidebar_nav_interactive),
                    onClick = toggle,
                    isActive = isInteractiveMode,
                )
            }
            onOpenWorkspace?.let {
                SidebarLink(
                    icon = Icons.Default.Terminal,
                    label = stringResource(Res.string.sidebar_nav_workspace),
                    onClick = it,
                )
            }
            SidebarLink(
                icon = vectorResource(Res.drawable.ic_settings),
                label = stringResource(Res.string.sidebar_nav_settings),
                onClick = onNavigateToSettings,
            )
        }

        // Polished 2027 Brand Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LogoAnimation(size = 28.dp)
                Text(
                    text = stringResource(Res.string.dashboard_title),
                    style = DoraiTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tokens.PrimaryText,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tokens.PrimaryCyan.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, tokens.PrimaryCyan.copy(alpha = 0.25f)), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.sidebar_version),
                    style = DoraiTypography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.PrimaryCyan,
                )
            }
        }
    }

    if (showCreateProjectDialog && workspaceProjects != null && onCreateProject != null) {
        CreateProjectDialog(
            projects = workspaceProjects,
            onDismiss = { showCreateProjectDialog = false },
            onCreate = { name ->
                showCreateProjectDialog = false
                onCreateProject(name)
            },
        )
    }
}

/** Which sidebar Projects state to render — pure, exported for tests. */
enum class SidebarProjectSection {
    /** Debian not ready: show the setup row instead of an empty list. */
    NotReady,

    /** Ready but no folders yet. */
    Empty,

    /** Ready with folders to list. */
    Ready,
}

/**
 * Pure section classifier for the sidebar Projects block. Keeps the composable
 * presentational over `ImmutableList<String>` with no filesystem access.
 */
fun sidebarProjectSection(projects: ImmutableList<String>, isReady: Boolean): SidebarProjectSection = when {
    !isReady -> SidebarProjectSection.NotReady
    projects.isEmpty() -> SidebarProjectSection.Empty
    else -> SidebarProjectSection.Ready
}

/**
 * Projects section between the New Project button and Recent Chats. Folder icon,
 * `Projects` header, and a divider keep it visually separate from chat rows.
 */
@Composable
private fun SidebarProjectsSection(
    projects: ImmutableList<String>,
    isReady: Boolean,
    onOpenProject: ((String) -> Unit)?,
    onOpenWorkspace: (() -> Unit)?,
) {
    val tokens = LocalDesignTokens.current
    Text(
        text = stringResource(Res.string.sidebar_projects).uppercase(),
        style = DoraiTypography.labelSmall.copy(letterSpacing = 1.sp),
        fontWeight = FontWeight.Bold,
        color = tokens.MutedText.copy(alpha = 0.7f),
        modifier = Modifier.padding(
            start = ChatSpacing.xs,
            top = ChatSpacing.xs,
            bottom = ChatSpacing.xxs,
        ),
    )
    when (sidebarProjectSection(projects, isReady)) {
        SidebarProjectSection.NotReady -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .handCursor()
                    .clickable(role = Role.Button) { onOpenWorkspace?.invoke() }
                    .padding(horizontal = ChatSpacing.xs, vertical = ChatSpacing.sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = tokens.PrimaryCyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.sidebar_projects_setup),
                        style = DoraiTypography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = tokens.PrimaryCyan,
                    )
                }
            }
        }

        SidebarProjectSection.Empty -> {
            Text(
                text = stringResource(Res.string.sidebar_projects_empty),
                style = DoraiTypography.bodySmall,
                color = tokens.MutedText,
                modifier = Modifier.padding(vertical = ChatSpacing.sm, horizontal = ChatSpacing.xs),
            )
        }

        SidebarProjectSection.Ready -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 192.dp),
                verticalArrangement = Arrangement.spacedBy(ChatSpacing.xxs),
            ) {
                items(projects, key = { it }) { project ->
                    SidebarProjectRow(
                        project = project,
                        onClick = { onOpenProject?.invoke(project) },
                    )
                }
            }
        }
    }
    HorizontalDivider(color = tokens.BorderSubtle)
}

@Composable
private fun SidebarProjectRow(
    project: String,
    onClick: () -> Unit,
) {
    val tokens = LocalDesignTokens.current
    val rowShape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .handCursor()
            .clip(rowShape)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = ChatSpacing.xs, vertical = ChatSpacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = tokens.PrimaryCyan,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = project,
                style = DoraiTypography.bodySmall,
                color = tokens.SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationSummary,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onTogglePin: () -> Unit,
) {
    val tokens = LocalDesignTokens.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val rowShape = RoundedCornerShape(10.dp)

    if (showRenameDialog) {
        RenameDialog(
            initial = conversation.title,
            onConfirm = { newTitle ->
                showRenameDialog = false
                if (newTitle.isNotBlank()) onRename(newTitle)
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .handCursor()
            .clip(rowShape)
            .clickable(onClick = onSelect, role = Role.Button)
            .background(
                if (isSelected) tokens.SurfaceElevated else Color.Transparent,
                rowShape,
            )
            .border(
                if (isSelected) BorderStroke(1.dp, tokens.PrimaryCyan.copy(alpha = 0.35f)) else BorderStroke(0.dp, Color.Transparent),
                rowShape,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(tokens.PrimaryCyan),
                )
            }

            Icon(
                imageVector = if (conversation.isPinned) Icons.Default.PushPin else Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = when {
                    isSelected -> tokens.PrimaryCyan
                    conversation.isPinned -> tokens.PrimaryCyan.copy(alpha = 0.85f)
                    else -> tokens.MutedText
                },
                modifier = Modifier.size(16.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                if (conversation.isHeartbeat) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(tokens.AgentViolet),
                        )
                        Text(
                            text = stringResource(Res.string.chat_history_heartbeat_label),
                            style = DoraiTypography.labelSmall,
                            color = tokens.AgentViolet,
                        )
                    }
                }
                Text(
                    text = conversation.title.ifBlank { stringResource(Res.string.chat_untitled) },
                    style = DoraiTypography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) tokens.PrimaryText else tokens.SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).handCursor(),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.sidebar_options),
                        tint = if (isSelected) tokens.SecondaryText else tokens.MutedText,
                        modifier = Modifier.size(15.dp),
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(tokens.SurfaceElevated)
                        .border(BorderStroke(1.dp, tokens.BorderSubtle), RoundedCornerShape(8.dp)),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.action_rename), color = tokens.PrimaryText) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = tokens.SecondaryText,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (conversation.isPinned) stringResource(Res.string.sidebar_unpin) else stringResource(Res.string.sidebar_pin), color = tokens.PrimaryText) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = tokens.SecondaryText,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        onClick = {
                            showMenu = false
                            onTogglePin()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.action_delete), color = DesignTokens.Error) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = DesignTokens.Error,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalDesignTokens.current
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.sidebar_rename_chat),
                style = DoraiTypography.titleMedium,
                color = tokens.PrimaryText,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tokens.BorderFocus,
                    unfocusedBorderColor = tokens.BorderSubtle,
                    focusedTextColor = tokens.PrimaryText,
                    unfocusedTextColor = tokens.PrimaryText,
                    focusedContainerColor = tokens.SurfaceElevated,
                    unfocusedContainerColor = tokens.SurfaceElevated,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = tokens.PrimaryCyan,
                ),
            ) {
                Text(stringResource(Res.string.action_save), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = tokens.MutedText,
                ),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        containerColor = tokens.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun SidebarActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val tokens = LocalDesignTokens.current
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .handCursor(),
        shape = shape,
        color = tokens.SurfaceElevated.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, tokens.BorderSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.SecondaryText,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = DoraiTypography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = tokens.PrimaryText,
            )
        }
    }
}

@Composable
private fun SidebarLink(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
) {
    val tokens = LocalDesignTokens.current
    val linkShape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .handCursor()
            .clickable(onClick = onClick, role = Role.Button)
            .clip(linkShape)
            .background(
                if (isActive) tokens.PrimaryCyan.copy(alpha = 0.12f) else Color.Transparent,
                linkShape,
            )
            .border(
                if (isActive) BorderStroke(1.dp, tokens.PrimaryCyan.copy(alpha = 0.35f)) else BorderStroke(0.dp, Color.Transparent),
                linkShape,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ChatSpacing.xs, vertical = ChatSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) tokens.PrimaryCyan else tokens.SecondaryText,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = DoraiTypography.bodyMedium,
                color = if (isActive) tokens.PrimaryCyan else tokens.PrimaryText,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

private fun groupConversations(
    conversations: ImmutableList<ConversationSummary>,
): List<Pair<StringResource, List<ConversationSummary>>> {
    val pinned = conversations.filter { it.isPinned }
    val unpinned = conversations.filter { !it.isPinned }

    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date
    fun daysAgo(epochMillis: Long): Int {
        val d = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(tz).date
        return (today.toEpochDays() - d.toEpochDays()).toInt()
    }

    val todayList = mutableListOf<ConversationSummary>()
    val yesterdayList = mutableListOf<ConversationSummary>()
    val weekList = mutableListOf<ConversationSummary>()
    val monthList = mutableListOf<ConversationSummary>()
    val olderList = mutableListOf<ConversationSummary>()

    for (c in unpinned) {
        when (val d = daysAgo(c.updatedAt)) {
            0 -> todayList.add(c)
            1 -> yesterdayList.add(c)
            in 2..7 -> weekList.add(c)
            in 8..30 -> monthList.add(c)
            else -> olderList.add(c)
        }
    }

    val result = mutableListOf<Pair<StringResource, List<ConversationSummary>>>()
    if (pinned.isNotEmpty()) result.add(Res.string.sidebar_pinned_group to pinned)
    if (todayList.isNotEmpty()) result.add(Res.string.chat_today to todayList)
    if (yesterdayList.isNotEmpty()) result.add(Res.string.chat_yesterday to yesterdayList)
    if (weekList.isNotEmpty()) result.add(Res.string.sidebar_previous_7_days to weekList)
    if (monthList.isNotEmpty()) result.add(Res.string.sidebar_previous_30_days to monthList)
    if (olderList.isNotEmpty()) result.add(Res.string.sidebar_older to olderList)
    return result
}
