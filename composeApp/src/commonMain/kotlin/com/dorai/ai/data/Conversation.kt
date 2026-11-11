package com.dorai.ai.data

import androidx.compose.runtime.Immutable
import com.dorai.ai.TerminalLine
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * A single file attachment on a chat message. Used both in-memory on `History` and
 * persisted on `Conversation.Message`. Binary content is base64-encoded.
 */
@Immutable
@Serializable
data class Attachment(
    val data: String,
    val mimeType: String,
    val fileName: String? = null,
)

/**
 * A persisted tool call on an assistant message, or the id pairing on a tool result.
 * Mirrors the in-memory `ToolCallInfo` (ui layer) without making the data layer depend on it.
 * thoughtSignature carries the provider round-trip signature some models (Gemini, DeepSeek)
 * require to be echoed back on subsequent tool requests.
 */
@OptIn(ExperimentalSerializationApi::class)
@Immutable
@Serializable
data class ConversationToolCall(
    val id: String,
    val name: String,
    val arguments: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val thoughtSignature: String? = null,
)

@Serializable
data class Conversation(
    val id: String,
    val messages: List<Message>,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String = "",
    val type: String = TYPE_CHAT,
    val shellTranscript: List<TerminalLine> = emptyList(),
    val isPinned: Boolean = false,
    val pinnedAt: Long = 0L,
) {
    companion object {
        const val TYPE_CHAT = "chat"
        const val TYPE_HEARTBEAT = "heartbeat"
        const val TYPE_INTERACTIVE = "interactive"
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Message(
        val id: String,
        val role: String,
        val content: String,
        val attachments: List<Attachment> = emptyList(),
        val uiSubmission: UiSubmission? = null,
        val isThinking: Boolean = false,
        // Most messages have no reasoning trace; skip the null to keep the persisted blob lean.
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val reasoningContent: String? = null,
        // Tool calls requested by the assistant (role = "assistant"), so a restored conversation
        // can round-trip OpenAI tool_calls pairing, and DeepSeek/Gemini thought signatures.
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val toolCalls: List<ConversationToolCall>? = null,
        // Pairing for tool result rows (role = "tool").
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val toolCallId: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val toolName: String? = null,
        // Milliseconds since epoch when the message was sent. Null for messages saved before
        // this field existed (shown without a timestamp in the UI).
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val sentAt: Long? = null,
        // Legacy single-file fields — retained for reading old persisted conversations.
        // New code writes only `attachments`; these remain null on newly saved messages.
        val mimeType: String? = null,
        val data: String? = null,
        val fileName: String? = null,
    )
}

/**
 * Snapshot of a dorai-ui form the user submitted. Attached to the resulting User message so
 * the bubble renders as a frozen form (with the values the user picked) instead of the
 * cryptic "Responded with: ..." text. `sourceContent` holds the assistant message body that
 * originated the form — it's re-parsed at render time.
 */
@Immutable
@Serializable
data class UiSubmission(
    val sourceContent: String,
    val values: Map<String, String> = emptyMap(),
    val pressedEvent: String? = null,
)

@Serializable
data class ConversationsData(
    val version: Int = 2,
    val conversations: List<Conversation>,
)
