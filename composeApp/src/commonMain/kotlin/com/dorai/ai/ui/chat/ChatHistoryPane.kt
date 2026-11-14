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
import androidx.compose.ui.focus.focusRequester
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
internal fun ChatHistoryPane(
    conversations: List<ChatConversationSummary>,
    onOpenConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onTogglePin: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    onCloseDrawer: (() -> Unit)? = null,
    activeConversationId: String? = null,
    onRename: ((String, String) -> Unit)? = null,
    /** True when the pane should pull keyboard focus into the search box (opening drawer). */
    autoFocusSearch: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current
    var searchQuery by remember { mutableStateOf("") }
    // Focus the search box when the pane is meant to grab attention (chat drawer opening
    // on a compact screen). The persistent list pane on expanded windows stays passive.
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(autoFocusSearch) {
        if (autoFocusSearch) {
            runCatching { searchFocus.requestFocus() }
        }
    }
    var renamingConv by remember { mutableStateOf<ChatConversationSummary?>(null) }

    val filtered = remember(conversations, searchQuery) {
        conversations
            .filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.preview.contains(searchQuery, ignoreCase = true)
            }
            .sortedByDescending { it.isPinned }
    }

    val todayConvs = remember(filtered) { filtered.filter { it.isToday } }
    val earlierConvs = remember(filtered) { filtered.filter { !it.isToday } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(ChatSpacing.md),
    ) {
        if (widthClass.isCompact && onCloseDrawer != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = ChatSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoAnimation(
                        size = 26.dp,
                        animate = true,
                    )
                    Spacer(modifier = Modifier.width(ChatSpacing.sm))
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
                .heightIn(min = 48.dp),
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

        Spacer(modifier = Modifier.height(ChatSpacing.sm))

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
                .focusRequester(searchFocus)
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(ChatSpacing.sm))

        // Conversation items with Today and Earlier grouping
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(ChatSpacing.xs),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.chat_no_chats_found),
                        color = colors.muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = ChatSpacing.md, horizontal = 4.dp),
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
                    .padding(ChatSpacing.lg),
            ) {
                Text(
                    text = stringResource(Res.string.chat_rename_title),
                    color = sheetColors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(ChatSpacing.md))
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
                Spacer(modifier = Modifier.height(ChatSpacing.md))
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
            .clickable(onClick = onOpen, role = Role.Button, onClickLabel = conv.title)
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
                    Spacer(modifier = Modifier.width(ChatSpacing.xs))
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
