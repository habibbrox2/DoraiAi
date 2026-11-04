package com.dorai.ai.ui.chat.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dorai.ai.BackIcon
import com.dorai.ai.data.AppSettings
import com.dorai.ai.getBackgroundDispatcher
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.chat.ChatUiState
import com.dorai.ai.ui.chat.History
import com.dorai.ai.ui.chat.activities.ActivityDetails
import com.dorai.ai.ui.chat.activities.ActivityMapper
import com.dorai.ai.ui.chat.deriveChatTimelineData
import com.dorai.ai.ui.chat.lastRenderedAssistant
import com.dorai.ai.ui.components.LogoAnimation
import com.dorai.ai.ui.components.VerticalScrollbarForList
import com.dorai.ai.ui.dynamicui.toSpeakableText
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.markdown.DoraiUiBlock
import com.dorai.ai.ui.markdown.parseMarkdown
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.fallback_answered_by
import dorai.composeapp.generated.resources.fallback_service_failed
import dorai.composeapp.generated.resources.fallback_trying_next
import dorai.composeapp.generated.resources.scroll_to_bottom_content_description
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.errors.TextToSpeechSynthesisInterruptedError
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlinx.collections.immutable.ImmutableList as IImmutableList

/**
 * The conversation timeline: scrollable list of user/assistant messages, tool
 * activity, streaming row, fallback/error states and the scroll-to-bottom affordance.
 *
 * Extracted from ChatModeScreen (Phase 2 of the chat UI upgrade plan) so the
 * shell can compose it independently. All business state still comes from
 * [ChatUiState]; no provider/persistence logic lives here.
 */
@Composable
fun ChatTimeline(
    uiState: ChatUiState,
    textToSpeech: TextToSpeechInstance?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val componentScope = rememberCoroutineScope()
    val appSettings: AppSettings = koinInject()

    val lastAssistantId = remember(uiState.history) { uiState.history.lastRenderedAssistant()?.id }
    // Scroll to bottom & speak only when a NEW assistant message is appended (not on clear, regenerate, etc.)
    var lastScrollId by remember { mutableStateOf(lastAssistantId) }
    LaunchedEffect(lastAssistantId) {
        if (lastScrollId == lastAssistantId) return@LaunchedEffect
        lastScrollId = lastAssistantId
        val history = uiState.history
        if (history.isNotEmpty()) {
            listState.requestScrollToItem(history.lastIndex)
            val lastMessage = history.last()
            if (uiState.isSpeechOutputEnabled && lastMessage.role == History.Role.ASSISTANT) {
                componentScope.launch(getBackgroundDispatcher()) {
                    textToSpeech?.stop()
                    uiState.actions.setIsSpeaking(true, lastMessage.id)
                    try {
                        try {
                            textToSpeech?.rate = appSettings.getTtsRate()
                        } catch (_: Exception) {
                        }
                        try {
                            textToSpeech?.pitch = appSettings.getTtsPitch()
                        } catch (_: Exception) {
                        }
                        try {
                            textToSpeech?.volume = (appSettings.getTtsVolume() * 100).toInt()
                        } catch (_: Exception) {
                        }
                        textToSpeech?.say(lastMessage.content.toSpeakableText())
                    } catch (_: TextToSpeechSynthesisInterruptedError) {
                        // Speech was interrupted by user
                    } catch (_: Exception) {
                        // Handle TTS errors gracefully
                    } finally {
                        uiState.actions.setIsSpeaking(false, lastMessage.id)
                    }
                }
            }
        }
    }
    val timelineData = remember(uiState.history, uiState.isLoading) {
        deriveChatTimelineData(uiState.history, uiState.isLoading)
    }
    val frozenByAssistantId = timelineData.frozenByAssistantId
    val userIdByAssistantId = timelineData.userIdByAssistantId
    val agentActivitiesByRunId = remember(uiState.history) {
        ActivityMapper.historyToActivities(uiState.history).groupBy { it.runId }
    }
    val mappedActivityIds = remember(agentActivitiesByRunId) {
        agentActivitiesByRunId.values.flatten().map { it.id }.toSet()
    }
    val executingToolsState = rememberTimelineExecutingTools(uiState.history)

    val fallbackStatusText = uiState.fallbackStatus?.let { status ->
        val failed = stringResource(Res.string.fallback_service_failed, status.serviceName, uiErrorText(status.errorReason))
        val next = status.nextServiceName?.let { stringResource(Res.string.fallback_trying_next, it) }
        if (next != null) "$failed\n$next" else failed
    }

    val firstAssistantId = remember(uiState.history) {
        uiState.history.firstOrNull { it.role == History.Role.ASSISTANT && it.content.isNotEmpty() && !it.isThinking }?.id
    }
    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index < listState.layoutInfo.totalItemsCount - 1
        }
    }

    Box(modifier = modifier.fillMaxHeight().clipToBounds(), contentAlignment = Alignment.Center) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .widthIn(max = 820.dp)
                .clipToBounds()
                .semantics { liveRegion = LiveRegionMode.Polite },
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 12.dp,
                bottom = 16.dp,
            ),
        ) {
            items(uiState.history, key = { it.id }, contentType = { it.role }) { history ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                        slideInVertically(animationSpec = tween(durationMillis = 300)) { it / 4 },
                ) {
                    when (history.role) {
                        History.Role.USER -> {
                            if (history.uiSubmission == null) {
                                UserMessage(
                                    message = history.content,
                                    attachments = history.attachments,
                                )
                            }
                        }

                        History.Role.ASSISTANT -> {
                            agentActivitiesByRunId[history.id]?.let { activities ->
                                AgentActivityDisclosure(activities = activities, fileReviewProposals = uiState.fileReviewProposals, fileReviewPreviews = uiState.fileReviewPreviews, actions = uiState.actions)
                            }
                            if (history.content.isNotEmpty() && !history.isThinking) {
                                val isLastAssistant = history.id == lastAssistantId
                                val frozen = frozenByAssistantId[history.id]
                                val pairedUserId = userIdByAssistantId[history.id]
                                BotMessage(
                                    message = history.content,
                                    textToSpeech = textToSpeech,
                                    isSpeaking = uiState.isSpeaking && uiState.isSpeakingContentId == history.id,
                                    setIsSpeaking = {
                                        uiState.actions.setIsSpeaking(it, history.id)
                                    },
                                    onRegenerate = if (isLastAssistant) uiState.actions.regenerate else null,
                                    isInteractive = isLastAssistant && !uiState.isLoading && frozen == null,
                                    onUiCallback = { event, data ->
                                        uiState.actions.submitUiCallback(event, data)
                                    },
                                    frozen = frozen,
                                    onResubmit = if (pairedUserId != null && !uiState.isLoading) {
                                        { event, data -> uiState.actions.resubmit(pairedUserId, event, data) }
                                    } else {
                                        null
                                    },
                                    isFirstMessage = history.id == firstAssistantId,
                                )
                                if (history.fallbackServiceName != null) {
                                    Text(
                                        text = stringResource(Res.string.fallback_answered_by, history.fallbackServiceName),
                                        style = DoraiTypography.labelSmall,
                                        color = LocalDesignColorTokens.current.textSecondary,
                                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                                    )
                                }
                            }
                        }

                        History.Role.TOOL_EXECUTING -> {
                            if ((history.toolCallId ?: history.id) !in mappedActivityIds) {
                                val isShell = history.content == "execute_shell_command" || history.toolName == "execute_shell_command"
                                if (isShell) {
                                    val cmd = history.toolName.takeUnless { it == "execute_shell_command" } ?: history.content
                                    ShellExecutionCard(command = cmd, isRunning = true)
                                } else {
                                    ToolExecutionCard(toolName = history.toolName ?: history.content, isRunning = true)
                                }
                            }
                        }

                        History.Role.TOOL -> {
                            if ((history.toolCallId ?: history.id) !in mappedActivityIds) {
                                val isShell = history.toolName == "execute_shell_command"
                                if (isShell) {
                                    val matchingToolCall = timelineData.findShellToolCall(history.toolCallId)
                                    val shellDetails = if (matchingToolCall != null) {
                                        val parsedCmd = ActivityMapper.parseShellCommand(matchingToolCall.arguments)
                                        ActivityMapper.parseShellResult(parsedCmd, history.content)
                                    } else {
                                        ActivityMapper.parseShellResult(
                                            ActivityDetails.ShellCommand(command = "execute_shell_command"),
                                            history.content,
                                        )
                                    }
                                    ShellExecutionCard(
                                        command = shellDetails.command.ifEmpty { "execute_shell_command" },
                                        workingDirectory = shellDetails.workingDirectory,
                                        stdout = shellDetails.stdout,
                                        stderr = shellDetails.stderr,
                                        exitCode = shellDetails.exitCode,
                                        isRunning = false,
                                    )
                                } else {
                                    val matchingToolCall = history.toolCallId?.let(timelineData.toolCallsById::get)
                                    val isError = history.content.contains("\"success\":false") || history.content.contains("\"error\"")
                                    ToolExecutionCard(
                                        toolName = history.toolName ?: "tool",
                                        arguments = matchingToolCall?.arguments,
                                        result = history.content,
                                        isRunning = false,
                                        isError = isError,
                                    )
                                }
                            }
                        }

                        History.Role.TOOL_CONFIRMATION -> {
                            val toolCallId = history.toolConfirmationId ?: history.toolCallId ?: history.id
                            ToolConfirmationCard(
                                toolName = history.toolName ?: history.content,
                                toolCallId = toolCallId,
                                arguments = history.content,
                                onDeny = { uiState.actions.denyToolExecution(toolCallId) },
                                onAllowOnce = { uiState.actions.confirmToolExecution(toolCallId, true, false, false) },
                                onAllowSession = { uiState.actions.confirmToolExecution(toolCallId, false, false, true) },
                                onAlwaysAllow = { uiState.actions.confirmToolExecution(toolCallId, false, true, false) },
                            )
                        }
                    }
                }
            }
            val showWaitingRow = uiState.isLoading &&
                frozenByAssistantId.values.none { it.isPending } &&
                uiState.history.none { it.role == History.Role.TOOL_EXECUTING }
            if (showWaitingRow) {
                item(key = "loading") {
                    WaitingResponseRow(
                        executingTools = executingToolsState.tools,
                        isStatusOnly = executingToolsState.isStatusOnly,
                        statusText = fallbackStatusText,
                    )
                }
            }
            uiState.error?.let { error ->
                item(key = "error") {
                    if (uiState.showFreeProviderSuggestions) {
                        FreeProviderSuggestionsPanel(
                            error = error,
                            retry = uiState.actions.retry,
                        )
                    }
                }
            }
            if (!uiState.isLoading && uiState.followUpSuggestions.isNotEmpty()) {
                item(key = "followups") {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 820.dp)
                            .padding(horizontal = com.dorai.ai.ui.chat.ChatSpacing.lg, vertical = com.dorai.ai.ui.chat.ChatSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.followUpSuggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { uiState.actions.ask(suggestion) },
                                label = { Text(suggestion) },
                            )
                        }
                    }
                }
            }
        }

        VerticalScrollbarForList(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )

        AnimatedVisibility(
            visible = showScrollToBottom,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = com.dorai.ai.ui.chat.ChatSpacing.lg),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            SmallFloatingActionButton(
                modifier = Modifier
                    .handCursor().shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.2f),
                        spotColor = com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.3f),
                    ),
                onClick = {
                    componentScope.launch {
                        val totalItems = listState.layoutInfo.totalItemsCount
                        if (totalItems > 0) {
                            if (prefersReducedMotion()) {
                                listState.scrollToItem(totalItems - 1)
                            } else {
                                listState.animateScrollToItem(totalItems - 1)
                            }
                        }
                    }
                },
                containerColor = com.dorai.ai.ui.CardFill.copy(alpha = 0.95f),
                contentColor = LocalDesignColorTokens.current.textPrimary,
                shape = CircleShape,
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.scroll_to_bottom_content_description),
                    tint = com.dorai.ai.ui.AccentPrimary,
                )
            }
        }
    }
}

private data class TimelineExecutingToolsState(
    val tools: ImmutableList<Pair<String, String>>,
    val isStatusOnly: Boolean,
)

@Composable
private fun rememberTimelineExecutingTools(history: ImmutableList<History>): TimelineExecutingToolsState {
    val historyState = rememberUpdatedState(history)
    val state by remember {
        derivedStateOf {
            val executing = historyState.value.filter { it.role == History.Role.TOOL_EXECUTING }
            TimelineExecutingToolsState(
                tools = executing.map { it.id to (it.toolName ?: "tool") }.toImmutableList(),
                isStatusOnly = executing.any { it.isStatusMessage },
            )
        }
    }
    return state
}
