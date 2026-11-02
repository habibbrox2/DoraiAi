@file:OptIn(
    ExperimentalFoundationApi::class,
)

package com.inspiredandroid.kai.ui.chat

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inspiredandroid.kai.BackIcon
import com.inspiredandroid.kai.TerminalLine
import com.inspiredandroid.kai.data.Service
import com.inspiredandroid.kai.data.supportsAgenticFlows
import com.inspiredandroid.kai.getBackgroundDispatcher
import com.inspiredandroid.kai.onDragAndDropEventDropped
import com.inspiredandroid.kai.ui.CardFill
import com.inspiredandroid.kai.ui.DangerAlert
import com.inspiredandroid.kai.ui.KaiShapes
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.SurfaceRaised
import com.inspiredandroid.kai.ui.build.KaiBuildScreen
import com.inspiredandroid.kai.ui.chat.composables.AppSidebar
import com.inspiredandroid.kai.ui.chat.composables.BotMessage
import com.inspiredandroid.kai.ui.chat.composables.ChatComposer
import com.inspiredandroid.kai.ui.chat.composables.ChatTimeline
import com.inspiredandroid.kai.ui.chat.composables.EmptyState
import com.inspiredandroid.kai.ui.chat.composables.ErrorMessage
import com.inspiredandroid.kai.ui.chat.composables.HeartbeatBanner
import com.inspiredandroid.kai.ui.chat.composables.PendingSmsBanners
import com.inspiredandroid.kai.ui.chat.composables.QuestionInput
import com.inspiredandroid.kai.ui.chat.composables.TopBar
import com.inspiredandroid.kai.ui.chat.composables.UserMessage
import com.inspiredandroid.kai.ui.chat.composables.premiumChatBackdrop
import com.inspiredandroid.kai.ui.chat.composables.uiErrorText
import com.inspiredandroid.kai.ui.components.VerticalScrollbarForList
import com.inspiredandroid.kai.ui.dynamicui.toSpeakableText
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.sandbox.SandboxTabsContent
import com.inspiredandroid.kai.ui.settings.SandboxUiState
import com.inspiredandroid.kai.ui.settings.SandboxViewModel
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.fallback_answered_by
import kai.composeapp.generated.resources.fallback_service_failed
import kai.composeapp.generated.resources.fallback_trying_next
import kai.composeapp.generated.resources.ic_refresh
import kai.composeapp.generated.resources.ic_stop
import kai.composeapp.generated.resources.interactive_back_content_description
import kai.composeapp.generated.resources.scroll_to_bottom_content_description
import kai.composeapp.generated.resources.snackbar_conversation_deleted
import kai.composeapp.generated.resources.snackbar_undo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.errors.TextToSpeechSynthesisInterruptedError
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel(),
    textToSpeech: TextToSpeechInstance?,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: (() -> Unit)? = null,
    isSandboxAvailable: Boolean = false,
    isKaiBuildAvailable: Boolean = false,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    ChatScreenContent(
        uiState = uiState,
        textToSpeech = textToSpeech,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToDashboard = onNavigateToDashboard,
        isSandboxAvailable = isSandboxAvailable,
        isKaiBuildAvailable = isKaiBuildAvailable,
    )
}

@Composable
fun ChatScreenContent(
    uiState: ChatUiState,
    textToSpeech: TextToSpeechInstance? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDashboard: (() -> Unit)? = null,
    isSandboxAvailable: Boolean = false,
    isKaiBuildAvailable: Boolean = false,
    initialSandboxOpen: Boolean = false,
    previewSandboxState: SandboxUiState? = null,
    previewSandboxLines: ImmutableList<TerminalLine> = persistentListOf(),
) {
    // Kai Build is a transient surface, not a conversation — unlike interactive
    // mode it needs no persistence beyond surviving configuration changes.
    var isKaiBuildOpen by rememberSaveable { mutableStateOf(false) }

    // Shared text opens a new chat; leave Kai Build so the composer is visible.
    LaunchedEffect(uiState.composerPrefill) {
        if (uiState.composerPrefill != null && isKaiBuildOpen) {
            isKaiBuildOpen = false
        }
    }

    when {
        isKaiBuildOpen -> {
            com.inspiredandroid.kai.PlatformBackHandler(true) { isKaiBuildOpen = false }
            KaiBuildScreen(onExit = { isKaiBuildOpen = false })
        }

        else -> ChatModeScreen(
            uiState = uiState,
            textToSpeech = textToSpeech,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDashboard = onNavigateToDashboard,
            isSandboxAvailable = isSandboxAvailable,
            onOpenKaiBuild = { isKaiBuildOpen = true }.takeIf { isKaiBuildAvailable },
            initialSandboxOpen = initialSandboxOpen,
            previewSandboxState = previewSandboxState,
            previewSandboxLines = previewSandboxLines,
        )
    }
}

// --- Regular Chat Mode ---

@Composable
private fun ChatModeScreen(
    uiState: ChatUiState,
    textToSpeech: TextToSpeechInstance?,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: (() -> Unit)?,
    isSandboxAvailable: Boolean,
    onOpenKaiBuild: (() -> Unit)?,
    initialSandboxOpen: Boolean = false,
    previewSandboxState: SandboxUiState? = null,
    previewSandboxLines: ImmutableList<TerminalLine> = persistentListOf(),
) {
    var showHistorySheet by remember { mutableStateOf(false) }
    var isSandboxOpen by rememberSaveable { mutableStateOf(initialSandboxOpen) }
    // Hoisted here so the draft survives toggling the sandbox/terminal view, which
    // removes QuestionInput from composition and would otherwise drop the text.
    var questionInputText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 1. Close Sandbox if open
    com.inspiredandroid.kai.PlatformBackHandler(enabled = isSandboxOpen) {
        isSandboxOpen = false
    }

    // 2. Close Sidebar Drawer if open
    com.inspiredandroid.kai.PlatformBackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // 3. Exit interactive mode if active
    com.inspiredandroid.kai.PlatformBackHandler(enabled = uiState.isInteractiveMode) {
        uiState.actions.setInteractiveMode(false)
    }

    // 4. Double-back to exit on root Chat screen
    var lastBackPressTime by remember { mutableStateOf(0L) }
    val isRootChatActive = !drawerState.isOpen && !isSandboxOpen && !uiState.isInteractiveMode
    com.inspiredandroid.kai.PlatformBackHandler(enabled = isRootChatActive) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        if (now - lastBackPressTime < 2000L) {
            com.inspiredandroid.kai.exitApp()
        } else {
            lastBackPressTime = now
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = "Press back again to exit",
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    LaunchedEffect(uiState.composerPrefill) {
        val prefill = uiState.composerPrefill ?: return@LaunchedEffect
        questionInputText = TextFieldValue(
            text = prefill,
            selection = TextRange(prefill.length),
        )
        if (isSandboxOpen) isSandboxOpen = false
        uiState.actions.consumeComposerPrefill()
    }

    // When the active conversation changes (e.g. user starts a new chat from the
    // top bar or taps the heartbeat banner), collapse the sandbox view so the
    // user lands on the chat they just opened. Tracking the previous id avoids
    // firing on the initial composition — important when returning from Settings,
    // where rememberSaveable has just restored isSandboxOpen.
    var lastConversationId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.currentConversationId) {
        if (lastConversationId != null && lastConversationId != uiState.currentConversationId) {
            if (isSandboxOpen) isSandboxOpen = false
        }
        lastConversationId = uiState.currentConversationId
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val resource = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(getString(resource))
        uiState.actions.clearSnackbar()
    }

    val deletedMessage = stringResource(Res.string.snackbar_conversation_deleted)
    val undoLabel = stringResource(Res.string.snackbar_undo)
    LaunchedEffect(uiState.pendingConversationDeletion) {
        val pendingId = uiState.pendingConversationDeletion ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = deletedMessage,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            uiState.actions.undoDeleteConversation()
        }
    }

    val filteredConversations = remember(uiState.savedConversations, uiState.pendingConversationDeletion) {
        val pendingId = uiState.pendingConversationDeletion
        if (pendingId != null) uiState.savedConversations.filter { it.id != pendingId }.toImmutableList() else uiState.savedConversations
    }

    val historyState = rememberUpdatedState(uiState.history)
    val isShellExecuting by remember {
        derivedStateOf {
            historyState.value.any { it.role == History.Role.TOOL_EXECUTING && it.content == "execute_shell_command" }
        }
    }

    val primaryIsOnDevice = uiState.availableServices
        .firstOrNull()
        ?.let { Service.fromId(it.serviceId).isOnDevice } == true

    // Interactive mode drives a tool-calling loop and emits kai-ui JSON, so its
    // service picker only lists services/models capable of agentic flows.
    val pickerServices = remember(uiState.availableServices, uiState.isInteractiveMode) {
        if (uiState.isInteractiveMode) {
            uiState.availableServices
                .filter { supportsAgenticFlows(it.serviceId, it.modelId) }
                .toImmutableList()
        } else {
            uiState.availableServices
        }
    }

    val sidebar: @Composable () -> Unit = {
        AppSidebar(
            uiState = uiState,
            filteredConversations = filteredConversations,
            onNewChat = { uiState.actions.startNewChat() },
            onConversationSelected = { id ->
                uiState.actions.loadConversation(id)
                isSandboxOpen = false
                scope.launch { drawerState.close() }
            },
            onDeleteConversation = uiState.actions.deleteConversation,
            onRenameConversation = uiState.actions.renameConversation,
            onTogglePinConversation = uiState.actions.togglePinConversation,
            onNavigateToDashboard = onNavigateToDashboard,
            onNavigateToSettings = onNavigateToSettings,
            isInteractiveMode = uiState.isInteractiveMode,
            onToggleInteractiveMode = {
                uiState.actions.setInteractiveMode(!uiState.isInteractiveMode)
            }.takeUnless { primaryIsOnDevice },
            onOpenKaiBuild = onOpenKaiBuild,
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = ChatAdaptivePolicy.useExpandedLayout(maxWidth, hasSidePanel = isSandboxOpen)
        val isPreview = LocalInspectionMode.current
        // Hoisted unconditional to satisfy Koin/Compose ViewModel scoping rules
        val sandboxViewModel: SandboxViewModel? = if (isPreview) null else koinViewModel<SandboxViewModel>()
        val sandboxState = if (isSandboxOpen && sandboxViewModel != null) {
            sandboxViewModel.state.collectAsStateWithLifecycle().value
        } else {
            previewSandboxState ?: SandboxUiState()
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !expanded,
            drawerContent = {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .background(com.inspiredandroid.kai.ui.theme.LocalDesignTokens.current.SurfaceDark),
                ) {
                    sidebar()
                }
            },
        ) {
            ChatShell(
                header = {
                    TopBar(
                        textToSpeech = textToSpeech,
                        isSpeechOutputEnabled = uiState.isSpeechOutputEnabled,
                        isSpeaking = uiState.isSpeaking,
                        actions = uiState.actions,
                        onNavigateToSettings = onNavigateToSettings,
                        isSandboxAvailable = isSandboxAvailable,
                        isSandboxOpen = isSandboxOpen,
                        isShellExecuting = isShellExecuting,
                        isGenerating = uiState.isLoading,
                        onToggleSandbox = { isSandboxOpen = !isSandboxOpen },
                        onOpenDrawer = {
                            scope.launch {
                                keyboardController?.hide()
                                drawerState.open()
                            }
                            Unit
                        }.takeIf { !expanded },
                        services = pickerServices,
                        models = uiState.availableModels,
                        isLoadingModels = uiState.isLoadingModels,
                        isInteractiveMode = uiState.isInteractiveMode,
                        onToggleInteractiveMode = {
                            uiState.actions.setInteractiveMode(!uiState.isInteractiveMode)
                        },
                        onGoBack = uiState.actions.goBackInteractiveMode.takeIf { uiState.isInteractiveMode },
                    )
                },
                conversationRail = if (expanded) {
                    { sidebar() }
                } else {
                    null
                },
                mainContent = {
                    HeartbeatBanner(
                        visible = uiState.hasUnreadHeartbeat,
                        onTap = {
                            uiState.heartbeatConversationId?.let { uiState.actions.loadConversation(it) }
                            uiState.actions.clearUnreadHeartbeat()
                            isSandboxOpen = false
                        },
                        onDismiss = {
                            uiState.actions.clearUnreadHeartbeat()
                        },
                    )

                    PendingSmsBanners(
                        drafts = uiState.smsDrafts,
                        onSend = uiState.actions.sendSmsDraft,
                        onDiscard = uiState.actions.discardSmsDraft,
                    )

                    uiState.warning?.let { warning ->
                        Text(
                            text = stringResource(warning),
                            style = KaiTypography.bodySmall,
                            color = DangerAlert,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    if (isSandboxOpen && !expanded) {
                        SandboxTabsContent(
                            sandboxState = sandboxState,
                            onSetupSandbox = sandboxViewModel?.let { { it.onSetupSandbox() } } ?: {},
                            onCancelSandbox = sandboxViewModel?.let { { it.onCancelSandbox() } } ?: {},
                            previewLines = previewSandboxLines,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    } else {
                        Box(Modifier.weight(1f)) {
                            var isDropping by remember {
                                mutableStateOf(false)
                            }
                            val addFile by rememberUpdatedState(uiState.actions.addFile)
                            val canAcceptDrop by rememberUpdatedState(uiState.supportedFileExtensions.isNotEmpty())
                            val shouldStartDragAndDrop = remember { { _: DragAndDropEvent -> canAcceptDrop } }
                            val dropTarget = remember(canAcceptDrop, addFile) {
                                object : DragAndDropTarget {
                                    override fun onEntered(event: DragAndDropEvent) {
                                        super.onEntered(event)
                                        isDropping = true
                                    }
                                    override fun onExited(event: DragAndDropEvent) {
                                        super.onExited(event)
                                        isDropping = false
                                    }
                                    override fun onDrop(event: DragAndDropEvent): Boolean {
                                        val file = onDragAndDropEventDropped(event)
                                        if (file != null) addFile(file)
                                        isDropping = false
                                        return file != null
                                    }
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(radius = if (isDropping) 4.dp else 0.dp)
                                    .dragAndDropTarget(
                                        shouldStartDragAndDrop = shouldStartDragAndDrop,
                                        target = dropTarget,
                                    ),
                            ) {
                                if (uiState.history.isEmpty()) {
                                    val primaryIsOnDeviceLocal = primaryIsOnDevice
                                    EmptyState(
                                        modifier = Modifier.fillMaxWidth().weight(1f).widthIn(max = 820.dp),
                                        isUsingSharedKey = uiState.showPrivacyInfo,
                                        isGenerating = uiState.isLoading,
                                        isShellExecuting = isShellExecuting,
                                        isSpeaking = uiState.isSpeaking,
                                        isSandboxAvailable = isSandboxAvailable,
                                        onQuickStart = { prompt -> uiState.actions.ask(prompt) },
                                    )
                                } else {
                                    ChatTimeline(
                                        uiState = uiState,
                                        textToSpeech = textToSpeech,
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    if (!isSandboxOpen) {
                        ChatComposer(
                            uiState = uiState,
                            questionInputText = questionInputText,
                            onTextStateChange = { questionInputText = it },
                            isSandboxOpen = isSandboxOpen,
                            availableServices = pickerServices,
                            modifier = Modifier.navigationBarsPadding().padding(bottom = 20.dp),
                        )
                    }
                },
                sidePanel = if (isSandboxOpen && expanded) {
                    {
                        val sbState = sandboxState
                        SandboxTabsContent(
                            sandboxState = sbState,
                            onSetupSandbox = sandboxViewModel?.let { { it.onSetupSandbox() } } ?: {},
                            onCancelSandbox = sandboxViewModel?.let { { it.onCancelSandbox() } } ?: {},
                            previewLines = previewSandboxLines,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    null
                },
                overlays = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(BottomCenter).padding(bottom = 80.dp),
                    ) { data ->
                        Snackbar(snackbarData = data)
                    }
                    // Toast-style error badge (Plan 7, Phase 6): replaces the
                    // centered ErrorMessage blocks; auto-dismisses after 6s.
                    uiState.error?.let { error ->
                        if (!uiState.showFreeProviderSuggestions) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = true,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(KaiShapes.row)
                                        .background(com.inspiredandroid.kai.ui.theme.DesignTokens.SurfaceElevated, KaiShapes.row)
                                        .border(BorderStroke(1.dp, com.inspiredandroid.kai.ui.theme.DesignTokens.BorderSubtle), KaiShapes.row),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = uiErrorText(error),
                                            style = KaiTypography.bodySmall,
                                            color = LocalDesignColorTokens.current.textPrimary,
                                            modifier = Modifier.widthIn(max = 260.dp),
                                        )
                                        IconButton(
                                            onClick = uiState.actions.retry,
                                            modifier = Modifier.size(28.dp).handCursor(),
                                        ) {
                                            Icon(
                                                imageVector = vectorResource(Res.drawable.ic_refresh),
                                                contentDescription = "Retry",
                                                tint = LocalDesignColorTokens.current.textPrimary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .premiumChatBackdrop()
                    .navigationBarsPadding()
                    .statusBarsPadding()
                    .imePadding(),
            )
        }
    }
}

private data class ExecutingToolsState(
    val tools: ImmutableList<Pair<String, String>>,
    val isStatusOnly: Boolean,
)

@Composable
private fun rememberExecutingTools(history: ImmutableList<History>): ExecutingToolsState {
    // Wrap the history parameter in State so derivedStateOf can observe it, then
    // only recompute (and only emit) when the executing-tools subset actually changes.
    // Streaming tokens mutate `history` on every frame but rarely change this derived slice.
    val historyState = rememberUpdatedState(history)
    val state by remember {
        derivedStateOf {
            val executing = historyState.value.filter { it.role == History.Role.TOOL_EXECUTING }
            ExecutingToolsState(
                tools = executing.map { it.id to (it.toolName ?: "tool") }.toImmutableList(),
                isStatusOnly = executing.any { it.isStatusMessage },
            )
        }
    }
    return state
}
