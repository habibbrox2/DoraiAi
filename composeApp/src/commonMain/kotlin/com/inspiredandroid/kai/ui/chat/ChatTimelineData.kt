package com.inspiredandroid.kai.ui.chat

import com.inspiredandroid.kai.ui.dynamicui.FrozenSubmission
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Immutable, precomputed indexes consumed by [ChatTimeline].
 *
 * All history-wide relationships are derived in linear passes instead of
 * rescanning the complete history for every message during composition.
 */
internal data class ChatTimelineData(
    val frozenByAssistantId: Map<String, FrozenSubmission>,
    val userIdByAssistantId: Map<String, String>,
    val reasoningSegmentsByAssistantId: Map<String, ImmutableList<String>>,
    val suppressedThinkingIds: Set<String>,
    val toolCallsById: Map<String, ToolCallInfo>,
    private val toolCallOrderById: Map<String, Int>,
    private val firstShellToolCall: IndexedValue<ToolCallInfo>?,
) {
    /**
     * Preserves the previous shell-call lookup order: the first call in history
     * matching either the requested id or the shell command name wins.
     */
    fun findShellToolCall(toolCallId: String?): ToolCallInfo? {
        val exactCall = toolCallId?.let(toolCallsById::get)
        val exactOrder = toolCallId?.let(toolCallOrderById::get)
        val shellCall = firstShellToolCall ?: return exactCall

        if (exactCall == null || exactOrder == null) return shellCall.value
        return if (shellCall.index < exactOrder) shellCall.value else exactCall
    }
}

internal fun deriveChatTimelineData(
    history: List<History>,
    isLoading: Boolean,
): ChatTimelineData {
    val lastUserIndex = history.indexOfLast { it.role == History.Role.USER }
    val frozenByAssistantId = linkedMapOf<String, FrozenSubmission>()
    val userIdByAssistantId = linkedMapOf<String, String>()
    val reasoningByAssistantId = linkedMapOf<String, ImmutableList<String>>()
    val latestAssistantByContent = mutableMapOf<String, String>()

    var latestAssistantId: String? = null
    var priorThinkingSegments = persistentListOf<String>()

    history.forEachIndexed { index, entry ->
        when (entry.role) {
            History.Role.ASSISTANT -> {
                if (entry.isThinking) {
                    if (entry.content.isNotEmpty()) {
                        priorThinkingSegments = priorThinkingSegments.adding(entry.content)
                    }
                } else if (entry.content.isNotEmpty()) {
                    val combined = buildList {
                        addAll(priorThinkingSegments.asReversed())
                        entry.reasoningContent?.takeIf { it.isNotBlank() }?.let(::add)
                    }.toImmutableList()

                    if (combined.isNotEmpty()) {
                        reasoningByAssistantId[entry.id] = combined
                    }
                    latestAssistantId = entry.id
                    latestAssistantByContent[entry.content] = entry.id
                }
            }

            History.Role.TOOL_CONFIRMATION -> {
                // Skip confirmation entries for timeline data processing
            }

            History.Role.USER -> {
                val submission = entry.uiSubmission
                if (submission != null) {
                    val originId = latestAssistantByContent[submission.sourceContent] ?: latestAssistantId
                    if (originId != null) {
                        frozenByAssistantId[originId] = FrozenSubmission(
                            values = submission.values,
                            pressedEvent = submission.pressedEvent,
                            isPending = isLoading && index == lastUserIndex,
                        )
                    }
                }

                val sourceContent = submission?.sourceContent ?: ""
                val originId = latestAssistantByContent[sourceContent] ?: latestAssistantId
                if (originId != null) {
                    userIdByAssistantId[originId] = entry.id
                }
            }

            History.Role.TOOL_EXECUTING,
            History.Role.TOOL,
            -> Unit
        }
    }

    // Reverse scan replaces one forward scan per thinking entry.
    val suppressedThinkingIds = linkedSetOf<String>()
    var hasLaterAnswer = false
    for (index in history.indices.reversed()) {
        val entry = history[index]
        if (entry.role != History.Role.ASSISTANT) continue

        if (!entry.isThinking && entry.content.isNotEmpty()) {
            hasLaterAnswer = true
        } else if (entry.isThinking && entry.content.isNotEmpty() && hasLaterAnswer) {
            suppressedThinkingIds += entry.id
        }
    }

    // Index tool calls once; TOOL rows can then resolve their call in O(1).
    val toolCallsById = linkedMapOf<String, ToolCallInfo>()
    val toolCallOrderById = mutableMapOf<String, Int>()
    var firstShellToolCall: IndexedValue<ToolCallInfo>? = null
    var toolCallOrder = 0
    history.forEach { entry ->
        entry.toolCalls?.forEach { toolCall ->
            if (toolCall.id !in toolCallsById) {
                toolCallsById[toolCall.id] = toolCall
                toolCallOrderById[toolCall.id] = toolCallOrder
            }
            if (firstShellToolCall == null && toolCall.name == "execute_shell_command") {
                firstShellToolCall = IndexedValue(toolCallOrder, toolCall)
            }
            toolCallOrder++
        }
    }

    return ChatTimelineData(
        frozenByAssistantId = frozenByAssistantId,
        userIdByAssistantId = userIdByAssistantId,
        reasoningSegmentsByAssistantId = reasoningByAssistantId,
        suppressedThinkingIds = suppressedThinkingIds,
        toolCallsById = toolCallsById,
        toolCallOrderById = toolCallOrderById,
        firstShellToolCall = firstShellToolCall,
    )
}
