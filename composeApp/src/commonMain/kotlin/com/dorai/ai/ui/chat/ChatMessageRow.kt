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

@OptIn(ExperimentalEncodingApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun ChatMessageRow(
    message: ChatMessageItem,
    showHeader: Boolean = true,
    showStreamingCaret: Boolean = false,
    onRegenerate: (() -> Unit)?,
    onSpeak: ((String) -> Unit)? = null,
    onAllowOnce: ((String) -> Unit)? = null,
    onAllowSession: ((String) -> Unit)? = null,
    onDeny: ((String) -> Unit)? = null,
    onRetryTool: ((String) -> Unit)? = null,
    onAskFixTool: ((String) -> Unit)? = null,
    onDynamicUiOptionSelected: ((String) -> Unit)? = null,
    onDynamicUiSubmit: ((String) -> Unit)? = null,
    isInteractive: Boolean = false,
    frozen: FrozenSubmission? = null,
    onUiCallback: ((String, Map<String, String>) -> Unit)? = null,
    onRunPlan: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val copyToClipboard = rememberCopyToClipboard()
    var isUserCopied by remember { mutableStateOf(false) }
    val sentAtCd = message.sentAt?.let { stringResource(Res.string.chat_message_sent_at, formatMessageTimestamp(it)) }
    LaunchedEffect(isUserCopied) {
        if (isUserCopied) {
            delay(2000)
            isUserCopied = false
        }
    }

    if (message.isUser) {
        // User message bubble on right, with a trailing copy affordance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (message.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        copyToClipboard(message.text)
                        isUserCopied = true
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(Res.string.chat_copy_message),
                        tint = if (isUserCopied) colors.ok else colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
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
                        AttachmentImage(
                            data = att.data,
                            contentDescription = stringResource(Res.string.chat_attachment_image),
                        )
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
                                        modifier = Modifier.padding(horizontal = ChatSpacing.sm, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_file),
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(ChatSpacing.xs))
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
                    if (message.sentAt != null) {
                        Text(
                            text = formatMessageTimestamp(message.sentAt),
                            color = colors.muted,
                            fontSize = 11.sp,
                            // Explicit line height so Bengali digits/matras are not clipped.
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = ChatSpacing.xs)
                                .semantics { if (sentAtCd != null) contentDescription = sentAtCd },
                        )
                    }
                }
            }
        }
    } else {
        // AI message full-width, clean modern layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ChatSpacing.xs),
        ) {
            // Who row: Brand Mark (20dp) + Model name + Badge + Optional Fallback indicator
            if (showHeader) {
                AssistantWhoRow(message = message)
            } else if (message.fallbackServiceName != null) {
                Text(
                    text = stringResource(Res.string.fallback_answered_by, message.fallbackServiceName),
                    color = colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            // Time tracking — subtle indicator at the very top of the agent's response block
            if (!message.isThinking && (message.agentWorkedDurationMs ?: 0L) >= 2_000L) {
                WorkedForIndicator(durationMs = message.agentWorkedDurationMs!!)
            }

            // Reasoning / Thinking block
            if (message.reasoningSegments.isNotEmpty() || message.isThinking) {
                DoraiReasoningBlock(
                    segments = message.reasoningSegments,
                    isComplete = !message.isThinking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = ChatSpacing.sm),
                )
            }

            // Approval-required and failed tools must stay visible outside the accordion —
            // the agent turn is blocked until the user answers them.
            val attentionTools = message.toolExecutions.filter {
                it.state == ToolExecutionState.PendingApproval || it.state == ToolExecutionState.Failed
            }
            val backgroundTools = message.toolExecutions.filter {
                it.state != ToolExecutionState.PendingApproval && it.state != ToolExecutionState.Failed
            }
            attentionTools.forEach { tool ->
                Spacer(modifier = Modifier.height(ChatSpacing.xs))
                DoraiToolCard(
                    model = tool,
                    onAllowOnce = onAllowOnce?.let { cb -> { cb(tool.toolCallId) } },
                    onAllowSession = onAllowSession?.let { cb -> { cb(tool.toolCallId) } },
                    onDeny = onDeny?.let { cb -> { cb(tool.toolCallId) } },
                    onRetry = onRetryTool?.let { cb -> { cb(tool.toolCallId) } },
                    onAskFix = onAskFixTool?.let { cb -> { cb(tool.toolCallId) } },
                )
            }

            // Collapsible processing blocks — chronological, collapsed by default; expandable for the raw logs
            if (backgroundTools.isNotEmpty()) {
                Spacer(modifier = Modifier.height(ChatSpacing.sm))
                val allShells = backgroundTools.all { isShellToolName(it.toolName) }
                val toolCount = backgroundTools.size
                val title = if (allShells) {
                    if (toolCount == 1) {
                        stringResource(Res.string.chat_agent_activity_shell_one)
                    } else {
                        stringResource(Res.string.chat_agent_activity_shell_many, toolCount)
                    }
                } else {
                    if (toolCount == 1) {
                        stringResource(Res.string.chat_agent_activity_tools_one)
                    } else {
                        stringResource(Res.string.chat_agent_activity_tools_many, toolCount)
                    }
                }
                DoraiAccordionBlock(
                    title = title,
                    icon = if (allShells) Icons.Default.Terminal else Icons.Default.Bolt,
                ) {
                    backgroundTools.forEach { tool ->
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
                Spacer(modifier = Modifier.height(ChatSpacing.sm))
            }

            // Final human-readable output (bottom) — rich Markdown, below the processing blocks
            if (message.text.isNotEmpty()) {
                val document = remember(message.text) { parseMarkdown(message.text) }
                // Pin the v2 text token: without an explicit LocalContentColor the
                // markdown falls back to MaterialTheme onSurface, which can resolve
                // dark-on-dark in some presets — rendering the answer unreadable.
                // Selection is handled inside MarkdownContent per text-run segment
                // so interactive DORAI UI blocks stay outside any selection container.
                CompositionLocalProvider(LocalContentColor provides colors.text) {
                    MarkdownContent(
                        document = document,
                        modifier = Modifier.fillMaxWidth(),
                        isInteractive = isInteractive,
                        onUiCallback = onUiCallback ?: { _, _ -> },
                        frozen = frozen,
                    )
                }
            }

            // Streaming caret — trailing blink bar while this reply is generating
            if (showStreamingCaret && message.text.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = ChatSpacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StreamingCaret()
                }
            }

            // Plan Card (matching prototype planCard)
            if (message.planItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(ChatSpacing.sm))
                DoraiPlanCard(
                    items = message.planItems,
                    onRunPlan = { onRunPlan?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Embedded Dynamic UI block
            if (message.dynamicUi != null) {
                Spacer(modifier = Modifier.height(ChatSpacing.sm))
                DoraiDynamicUiBlock(
                    model = message.dynamicUi,
                    onOptionSelected = { onDynamicUiOptionSelected?.invoke(it) },
                    onConfirm = { selectedId -> onDynamicUiSubmit?.invoke(selectedId) },
                )
            }

            // Action row (copy, text-to-speech speaker, regenerate) for assistant response
            if (message.text.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = ChatSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
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

            // Timestamp — always visible under the reply (kept out of the who-row so
            // grouped consecutive assistant messages still get their time)
            if (message.sentAt != null) {
                Text(
                    text = formatMessageTimestamp(message.sentAt),
                    color = colors.muted,
                    fontSize = 11.sp,
                    // Explicit line height so Bengali digits/matras are not clipped.
                    lineHeight = 15.sp,
                    modifier = Modifier
                        .padding(top = ChatSpacing.xxs)
                        .semantics { contentDescription = sentAtCd ?: "" },
                )
            }
        }
    }
}

/**
 * Assistant "who" row: brand mark + model name + local/badge + optional fallback note.
 * Extracted so consecutive assistant messages can omit it (see ChatMessageRow).
 */
@Composable
private fun AssistantWhoRow(message: ChatMessageItem) {
    val colors = LocalDoraiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        // Provider identity icon when known (matches the configured-service card in
        // settings); the generic app logo is the fallback for unknown/legacy paths.
        if (message.serviceIcon != null) {
            Icon(
                painter = painterResource(message.serviceIcon),
                contentDescription = null, // decorative — the model name text follows
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = message.modelName ?: stringResource(Res.string.nav_brand_title),
            color = colors.muted,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(6.dp))
        DoraiProviderBadge(isLocal = message.isModelLocal, iconOnly = true)
        if (message.fallbackServiceName != null) {
            Spacer(modifier = Modifier.width(ChatSpacing.sm))
            Text(
                text = stringResource(Res.string.fallback_answered_by, message.fallbackServiceName),
                color = colors.muted,
                fontSize = 11.5.sp,
            )
        }
    }
}

/** Formats an epoch-millis timestamp as HH:mm (24h, local zone). Returns "" on failure. */
private fun formatMessageTimestamp(epochMs: Long): String = try {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
} catch (_: Exception) {
    ""
}

/** Blinking trailing bar shown on the in-flight assistant message. Static when reduced motion is on. */
@Composable
private fun StreamingCaret(modifier: Modifier = Modifier) {
    val colors = LocalDoraiColors.current
    if (prefersReducedMotion()) {
        Box(
            modifier
                .size(width = 8.dp, height = 18.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.primary),
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "streamingCaret")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 550), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    Box(
        modifier
            .size(width = 8.dp, height = 18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.primary.copy(alpha = alpha)),
    )
}

/**
 * Renders a base64-encoded image attachment. Decoding (Base64 -> bytes -> ImageBitmap) happens
 * off the main thread via [getBackgroundDispatcher]; the composition only observes the result.
 */
@OptIn(ExperimentalEncodingApi::class)
@Composable
private fun AttachmentImage(
    data: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(data) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(data) {
        bitmap = withContext(getBackgroundDispatcher()) {
            try {
                decodeToImageBitmap(Base64.decode(data))
            } catch (_: Exception) {
                null
            }
        }
    }
    val image = bitmap ?: return
    Image(
        bitmap = image,
        contentDescription = contentDescription,
        modifier = modifier
            .widthIn(max = 240.dp)
            .heightIn(max = 360.dp)
            .clip(RoundedCornerShape(10.dp))
            .padding(bottom = 6.dp),
        contentScale = ContentScale.Fit,
    )
}
