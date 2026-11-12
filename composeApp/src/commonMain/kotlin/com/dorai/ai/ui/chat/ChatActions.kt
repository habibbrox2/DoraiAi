package com.dorai.ai.ui.chat

import androidx.compose.runtime.Immutable
import io.github.vinceglb.filekit.PlatformFile

@Immutable
data class ChatActions(
    val ask: (String) -> Unit,
    val retry: () -> Unit,
    val clearHistory: () -> Unit,
    val setIsSpeaking: (Boolean, String) -> Unit,
    val addFile: (PlatformFile) -> Unit,
    val removeFile: (PlatformFile) -> Unit,
    val startNewChat: () -> Unit,
    val regenerate: () -> Unit,
    val cancel: () -> Unit,
    val selectService: (String) -> Unit,
    /** Picks a model for the active service instance. */
    val selectModel: (String) -> Unit,
    /** Loads the provider's model list for the active service instance. */
    val loadModels: () -> Unit,
    val loadConversation: (String) -> Unit,
    val deleteConversation: (String) -> Unit,
    val renameConversation: (String, String) -> Unit,
    val togglePinConversation: (String) -> Unit,
    val clearUnreadHeartbeat: () -> Unit,
    val clearSnackbar: () -> Unit,
    val undoDeleteConversation: () -> Unit,
    val submitUiCallback: (event: String, data: Map<String, String>) -> Unit,
    val resubmit: (messageId: String, event: String, data: Map<String, String>) -> Unit,
    /** Turns the interactive-UI mode on or off for the current chat. */
    val setInteractiveMode: (Boolean) -> Unit,
    /** Selects the UI v2 agent mode ("ask"/"plan"/"build"/"review") for the current chat. */
    val setAgentMode: (String) -> Unit = {},
    val sendSmsDraft: (String) -> Unit,
    val discardSmsDraft: (String) -> Unit,
    val consumeComposerPrefill: () -> Unit,
    val confirmToolExecution: (toolCallId: String, allowOnce: Boolean, alwaysAllow: Boolean, allowSession: Boolean) -> Unit = { _, _, _, _ -> },
    val denyToolExecution: (toolCallId: String) -> Unit = {},
    val acceptFileChange: (proposalId: String) -> Unit = {},
    val rejectFileChange: (proposalId: String) -> Unit = {},
    val rollbackFileChange: (proposalId: String) -> Unit = {},
    val clearError: () -> Unit = {},
    /** Starts a voice turn. The microphone permission is requested by the caller. */
    val startVoiceInput: () -> Unit = {},
    /** Ends the current voice turn, keeping whatever was recognised so far. */
    val stopVoiceInput: () -> Unit = {},
    /** Reports that the assistant's answer is being spoken, or has finished. */
    val setVoiceSpeaking: (Boolean) -> Unit = {},
    /** Marks the pending voice reply as spoken so it is not repeated. */
    val markVoiceReplySpoken: () -> Unit = {},
)
