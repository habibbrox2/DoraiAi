package com.dorai.ai.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.BackIcon
import com.dorai.ai.SandboxController
import com.dorai.ai.WorkspaceController
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.BorderSoft
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DoraiBorders
import com.dorai.ai.ui.DoraiElevation
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.chat.ChatUiState
import com.dorai.ai.ui.chat.ConversationSummary
import com.dorai.ai.ui.components.CollectionEmptyState
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.ui.components.StatusBadge
import com.dorai.ai.ui.components.rememberOperationalStatusColors
import com.dorai.ai.ui.doraiAdaptiveCardBorder
import com.dorai.ai.ui.doraiAdaptiveCardColors
import com.dorai.ai.ui.glassmorphicCard
import com.dorai.ai.ui.glassmorphicCardBorder
import com.dorai.ai.ui.glassmorphicCardColors
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.settings.SettingsTab
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_back_to_chat
import dorai.composeapp.generated.resources.a11y_project
import dorai.composeapp.generated.resources.dash_card_chat
import dorai.composeapp.generated.resources.dash_card_chat_desc
import dorai.composeapp.generated.resources.dash_card_design
import dorai.composeapp.generated.resources.dash_card_design_desc
import dorai.composeapp.generated.resources.dash_card_tools
import dorai.composeapp.generated.resources.dash_card_tools_desc
import dorai.composeapp.generated.resources.dash_card_workspace
import dorai.composeapp.generated.resources.dash_card_workspace_desc
import dorai.composeapp.generated.resources.dash_checking_services
import dorai.composeapp.generated.resources.dash_coming_soon
import dorai.composeapp.generated.resources.dash_no_service_configured
import dorai.composeapp.generated.resources.dash_shared_free_service
import dorai.composeapp.generated.resources.dash_workspace_android_only
import dorai.composeapp.generated.resources.dashboard_empty_no_saved_work
import dorai.composeapp.generated.resources.dashboard_recent_conversations
import dorai.composeapp.generated.resources.dashboard_recent_projects
import dorai.composeapp.generated.resources.dashboard_setup_service
import dorai.composeapp.generated.resources.dashboard_subtitle
import dorai.composeapp.generated.resources.dashboard_title
import dorai.composeapp.generated.resources.dashboard_view_all
import dorai.composeapp.generated.resources.logo
import dorai.composeapp.generated.resources.status_ready
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private fun formatDashboardDate(epochMillis: Long): String = try {
    kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)
        .toString()
        .take(10)
} catch (_: Exception) {
    ""
}

@Composable
fun DashboardScreen(
    chatUiState: ChatUiState,
    isBuildAvailable: Boolean,
    isSandboxAvailable: Boolean,
    isDesignAvailable: Boolean,
    onNavigateToChat: () -> Unit,
    onOpenBuild: (String) -> Unit,
    onOpenSettings: (SettingsTab) -> Unit,
    onStartDesign: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    navigationTabBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val sandboxController = koinInject<SandboxController>()
    val buildController = koinInject<WorkspaceController>()
    val sandboxStatus by sandboxController.status.collectAsStateWithLifecycle()
    val buildState by buildController.state.collectAsStateWithLifecycle()

    DashboardScreenContent(
        state = buildDashboardUiState(
            chatUiState = chatUiState,
            sandboxStatus = sandboxStatus,
            buildState = buildState,
            isBuildAvailable = isBuildAvailable,
            isSandboxAvailable = isSandboxAvailable,
            isDesignAvailable = isDesignAvailable,
        ),
        onNavigateToChat = onNavigateToChat,
        onOpenBuild = onOpenBuild,
        onOpenSettings = onOpenSettings,
        onStartDesign = onStartDesign,
        onOpenConversation = onOpenConversation,
        onNavigateBack = onNavigateBack,
        navigationTabBar = navigationTabBar,
        modifier = modifier,
    )
}

internal fun dashboardColumnsFor(width: Dp): Int = when {
    width < 600.dp -> 1
    width < 1000.dp -> 2
    else -> 4
}

@Composable
private fun FactText.resolve(): String = when (this) {
    is FactText.Raw -> text
    is FactText.Localized -> stringResource(resource)
    is FactText.LocalizedWithParam -> stringResource(resource, param)
}

@Composable
fun DashboardScreenContent(
    state: DashboardUiState,
    onNavigateToChat: () -> Unit,
    onOpenBuild: (String) -> Unit,
    onOpenSettings: (SettingsTab) -> Unit,
    onStartDesign: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    navigationTabBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val columns = dashboardColumnsFor(maxWidth)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (columns == 1) 16.dp else 32.dp)
                .padding(vertical = if (columns == 1) 16.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            DashboardHeader(
                navigationTabBar = navigationTabBar,
                onNavigateBack = onNavigateBack,
            )
            ReadinessCard(state = state, onOpenSettings = onOpenSettings)
            DashboardActionGrid(
                state = state,
                columns = columns,
                onNavigateToChat = onNavigateToChat,
                onOpenBuild = onOpenBuild,
                onOpenSettings = onOpenSettings,
                onStartDesign = onStartDesign,
            )
            RecentWorkSection(
                state = state,
                onOpenConversation = onOpenConversation,
                onOpenBuild = onOpenBuild,
                onViewAllConversations = onNavigateToChat,
                onViewAllProjects = { onOpenBuild("") },
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    navigationTabBar: (@Composable () -> Unit)?,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val brand: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .handCursor()
                        .size(48.dp),
                ) {
                    Icon(
                        imageVector = BackIcon,
                        contentDescription = stringResource(Res.string.a11y_back_to_chat),
                        tint = LocalDesignColorTokens.current.textPrimary,
                    )
                }
            }
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(DoraiShapes.sm),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.dashboard_title),
                    style = DoraiTypography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Text(
                    text = stringResource(Res.string.dashboard_subtitle),
                    style = DoraiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textTertiary,
                )
            }
        }
    }
    if (navigationTabBar == null) {
        Box(modifier = modifier.fillMaxWidth()) {
            brand()
        }
    } else {
        Box(
            modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp),
        ) {
            Row(modifier = Modifier.align(Alignment.CenterStart)) {
                brand()
            }
            Box(modifier = Modifier.align(Alignment.Center)) {
                navigationTabBar()
            }
        }
    }
}

@Composable
private fun ReadinessCard(
    state: DashboardUiState,
    onOpenSettings: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphicCard(shape = DoraiShapes.card),
        colors = glassmorphicCardColors(),
        border = glassmorphicCardBorder(),
        shape = DoraiShapes.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val service = state.service) {
                null -> if (state.isCheckingServices) {
                    StatusBadge(
                        label = stringResource(Res.string.dash_checking_services),
                        status = OperationalStatus.Connecting,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatusBadge(
                            label = stringResource(Res.string.dash_no_service_configured),
                            status = OperationalStatus.NeedsSetup,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { onOpenSettings(SettingsTab.Services) },
                            modifier = Modifier.handCursor(),
                        ) {
                            Text(
                                text = stringResource(Res.string.dashboard_setup_service),
                                style = DoraiTypography.labelLarge,
                                color = AccentPrimary,
                            )
                        }
                    }
                }

                else -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatusBadge(
                        label = if (service.isSharedKey) {
                            stringResource(Res.string.dash_shared_free_service)
                        } else {
                            stringResource(Res.string.status_ready)
                        },
                        status = if (service.isSharedKey) {
                            OperationalStatus.NeedsSetup
                        } else {
                            OperationalStatus.Ready
                        },
                    )
                    if (service.isSharedKey) {
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { onOpenSettings(SettingsTab.Services) },
                            modifier = Modifier.handCursor(),
                        ) {
                            Text(
                                text = stringResource(Res.string.dashboard_setup_service),
                                style = DoraiTypography.labelLarge,
                                color = AccentPrimary,
                            )
                        }
                    } else {
                        Text(
                            text = if (service.modelId.isBlank()) {
                                service.serviceName
                            } else {
                                "${service.serviceName} · ${service.modelId}"
                            },
                            style = DoraiTypography.bodyMedium,
                            color = LocalDesignColorTokens.current.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (state.facts.isNotEmpty()) {
                HorizontalDivider(
                    color = BorderSoft,
                    thickness = 1.dp,
                )
                state.facts.forEach { fact ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = fact.label.resolve(),
                            style = DoraiTypography.bodyMedium,
                            color = LocalDesignColorTokens.current.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        StatusBadge(label = fact.value.resolve(), status = fact.status)
                    }
                }
            }
        }
    }
}

private data class DashboardAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isPrimary: Boolean,
    val isEnabled: Boolean,
    val disabledNote: String?,
    val onClick: () -> Unit,
)

@Composable
private fun DashboardActionGrid(
    state: DashboardUiState,
    columns: Int,
    onNavigateToChat: () -> Unit,
    onOpenBuild: (String) -> Unit,
    onOpenSettings: (SettingsTab) -> Unit,
    onStartDesign: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        DashboardAction(
            title = stringResource(Res.string.dash_card_chat),
            subtitle = stringResource(Res.string.dash_card_chat_desc),
            icon = Icons.AutoMirrored.Filled.Chat,
            isPrimary = true,
            isEnabled = true,
            disabledNote = null,
            onClick = onNavigateToChat,
        ),
        DashboardAction(
            title = stringResource(Res.string.dash_card_workspace),
            subtitle = stringResource(Res.string.dash_card_workspace_desc),
            icon = Icons.Filled.Build,
            isPrimary = false,
            isEnabled = state.isBuildAvailable,
            disabledNote = stringResource(Res.string.dash_workspace_android_only),
            onClick = { onOpenBuild("") },
        ),
        DashboardAction(
            title = stringResource(Res.string.dash_card_design),
            subtitle = stringResource(Res.string.dash_card_design_desc),
            icon = Icons.Filled.Palette,
            isPrimary = false,
            isEnabled = state.isDesignAvailable,
            disabledNote = stringResource(Res.string.dash_coming_soon),
            onClick = { onStartDesign(DESIGN_STARTER_PROMPT) },
        ),
        DashboardAction(
            title = stringResource(Res.string.dash_card_tools),
            subtitle = stringResource(Res.string.dash_card_tools_desc),
            icon = Icons.Filled.Tune,
            isPrimary = false,
            isEnabled = true,
            disabledNote = null,
            onClick = { onOpenSettings(SettingsTab.Tools) },
        ),
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        actions.chunked(columns).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowActions.forEach { action ->
                    DashboardActionCard(
                        action = action,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                repeat(columns - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DashboardActionCard(
    action: DashboardAction,
    modifier: Modifier = Modifier,
) {
    val statusColors = rememberOperationalStatusColors()
    val iconBg = if (action.isPrimary) {
        AccentPrimary.copy(alpha = 0.16f)
    } else {
        SurfaceRaised.copy(alpha = 0.8f)
    }
    val iconTint = if (action.isPrimary) {
        AccentPrimary
    } else {
        LocalDesignColorTokens.current.textPrimary
    }
    val border = if (action.isPrimary) {
        BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, BorderSoft)
    }

    val cardShape = DoraiShapes.card

    if (action.isEnabled) {
        Button(
            onClick = action.onClick,
            modifier = modifier
                .handCursor()
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CardFill,
                contentColor = LocalDesignColorTokens.current.textPrimary,
                disabledContainerColor = CardFill.copy(alpha = 0.5f),
                disabledContentColor = LocalDesignColorTokens.current.textPrimary.copy(alpha = 0.4f),
            ),
            shape = cardShape,
            border = border,
            contentPadding = PaddingValues(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(DoraiShapes.sm)
                        .background(iconBg)
                        .border(
                            BorderStroke(1.dp, if (action.isPrimary) AccentPrimary.copy(alpha = 0.4f) else BorderSoft),
                            DoraiShapes.sm,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = action.title,
                        style = DoraiTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalDesignColorTokens.current.textPrimary,
                    )
                    Text(
                        text = action.subtitle,
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = LocalDesignColorTokens.current.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    } else {
        Card(
            modifier = modifier
                .alpha(0.5f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = CardFill),
            border = BorderStroke(1.dp, BorderSoft),
            shape = cardShape,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(DoraiShapes.sm)
                        .background(SurfaceRaised.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.title,
                        tint = LocalDesignColorTokens.current.textTertiary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = action.title,
                        style = DoraiTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalDesignColorTokens.current.textPrimary.copy(alpha = 0.7f),
                    )
                    Text(
                        text = action.subtitle,
                        style = DoraiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textTertiary.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (action.disabledNote != null) {
                        Text(
                            text = action.disabledNote,
                            style = DoraiTypography.labelSmall,
                            color = statusColors.needsSetup,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentWorkSection(
    state: DashboardUiState,
    onOpenConversation: (String) -> Unit,
    onOpenBuild: (String) -> Unit,
    onViewAllConversations: () -> Unit,
    onViewAllProjects: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conversations = state.recentConversations.take(4)
    val projects = state.recentProjects.take(4)

    if (conversations.isEmpty() && projects.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardFill),
            border = BorderStroke(1.dp, BorderSoft),
            shape = DoraiShapes.card,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CollectionEmptyState(
                    message = stringResource(Res.string.dashboard_empty_no_saved_work),
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (conversations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.dashboard_recent_conversations),
                        style = DoraiTypography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalDesignColorTokens.current.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onViewAllConversations,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(
                            text = stringResource(Res.string.dashboard_view_all),
                            style = DoraiTypography.labelLarge,
                            color = AccentPrimary,
                        )
                    }
                }
                conversations.forEach { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { onOpenConversation(conversation.id) },
                    )
                }
            }
        }
        if (projects.isNotEmpty() && state.isBuildAvailable) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.dashboard_recent_projects),
                        style = DoraiTypography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalDesignColorTokens.current.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onViewAllProjects,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(
                            text = stringResource(Res.string.dashboard_view_all),
                            style = DoraiTypography.labelLarge,
                            color = AccentPrimary,
                        )
                    }
                }
                projects.forEach { project ->
                    ProjectRow(
                        project = project,
                        onClick = { onOpenBuild(project) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = DoraiShapes.row

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .handCursor(),
        colors = ButtonDefaults.buttonColors(
            containerColor = CardFill,
            contentColor = LocalDesignColorTokens.current.textPrimary,
        ),
        shape = cardShape,
        border = BorderStroke(1.dp, BorderSoft),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(DoraiShapes.sm)
                    .background(AccentPrimary.copy(alpha = 0.14f))
                    .border(BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f)), DoraiShapes.sm),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = conversation.title,
                    style = DoraiTypography.bodyLarge,
                    color = LocalDesignColorTokens.current.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatDashboardDate(conversation.updatedAt),
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textTertiary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = LocalDesignColorTokens.current.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ProjectRow(
    project: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = DoraiShapes.row

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .handCursor(),
        colors = ButtonDefaults.buttonColors(
            containerColor = CardFill,
            contentColor = LocalDesignColorTokens.current.textPrimary,
        ),
        shape = cardShape,
        border = BorderStroke(1.dp, BorderSoft),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(DoraiShapes.sm)
                    .background(SurfaceRaised.copy(alpha = 0.8f))
                    .border(BorderStroke(1.dp, BorderSoft), DoraiShapes.sm),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Build,
                    contentDescription = stringResource(Res.string.a11y_project),
                    tint = LocalDesignColorTokens.current.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = project,
                style = DoraiTypography.bodyLarge,
                color = LocalDesignColorTokens.current.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = LocalDesignColorTokens.current.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
