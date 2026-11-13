@file:OptIn(ExperimentalMaterial3Api::class)

package com.dorai.ai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.BackIcon
import com.dorai.ai.Version
import com.dorai.ai.data.EmailAccount
import com.dorai.ai.data.Service
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.BorderSoft
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.components.VerticalScrollbarForScroll
import com.dorai.ai.ui.doraiAdaptiveCardBorder
import com.dorai.ai.ui.doraiAdaptiveCardColors
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_navigate_back
import dorai.composeapp.generated.resources.settings_ai_mistakes_warning
import dorai.composeapp.generated.resources.settings_licenses
import dorai.composeapp.generated.resources.settings_licenses_close
import dorai.composeapp.generated.resources.settings_screen_title
import dorai.composeapp.generated.resources.settings_tab_account
import dorai.composeapp.generated.resources.settings_tab_agent
import dorai.composeapp.generated.resources.settings_tab_appearance
import dorai.composeapp.generated.resources.settings_tab_data
import dorai.composeapp.generated.resources.settings_tab_general
import dorai.composeapp.generated.resources.settings_tab_github
import dorai.composeapp.generated.resources.settings_tab_services
import dorai.composeapp.generated.resources.settings_tab_tools
import dorai.composeapp.generated.resources.snackbar_email_removed
import dorai.composeapp.generated.resources.snackbar_mcp_server_removed
import dorai.composeapp.generated.resources.snackbar_memory_deleted
import dorai.composeapp.generated.resources.snackbar_service_removed
import dorai.composeapp.generated.resources.snackbar_skill_removed
import dorai.composeapp.generated.resources.snackbar_task_cancelled
import dorai.composeapp.generated.resources.snackbar_undo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

internal val StatusColorConnected = SettingsStatusColors.connected
internal val StatusColorChecking = SettingsStatusColors.checking
internal val StatusColorError = SettingsStatusColors.error
internal val StatusColorUnknown = SettingsStatusColors.unknown

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    sandboxViewModel: SandboxViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    initialSettingsTab: SettingsTab? = null,
    navigationTabBar: (@Composable () -> Unit)? = null,
    androidUpdateContent: (@Composable () -> Unit)? = null,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val sandboxState by sandboxViewModel.state.collectAsStateWithLifecycle()

    // A dashboard deep link (Plan 6) selects its tab once; later switches
    // stay with the user.
    LaunchedEffect(initialSettingsTab) {
        val tab = initialSettingsTab ?: return@LaunchedEffect
        if (viewModel.state.value.currentTab != tab) {
            viewModel.actions.onSelectTab(tab)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenVisible()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsScreenContent(
        uiState = uiState,
        actions = viewModel.actions,
        sandboxState = sandboxState,
        onNavigateBack = onNavigateBack,
        navigationTabBar = navigationTabBar,
        androidUpdateContent = androidUpdateContent,
    )
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    actions: SettingsActions = SettingsActions.NoOp,
    sandboxState: SandboxUiState = SandboxUiState(),
    onNavigateBack: () -> Unit = {},
    navigationTabBar: (@Composable () -> Unit)? = null,
    androidUpdateContent: (@Composable () -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(Res.string.snackbar_undo)
    val memoryDeletedMsg = stringResource(Res.string.snackbar_memory_deleted)
    val taskCancelledMsg = stringResource(Res.string.snackbar_task_cancelled)
    val emailRemovedMsg = stringResource(Res.string.snackbar_email_removed)
    val serviceRemovedMsg = stringResource(Res.string.snackbar_service_removed)
    val mcpServerRemovedMsg = stringResource(Res.string.snackbar_mcp_server_removed)
    val skillRemovedMsg = stringResource(Res.string.snackbar_skill_removed)

    val pendingDeletionState = androidx.compose.runtime.rememberUpdatedState(uiState.pendingDeletion)
    LaunchedEffect(uiState.pendingDeletion) {
        val deletion = uiState.pendingDeletion ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val message = when (deletion) {
            is PendingDeletion.Memory -> memoryDeletedMsg
            is PendingDeletion.Task -> taskCancelledMsg
            is PendingDeletion.EmailAccount -> emailRemovedMsg
            is PendingDeletion.Service -> serviceRemovedMsg
            is PendingDeletion.McpServer -> mcpServerRemovedMsg
            is PendingDeletion.Skill -> skillRemovedMsg
        }
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        // Only undo the deletion we actually showed; if a newer deletion is
        // already pending, the snackbar action is stale and must be ignored.
        if (result == SnackbarResult.ActionPerformed && pendingDeletionState.value == deletion) {
            actions.onUndoDelete()
        }
    }

    val pendingDeletion = uiState.pendingDeletion
    val filteredMemories = remember(uiState.memories, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.Memory) uiState.memories.filter { it.key != pendingDeletion.key }.toImmutableList() else uiState.memories
    }
    val filteredTasks = remember(uiState.scheduledTasks, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.Task) uiState.scheduledTasks.filter { it.id != pendingDeletion.id }.toImmutableList() else uiState.scheduledTasks
    }
    val filteredEmailAccounts = remember(uiState.emailAccounts, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.EmailAccount) uiState.emailAccounts.filter { it.id != pendingDeletion.id }.toImmutableList() else uiState.emailAccounts
    }
    val filteredServices = remember(uiState.configuredServices, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.Service) uiState.configuredServices.filter { it.instanceId != pendingDeletion.instanceId }.toImmutableList() else uiState.configuredServices
    }
    val filteredMcpServers = remember(uiState.mcpServers, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.McpServer) uiState.mcpServers.filter { it.id != pendingDeletion.serverId }.toImmutableList() else uiState.mcpServers
    }
    val filteredSkills = remember(uiState.skills, pendingDeletion) {
        if (pendingDeletion is PendingDeletion.Skill) uiState.skills.filter { it.id != pendingDeletion.id }.toImmutableList() else uiState.skills
    }

    val filteredUiState = remember(uiState, filteredMemories, filteredTasks, filteredEmailAccounts, filteredServices, filteredMcpServers, filteredSkills) {
        uiState.copy(
            memories = filteredMemories,
            scheduledTasks = filteredTasks,
            emailAccounts = filteredEmailAccounts,
            configuredServices = filteredServices,
            mcpServers = filteredMcpServers,
            skills = filteredSkills,
        )
    }

    // Use OLED black background for pure dark theme
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(Modifier.fillMaxSize().background(backgroundColor).navigationBarsPadding().statusBarsPadding().imePadding()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val expanded = maxWidth >= 840.dp
            val visibleTabs = remember {
                SettingsTab.entries.toImmutableList()
            }
            if (expanded) {
                Row(Modifier.fillMaxSize()) {
                    SettingsNavRail(
                        tabs = visibleTabs,
                        currentTab = filteredUiState.currentTab,
                        onSelectTab = actions.onSelectTab,
                    )
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        if (navigationTabBar != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = CenterVertically,
                            ) {
                                navigationTabBar()
                            }
                        } else {
                            TopBar(onNavigateBack = onNavigateBack)
                        }
                        SettingsDetailPane(
                            uiState = filteredUiState,
                            actions = actions,
                            sandboxState = sandboxState,
                            androidUpdateContent = androidUpdateContent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = CenterHorizontally) {
                    if (navigationTabBar != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = CenterVertically,
                        ) {
                            navigationTabBar()
                        }
                    } else {
                        TopBar(onNavigateBack = onNavigateBack)
                    }
                    SettingsTabSelector(
                        tabs = visibleTabs,
                        currentTab = filteredUiState.currentTab,
                        onSelectTab = actions.onSelectTab,
                    )
                    SettingsDetailPane(
                        uiState = filteredUiState,
                        actions = actions,
                        sandboxState = sandboxState,
                        androidUpdateContent = androidUpdateContent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
    val barBg = LocalDesignColorTokens.current.surfaceElevated

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(SettingsShapes.panel)
            .background(barBg)
            .border(
                BorderStroke(1.dp, BorderSoft.copy(alpha = 0.6f)),
                SettingsShapes.panel,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .handCursor(),
                onClick = onNavigateBack,
            ) {
                Icon(
                    imageVector = BackIcon,
                    contentDescription = stringResource(Res.string.a11y_navigate_back),
                    tint = LocalDesignColorTokens.current.textPrimary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.settings_screen_title),
                style = DoraiTypography.titleMedium,
                color = LocalDesignColorTokens.current.textPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SettingsTabSelector(
    tabs: ImmutableList<SettingsTab>,
    currentTab: SettingsTab,
    onSelectTab: (SettingsTab) -> Unit,
) {
    val scrollState = rememberScrollState()
    // The strip is wider than the screen at large font scales, and the default tab
    // (Services) sits far enough along that it opens half off the right edge. Track
    // each pill's bounds so the selected one can be scrolled into view.
    var viewportWidth by remember { mutableStateOf(0) }
    val tabBounds = remember { mutableStateMapOf<SettingsTab, IntRange>() }

    // Focus management for keyboard navigation
    val tabFocusRequesters = remember { tabs.map { _ -> FocusRequester() } }
    var focusedTabIndex by remember { mutableStateOf(tabs.indexOf(currentTab).coerceAtLeast(0)) }

    LaunchedEffect(currentTab, viewportWidth, tabBounds[currentTab], scrollState.maxValue) {
        val bounds = tabBounds[currentTab] ?: return@LaunchedEffect
        val target = settingsTabScrollTarget(
            currentScroll = scrollState.value,
            viewportWidth = viewportWidth,
            maxScroll = scrollState.maxValue,
            itemStart = bounds.first,
            itemEnd = bounds.last,
        ) ?: return@LaunchedEffect
        if (prefersReducedMotion()) {
            scrollState.scrollTo(target)
        } else {
            scrollState.animateScrollTo(target)
        }
    }

    // Sync focused tab with selected tab
    LaunchedEffect(currentTab) {
        focusedTabIndex = tabs.indexOf(currentTab).coerceAtLeast(0)
    }

    // Neural minimal tab bar container
    val tabBarBg = LocalDesignColorTokens.current.surfaceElevated

    Box(
        modifier = Modifier
            .widthIn(max = 960.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(SettingsShapes.pill)
            .background(tabBarBg)
            .border(
                BorderStroke(
                    1.dp,
                    BorderSoft.copy(alpha = 0.6f),
                ),
                SettingsShapes.pill,
            )
            .onKeyEvent { event ->
                if (tabs.isEmpty()) return@onKeyEvent false

                when (event.key) {
                    Key.DirectionLeft -> {
                        val nextIndex = (focusedTabIndex - 1).coerceAtLeast(0)
                        focusedTabIndex = nextIndex
                        tabFocusRequesters[nextIndex].requestFocus()
                        onSelectTab(tabs[nextIndex])
                        true
                    }

                    Key.DirectionRight -> {
                        val nextIndex = (focusedTabIndex + 1).coerceAtMost(tabs.lastIndex)
                        focusedTabIndex = nextIndex
                        tabFocusRequesters[nextIndex].requestFocus()
                        onSelectTab(tabs[nextIndex])
                        true
                    }

                    Key.Enter, Key.NumPadEnter -> {
                        tabFocusRequesters[focusedTabIndex].requestFocus()
                        onSelectTab(tabs[focusedTabIndex])
                        true
                    }

                    else -> false
                }
            },
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .height(44.dp)
                .onSizeChanged { viewportWidth = it.width }
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = currentTab == tab
                val isFocused = focusedTabIndex == index

                val label = when (tab) {
                    SettingsTab.General -> stringResource(Res.string.settings_tab_general)
                    SettingsTab.Appearance -> stringResource(Res.string.settings_tab_appearance)
                    SettingsTab.Agent -> stringResource(Res.string.settings_tab_agent)
                    SettingsTab.Services -> stringResource(Res.string.settings_tab_services)
                    SettingsTab.Tools -> stringResource(Res.string.settings_tab_tools)
                    SettingsTab.GitHub -> stringResource(Res.string.settings_tab_github)
                    SettingsTab.Account -> stringResource(Res.string.settings_tab_account)
                }

                val buttonColors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) {
                        com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.18f)
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (isSelected) {
                        com.dorai.ai.ui.AccentPrimary
                    } else {
                        LocalDesignColorTokens.current.textSecondary
                    },
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = LocalDesignColorTokens.current.textSecondary.copy(alpha = 0.5f),
                )

                val border = when {
                    isFocused -> androidx.compose.foundation.BorderStroke(1.dp, com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.55f))
                    isSelected -> androidx.compose.foundation.BorderStroke(1.dp, com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.38f))
                    else -> null
                }

                Button(
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val start = coordinates.positionInParent().x.toInt()
                            tabBounds[tab] = start..(start + coordinates.size.width)
                        }
                        .handCursor()
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                        .focusRequester(tabFocusRequesters[index])
                        .onFocusChanged { focused ->
                            if (focused.isFocused) {
                                focusedTabIndex = index
                            }
                        }
                        .semantics {
                            role = androidx.compose.ui.semantics.Role.Tab
                            selected = isSelected
                        },
                    colors = buttonColors,
                    shape = SettingsShapes.pill,
                    border = border,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isSelected) {
                            com.dorai.ai.ui.AccentPrimary
                        } else {
                            LocalDesignColorTokens.current.textSecondary
                        },
                        style = DoraiTypography.labelLarge,
                        maxLines = 1,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BottomInfo() {
    Text(
        text = stringResource(Res.string.settings_ai_mistakes_warning),
        style = DoraiTypography.bodySmall,
        textAlign = TextAlign.Center,
        color = LocalDesignColorTokens.current.textSecondary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

// The Android app ships GPL-2.0 PRoot and LGPL-3.0 talloc binaries, whose licenses
// require their text and a source offer to travel with the app itself.
@Suppress("UnusedPrivateMember")
@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    val notices by produceState<String?>(null) {
        value = Res.readBytes("files/licenses/THIRD_PARTY_NOTICES.txt").decodeToString()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_licenses)) },
        text = {
            Text(
                text = notices.orEmpty(),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                style = DoraiTypography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.handCursor()) {
                Text(stringResource(Res.string.settings_licenses_close))
            }
        },
    )
}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    innerPadding: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cardBg = LocalDesignColorTokens.current.surfaceElevated
    val cardBorder = BorderStroke(1.dp, LocalDesignColorTokens.current.borderSubtle)
    val cardShape = SettingsShapes.card

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = cardShape,
                ambientColor = com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.06f),
                spotColor = com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.1f),
            )
            .clip(cardShape)
            .background(cardBg)
            .border(cardBorder, cardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button).handCursor() else Modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (innerPadding) Modifier.padding(SettingsSpacing.xl) else Modifier),
        ) {
            content()
        }
    }
}

/**
 * The two-column-on-wide, one-column-on-narrow arrangement the settings tabs share.
 *
 * Each section is declared exactly once. [start] and [end] are the two columns a window of at
 * least 600.dp gets; below that they run into a single column, [start] first. Writing a tab as
 * two independent branches instead is what let the same section drift into two different
 * argument lists, so the branch lives here and the tabs only say what goes in which column.
 */
@Composable
internal fun StaggeredSettingsColumns(
    start: @Composable ColumnScope.() -> Unit,
    end: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionGap),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.cardGap),
                    content = start,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.cardGap),
                    content = end,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(SettingsSpacing.cardGap)) {
                start()
                end()
            }
        }
    }
}

@Composable
internal fun ToggleableHeadline(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val switchInteractionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = switchInteractionSource,
                indication = LocalIndication.current,
                role = Role.Switch,
            ) { onCheckedChange(!checked) }
            .handCursor(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DoraiTypography.titleMedium,
                color = LocalDesignColorTokens.current.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = description,
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
        }
        actions()
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = null,
            interactionSource = switchInteractionSource,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = com.dorai.ai.ui.AccentPrimary,
                checkedTrackColor = com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.32f),
                uncheckedThumbColor = LocalDesignColorTokens.current.textSecondary,
                uncheckedTrackColor = BorderSoft,
            ),
        )
    }
}
