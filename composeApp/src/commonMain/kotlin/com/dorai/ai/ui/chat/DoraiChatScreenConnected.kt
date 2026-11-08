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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import dorai.composeapp.generated.resources.snackbar_conversation_deleted
import dorai.composeapp.generated.resources.snackbar_undo
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.errors.TextToSpeechSynthesisInterruptedError
import org.jetbrains.compose.resources.getString
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
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val appSettings: AppSettings = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedModeId by remember { mutableStateOf("ask") }
    var showModelPicker by remember { mutableStateOf(false) }

    // Precompute timeline relationships (reasoning blocks, frozen forms, etc.)
    val timelineData = remember(uiState.history, uiState.isLoading) {
        deriveChatTimelineData(uiState.history, uiState.isLoading)
    }
    val reasoningByAssistantId = timelineData.reasoningSegmentsByAssistantId

    val messages = remember(uiState.history, uiState.pendingToolConfirmations, uiState.isSpeaking, uiState.isSpeakingContentId, reasoningByAssistantId) {
        uiState.history.mapNotNull { item ->
            when (item.role) {
                History.Role.USER -> {
                    ChatMessageItem(
                        id = item.id,
                        isUser = true,
                        text = item.content,
                        attachments = item.attachments,
                    )
                }

                History.Role.ASSISTANT -> {
                    if (item.content.isEmpty() && item.toolCalls.isNullOrEmpty() && !item.isThinking) return@mapNotNull null

                    val tools = item.toolCalls?.map { tc ->
                        val pending = uiState.pendingToolConfirmations.find { it.toolCallId == tc.id }
                        ToolExecutionModel(
                            toolName = tc.name,
                            argsSummary = tc.arguments,
                            state = if (pending != null) ToolExecutionState.PendingApproval else ToolExecutionState.Completed,
                            commandText = tc.arguments,
                            toolCallId = tc.id,
                        )
                    } ?: emptyList()

                    val dynamicUi = item.uiSubmission?.let { sub ->
                        DynamicUiModel(
                            title = "Form Submission",
                            description = sub.sourceContent.takeIf { it.isNotBlank() },
                            options = sub.values.map { (k, v) -> DynamicUiOption(id = k, title = "$k: $v") },
                            isLocked = true,
                        )
                    }

                    val reasoning = reasoningByAssistantId[item.id] ?: emptyList()
                    val isSpeakingThis = uiState.isSpeaking && uiState.isSpeakingContentId == item.id

                    ChatMessageItem(
                        id = item.id,
                        isUser = false,
                        text = item.content,
                        modelName = item.fallbackServiceName ?: uiState.availableServices.firstOrNull()?.serviceName,
                        isModelLocal = false,
                        toolExecutions = tools,
                        dynamicUi = dynamicUi,
                        attachments = item.attachments,
                        reasoningSegments = reasoning,
                        isThinking = item.isThinking,
                        isSpeaking = isSpeakingThis,
                        fallbackServiceName = item.fallbackServiceName,
                    )
                }

                History.Role.TOOL_EXECUTING -> {
                    val pending = uiState.pendingToolConfirmations.find { it.toolCallId == item.toolCallId }
                    val isPending = pending != null || item.pendingToolConfirmation
                    ChatMessageItem(
                        id = item.id,
                        isUser = false,
                        text = "",
                        modelName = "Tool Execution",
                        isModelLocal = false,
                        toolExecutions = listOf(
                            ToolExecutionModel(
                                toolName = item.toolName ?: pending?.toolName ?: "Tool",
                                argsSummary = pending?.arguments ?: item.content,
                                state = if (isPending) ToolExecutionState.PendingApproval else ToolExecutionState.Running,
                                commandText = pending?.arguments ?: item.content,
                                toolCallId = item.toolCallId ?: pending?.toolCallId ?: item.id,
                            ),
                        ),
                    )
                }

                History.Role.TOOL -> {
                    ChatMessageItem(
                        id = item.id,
                        isUser = false,
                        text = "",
                        modelName = "Tool Result",
                        isModelLocal = false,
                        toolExecutions = listOf(
                            ToolExecutionModel(
                                toolName = item.toolName ?: "Tool",
                                outputText = item.content,
                                state = ToolExecutionState.Completed,
                                toolCallId = item.toolCallId ?: item.id,
                            ),
                        ),
                    )
                }

                History.Role.TOOL_CONFIRMATION -> {
                    val pending = uiState.pendingToolConfirmations.find { it.toolCallId == item.toolCallId }
                    ChatMessageItem(
                        id = item.id,
                        isUser = false,
                        text = "",
                        modelName = "Tool Approval",
                        isModelLocal = false,
                        toolExecutions = listOf(
                            ToolExecutionModel(
                                toolName = item.toolName ?: pending?.toolName ?: "Tool",
                                argsSummary = pending?.arguments ?: item.content,
                                state = ToolExecutionState.PendingApproval,
                                commandText = pending?.arguments ?: item.content,
                                toolCallId = item.toolCallId ?: pending?.toolCallId ?: item.id,
                            ),
                        ),
                    )
                }
            }
        }
    }

    val conversations = remember(uiState.savedConversations, uiState.pendingConversationDeletion) {
        val pendingId = uiState.pendingConversationDeletion
        uiState.savedConversations
            .filter { it.id != pendingId }
            .map { conv ->
                ChatConversationSummary(
                    id = conv.id,
                    title = conv.title,
                    preview = "",
                    isToday = true,
                    isPinned = conv.isPinned,
                )
            }
    }

    val activeService = uiState.availableServices.firstOrNull()
    val currentModelName = activeService?.serviceName ?: "Free FAST"
    val isModelLocal = activeService?.serviceId == "ollama" || activeService?.serviceId == "litert"

    // File picker launcher for composer attachments
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
            onAttachFile = { filePickerLauncher?.launch() },
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
            }
        }
    }
}
