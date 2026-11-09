package com.dorai.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.engine.doraiEdgeStyle
import com.dorai.ai.theme.engine.doraiSurfaceStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.DoraiWindowWidthClass
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.components.LogoAnimation
import com.dorai.ai.ui.components.v2.DoraiProviderBadge
import com.dorai.ai.ui.components.v2.DoraiStatusBadge
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.app_updated_recently
import dorai.composeapp.generated.resources.home_continue_project
import dorai.composeapp.generated.resources.home_model_label
import dorai.composeapp.generated.resources.home_models
import dorai.composeapp.generated.resources.home_need_approval
import dorai.composeapp.generated.resources.home_new_chat
import dorai.composeapp.generated.resources.home_no_approvals
import dorai.composeapp.generated.resources.home_no_recent_chats
import dorai.composeapp.generated.resources.home_open_project
import dorai.composeapp.generated.resources.home_quick_start
import dorai.composeapp.generated.resources.home_recent_chats
import dorai.composeapp.generated.resources.home_review_approvals
import dorai.composeapp.generated.resources.home_running
import dorai.composeapp.generated.resources.home_running_count
import dorai.composeapp.generated.resources.home_sandbox_label
import dorai.composeapp.generated.resources.home_setup_workspace
import dorai.composeapp.generated.resources.home_setup_workspace_desc
import dorai.composeapp.generated.resources.home_start_setup
import dorai.composeapp.generated.resources.home_status
import dorai.composeapp.generated.resources.home_stopped
import dorai.composeapp.generated.resources.home_storage_label
import dorai.composeapp.generated.resources.home_terminal
import dorai.composeapp.generated.resources.home_today_glance
import dorai.composeapp.generated.resources.nav_brand_title
import dorai.composeapp.generated.resources.nav_home
import org.jetbrains.compose.resources.stringResource

data class HomeProjectSummary(
    val name: String,
    val branch: String,
    val changesCount: Int,
)

data class HomeChatSummary(
    val id: String,
    val title: String,
    /**
     * Last-message preview shown under the title. `null` renders the localized
     * "Updated recently" placeholder — recency metadata is a display concern,
     * not conversation data, so it must not be baked in as an English literal.
     */
    val snippet: String? = null,
)

data class HomeUiState(
    val isWorkspaceSetupDone: Boolean = true,
    val activeProject: HomeProjectSummary? = null,
    val runningCount: Int = 0,
    val waitingApprovalCount: Int = 0,
    val currentModelName: String = "Free FAST",
    val isModelLocal: Boolean = false,
    val isSandboxRunning: Boolean = true,
    val storageUsageText: String = "1.2 / 4 GB",
    val recentChats: List<HomeChatSummary> = emptyList(),
)

/**
 * UI v2 Home / Dashboard screen:
 * "Today at a glance" overview with Continue Project / Setup, Agent Activity,
 * Quick Start actions, System Status, and Recent Chats.
 */
@Composable
fun DoraiHomeScreen(
    state: HomeUiState,
    onNavigateToChat: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenModels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoAnimation(
                size = 28.dp,
                animate = true,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(Res.string.nav_brand_title),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1000.dp)
                    .fillMaxWidth()
                    .padding(DoraiSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
            ) {
                Text(
                    text = stringResource(Res.string.home_today_glance),
                    color = colors.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )

                // Responsive 2-column or 1-column grid
                if (widthClass == DoraiWindowWidthClass.Compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD)) {
                        ContinueOrSetupCard(state, onNavigateToWorkspace)
                        AgentActivityCard(state, onNavigateToWorkspace)
                        QuickStartCard(onNavigateToChat, onNavigateToWorkspace, onOpenModels)
                        StatusCard(state)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ContinueOrSetupCard(state, onNavigateToWorkspace)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                AgentActivityCard(state, onNavigateToWorkspace)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                QuickStartCard(onNavigateToChat, onNavigateToWorkspace, onOpenModels)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                StatusCard(state)
                            }
                        }
                    }
                }

                // Recent Chats section
                Text(
                    text = stringResource(Res.string.home_recent_chats),
                    color = colors.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = DoraiSpacing.SM),
                )

                RecentChatsCard(
                    recentChats = state.recentChats,
                    onOpenConversation = onOpenConversation,
                )
            }
        }
    }
}

@Composable
private fun ContinueOrSetupCard(
    state: HomeUiState,
    onNavigateToWorkspace: () -> Unit,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .doraiSurfaceStyle(shape = shape)
            .padding(DoraiSpacing.MD),
    ) {
        if (!state.isWorkspaceSetupDone || state.activeProject == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(DoraiSpacing.MD))
                Column {
                    Text(
                        text = stringResource(Res.string.home_setup_workspace),
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.home_setup_workspace_desc),
                        color = colors.muted,
                        fontSize = 13.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(DoraiSpacing.MD))
            Button(
                onClick = onNavigateToWorkspace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .doraiEdgeStyle(shape = RoundedCornerShape(12.dp), cornerRadius = 12.dp, edgeColor = colors.primary),
            ) {
                Text(
                    text = stringResource(Res.string.home_start_setup),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            val proj = state.activeProject
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(DoraiSpacing.MD))
                Column {
                    Text(
                        text = proj.name,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${proj.branch} · ${proj.changesCount} changes",
                        color = colors.muted,
                        fontSize = 13.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(DoraiSpacing.MD))
            Button(
                onClick = onNavigateToWorkspace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .doraiEdgeStyle(shape = RoundedCornerShape(12.dp), cornerRadius = 12.dp, edgeColor = colors.primary),
            ) {
                Text(
                    text = stringResource(Res.string.home_continue_project),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AgentActivityCard(
    state: HomeUiState,
    onNavigateToWorkspace: () -> Unit,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .doraiSurfaceStyle(shape = shape)
            .padding(DoraiSpacing.MD),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(DoraiSpacing.MD))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.runningCount} ",
                    color = colors.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    text = stringResource(Res.string.home_running_count),
                    color = colors.muted,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.width(DoraiSpacing.LG))
                Text(
                    text = "${state.waitingApprovalCount} ",
                    color = if (state.waitingApprovalCount > 0) colors.warn else colors.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    text = stringResource(Res.string.home_need_approval),
                    color = colors.muted,
                    fontSize = 13.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.MD))

        if (state.waitingApprovalCount > 0) {
            Button(
                onClick = onNavigateToWorkspace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surface2,
                    contentColor = colors.text,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.home_review_approvals),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            Text(
                text = stringResource(Res.string.home_no_approvals),
                color = colors.muted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun QuickStartCard(
    onNavigateToChat: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
    onOpenModels: () -> Unit,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .doraiSurfaceStyle(shape = shape)
            .padding(DoraiSpacing.MD),
    ) {
        Text(
            text = stringResource(Res.string.home_quick_start),
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
        ) {
            QuickChip(
                icon = Icons.AutoMirrored.Filled.Chat,
                label = stringResource(Res.string.home_new_chat),
                onClick = onNavigateToChat,
            )
            QuickChip(
                icon = Icons.Default.Folder,
                label = stringResource(Res.string.home_open_project),
                onClick = onNavigateToWorkspace,
            )
            QuickChip(
                icon = Icons.Default.Terminal,
                label = stringResource(Res.string.home_terminal),
                onClick = onNavigateToWorkspace,
            )
            QuickChip(
                icon = Icons.Default.Key,
                label = stringResource(Res.string.home_models),
                onClick = onOpenModels,
            )
        }
    }
}

@Composable
private fun QuickChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalDoraiColors.current
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(CircleShape)
            .background(colors.surface2)
            .border(1.dp, colors.line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .doraiSurfaceStyle(shape = shape)
            .padding(DoraiSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
    ) {
        Text(
            text = stringResource(Res.string.home_status),
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )

        // Model
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.home_model_label), color = colors.muted, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.currentModelName, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(6.dp))
                DoraiProviderBadge(isLocal = state.isModelLocal, iconOnly = true)
            }
        }

        // Sandbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.home_sandbox_label), color = colors.muted, fontSize = 13.sp)
            DoraiStatusBadge(
                text = if (state.isSandboxRunning) stringResource(Res.string.home_running) else stringResource(Res.string.home_stopped),
                color = if (state.isSandboxRunning) colors.ok else colors.muted,
            )
        }

        // Storage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.home_storage_label), color = colors.muted, fontSize = 13.sp)
            Text(state.storageUsageText, color = colors.text, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RecentChatsCard(
    recentChats: List<HomeChatSummary>,
    onOpenConversation: (String) -> Unit,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .doraiSurfaceStyle(shape = shape)
            .padding(DoraiSpacing.SM),
    ) {
        if (recentChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DoraiSpacing.LG),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.home_no_recent_chats),
                    color = colors.muted,
                    fontSize = 13.sp,
                )
            }
        } else {
            recentChats.take(3).forEach { chat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenConversation(chat.id) }
                        .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chat.title,
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = chat.snippet ?: stringResource(Res.string.app_updated_recently),
                            color = colors.muted,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
