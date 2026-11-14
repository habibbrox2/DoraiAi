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

/**
 * A quick-start prompt, described by resource ids rather than resolved strings.
 *
 * Resolving the strings happens inside the card, so the empty state no longer
 * re-reads every localized string on each recomposition and the spec list can
 * be memoized as a constant.
 */
private data class QuickPromptSpec(
    val titleRes: StringResource,
    val promptRes: StringResource,
    val icon: ImageVector,
    val tagRes: StringResource,
)

@Composable
private fun QuickPromptCard(
    spec: QuickPromptSpec,
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(12.dp)
    val title = stringResource(spec.titleRes)
    val prompt = stringResource(spec.promptRes)
    val tag = stringResource(spec.tagRes)
    // Stable click lambda: the card does not create a new callback on every
    // recomposition of the empty state.
    val onClick = remember(prompt, onSelectPrompt) { { onSelectPrompt(prompt) } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = title)
            .padding(ChatSpacing.md),
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
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(modifier = Modifier.width(ChatSpacing.sm))
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = tag,
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
                text = prompt,
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
            .clickable(onClick = onClick, role = Role.Button)
            .minimumInteractiveComponentSize()
            .padding(horizontal = ChatSpacing.md, vertical = 8.dp),
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
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun ChatEmptyState(
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
        stringResource(activeMode.nameRes) + " Â· " + stringResource(activeMode.descRes)
    } else {
        selectedModeId
    }

    // The prompt list only depends on the selected mode, so it is built once per
    // mode instead of re-resolving twelve localized strings on every recomposition.
    val prompts = remember(selectedModeId) {
        when (selectedModeId) {
            "plan" -> listOf(
                QuickPromptSpec(Res.string.qp_plan_1_title, Res.string.qp_plan_1_prompt, Icons.AutoMirrored.Filled.Rule, Res.string.qp_plan_1_tag),
                QuickPromptSpec(Res.string.qp_plan_2_title, Res.string.qp_plan_2_prompt, Icons.Default.Security, Res.string.qp_plan_2_tag),
                QuickPromptSpec(Res.string.qp_plan_3_title, Res.string.qp_plan_3_prompt, Icons.Default.Build, Res.string.qp_plan_3_tag),
                QuickPromptSpec(Res.string.qp_plan_4_title, Res.string.qp_plan_4_prompt, Icons.Default.CheckCircle, Res.string.qp_plan_4_tag),
            )

            "build" -> listOf(
                QuickPromptSpec(Res.string.qp_build_1_title, Res.string.qp_build_1_prompt, Icons.Default.Widgets, Res.string.qp_build_1_tag),
                QuickPromptSpec(Res.string.qp_build_2_title, Res.string.qp_build_2_prompt, Icons.Default.Code, Res.string.qp_build_2_tag),
                QuickPromptSpec(Res.string.qp_build_3_title, Res.string.qp_build_3_prompt, Icons.Default.Terminal, Res.string.qp_build_3_tag),
                QuickPromptSpec(Res.string.qp_build_4_title, Res.string.qp_build_4_prompt, Icons.Default.Build, Res.string.qp_build_4_tag),
            )

            "review" -> listOf(
                QuickPromptSpec(Res.string.qp_review_1_title, Res.string.qp_review_1_prompt, Icons.Default.Speed, Res.string.qp_review_1_tag),
                QuickPromptSpec(Res.string.qp_review_2_title, Res.string.qp_review_2_prompt, Icons.Default.Security, Res.string.qp_review_2_tag),
                QuickPromptSpec(Res.string.qp_review_3_title, Res.string.qp_review_3_prompt, Icons.Default.Search, Res.string.qp_review_3_tag),
                QuickPromptSpec(Res.string.qp_review_4_title, Res.string.qp_review_4_prompt, Icons.Default.Visibility, Res.string.qp_review_4_tag),
            )

            else -> listOf(
                QuickPromptSpec(Res.string.qp_ask_1_title, Res.string.qp_ask_1_prompt, Icons.Default.Lightbulb, Res.string.qp_ask_1_tag),
                QuickPromptSpec(Res.string.qp_ask_2_title, Res.string.qp_ask_2_prompt, Icons.Default.Bolt, Res.string.qp_ask_2_tag),
                QuickPromptSpec(Res.string.qp_ask_3_title, Res.string.qp_ask_3_prompt, Icons.AutoMirrored.Filled.CompareArrows, Res.string.qp_ask_3_tag),
                QuickPromptSpec(Res.string.qp_ask_4_title, Res.string.qp_ask_4_prompt, Icons.Default.Extension, Res.string.qp_ask_4_tag),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(ChatSpacing.md),
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

            Spacer(modifier = Modifier.height(ChatSpacing.md))

            Text(
                text = welcomeTitle,
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(ChatSpacing.xs))

            Text(
                text = stringResource(Res.string.chat_empty_subtitle),
                color = colors.muted,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
            )

            Spacer(modifier = Modifier.height(ChatSpacing.sm))

            // Active Mode Pill
            Text(
                text = modeBadge,
                color = colors.primary,
                fontSize = 12.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.08f))
                    .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(ChatSpacing.lg))

            // Quick Prompt Suggestions Grid
            if (widthClass.isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
                ) {
                    prompts.forEach { spec ->
                        QuickPromptCard(
                            spec = spec,
                            onSelectPrompt = onSelectQuickPrompt,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
                ) {
                    prompts.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
                        ) {
                            rowItems.forEach { spec ->
                                QuickPromptCard(
                                    spec = spec,
                                    onSelectPrompt = onSelectQuickPrompt,
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

            Spacer(modifier = Modifier.height(ChatSpacing.md))

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
                    Spacer(modifier = Modifier.width(ChatSpacing.sm))
                }
                if (onAttachFile != null) {
                    ActionShortcutPill(
                        label = stringResource(Res.string.chat_empty_file),
                        painter = painterResource(Res.drawable.ic_file),
                        onClick = onAttachFile,
                    )
                    Spacer(modifier = Modifier.width(ChatSpacing.sm))
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
