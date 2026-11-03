package com.inspiredandroid.kai.ui.chat

import com.inspiredandroid.kai.data.UiSubmission
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatTimelineDataTest {

    @Test
    fun `submission pairing prefers matching assistant and falls back to latest assistant`() {
        val history = persistentListOf(
            History(id = "assistant-form", role = History.Role.ASSISTANT, content = "form source"),
            History(id = "assistant-latest", role = History.Role.ASSISTANT, content = "latest answer"),
            History(
                id = "user-matched",
                role = History.Role.USER,
                content = "submitted form",
                uiSubmission = UiSubmission(
                    sourceContent = "form source",
                    values = mapOf("choice" to "blue"),
                    pressedEvent = "confirm",
                ),
            ),
            History(
                id = "user-fallback",
                role = History.Role.USER,
                content = "another submission",
                uiSubmission = UiSubmission(sourceContent = "no matching assistant"),
            ),
        )

        val result = deriveChatTimelineData(history, isLoading = true)

        assertEquals("user-matched", result.userIdByAssistantId["assistant-form"])
        assertEquals("user-fallback", result.userIdByAssistantId["assistant-latest"])
        assertEquals(mapOf("choice" to "blue"), result.frozenByAssistantId["assistant-form"]?.values)
        assertFalse(result.frozenByAssistantId["assistant-form"]?.isPending ?: true)
        assertTrue(result.frozenByAssistantId["assistant-latest"]?.isPending ?: false)
    }

    @Test
    fun `reasoning segments retain reverse chronology and only prior thinking is suppressed`() {
        val history = persistentListOf(
            History(id = "thinking-1", role = History.Role.ASSISTANT, content = "first thought", isThinking = true),
            History(id = "thinking-2", role = History.Role.ASSISTANT, content = "second thought", isThinking = true),
            History(
                id = "answer-1",
                role = History.Role.ASSISTANT,
                content = "first answer",
                reasoningContent = "provider reasoning",
            ),
            History(id = "thinking-3", role = History.Role.ASSISTANT, content = "third thought", isThinking = true),
            History(id = "answer-2", role = History.Role.ASSISTANT, content = "second answer"),
            History(id = "thinking-4", role = History.Role.ASSISTANT, content = "unfinished thought", isThinking = true),
        )

        val result = deriveChatTimelineData(history, isLoading = false)

        assertEquals(
            listOf("second thought", "first thought", "provider reasoning"),
            result.reasoningSegmentsByAssistantId["answer-1"].orEmpty(),
        )
        assertEquals(
            listOf("third thought", "second thought", "first thought"),
            result.reasoningSegmentsByAssistantId["answer-2"].orEmpty(),
        )
        assertEquals(setOf("thinking-1", "thinking-2", "thinking-3"), result.suppressedThinkingIds)
        assertFalse("thinking-4" in result.suppressedThinkingIds)
    }

    @Test
    fun `tool lookup preserves first matching shell call order`() {
        val history = persistentListOf(
            History(
                id = "assistant-tools",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(
                    ToolCallInfo(id = "shell-first", name = "execute_shell_command", arguments = "{}"),
                    ToolCallInfo(id = "exact-later", name = "other_tool", arguments = "{}"),
                ),
            ),
        )

        val result = deriveChatTimelineData(history, isLoading = false)

        assertEquals("shell-first", result.findShellToolCall("exact-later")?.id)
        assertEquals("shell-first", result.findShellToolCall("missing")?.id)
        assertEquals("exact-later", result.toolCallsById["exact-later"]?.id)

        val noShellCall = deriveChatTimelineData(
            listOf(
                History(
                    id = "assistant-other-tool",
                    role = History.Role.ASSISTANT,
                    content = "",
                    toolCalls = persistentListOf(ToolCallInfo(id = "other", name = "search", arguments = "{}")),
                ),
            ),
            isLoading = false,
        )
        assertNull(noShellCall.findShellToolCall("missing"))
    }

    @Test
    fun `empty history produces empty derived state`() {
        val result = deriveChatTimelineData(emptyList(), isLoading = false)

        assertTrue(result.frozenByAssistantId.isEmpty())
        assertTrue(result.userIdByAssistantId.isEmpty())
        assertTrue(result.reasoningSegmentsByAssistantId.isEmpty())
        assertTrue(result.suppressedThinkingIds.isEmpty())
        assertTrue(result.toolCallsById.isEmpty())
    }
}
