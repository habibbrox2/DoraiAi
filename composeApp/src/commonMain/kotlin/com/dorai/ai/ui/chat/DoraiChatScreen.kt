package com.dorai.ai.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.data.Attachment
import com.dorai.ai.data.SmsDraft
import com.dorai.ai.decodeToImageBitmap
import com.dorai.ai.network.UiError
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.DoraiListDetailPaneScaffold
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.chat.composables.HeartbeatBanner
import com.dorai.ai.ui.chat.composables.PendingSmsBanners
import com.dorai.ai.ui.chat.composables.uiErrorText
import com.dorai.ai.ui.components.LogoAnimation
import com.dorai.ai.ui.components.v2.DoraiComposer
import com.dorai.ai.ui.components.v2.DoraiDynamicUiBlock
import com.dorai.ai.ui.components.v2.DoraiModalSheet
import com.dorai.ai.ui.components.v2.DoraiModeSelector
import com.dorai.ai.ui.components.v2.DoraiProviderBadge
import com.dorai.ai.ui.components.v2.DoraiStatusBadge
import com.dorai.ai.ui.components.v2.DoraiToolCard
import com.dorai.ai.ui.components.v2.DynamicUiModel
import com.dorai.ai.ui.components.v2.ToolExecutionModel
import com.dorai.ai.ui.markdown.MarkdownContent
import com.dorai.ai.ui.markdown.parseMarkdown
import com.dorai.ai.ui.rememberCopyToClipboard
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.bot_message_thinking_expand_content_description
import dorai.composeapp.generated.resources.bot_message_thinking_label
import dorai.composeapp.generated.resources.chat_account_settings
import dorai.composeapp.generated.resources.chat_agent_activity
import dorai.composeapp.generated.resources.chat_attachment_image
import dorai.composeapp.generated.resources.chat_choose_model
import dorai.composeapp.generated.resources.chat_clear_cd
import dorai.composeapp.generated.resources.chat_close_cd
import dorai.composeapp.generated.resources.chat_copy_message
import dorai.composeapp.generated.resources.chat_copy_thoughts
import dorai.composeapp.generated.resources.chat_delete_cd
import dorai.composeapp.generated.resources.chat_dismiss
import dorai.composeapp.generated.resources.chat_empty_file
import dorai.composeapp.generated.resources.chat_empty_image
import dorai.composeapp.generated.resources.chat_empty_subtitle
import dorai.composeapp.generated.resources.chat_empty_title
import dorai.composeapp.generated.resources.chat_empty_workspace
import dorai.composeapp.generated.resources.chat_history_cd
import dorai.composeapp.generated.resources.chat_history_title
import dorai.composeapp.generated.resources.chat_jump_to_latest
import dorai.composeapp.generated.resources.chat_model_picker_title
import dorai.composeapp.generated.resources.chat_no_chats_found
import dorai.composeapp.generated.resources.chat_older
import dorai.composeapp.generated.resources.chat_pin_toggle_cd
import dorai.composeapp.generated.resources.chat_pinned_cd
import dorai.composeapp.generated.resources.chat_plan_run_build
import dorai.composeapp.generated.resources.chat_plan_title
import dorai.composeapp.generated.resources.chat_quick_ask_desc
import dorai.composeapp.generated.resources.chat_quick_ask_title
import dorai.composeapp.generated.resources.chat_quick_build_desc
import dorai.composeapp.generated.resources.chat_quick_build_title
import dorai.composeapp.generated.resources.chat_quick_file_desc
import dorai.composeapp.generated.resources.chat_quick_file_title
import dorai.composeapp.generated.resources.chat_quick_image_desc
import dorai.composeapp.generated.resources.chat_quick_image_title
import dorai.composeapp.generated.resources.chat_quick_plan_desc
import dorai.composeapp.generated.resources.chat_quick_plan_title
import dorai.composeapp.generated.resources.chat_quick_project_desc
import dorai.composeapp.generated.resources.chat_quick_project_title
import dorai.composeapp.generated.resources.chat_quick_review_desc
import dorai.composeapp.generated.resources.chat_quick_review_title
import dorai.composeapp.generated.resources.chat_read_aloud
import dorai.composeapp.generated.resources.chat_reasoning_label
import dorai.composeapp.generated.resources.chat_regenerate_message
import dorai.composeapp.generated.resources.chat_rename_cd
import dorai.composeapp.generated.resources.chat_rename_hint
import dorai.composeapp.generated.resources.chat_rename_title
import dorai.composeapp.generated.resources.chat_retry
import dorai.composeapp.generated.resources.chat_search_placeholder
import dorai.composeapp.generated.resources.chat_stop_speaking
import dorai.composeapp.generated.resources.chat_tagline
import dorai.composeapp.generated.resources.chat_thought_steps
import dorai.composeapp.generated.resources.chat_today
import dorai.composeapp.generated.resources.chat_welcome_ask
import dorai.composeapp.generated.resources.chat_welcome_build
import dorai.composeapp.generated.resources.chat_welcome_plan
import dorai.composeapp.generated.resources.chat_welcome_review
import dorai.composeapp.generated.resources.composer_stop
import dorai.composeapp.generated.resources.fallback_answered_by
import dorai.composeapp.generated.resources.ic_file
import dorai.composeapp.generated.resources.ic_image
import dorai.composeapp.generated.resources.logo
import dorai.composeapp.generated.resources.nav_brand_title
import dorai.composeapp.generated.resources.new_chat_content_description
import dorai.composeapp.generated.resources.qp_ask_1_prompt
import dorai.composeapp.generated.resources.qp_ask_1_tag
import dorai.composeapp.generated.resources.qp_ask_1_title
import dorai.composeapp.generated.resources.qp_ask_2_prompt
import dorai.composeapp.generated.resources.qp_ask_2_tag
import dorai.composeapp.generated.resources.qp_ask_2_title
import dorai.composeapp.generated.resources.qp_ask_3_prompt
import dorai.composeapp.generated.resources.qp_ask_3_tag
import dorai.composeapp.generated.resources.qp_ask_3_title
import dorai.composeapp.generated.resources.qp_ask_4_prompt
import dorai.composeapp.generated.resources.qp_ask_4_tag
import dorai.composeapp.generated.resources.qp_ask_4_title
import dorai.composeapp.generated.resources.qp_build_1_prompt
import dorai.composeapp.generated.resources.qp_build_1_tag
import dorai.composeapp.generated.resources.qp_build_1_title
import dorai.composeapp.generated.resources.qp_build_2_prompt
import dorai.composeapp.generated.resources.qp_build_2_tag
import dorai.composeapp.generated.resources.qp_build_2_title
import dorai.composeapp.generated.resources.qp_build_3_prompt
import dorai.composeapp.generated.resources.qp_build_3_tag
import dorai.composeapp.generated.resources.qp_build_3_title
import dorai.composeapp.generated.resources.qp_build_4_prompt
import dorai.composeapp.generated.resources.qp_build_4_tag
import dorai.composeapp.generated.resources.qp_build_4_title
import dorai.composeapp.generated.resources.qp_plan_1_prompt
import dorai.composeapp.generated.resources.qp_plan_1_tag
import dorai.composeapp.generated.resources.qp_plan_1_title
import dorai.composeapp.generated.resources.qp_plan_2_prompt
import dorai.composeapp.generated.resources.qp_plan_2_tag
import dorai.composeapp.generated.resources.qp_plan_2_title
import dorai.composeapp.generated.resources.qp_plan_3_prompt
import dorai.composeapp.generated.resources.qp_plan_3_tag
import dorai.composeapp.generated.resources.qp_plan_3_title
import dorai.composeapp.generated.resources.qp_plan_4_prompt
import dorai.composeapp.generated.resources.qp_plan_4_tag
import dorai.composeapp.generated.resources.qp_plan_4_title
import dorai.composeapp.generated.resources.qp_review_1_prompt
import dorai.composeapp.generated.resources.qp_review_1_tag
import dorai.composeapp.generated.resources.qp_review_1_title
import dorai.composeapp.generated.resources.qp_review_2_prompt
import dorai.composeapp.generated.resources.qp_review_2_tag
import dorai.composeapp.generated.resources.qp_review_2_title
import dorai.composeapp.generated.resources.qp_review_3_prompt
import dorai.composeapp.generated.resources.qp_review_3_tag
import dorai.composeapp.generated.resources.qp_review_3_title
import dorai.composeapp.generated.resources.qp_review_4_prompt
import dorai.composeapp.generated.resources.qp_review_4_tag
import dorai.composeapp.generated.resources.qp_review_4_title
import dorai.composeapp.generated.resources.settings_memories_edit_cancel
import dorai.composeapp.generated.resources.settings_memories_edit_save
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class ChatMessageItem(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val modelName: String? = null,
    val isModelLocal: Boolean = false,
    val toolExecutions: List<ToolExecutionModel> = emptyList(),
    val dynamicUi: DynamicUiModel? = null,
    val attachments: List<Attachment> = emptyList(),
    val reasoningSegments: List<String> = emptyList(),
    val planItems: List<String> = emptyList(),
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val fallbackServiceName: String? = null,
)

data class ChatConversationSummary(
    val id: String,
    val title: String,
    val preview: String = "",
    val isToday: Boolean = true,
    val isPinned: Boolean = false,
)

/**
 * UI v2 Chat screen featuring:
 * - Adaptive list-detail scaffold (History pane + Conversation pane)
 * - Empty state with mode-dependent quick starts
 * - Message timeline with Markdown, code highlighting, tool cards, and dynamic UI blocks
 * - Collapsible reasoning/thinking blocks
 * - Multimodal user attachments rendering
 * - Text-to-speech audio playback action
 * - Data-driven mode selector & multiline composer with file attachment preview chips
 * - Model picker sheet
 */
@Composable
fun DoraiChatScreen(
    messages: List<ChatMessageItem>,
    conversations: List<ChatConversationSummary>,
    currentModelName: String,
    isModelLocal: Boolean,
    selectedModeId: String,
    onModeSelected: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeConversationId: String? = null,
    isStreaming: Boolean = false,
    onStopStreaming: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onOpenModelPicker: (() -> Unit)? = null,
    onAllowOnce: ((String) -> Unit)? = null,
    onAllowSession: ((String) -> Unit)? = null,
    onDeny: ((String) -> Unit)? = null,
    onRetryTool: ((String) -> Unit)? = null,
    onAskFixTool: ((String) -> Unit)? = null,
    onDynamicUiOptionSelected: ((String) -> Unit)? = null,
    onDynamicUiSubmit: (() -> Unit)? = null,
    onRunPlan: (() -> Unit)? = null,
    onSpeak: ((String) -> Unit)? = null,
    files: ImmutableList<PlatformFile> = persistentListOf(),
    onRemoveFile: ((PlatformFile) -> Unit)? = null,
    onAttachFile: (() -> Unit)? = null,
    onAttachImage: (() -> Unit)? = null,
    onMicClick: (() -> Unit)? = null,
    hasUnreadHeartbeat: Boolean = false,
    onHeartbeatTap: (() -> Unit)? = null,
    onHeartbeatDismiss: (() -> Unit)? = null,
    smsDrafts: ImmutableList<SmsDraft> = persistentListOf(),
    onSendSms: ((String) -> Unit)? = null,
    onDiscardSms: ((String) -> Unit)? = null,
    onTogglePinConversation: ((String) -> Unit)? = null,
    onDeleteConversation: ((String) -> Unit)? = null,
    onRenameConversation: ((String, String) -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToWorkspace: (() -> Unit)? = null,
    maxContextTokens: Int? = null,
    warning: StringResource? = null,
    error: UiError? = null,
    showFreeProviderSuggestions: Boolean = false,
    onRetryError: (() -> Unit)? = null,
    onDismissError: (() -> Unit)? = null,
) {
    val widthClass = LocalDoraiWindowWidthClass.current
    val colors = LocalDoraiColors.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (widthClass.isCompact) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .background(colors.surface),
                ) {
                    ChatHistoryPane(
                        conversations = conversations,
                        activeConversationId = activeConversationId,
                        onOpenConversation = { id ->
                            onOpenConversation(id)
                            scope.launch { drawerState.close() }
                        },
                        onNewChat = {
                            onNewChat()
                            scope.launch { drawerState.close() }
                        },
                        onCloseDrawer = { scope.launch { drawerState.close() } },
                        onTogglePin = onTogglePinConversation,
                        onDelete = onDeleteConversation,
                        onRename = onRenameConversation,
                    )
                }
            },
        ) {
            ChatDetailPane(
                messages = messages,
                currentModelName = currentModelName,
                isModelLocal = isModelLocal,
                selectedModeId = selectedModeId,
                onModeSelected = onModeSelected,
                onSendMessage = onSendMessage,
                onNewChat = onNewChat,
                isStreaming = isStreaming,
                onStopStreaming = onStopStreaming,
                onRegenerate = onRegenerate,
                onOpenModelPicker = onOpenModelPicker,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onAllowOnce = onAllowOnce,
                onAllowSession = onAllowSession,
                onDeny = onDeny,
                onRetryTool = onRetryTool,
                onAskFixTool = onAskFixTool,
                onDynamicUiOptionSelected = onDynamicUiOptionSelected,
                onDynamicUiSubmit = onDynamicUiSubmit,
                onRunPlan = onRunPlan,
                onSpeak = onSpeak,
                files = files,
                onRemoveFile = onRemoveFile,
                onAttachFile = onAttachFile,
                onAttachImage = onAttachImage,
                onMicClick = onMicClick,
                hasUnreadHeartbeat = hasUnreadHeartbeat,
                onHeartbeatTap = onHeartbeatTap,
                onHeartbeatDismiss = onHeartbeatDismiss,
                smsDrafts = smsDrafts,
                onSendSms = onSendSms,
                onDiscardSms = onDiscardSms,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToWorkspace = onNavigateToWorkspace,
                maxContextTokens = maxContextTokens,
                warning = warning,
                error = error,
                showFreeProviderSuggestions = showFreeProviderSuggestions,
                onRetryError = onRetryError,
                onDismissError = onDismissError,
            )
        }
    } else {
        DoraiListDetailPaneScaffold(
            listPane = {
                ChatHistoryPane(
                    conversations = conversations,
                    activeConversationId = activeConversationId,
                    onOpenConversation = onOpenConversation,
                    onNewChat = onNewChat,
                    onCloseDrawer = null,
                    onTogglePin = onTogglePinConversation,
                    onDelete = onDeleteConversation,
                    onRename = onRenameConversation,
                )
            },
            detailPane = {
                ChatDetailPane(
                    messages = messages,
                    currentModelName = currentModelName,
                    isModelLocal = isModelLocal,
                    selectedModeId = selectedModeId,
                    onModeSelected = onModeSelected,
                    onSendMessage = onSendMessage,
                    onNewChat = onNewChat,
                    isStreaming = isStreaming,
                    onStopStreaming = onStopStreaming,
                    onRegenerate = onRegenerate,
                    onOpenModelPicker = onOpenModelPicker,
                    onOpenDrawer = null,
                    onAllowOnce = onAllowOnce,
                    onAllowSession = onAllowSession,
                    onDeny = onDeny,
                    onRetryTool = onRetryTool,
                    onAskFixTool = onAskFixTool,
                    onDynamicUiOptionSelected = onDynamicUiOptionSelected,
                    onDynamicUiSubmit = onDynamicUiSubmit,
                    onRunPlan = onRunPlan,
                    onSpeak = onSpeak,
                    files = files,
                    onRemoveFile = onRemoveFile,
                    onAttachFile = onAttachFile,
                    onAttachImage = onAttachImage,
                    onMicClick = onMicClick,
                    hasUnreadHeartbeat = hasUnreadHeartbeat,
                    onHeartbeatTap = onHeartbeatTap,
                    onHeartbeatDismiss = onHeartbeatDismiss,
                    smsDrafts = smsDrafts,
                    onSendSms = onSendSms,
                    onDiscardSms = onDiscardSms,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToWorkspace = onNavigateToWorkspace,
                    maxContextTokens = maxContextTokens,
                    warning = warning,
                    error = error,
                    showFreeProviderSuggestions = showFreeProviderSuggestions,
                    onRetryError = onRetryError,
                    onDismissError = onDismissError,
                )
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun ChatHistoryPane(
    conversations: List<ChatConversationSummary>,
    onOpenConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onTogglePin: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    onCloseDrawer: (() -> Unit)? = null,
    activeConversationId: String? = null,
    onRename: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current
    var searchQuery by remember { mutableStateOf("") }
    var renamingConv by remember { mutableStateOf<ChatConversationSummary?>(null) }

    val filtered = conversations.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.preview.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.isPinned }

    val todayConvs = remember(filtered) { filtered.filter { it.isToday } }
    val earlierConvs = remember(filtered) { filtered.filter { !it.isToday } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(DoraiSpacing.MD),
    ) {
        if (widthClass.isCompact && onCloseDrawer != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = DoraiSpacing.SM),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoAnimation(
                        size = 26.dp,
                        animate = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.nav_brand_title),
                        color = colors.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.chat_close_cd),
                        tint = colors.muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Full-width + New Chat button matching prototype: <button class="btn primary wide" data-act="newchat">
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.new_chat_content_description),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        // Search box: <label class="sbox"><svg>search</svg><input placeholder="Search chats"></label>
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(Res.string.chat_search_placeholder), fontSize = 13.sp, color = colors.muted) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(16.dp),
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(Res.string.chat_clear_cd),
                            tint = colors.muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface2,
                unfocusedContainerColor = colors.surface2,
                focusedBorderColor = colors.info,
                unfocusedBorderColor = colors.line,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        // Conversation items with Today and Earlier grouping
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.chat_no_chats_found),
                        color = colors.muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = DoraiSpacing.MD, horizontal = 4.dp),
                    )
                }
            } else {
                if (todayConvs.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.chat_today),
                            color = colors.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                    items(todayConvs, key = { it.id }) { conv ->
                        ConversationItemRow(
                            conv = conv,
                            isActive = conv.id == activeConversationId,
                            onOpen = { onOpenConversation(conv.id) },
                            onTogglePin = onTogglePin,
                            onDelete = onDelete,
                            onRenameRequest = onRename?.let { { renamingConv = conv } },
                        )
                    }
                }

                if (earlierConvs.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.chat_older),
                            color = colors.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                    items(earlierConvs, key = { it.id }) { conv ->
                        ConversationItemRow(
                            conv = conv,
                            isActive = conv.id == activeConversationId,
                            onOpen = { onOpenConversation(conv.id) },
                            onTogglePin = onTogglePin,
                            onDelete = onDelete,
                            onRenameRequest = onRename?.let { { renamingConv = conv } },
                        )
                    }
                }
            }
        }
    }

    if (renamingConv != null && onRename != null) {
        val conv = renamingConv!!
        var newTitle by remember(conv.id) { mutableStateOf(conv.title) }
        DoraiModalSheet(onDismissRequest = { renamingConv = null }) {
            val sheetColors = LocalDoraiColors.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DoraiSpacing.LG),
            ) {
                Text(
                    text = stringResource(Res.string.chat_rename_title),
                    color = sheetColors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(DoraiSpacing.MD))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = { Text(stringResource(Res.string.chat_rename_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = sheetColors.surface2,
                        unfocusedContainerColor = sheetColors.surface2,
                        focusedTextColor = sheetColors.text,
                        unfocusedTextColor = sheetColors.text,
                    ),
                )
                Spacer(modifier = Modifier.height(DoraiSpacing.MD))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { renamingConv = null }) {
                        Text(stringResource(Res.string.settings_memories_edit_cancel))
                    }
                    Button(
                        onClick = {
                            val trimmed = newTitle.trim()
                            if (trimmed.isNotBlank()) {
                                onRename(conv.id, trimmed)
                            }
                            renamingConv = null
                        },
                        enabled = newTitle.trim().isNotBlank(),
                    ) {
                        Text(stringResource(Res.string.settings_memories_edit_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItemRow(
    conv: ChatConversationSummary,
    isActive: Boolean,
    onOpen: () -> Unit,
    onTogglePin: ((String) -> Unit)?,
    onDelete: ((String) -> Unit)?,
    onRenameRequest: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val itemShape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(if (isActive) colors.surface2 else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isActive) colors.primary.copy(alpha = 0.6f) else Color.Transparent,
                shape = itemShape,
            )
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conv.title,
                    color = if (isActive) colors.primary else colors.text,
                    fontSize = 13.5.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (conv.isPinned) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = stringResource(Res.string.chat_pinned_cd),
                        tint = colors.primary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            if (conv.preview.isNotBlank()) {
                Text(
                    text = conv.preview,
                    color = colors.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (onRenameRequest != null) {
            IconButton(
                onClick = onRenameRequest,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.chat_rename_cd),
                    tint = colors.muted.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (onTogglePin != null) {
            IconButton(
                onClick = { onTogglePin(conv.id) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = stringResource(Res.string.chat_pin_toggle_cd),
                    tint = if (conv.isPinned) colors.primary else colors.muted.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (onDelete != null) {
            IconButton(
                onClick = { onDelete(conv.id) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.chat_delete_cd),
                    tint = colors.muted.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatDetailPane(
    messages: List<ChatMessageItem>,
    currentModelName: String,
    isModelLocal: Boolean,
    selectedModeId: String,
    onModeSelected: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onNewChat: () -> Unit,
    isStreaming: Boolean,
    onStopStreaming: (() -> Unit)?,
    onRegenerate: (() -> Unit)?,
    onOpenModelPicker: (() -> Unit)?,
    onOpenDrawer: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onAllowOnce: ((String) -> Unit)? = null,
    onAllowSession: ((String) -> Unit)? = null,
    onDeny: ((String) -> Unit)? = null,
    onRetryTool: ((String) -> Unit)? = null,
    onAskFixTool: ((String) -> Unit)? = null,
    onDynamicUiOptionSelected: ((String) -> Unit)? = null,
    onDynamicUiSubmit: (() -> Unit)? = null,
    onRunPlan: (() -> Unit)? = null,
    onSpeak: ((String) -> Unit)? = null,
    files: ImmutableList<PlatformFile> = persistentListOf(),
    onRemoveFile: ((PlatformFile) -> Unit)? = null,
    onAttachFile: (() -> Unit)? = null,
    onAttachImage: (() -> Unit)? = null,
    onMicClick: (() -> Unit)? = null,
    hasUnreadHeartbeat: Boolean = false,
    onHeartbeatTap: (() -> Unit)? = null,
    onHeartbeatDismiss: (() -> Unit)? = null,
    smsDrafts: ImmutableList<SmsDraft> = persistentListOf(),
    onSendSms: ((String) -> Unit)? = null,
    onDiscardSms: ((String) -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToWorkspace: (() -> Unit)? = null,
    maxContextTokens: Int? = null,
    warning: StringResource? = null,
    error: UiError? = null,
    showFreeProviderSuggestions: Boolean = false,
    onRetryError: (() -> Unit)? = null,
    onDismissError: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current
    val listState = rememberLazyListState()
    var composerInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val isAtBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || lastVisible >= total - 2
        }
    }

    var hasInitialScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && (!hasInitialScrolled || isAtBottom)) {
            listState.animateScrollToItem(messages.lastIndex)
            hasInitialScrolled = true
        }
    }

    // imePadding is provided by DoraiAdaptiveScaffold at the shell level.
    // Do NOT add a second imePadding here (double keyboard offset).
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // Chat Top Bar (56dp fixed height matching prototype header.top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = DoraiSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (widthClass.isCompact && onOpenDrawer != null) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(Res.string.chat_history_cd),
                        tint = colors.text,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Model pill with dropdown chevron (matching prototype)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surface2)
                    .border(1.dp, colors.line, CircleShape)
                    .clickable { onOpenModelPicker?.invoke() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentModelName,
                        color = colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    DoraiProviderBadge(isLocal = isModelLocal, iconOnly = true)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(Res.string.chat_choose_model),
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Active mode badge (all width classes; previously hidden on Compact).
            run {
                Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                val activeMode = com.dorai.ai.ui.components.v2.DefaultAgentModes.firstOrNull { it.id == selectedModeId }
                val activeModeIcon = activeMode?.icon ?: Icons.AutoMirrored.Filled.Chat
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.line, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = activeModeIcon,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activeMode?.let { stringResource(it.nameRes) } ?: selectedModeId.replaceFirstChar { it.uppercase() },
                            color = colors.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onNewChat, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.new_chat_content_description),
                    tint = colors.text,
                )
            }

            if (widthClass.isCompact && onNavigateToSettings != null) {
                IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(Res.string.chat_account_settings),
                        tint = colors.text,
                    )
                }
            }
        }

        HorizontalDivider(color = colors.line.copy(alpha = 0.5f), thickness = 1.dp)

        // Proactive Heartbeat and SMS Banners
        if (hasUnreadHeartbeat && onHeartbeatTap != null && onHeartbeatDismiss != null) {
            HeartbeatBanner(
                visible = true,
                onTap = onHeartbeatTap,
                onDismiss = onHeartbeatDismiss,
            )
        }
        if (smsDrafts.isNotEmpty() && onSendSms != null && onDiscardSms != null) {
            PendingSmsBanners(
                drafts = smsDrafts,
                onSend = onSendSms,
                onDiscard = onDiscardSms,
            )
        }
        warning?.let { warningRes ->
            Text(
                text = stringResource(warningRes),
                color = colors.err,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
            )
        }

        // Main Messages / Empty State Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (messages.isEmpty()) {
                ChatEmptyState(
                    selectedModeId = selectedModeId,
                    onSelectQuickPrompt = { prompt: String ->
                        composerInput = prompt
                    },
                    onAttachImage = onAttachImage,
                    onAttachFile = onAttachFile,
                    onNavigateToWorkspace = onNavigateToWorkspace,
                )
            } else {
                // Centered max-width column for large windows (was unbounded).
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .widthIn(max = ChatAdaptivePolicy.conversationColumnMax)
                            .fillMaxSize()
                            .padding(horizontal = DoraiSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageRow(
                                message = msg,
                                onRegenerate = onRegenerate,
                                onSpeak = onSpeak,
                                onAllowOnce = onAllowOnce,
                                onAllowSession = onAllowSession,
                                onDeny = onDeny,
                                onRetryTool = onRetryTool,
                                onAskFixTool = onAskFixTool,
                                onDynamicUiOptionSelected = onDynamicUiOptionSelected,
                                onDynamicUiSubmit = onDynamicUiSubmit,
                                onRunPlan = onRunPlan,
                            )
                        }
                    }
                }
            }

            // Jump to latest button
            androidx.compose.animation.AnimatedVisibility(
                visible = !isAtBottom && messages.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DoraiSpacing.SM),
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                        }
                    },
                    containerColor = colors.surface2,
                    contentColor = colors.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = stringResource(Res.string.chat_jump_to_latest),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Error toast badge (ported from legacy ChatScreen; hidden while free-tier upsell shows)
            if (error != null && !showFreeProviderSuggestions) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = uiErrorText(error),
                            color = colors.text,
                            fontSize = 13.sp,
                            modifier = Modifier.widthIn(max = 260.dp),
                        )
                        if (onRetryError != null) {
                            IconButton(
                                onClick = onRetryError,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(Res.string.chat_retry),
                                    tint = colors.text,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (onDismissError != null) {
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.chat_dismiss),
                                    tint = colors.muted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Composer Section at Bottom (centered to match the message column)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ChatAdaptivePolicy.conversationColumnMax)
                    .fillMaxWidth()
                    .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
            ) {
                DoraiModeSelector(
                    selectedModeId = selectedModeId,
                    onModeSelected = onModeSelected,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                DoraiComposer(
                    value = composerInput,
                    onValueChange = { composerInput = it },
                    onSend = {
                        val prompt = composerInput.trim()
                        if (prompt.isNotEmpty() || files.isNotEmpty()) {
                            composerInput = ""
                            onSendMessage(prompt)
                        }
                    },
                    isStreaming = isStreaming,
                    onStopStreaming = onStopStreaming,
                    onAttach = onAttachFile,
                    onMicClick = onMicClick,
                    files = files,
                    onRemoveFile = onRemoveFile,
                    maxContextTokens = maxContextTokens,
                )
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageRow(
    message: ChatMessageItem,
    onRegenerate: (() -> Unit)?,
    onSpeak: ((String) -> Unit)? = null,
    onAllowOnce: ((String) -> Unit)? = null,
    onAllowSession: ((String) -> Unit)? = null,
    onDeny: ((String) -> Unit)? = null,
    onRetryTool: ((String) -> Unit)? = null,
    onAskFixTool: ((String) -> Unit)? = null,
    onDynamicUiOptionSelected: ((String) -> Unit)? = null,
    onDynamicUiSubmit: (() -> Unit)? = null,
    onRunPlan: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val copyToClipboard = rememberCopyToClipboard()

    if (message.isUser) {
        // User message bubble on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.line.copy(alpha = 0.4f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Column {
                    // Image attachments
                    val images = message.attachments.filter { it.mimeType.startsWith("image/") }
                    val otherFiles = message.attachments.filter { !it.mimeType.startsWith("image/") }
                    for (att in images) {
                        val imageBitmap = remember(att.data) {
                            try {
                                decodeToImageBitmap(Base64.decode(att.data))
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = stringResource(Res.string.chat_attachment_image),
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .padding(bottom = 6.dp),
                                contentScale = ContentScale.FillWidth,
                            )
                        }
                    }
                    if (otherFiles.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp),
                        ) {
                            for (att in otherFiles) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surface,
                                    border = BorderStroke(1.dp, colors.line),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_file),
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = att.fileName ?: att.mimeType,
                                            color = colors.text,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (message.text.isNotEmpty()) {
                        SelectionContainer {
                            Text(
                                text = message.text,
                                color = colors.text,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }
            }
        }
    } else {
        // AI message full-width, clean modern layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            // Who row: Brand Mark (20dp) + Model name + Badge + Optional Fallback indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message.modelName ?: stringResource(Res.string.nav_brand_title),
                    color = colors.muted,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                DoraiProviderBadge(isLocal = message.isModelLocal, iconOnly = true)
                if (message.fallbackServiceName != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.fallback_answered_by, message.fallbackServiceName),
                        color = colors.muted,
                        fontSize = 11.5.sp,
                    )
                }
            }

            // Reasoning / Thinking block
            if (message.reasoningSegments.isNotEmpty() || message.isThinking) {
                DoraiReasoningBlock(
                    segments = message.reasoningSegments,
                    isComplete = !message.isThinking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }

            // Message text parsed with rich Markdown (code highlighting, math, tables)
            if (message.text.isNotEmpty()) {
                val document = remember(message.text) { parseMarkdown(message.text) }
                SelectionContainer {
                    // Pin the v2 text token: without an explicit LocalContentColor the
                    // markdown falls back to MaterialTheme onSurface, which can resolve
                    // dark-on-dark in some presets — rendering the answer unreadable.
                    CompositionLocalProvider(LocalContentColor provides colors.text) {
                        MarkdownContent(
                            document = document,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Plan Card (matching prototype planCard)
            if (message.planItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                DoraiPlanCard(
                    items = message.planItems,
                    onRunPlan = { onRunPlan?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Embedded Agent Activity / Tool cards
            if (message.toolExecutions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                if (message.toolExecutions.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = colors.info,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.chat_agent_activity, message.toolExecutions.size),
                            color = colors.muted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                message.toolExecutions.forEach { tool ->
                    Spacer(modifier = Modifier.height(DoraiSpacing.XS))
                    DoraiToolCard(
                        model = tool,
                        onAllowOnce = onAllowOnce?.let { cb -> { cb(tool.toolCallId) } },
                        onAllowSession = onAllowSession?.let { cb -> { cb(tool.toolCallId) } },
                        onDeny = onDeny?.let { cb -> { cb(tool.toolCallId) } },
                        onRetry = onRetryTool?.let { cb -> { cb(tool.toolCallId) } },
                        onAskFix = onAskFixTool?.let { cb -> { cb(tool.toolCallId) } },
                    )
                }
            }

            // Embedded Dynamic UI block
            if (message.dynamicUi != null) {
                Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                DoraiDynamicUiBlock(
                    model = message.dynamicUi,
                    onOptionSelected = { onDynamicUiOptionSelected?.invoke(it) },
                    onConfirm = { onDynamicUiSubmit?.invoke() },
                )
            }

            // Action row (copy, text-to-speech speaker, regenerate) for assistant response
            if (message.text.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var isCopied by remember { mutableStateOf(false) }
                    LaunchedEffect(isCopied) {
                        if (isCopied) {
                            delay(2000)
                            isCopied = false
                        }
                    }
                    IconButton(
                        onClick = {
                            copyToClipboard(message.text)
                            isCopied = true
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(Res.string.chat_copy_message),
                            tint = if (isCopied) colors.ok else colors.muted,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    if (onSpeak != null && message.text.isNotEmpty()) {
                        IconButton(
                            onClick = { onSpeak(message.id) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = if (message.isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(if (message.isSpeaking) Res.string.chat_stop_speaking else Res.string.chat_read_aloud),
                                tint = if (message.isSpeaking) colors.primary else colors.muted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    if (onRegenerate != null) {
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(Res.string.chat_regenerate_message),
                                tint = colors.muted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * UI v2 Plan Card:
 * Displays an ordered step-by-step plan with a primary CTA button to run in Build mode.
 */
@Composable
fun DoraiPlanCard(
    items: List<String>,
    onRunPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Rule,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.chat_plan_title),
                color = colors.text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "${idx + 1}. ",
                        color = colors.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = item,
                        color = colors.text,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onRunPlan,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.chat_plan_run_build),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DoraiReasoningBlock(
    segments: List<String>,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val copyToClipboard = rememberCopyToClipboard()
    var userToggled by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(!isComplete) }
    LaunchedEffect(isComplete) {
        if (!userToggled && isComplete) {
            expanded = false
        }
    }
    val preview = remember(segments) {
        segments.lastOrNull()
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() }
            .orEmpty()
    }

    val blockShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .clip(blockShape)
            .background(colors.surface2.copy(alpha = 0.6f))
            .border(1.dp, colors.line.copy(alpha = 0.7f), blockShape),
    ) {
        // Compact Activity Summary Bar (min-height 36dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    userToggled = true
                    expanded = !expanded
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(12.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (!isComplete) stringResource(Res.string.bot_message_thinking_label) else stringResource(Res.string.chat_reasoning_label),
                    color = colors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (segments.size > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.chat_thought_steps, segments.size),
                            color = colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                if (!expanded && preview.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "· $preview",
                        color = colors.muted,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.bot_message_thinking_expand_content_description),
                tint = colors.muted,
                modifier = Modifier.size(16.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState())
                    .background(colors.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val fullThought = segments.joinToString("\n\n")
                    IconButton(
                        onClick = { copyToClipboard(fullThought) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(Res.string.chat_copy_thoughts),
                            tint = colors.muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                segments.forEachIndexed { index, segment ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = colors.line.copy(alpha = 0.4f),
                            thickness = 1.dp,
                        )
                    }
                    SelectionContainer {
                        Text(
                            text = segment,
                            color = colors.muted,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

private data class QuickPromptItem(
    val title: String,
    val prompt: String,
    val icon: ImageVector,
    val tag: String,
)

@Composable
private fun QuickPromptCard(
    item: QuickPromptItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.title,
                    color = colors.text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.tag,
                    color = colors.muted,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.surface2)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.prompt,
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionShortcutPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: Painter? = null,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surface2)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        } else if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = colors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ChatEmptyState(
    selectedModeId: String,
    onSelectQuickPrompt: (String) -> Unit,
    onAttachImage: (() -> Unit)? = null,
    onAttachFile: (() -> Unit)? = null,
    onNavigateToWorkspace: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current

    val welcomeTitleRes = when (selectedModeId) {
        "plan" -> Res.string.chat_welcome_plan
        "build" -> Res.string.chat_welcome_build
        "review" -> Res.string.chat_welcome_review
        else -> Res.string.chat_welcome_ask
    }
    val welcomeTitle = stringResource(welcomeTitleRes)

    val activeMode = com.dorai.ai.ui.components.v2.DefaultAgentModes.firstOrNull { it.id == selectedModeId }
    val modeBadge = if (activeMode != null) {
        stringResource(activeMode.nameRes) + " · " + stringResource(activeMode.descRes)
    } else {
        selectedModeId
    }

    val prompts = when (selectedModeId) {
        "plan" -> listOf(
            QuickPromptItem(stringResource(Res.string.qp_plan_1_title), stringResource(Res.string.qp_plan_1_prompt), Icons.AutoMirrored.Filled.Rule, stringResource(Res.string.qp_plan_1_tag)),
            QuickPromptItem(stringResource(Res.string.qp_plan_2_title), stringResource(Res.string.qp_plan_2_prompt), Icons.Default.Security, stringResource(Res.string.qp_plan_2_tag)),
            QuickPromptItem(stringResource(Res.string.qp_plan_3_title), stringResource(Res.string.qp_plan_3_prompt), Icons.Default.Build, stringResource(Res.string.qp_plan_3_tag)),
            QuickPromptItem(stringResource(Res.string.qp_plan_4_title), stringResource(Res.string.qp_plan_4_prompt), Icons.Default.CheckCircle, stringResource(Res.string.qp_plan_4_tag)),
        )

        "build" -> listOf(
            QuickPromptItem(stringResource(Res.string.qp_build_1_title), stringResource(Res.string.qp_build_1_prompt), Icons.Default.Widgets, stringResource(Res.string.qp_build_1_tag)),
            QuickPromptItem(stringResource(Res.string.qp_build_2_title), stringResource(Res.string.qp_build_2_prompt), Icons.Default.Code, stringResource(Res.string.qp_build_2_tag)),
            QuickPromptItem(stringResource(Res.string.qp_build_3_title), stringResource(Res.string.qp_build_3_prompt), Icons.Default.Terminal, stringResource(Res.string.qp_build_3_tag)),
            QuickPromptItem(stringResource(Res.string.qp_build_4_title), stringResource(Res.string.qp_build_4_prompt), Icons.Default.Build, stringResource(Res.string.qp_build_4_tag)),
        )

        "review" -> listOf(
            QuickPromptItem(stringResource(Res.string.qp_review_1_title), stringResource(Res.string.qp_review_1_prompt), Icons.Default.Speed, stringResource(Res.string.qp_review_1_tag)),
            QuickPromptItem(stringResource(Res.string.qp_review_2_title), stringResource(Res.string.qp_review_2_prompt), Icons.Default.Security, stringResource(Res.string.qp_review_2_tag)),
            QuickPromptItem(stringResource(Res.string.qp_review_3_title), stringResource(Res.string.qp_review_3_prompt), Icons.Default.Search, stringResource(Res.string.qp_review_3_tag)),
            QuickPromptItem(stringResource(Res.string.qp_review_4_title), stringResource(Res.string.qp_review_4_prompt), Icons.Default.Visibility, stringResource(Res.string.qp_review_4_tag)),
        )

        else -> listOf(
            QuickPromptItem(stringResource(Res.string.qp_ask_1_title), stringResource(Res.string.qp_ask_1_prompt), Icons.Default.Lightbulb, stringResource(Res.string.qp_ask_1_tag)),
            QuickPromptItem(stringResource(Res.string.qp_ask_2_title), stringResource(Res.string.qp_ask_2_prompt), Icons.Default.Bolt, stringResource(Res.string.qp_ask_2_tag)),
            QuickPromptItem(stringResource(Res.string.qp_ask_3_title), stringResource(Res.string.qp_ask_3_prompt), Icons.AutoMirrored.Filled.CompareArrows, stringResource(Res.string.qp_ask_3_tag)),
            QuickPromptItem(stringResource(Res.string.qp_ask_4_title), stringResource(Res.string.qp_ask_4_prompt), Icons.Default.Extension, stringResource(Res.string.qp_ask_4_tag)),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(DoraiSpacing.MD),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // AI-Focused Welcome Hero
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                LogoAnimation(
                    size = 48.dp,
                    animate = true,
                )
            }

            Spacer(modifier = Modifier.height(DoraiSpacing.MD))

            Text(
                text = welcomeTitle,
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(Res.string.chat_empty_subtitle),
                color = colors.muted,
                fontSize = 13.5.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Active Mode Pill
            Text(
                text = modeBadge,
                color = colors.primary,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.08f))
                    .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(DoraiSpacing.LG))

            // Quick Prompt Suggestions Grid
            if (widthClass.isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    prompts.forEach { item ->
                        QuickPromptCard(
                            item = item,
                            onClick = { onSelectQuickPrompt(item.prompt) },
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    prompts.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowItems.forEach { item ->
                                QuickPromptCard(
                                    item = item,
                                    onClick = { onSelectQuickPrompt(item.prompt) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DoraiSpacing.MD))

            // Quick Action Shortcuts Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onAttachImage != null) {
                    ActionShortcutPill(
                        label = stringResource(Res.string.chat_empty_image),
                        painter = painterResource(Res.drawable.ic_image),
                        onClick = onAttachImage,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (onAttachFile != null) {
                    ActionShortcutPill(
                        label = stringResource(Res.string.chat_empty_file),
                        painter = painterResource(Res.drawable.ic_file),
                        onClick = onAttachFile,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (onNavigateToWorkspace != null) {
                    ActionShortcutPill(
                        label = stringResource(Res.string.chat_empty_workspace),
                        icon = Icons.Default.Folder,
                        onClick = onNavigateToWorkspace,
                    )
                }
            }
        }
    }
}
