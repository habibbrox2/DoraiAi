package com.dorai.ai.network.dtos.openaicompatible

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One server-sent chunk from `POST /chat/completions` sent with `"stream": true`.
 *
 * OpenAI-compatible providers deliver the answer as a series of `delta` objects:
 * `content` carries answer text, `reasoning_content` / `reasoning` carry the thinking
 * trace, and `tool_calls` arrive as *fragments* keyed by [Delta.ToolCallFragment.index] —
 * the id and function name are typically sent once, while the JSON arguments are split
 * across many chunks and must be concatenated in order.
 */
@Serializable
data class OpenAICompatibleChatChunkDto(
    val choices: List<Choice> = emptyList(),
) {
    @Serializable
    data class Choice(
        val delta: Delta? = null,
        @SerialName("finish_reason") val finishReason: String? = null,
    )

    @Serializable
    data class Delta(
        val role: String? = null,
        /** Reuses the response DTO's tolerant reader: some gateways still stream content parts. */
        @Serializable(with = FlexibleContentSerializer::class)
        val content: String? = null,
        @SerialName("reasoning_content") val reasoningContent: String? = null,
        val reasoning: String? = null,
        @SerialName("tool_calls") val toolCalls: List<ToolCallFragment>? = null,
    ) {
        @Serializable
        data class ToolCallFragment(
            /** Position of this call in the assistant's `tool_calls` array — the join key. */
            val index: Int = 0,
            val id: String? = null,
            val type: String? = null,
            val function: FunctionFragment? = null,
        )

        @Serializable
        data class FunctionFragment(
            val name: String? = null,
            val arguments: String? = null,
        )
    }
}

/**
 * Fully assembled result of a streamed (or silently buffered) OpenAI-compatible completion.
 *
 * [asMessage] rebuilds the exact shape the non-streaming path parses, so the chat layer keeps
 * one code path for `content` / reasoning / inline-tool handling instead of two.
 */
data class OpenAICompatibleCompletion(
    val content: String?,
    val reasoning: String?,
    val toolCalls: List<OpenAICompatibleChatResponseDto.ToolCall> = emptyList(),
    val finishReason: String? = null,
) {
    fun asMessage(): OpenAICompatibleChatResponseDto.Choice.Message =
        OpenAICompatibleChatResponseDto.Choice.Message(
            role = "assistant",
            content = content,
            reasoningContent = reasoning,
            toolCalls = toolCalls.ifEmpty { null },
        )
}
