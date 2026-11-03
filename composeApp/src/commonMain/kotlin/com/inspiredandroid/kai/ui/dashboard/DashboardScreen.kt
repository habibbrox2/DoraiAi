package com.inspiredandroid.kai.ui.dashboard

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.inspiredandroid.kai.BackIcon
import com.inspiredandroid.kai.WorkspaceController
import com.inspiredandroid.kai.SandboxController
import com.inspiredandroid.kai.ui.AccentPrimary
import com.inspiredandroid.kai.ui.BorderSoft
import com.inspiredandroid.kai.ui.CardFill
import com.inspiredandroid.kai.ui.KaiBorders
import com.inspiredandroid.kai.ui.KaiElevation
import com.inspiredandroid.kai.ui.KaiShapes
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.SurfaceRaised
import com.inspiredandroid.kai.ui.chat.ChatUiState
import com.inspiredandroid.kai.ui.chat.ConversationSummary
import com.inspiredandroid.kai.ui.components.CollectionEmptyState
import com.inspiredandroid.kai.ui.components.OperationalStatus
import com.inspiredandroid.kai.ui.components.StatusBadge
import com.inspiredandroid.kai.ui.components.rememberOperationalStatusColors
import com.inspiredandroid.kai.ui.glassmorphicCard
import com.inspiredandroid.kai.ui.glassmorphicCardBorder
import com.inspiredandroid.kai.ui.glassmorphicCardColors
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.kaiAdaptiveCardBorder
import com.inspiredandroid.kai.ui.kaiAdaptiveCardColors
import com.inspiredandroid.kai.ui.settings.SettingsTab
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
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
            .background(MaterialTheme.colorScheme.background),
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
                        contentDescription = "Back to chat",
                        tint = LocalDesignColorTokens.current.textPrimary,
                    )
                }
            }
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(KaiShapes.sm),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Brox Kai",
                    style = KaiTypography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
                Text(
                    text = "What are you building today?",
                    style = KaiTypography.bodyMedium,
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
            .glassmorphicCard(shape = KaiShapes.card),
        colors = glassmorphicCardColors(),
        border = glassmorphicCardBorder(),
        shape = KaiShapes.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val service = state.service) {
                null -> StatusBadge(
                    label = "Checking services",
                    status = OperationalStatus.Connecting,
                )

                else -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatusBadge(
                        label = if (service.isSharedKey) "Shared free service" else "Ready",
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
                                text = "Set up a service",
                                style = KaiTypography.labelLarge,
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
                            style = KaiTypography.bodyMedium,
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
                            text = fact.label,
                            style = KaiTypography.bodyMedium,
                            color = LocalDesignColorTokens.current.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        StatusBadge(label = fact.value, status = fact.status)
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
            title = "Chat",
            subtitle = "Ask, plan, and write with your AI services",
            icon = Icons.AutoMirrored.Filled.Chat,
            isPrimary = true,
            isEnabled = true,
            disabledNote = null,
            onClick = onNavigateToChat,
        ),
        DashboardAction(
            title = "Workspace",
            subtitle = "Create apps in a Linux workspace on this device",
            icon = Icons.Filled.Build,
            isPrimary = false,
            isEnabled = state.isBuildAvailable,
            disabledNote = "Workspace runs on Android devices",
            onClick = { onOpenBuild("") },
        ),
        DashboardAction(
            title = "Design",
            subtitle = "Start a guided design conversation",
            icon = Icons.Filled.Palette,
            isPrimary = false,
            isEnabled = state.isDesignAvailable,
            disabledNote = "Coming soon",
            onClick = { onStartDesign(DESIGN_STARTER_PROMPT) },
        ),
        DashboardAction(
            title = "Tools",
            subtitle = "Configure services, tools, and the sandbox",
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

    val cardShape = KaiShapes.card

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
                        .clip(KaiShapes.sm)
                        .background(iconBg)
                        .border(
                            BorderStroke(1.dp, if (action.isPrimary) AccentPrimary.copy(alpha = 0.4f) else BorderSoft),
                            KaiShapes.sm,
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
                        style = KaiTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalDesignColorTokens.current.textPrimary,
                    )
                    Text(
                        text = action.subtitle,
                        style = KaiTypography.bodySmall,
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
                        .clip(KaiShapes.sm)
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
                        style = KaiTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalDesignColorTokens.current.textPrimary.copy(alpha = 0.7f),
                    )
                    Text(
                        text = action.subtitle,
                        style = KaiTypography.bodySmall,
                        color = LocalDesignColorTokens.current.textTertiary.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (action.disabledNote != null) {
                        Text(
                            text = action.disabledNote,
                            style = KaiTypography.labelSmall,
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
            shape = KaiShapes.card,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CollectionEmptyState(
                    message = "No saved work yet — conversations and build projects will show up here.",
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
                        text = "Recent conversations",
                        style = KaiTypography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalDesignColorTokens.current.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onViewAllConversations,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(
                            text = "View all",
                            style = KaiTypography.labelLarge,
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
                        text = "Recent projects",
                        style = KaiTypography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalDesignColorTokens.current.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onViewAllProjects,
                        modifier = Modifier.handCursor(),
                    ) {
                        Text(
                            text = "View all",
                            style = KaiTypography.labelLarge,
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
    val cardShape = KaiShapes.row

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
                    .clip(KaiShapes.sm)
                    .background(AccentPrimary.copy(alpha = 0.14f))
                    .border(BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f)), KaiShapes.sm),
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
                    style = KaiTypography.bodyLarge,
                    color = LocalDesignColorTokens.current.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatDashboardDate(conversation.updatedAt),
                    style = KaiTypography.bodySmall,
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
    val cardShape = KaiShapes.row

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
                    .clip(KaiShapes.sm)
                    .background(SurfaceRaised.copy(alpha = 0.8f))
                    .border(BorderStroke(1.dp, BorderSoft), KaiShapes.sm),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Build,
                    contentDescription = "Project",
                    tint = LocalDesignColorTokens.current.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = project,
                style = KaiTypography.bodyLarge,
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




