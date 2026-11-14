package com.dorai.ai.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.Platform
import com.dorai.ai.currentPlatform
import com.dorai.ai.data.Attachment
import com.dorai.ai.data.SmsDraft
import com.dorai.ai.decodeToImageBitmap
import com.dorai.ai.getBackgroundDispatcher
import com.dorai.ai.network.UiError
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.DoraiListDetailPaneScaffold
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.chat.composables.AnimatedTypingDots
import com.dorai.ai.ui.chat.composables.ChatCommandPalette
import com.dorai.ai.ui.chat.composables.FreeProviderSuggestionsPanel
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
import com.dorai.ai.ui.components.v2.ToolExecutionState
import com.dorai.ai.ui.dynamicui.FrozenSubmission
import com.dorai.ai.ui.markdown.MarkdownContent
import com.dorai.ai.ui.markdown.parseMarkdown
import com.dorai.ai.ui.rememberCopyToClipboard
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.bot_message_thinking_expand_content_description
import dorai.composeapp.generated.resources.bot_message_thinking_label
import dorai.composeapp.generated.resources.chat_account_settings
import dorai.composeapp.generated.resources.chat_agent_activity_shell_many
import dorai.composeapp.generated.resources.chat_agent_activity_shell_one
import dorai.composeapp.generated.resources.chat_agent_activity_tools_many
import dorai.composeapp.generated.resources.chat_agent_activity_tools_one
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
import dorai.composeapp.generated.resources.chat_message_sent_at
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
import dorai.composeapp.generated.resources.chat_worked_for
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
import dorai.composeapp.generated.resources.state_collapsed
import dorai.composeapp.generated.resources.state_expanded
import dorai.composeapp.generated.resources.waiting_thinking
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
internal fun ChatDetailPane(
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
    onDynamicUiSubmit: ((String) -> Unit)? = null,
    isInteractiveMode: Boolean = false,
    frozenByAssistantId: Map<String, FrozenSubmission> = emptyMap(),
    onUiCallback: ((String, Map<String, String>) -> Unit)? = null,
    composerPrefill: String? = null,
    onConsumeComposerPrefill: (() -> Unit)? = null,
    onRunPlan: (() -> Unit)? = null,
    onSpeak: ((String) -> Unit)? = null,
    files: ImmutableList<PlatformFile> = persistentListOf(),
    onRemoveFile: ((PlatformFile) -> Unit)? = null,
    onAttachFile: (() -> Unit)? = null,
    onAttachImage: (() -> Unit)? = null,
    onMicClick: (() -> Unit)? = null,
    /** True while a voice turn is capturing speech; the mic button becomes a stop button. */
    isVoiceRecording: Boolean = false,
    /** Short status line shown under the composer while a voice turn is running. */
    voiceStatusText: String? = null,
    hasUnreadHeartbeat: Boolean = false,
    onHeartbeatTap: (() -> Unit)? = null,
    onHeartbeatDismiss: (() -> Unit)? = null,
    smsDrafts: ImmutableList<SmsDraft> = persistentListOf(),
    onSendSms: ((String) -> Unit)? = null,
    onDiscardSms: ((String) -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    conversations: List<ChatConversationSummary> = emptyList(),
    onOpenConversation: ((String) -> Unit)? = null,
    onNavigateToWorkspace: (() -> Unit)? = null,
    maxContextTokens: Int? = null,
    warning: StringResource? = null,
    error: UiError? = null,
    showFreeProviderSuggestions: Boolean = false,
    onRetryError: (() -> Unit)? = null,
    onDismissError: (() -> Unit)? = null,
    /**
     * Optional focus handle for the composer. Callers that open modal sheets over the
     * chat (model picker, attach sheet) pass theirs so focus can return to the input
     * when the sheet closes. When null, the pane creates its own.
     */
    composerFocusRequester: FocusRequester? = null,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current
    val listState = rememberLazyListState()
    // Share/Assist/Design-chat prefill: seed the composer from the incoming prompt.
    var composerInput by remember { mutableStateOf(composerPrefill.orEmpty()) }
    // Focus handle for the composer text field. Callers (modal sheets closing, prefill
    // arriving) focus the input instead of leaving the screen without a focus target.
    val localComposerFocus = remember { FocusRequester() }
    val composerFocus = composerFocusRequester ?: localComposerFocus
    // Consume prefills that arrive after composition (e.g. a share intent that cold-started the app)
    // exactly once per distinct prefill value.
    LaunchedEffect(composerPrefill) {
        val prefill = composerPrefill ?: return@LaunchedEffect
        composerInput = prefill
        onConsumeComposerPrefill?.invoke()
        if (prefill.isNotBlank()) {
            runCatching { composerFocus.requestFocus() }
        }
    }
    var paletteVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Follow-the-tail state. `pinnedToTail` is durable "the user is following the newest output"
    // state rather than a layout snapshot sampled at a single instant
    // (docs/audit/chat-streaming-ui-invalidation-audit-2026-11-09.md, finding 3): dragging away from
    // the bottom unpins it, coming back (or tapping jump-to-latest) pins it again, and a prompt the
    // user just sent always re-pins. An answer therefore cannot be generated below the fold while
    // the user believes they are at the bottom.
    var pinnedToTail by remember { mutableStateOf(true) }
    // Opening a chat must not jump away from the restored position, so the follow effect skips its
    // first run and only starts following on the next content change (new prompt, new stream).
    var followingArmed by remember { mutableStateOf(false) }
    val lastMessage = messages.lastOrNull()
    // First-token card visibility, derived once and shared with the follow effect so both agree on
    // whether a trailing item exists below the messages.
    val showStreamingTail = isStreaming &&
        (
            lastMessage == null || lastMessage.isUser ||
                (
                    lastMessage.text.isEmpty() && lastMessage.reasoningSegments.isEmpty() &&
                        !lastMessage.isThinking && lastMessage.toolExecutions.isEmpty() &&
                        lastMessage.dynamicUi == null && lastMessage.planItems.isEmpty()
                    )
            )
    val followTextLength = lastMessage?.text?.length ?: 0
    // Reasoning and tool blocks change the newest item's height too (attached when the turn ends).
    val followExtraHeight = (lastMessage?.reasoningSegments?.size ?: 0) + (lastMessage?.toolExecutions?.size ?: 0)
    val lastUserId = messages.lastOrNull { it.isUser }?.id

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow true
            total == 0 || lastVisible >= total - 1
        }.collect { atBottom -> pinnedToTail = atBottom }
    }

    // Sending (or restoring) a prompt re-pins: the reply to it must be followed.
    LaunchedEffect(lastUserId) {
        if (lastUserId != null) pinnedToTail = true
    }

    // Follow the streaming tail while pinned. Keyed on the newest text length (not just message
    // count) so token-by-token growth keeps the list scrolled, and aligned to the newest item's end
    // so a message taller than the viewport shows its latest text instead of its first line.
    // Never scrolls while the list is being dragged or flung: the gesture wins, and the pin above
    // decides afterwards whether following continues.
    LaunchedEffect(messages.size, followTextLength, followExtraHeight, showStreamingTail) {
        if (!followingArmed) {
            followingArmed = true
            return@LaunchedEffect
        }
        if (messages.isEmpty() || !pinnedToTail || listState.isScrollInProgress) return@LaunchedEffect
        val targetIndex = if (showStreamingTail) messages.size else messages.lastIndex
        val info = listState.layoutInfo
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(0)
        val target = info.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        val endOffset = target?.let { (it.size - viewport).coerceAtLeast(0) } ?: 0
        listState.scrollToItem(targetIndex, endOffset)
        // Layout is one frame behind composition, so the item measured above may have grown since.
        // Settle once so text that arrived in the same frame is not left below the fold.
        withFrameNanos { }
        val settled = listState.layoutInfo
        val overflow = settled.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            ?.let { it.offset + it.size - settled.viewportEndOffset }
            ?: 0
        if (overflow > 0) listState.scrollBy(overflow.toFloat())
    }

    // imePadding is provided by DoraiAdaptiveScaffold at the shell level.
    // Do NOT add a second imePadding here (double keyboard offset).
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .onPreviewKeyEvent { keyEvent ->
                if (currentPlatform !is Platform.Mobile &&
                    keyEvent.type == KeyEventType.KeyDown &&
                    (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) &&
                    keyEvent.key == Key.K
                ) {
                    paletteVisible = !paletteVisible
                    true
                } else {
                    false
                }
            },
    ) {
        // Chat Top Bar (56dp minimum height matching prototype header.top; it grows with
        // the system font scale instead of clipping the controls).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = ChatSpacing.md),
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
                Spacer(modifier = Modifier.width(ChatSpacing.xs))
            }

            // Model pill with dropdown chevron (matching prototype)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surface2)
                    .border(1.dp, colors.line, CircleShape)
                    .clickable(onClickLabel = stringResource(Res.string.chat_choose_model), role = androidx.compose.ui.semantics.Role.Button) { onOpenModelPicker?.invoke() }
                    .padding(horizontal = ChatSpacing.md, vertical = 6.dp),
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
                        modifier = Modifier.widthIn(max = 160.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    DoraiProviderBadge(isLocal = isModelLocal, iconOnly = true)
                    Spacer(modifier = Modifier.width(ChatSpacing.xs))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        // decorative — the pill's onClickLabel carries the action name
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Active mode badge (all width classes; previously hidden on Compact).
            run {
                Spacer(modifier = Modifier.width(ChatSpacing.sm))
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
                        Spacer(modifier = Modifier.width(ChatSpacing.xs))
                        Text(
                            text = activeMode?.let { stringResource(it.nameRes) } ?: selectedModeId.replaceFirstChar { it.uppercase() },
                            color = colors.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp),
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
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ChatSpacing.md, vertical = ChatSpacing.sm),
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
                val lastAssistantId = remember(messages) { messages.lastOrNull { !it.isUser }?.id }
                // Only the newest assistant answer accepts dorai-ui callbacks; anything older is
                // either read-only (batch replies) or already frozen by the user's recorded
                // submission (frozenByAssistantId).
                val lastAssistantIndex = remember(messages) { messages.indexOfLast { !it.isUser } }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .widthIn(max = ChatAdaptivePolicy.conversationColumnMax)
                            .fillMaxSize()
                            .padding(horizontal = ChatSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(ChatSpacing.md),
                    ) {
                        itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                            // Consecutive assistant messages render as one continued block:
                            // hide the repeated logo + model header on follow-ups.
                            val showHeader = index == 0 || messages[index - 1].isUser
                            val frozenForMessage = frozenByAssistantId[msg.id]
                            ChatMessageRow(
                                message = msg,
                                showHeader = showHeader,
                                showStreamingCaret = isStreaming && index == messages.lastIndex && !msg.isUser,
                                onRegenerate = if (msg.id == lastAssistantId) onRegenerate else null,
                                onSpeak = onSpeak,
                                onAllowOnce = onAllowOnce,
                                onAllowSession = onAllowSession,
                                onDeny = onDeny,
                                onRetryTool = onRetryTool,
                                onAskFixTool = onAskFixTool,
                                onDynamicUiOptionSelected = onDynamicUiOptionSelected,
                                onDynamicUiSubmit = onDynamicUiSubmit,
                                isInteractive = isInteractiveMode && !isStreaming && index == lastAssistantIndex && frozenForMessage == null,
                                frozen = frozenForMessage,
                                onUiCallback = onUiCallback,
                                onRunPlan = onRunPlan,
                            )
                        }
                        if (isStreaming) {
                            if (showStreamingTail) {
                                item(key = "streaming-tail") {
                                    // First-token "thinking" card: model name + stop, replacing a bare spinner
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = colors.surface,
                                        border = BorderStroke(1.dp, colors.line.copy(alpha = 0.5f)),
                                        modifier = Modifier.padding(horizontal = ChatSpacing.xs, vertical = ChatSpacing.xs),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = ChatSpacing.md, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            AnimatedTypingDots(dotColor = colors.primary)
                                            Text(
                                                text = "${stringResource(Res.string.waiting_thinking)} — $currentModelName",
                                                color = colors.muted,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.widthIn(max = 320.dp),
                                            )
                                            if (onStopStreaming != null) {
                                                IconButton(
                                                    onClick = onStopStreaming,
                                                    modifier = Modifier.size(48.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Stop,
                                                        contentDescription = stringResource(Res.string.composer_stop),
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
                    }
                }
            }

            // Jump to latest button
            androidx.compose.animation.AnimatedVisibility(
                visible = !pinnedToTail && messages.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ChatSpacing.sm),
            ) {
                FloatingActionButton(
                    onClick = {
                        // Follow from here on, and let the next content change settle the scroll
                        // position; animating straight to the last index would hide the newest text
                        // of a message taller than the viewport.
                        pinnedToTail = true
                        scope.launch {
                            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                        }
                    },
                    containerColor = colors.surface2,
                    contentColor = colors.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = stringResource(Res.string.chat_jump_to_latest),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Free-tier upsell: when the error qualifies (no configured services), show the
            // free-provider suggestions panel instead of the generic error badge.
            if (error != null && showFreeProviderSuggestions) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = ChatSpacing.lg)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.line, RoundedCornerShape(16.dp)),
                ) {
                    FreeProviderSuggestionsPanel(
                        error = error,
                        retry = { onRetryError?.invoke() },
                    )
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(ChatSpacing.lg)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .padding(horizontal = ChatSpacing.md, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
                    ) {
                        Text(
                            text = uiErrorText(error),
                            color = colors.text,
                            fontSize = 13.sp,
                            // Explicit line height keeps Bengali matras and conjuncts
                            // inside the line box instead of being clipped.
                            lineHeight = 18.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
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
                    .padding(horizontal = ChatSpacing.md, vertical = ChatSpacing.sm),
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
                            // Keep the keyboard anchored to the input after a send so the
                            // user can keep typing without tapping the field again.
                            runCatching { composerFocus.requestFocus() }
                        }
                    },
                    isStreaming = isStreaming,
                    onStopStreaming = onStopStreaming,
                    onAttach = onAttachFile,
                    onMicClick = onMicClick,
                    isVoiceRecording = isVoiceRecording,
                    voiceStatusText = voiceStatusText,
                    files = files,
                    onRemoveFile = onRemoveFile,
                    maxContextTokens = maxContextTokens,
                    focusRequester = composerFocus,
                )
            }
        }
    }
    if (paletteVisible) {
        ChatCommandPalette(
            conversations = conversations,
            currentModelName = currentModelName,
            onDismiss = { paletteVisible = false },
            onNewChat = onNewChat,
            onOpenModelPicker = onOpenModelPicker,
            onOpenSettings = onNavigateToSettings,
            onOpenConversation = { id -> onOpenConversation?.invoke(id) },
        )
    }
}
