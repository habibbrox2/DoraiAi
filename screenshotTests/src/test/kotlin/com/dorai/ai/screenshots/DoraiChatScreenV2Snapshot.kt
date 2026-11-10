package com.dorai.ai.screenshots

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.adaptive.calculateDoraiWindowWidthClass
import com.dorai.ai.ui.chat.ChatConversationSummary
import com.dorai.ai.ui.chat.ChatMessageItem
import com.dorai.ai.ui.chat.DoraiChatScreen

/**
 * Snapshot wrapper for the UI v2 chat screen. [DoraiChatScreen] reads the adaptive
 * [LocalDoraiWindowWidthClass] composition local, which standard test snap helpers do
 * not provide — this wrapper computes it from the available width like the v2 suite does.
 */
@Composable
internal fun DoraiChatScreenV2(
    conversations: List<ChatConversationSummary>,
    messages: List<ChatMessageItem> = emptyList(),
    currentModelName: String = "Google Gemini",
    isModelLocal: Boolean = false,
    selectedModeId: String = "ask",
    onOpenConversation: (String) -> Unit = {},
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToWorkspace: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthClass = calculateDoraiWindowWidthClass(maxWidth)
        CompositionLocalProvider(LocalDoraiWindowWidthClass provides widthClass) {
            DoraiChatScreen(
                messages = messages,
                conversations = conversations,
                currentModelName = currentModelName,
                isModelLocal = isModelLocal,
                selectedModeId = selectedModeId,
                onModeSelected = {},
                onSendMessage = {},
                onNewChat = {},
                onOpenConversation = onOpenConversation,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToWorkspace = onNavigateToWorkspace,
            )
        }
    }
}
