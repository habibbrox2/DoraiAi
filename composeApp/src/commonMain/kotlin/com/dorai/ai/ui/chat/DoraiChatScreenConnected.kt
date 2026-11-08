package com.dorai.ai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.PlatformBackHandler
import com.dorai.ai.data.AppSettings
import com.dorai.ai.data.imageExtensions
import com.dorai.ai.exitApp
import com.dorai.ai.getBackgroundDispatcher
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.components.v2.DoraiModalSheet
import com.dorai.ai.ui.components.v2.DoraiProviderBadge
import com.dorai.ai.ui.components.v2.DoraiStatusBadge
import com.dorai.ai.ui.components.v2.DynamicUiModel
import com.dorai.ai.ui.components.v2.DynamicUiOption
import com.dorai.ai.ui.components.v2.ToolExecutionModel
import com.dorai.ai.ui.components.v2.ToolExecutionState
import com.dorai.ai.ui.dynamicui.toSpeakableText
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.chat_model_picker_title
import dorai.composeapp.generated.resources.ic_file
import dorai.composeapp.generated.resources.ic_image
import dorai.composeapp.generated.resources.snackbar_conversation_deleted
import dorai.composeapp.generated.resources.snackbar_undo
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.errors.TextToSpeechSynthesisInterruptedError
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Production-connected bridge that hooks the UI v2 [DoraiChatScreen] directly into [ChatViewModel].
 * This fully implements all Chat capabilities: multimodal attachments, Markdown formatting,
 * collapsible reasoning/thinking blocks, TTS speech, tool authorization approvals, dynamic UI,
 * proactive heartbeat & SMS banners, conversation management, and undo actions.
 */
@Composable
fun DoraiChatScreenConnected(
    viewModel: ChatViewModel,
    textToSpeech: TextToSpeechInstance? = null,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWorkspace: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val appSettings: AppSettings = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedModeId by remember { mutableStateOf("ask") }
    var showModelPicker by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }

    // Precompute timeline relationships (reasoning blocks, frozen forms, etc.)
    val timelineData = remember(uiState.history, uiState.isLoading) {
        deriveChatTimelineData(uiState.history, uiState.isLoading)
    }
    val reasoningByAssistantId = timelineData.reasoningSegmentsByAssistantId

    val messages = remember(
        uiState.history,
        uiState.pendingToolConfirmations,
        uiState.isSpeaking,
        uiState.isSpeakingContentId,
        reasoningByAssistantId,
        uiState.availableServices,
    ) {
        groupHistoryToChatMessages(
            history = uiState.history,
            pendingToolConfirmations = uiState.pendingToolConfirmations,
            reasoningByAssistantId = reasoningByAssistantId,
            isSpeaking = uiState.isSpeaking,
            isSpeakingContentId = uiState.isSpeakingContentId,
            availableServices = uiState.availableServices,
        )
    }

    val conversations = remember(uiState.savedConversations, uiState.pendingConversationDeletion) {
        val pendingId = uiState.pendingConversationDeletion
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val oneDayMs = 24 * 60 * 60 * 1000L
        uiState.savedConversations
            .filter { it.id != pendingId }
            .map { conv ->
                ChatConversationSummary(
                    id = conv.id,
                    title = conv.title,
                    preview = "",
                    isToday = (now - conv.updatedAt) < oneDayMs,
                    isPinned = conv.isPinned,
                )
            }
    }

    val activeService = uiState.availableServices.firstOrNull()
    val currentModelName = activeService?.serviceName ?: "Free FAST"
    val isModelLocal = activeService?.serviceId == "ollama" || activeService?.serviceId == "litert"
    val maxContextTokens = remember(activeService) {
        val modelId = activeService?.modelId ?: activeService?.serviceId ?: ""
        if (modelId.isNotBlank()) com.dorai.ai.data.ModelCatalog.estimateContextWindow(modelId) else null
    }

    // File & image picker launchers for composer attachments
    val allowFileAttachment = uiState.supportedFileExtensions.isNotEmpty()
    val filePickerLauncher = if (allowFileAttachment) {
        rememberFilePickerLauncher(
            type = FileKitType.File(extensions = uiState.supportedFileExtensions),
        ) { file ->
            if (file != null) uiState.actions.addFile(file)
        }
    } else {
        null
    }

    val imagePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = imageExtensions.toList()),
    ) { file ->
        if (file != null) uiState.actions.addFile(file)
    }

    // Snackbar notifications
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

    // Double-back to exit handling
    var lastBackPressTime by remember { mutableStateOf(0L) }
    PlatformBackHandler(enabled = true) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        if (now - lastBackPressTime < 2000L) {
            exitApp()
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

    Box(modifier = modifier.fillMaxSize()) {
        DoraiChatScreen(
            messages = messages,
            conversations = conversations,
            currentModelName = currentModelName,
            isModelLocal = isModelLocal,
            selectedModeId = selectedModeId,
            onModeSelected = { selectedModeId = it },
            onSendMessage = { prompt ->
                uiState.actions.ask(prompt)
            },
            onNewChat = { uiState.actions.startNewChat() },
            onOpenConversation = { id -> uiState.actions.loadConversation(id) },
            activeConversationId = uiState.currentConversationId,
            isStreaming = uiState.isLoading,
            onStopStreaming = { uiState.actions.cancel() },
            onRegenerate = { uiState.actions.regenerate() },
            onOpenModelPicker = { showModelPicker = true },
            onAllowOnce = { toolCallId ->
                uiState.actions.confirmToolExecution(toolCallId, true, false, false)
            },
            onAllowSession = { toolCallId ->
                uiState.actions.confirmToolExecution(toolCallId, false, false, true)
            },
            onDeny = { toolCallId ->
                uiState.actions.denyToolExecution(toolCallId)
            },
            onRetryTool = {
                uiState.actions.regenerate()
            },
            onAskFixTool = {
                uiState.actions.ask("Please fix the previous tool failure.")
            },
            onRunPlan = {
                selectedModeId = "build"
                uiState.actions.ask("Run this plan in build mode.")
            },
            onSpeak = { messageId ->
                val msg = messages.find { it.id == messageId }
                if (msg != null && textToSpeech != null) {
                    if (uiState.isSpeaking && uiState.isSpeakingContentId == messageId) {
                        textToSpeech.stop()
                        uiState.actions.setIsSpeaking(false, messageId)
                    } else {
                        scope.launch(getBackgroundDispatcher()) {
                            textToSpeech.stop()
                            uiState.actions.setIsSpeaking(true, messageId)
                            try {
                                try {
                                    textToSpeech.rate = appSettings.getTtsRate()
                                } catch (_: Exception) {}
                                try {
                                    textToSpeech.pitch = appSettings.getTtsPitch()
                                } catch (_: Exception) {}
                                try {
                                    textToSpeech.volume = (appSettings.getTtsVolume() * 100).toInt()
                                } catch (_: Exception) {}
                                textToSpeech.say(msg.text.toSpeakableText())
                            } catch (_: TextToSpeechSynthesisInterruptedError) {
                            } catch (_: Exception) {
                            } finally {
                                uiState.actions.setIsSpeaking(false, messageId)
                            }
                        }
                    }
                }
            },
            files = uiState.files,
            onRemoveFile = { file -> uiState.actions.removeFile(file) },
            onAttachFile = { showAttachSheet = true },
            onAttachImage = {
                showAttachSheet = false
                imagePickerLauncher.launch()
            },
            onMicClick = { /* Voice input callback */ },
            hasUnreadHeartbeat = uiState.hasUnreadHeartbeat,
            onHeartbeatTap = {
                uiState.heartbeatConversationId?.let { uiState.actions.loadConversation(it) }
                uiState.actions.clearUnreadHeartbeat()
            },
            onHeartbeatDismiss = { uiState.actions.clearUnreadHeartbeat() },
            smsDrafts = uiState.smsDrafts,
            onSendSms = { uiState.actions.sendSmsDraft(it) },
            onDiscardSms = { uiState.actions.discardSmsDraft(it) },
            onTogglePinConversation = { id -> uiState.actions.togglePinConversation(id) },
            onDeleteConversation = { id -> uiState.actions.deleteConversation(id) },
            onRenameConversation = { id, title -> uiState.actions.renameConversation(id, title) },
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToWorkspace = onNavigateToWorkspace,
            maxContextTokens = maxContextTokens,
            modifier = Modifier.fillMaxSize(),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }

    if (showModelPicker) {
        val colors = LocalDoraiColors.current
        DoraiModalSheet(onDismissRequest = { showModelPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DoraiSpacing.LG),
            ) {
                Text(
                    text = stringResource(Res.string.chat_model_picker_title),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(DoraiSpacing.MD))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
                ) {
                    items(uiState.availableServices, key = { it.instanceId }) { serviceEntry ->
                        val isSelected = serviceEntry.instanceId == activeService?.instanceId
                        val isLocal = serviceEntry.serviceId == "ollama" || serviceEntry.serviceId == "litert"
                        val shape = RoundedCornerShape(12.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(if (isSelected) colors.surface2 else colors.surface)
                                .border(1.dp, if (isSelected) colors.primary else colors.line, shape)
                                .clickable {
                                    uiState.actions.selectService(serviceEntry.instanceId)
                                    showModelPicker = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = serviceEntry.serviceName,
                                        color = colors.text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    DoraiProviderBadge(isLocal = isLocal, iconOnly = true)
                                }
                                Text(
                                    text = serviceEntry.modelId,
                                    color = colors.muted,
                                    fontSize = 12.sp,
                                )
                            }
                            if (isSelected) {
                                DoraiStatusBadge(text = "Active", color = colors.ok)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DoraiSpacing.MD))

                TextButton(
                    onClick = {
                        showModelPicker = false
                        onNavigateToSettings()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Manage providers",
                        color = colors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (showAttachSheet) {
        val colors = LocalDoraiColors.current
        DoraiModalSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DoraiSpacing.LG),
            ) {
                Text(
                    text = "Attach",
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(DoraiSpacing.MD))

                // Photo row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachSheet = false
                            imagePickerLauncher.launch()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_image),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Photo / Image",
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // File row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachSheet = false
                            filePickerLauncher?.launch()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_file),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "File / Document",
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(DoraiSpacing.MD))

                // Privacy disclosure footer from prototype
                Text(
                    text = if (isModelLocal) {
                        "Files stay on this device."
                    } else {
                        "You will be told before a file goes to a cloud model."
                    },
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
