package com.dorai.ai.ui.chat.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.data.AppSettings
import com.dorai.ai.getBackgroundDispatcher
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.KaiTypography
import com.dorai.ai.ui.SuccessGreen
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.dynamicui.FrozenSubmission
import com.dorai.ai.ui.dynamicui.toSpeakableText
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.markdown.MarkdownContent
import com.dorai.ai.ui.markdown.parseMarkdown
import com.dorai.ai.ui.rememberCopyToClipboard
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.bot_message_copy_content_description
import dorai.composeapp.generated.resources.bot_message_thinking_expand_content_description
import dorai.composeapp.generated.resources.bot_message_thinking_label
import dorai.composeapp.generated.resources.chat_cancel_edit_content_description
import dorai.composeapp.generated.resources.chat_edit_submission_content_description
import dorai.composeapp.generated.resources.ic_copy
import dorai.composeapp.generated.resources.logo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.errors.TextToSpeechSynthesisInterruptedError
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun BotMessage(
    message: String,
    textToSpeech: TextToSpeechInstance?,
    isSpeaking: Boolean,
    setIsSpeaking: (Boolean) -> Unit,
    onRegenerate: (() -> Unit)? = null,
    isInteractive: Boolean = false,
    onUiCallback: ((event: String, data: Map<String, String>) -> Unit)? = null,
    frozen: FrozenSubmission? = null,
    onResubmit: ((event: String, data: Map<String, String>) -> Unit)? = null,
    reasoningSegments: ImmutableList<String> = persistentListOf(),
    isFirstMessage: Boolean = false,
) {
    val document = remember(message) { parseMarkdown(message) }
    var isEditing by remember(frozen) { mutableStateOf(false) }
    val effectiveFrozen = if (isEditing && frozen != null) frozen.copy(pressedEvent = null) else frozen
    val effectiveInteractive = if (frozen != null) (onResubmit != null && isEditing) else isInteractive
    val kaiUiCallback: (String, Map<String, String>) -> Unit = if (onResubmit != null) {
        { event, data ->
            isEditing = false
            onResubmit(event, data)
        }
    } else {
        onUiCallback ?: { _, _ -> }
    }

    val copyToClipboard = rememberCopyToClipboard()
    val botBubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 4.dp,
        bottomEnd = 18.dp,
    )
    val botBubbleBg = DesignTokens.SurfaceElevated
    val botBubbleBorder = DesignTokens.BorderSubtle
    val botTextColor = LocalDesignColorTokens.current.textPrimary
    val botMutedColor = LocalDesignColorTokens.current.textTertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ChatSpacing.lg, vertical = ChatSpacing.sm),
    ) {
        // Message Content Box - ChatGPT Dark Style
        Box(
            modifier = Modifier
                .widthIn(min = 48.dp, max = 680.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = botBubbleShape,
                    ambientColor = Color(0x33000000),
                    spotColor = Color(0x33000000),
                )
                .clip(botBubbleShape)
                .background(botBubbleBg)
                .border(
                    width = 1.dp,
                    color = botBubbleBorder,
                    shape = botBubbleShape,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isFirstMessage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp),
                    ) {
                        Icon(painter = painterResource(Res.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "DORAI AI",
                            style = KaiTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = botTextColor,
                        )
                    }
                }

                // Markdown Message Body
                if (message.isNotEmpty()) {
                    val answerTopPadding = 2.dp
                    CompositionLocalProvider(
                        androidx.compose.material3.LocalContentColor provides botTextColor,
                    ) {
                        SelectionContainer {
                            MarkdownContent(
                                document = document,
                                isInteractive = effectiveInteractive,
                                onUiCallback = kaiUiCallback,
                                frozen = effectiveFrozen,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = answerTopPadding, end = if (message.isNotEmpty() && frozen == null) 52.dp else 0.dp),
                            )
                        }
                    }
                }
            }

            // Copy button on top right of bot bubble
            if (message.isNotEmpty() && frozen == null) {
                var isCopiedInside by remember { mutableStateOf(false) }
                LaunchedEffect(isCopiedInside) {
                    if (isCopiedInside) {
                        delay(2000)
                        isCopiedInside = false
                    }
                }
                androidx.compose.material3.IconButton(
                    onClick = {
                        copyToClipboard(message)
                        isCopiedInside = true
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .handCursor(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_copy),
                        contentDescription = stringResource(Res.string.bot_message_copy_content_description),
                        tint = if (isCopiedInside) SuccessGreen else botMutedColor,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            // Edit / Resubmit button when frozen
            if (frozen != null && onResubmit != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(com.dorai.ai.ui.theme.DesignTokens.SurfaceRaised)
                        .border(androidx.compose.foundation.BorderStroke(1.dp, com.dorai.ai.ui.theme.DesignTokens.BorderSubtle), CircleShape)
                        .handCursor()
                        .clickable { isEditing = !isEditing },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = stringResource(
                            if (isEditing) Res.string.chat_cancel_edit_content_description else Res.string.chat_edit_submission_content_description,
                        ),
                        modifier = Modifier.size(16.dp),
                        tint = com.dorai.ai.ui.theme.DesignTokens.PrimaryCyan,
                    )
                }
            }
        }

        // ChatGPT-style Clean Action Row below response (Copy, Retry, Speak, Thumbs, Flag)
        if (message.isNotEmpty()) {
            var thumbsUp by remember { mutableStateOf(false) }
            var thumbsDown by remember { mutableStateOf(false) }
            val componentScope = rememberCoroutineScope()
            val uriHandler = LocalUriHandler.current

            val appSettings: AppSettings = koinInject()
            MessageActionRow(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                onCopy = { copyToClipboard(message) },
                onSpeak = if (textToSpeech != null) {
                    {
                        componentScope.launch(getBackgroundDispatcher()) {
                            textToSpeech.stop()
                            if (isSpeaking) {
                                setIsSpeaking(false)
                            } else {
                                setIsSpeaking(true)
                                try {
                                    try {
                                        textToSpeech.rate = appSettings.getTtsRate()
                                    } catch (_: Exception) {
                                    }
                                    try {
                                        textToSpeech.pitch = appSettings.getTtsPitch()
                                    } catch (_: Exception) {
                                    }
                                    try {
                                        textToSpeech.volume = (appSettings.getTtsVolume() * 100).toInt()
                                    } catch (_: Exception) {
                                    }
                                    textToSpeech.say(text = message.toSpeakableText())
                                } catch (_: TextToSpeechSynthesisInterruptedError) {
                                    // Expected interruption
                                } catch (_: Exception) {
                                    // Handle gracefully
                                }
                                setIsSpeaking(false)
                            }
                        }
                    }
                } else {
                    null
                },
                onThumbsUp = {
                    thumbsUp = !thumbsUp
                    if (thumbsUp) thumbsDown = false
                },
                onThumbsDown = {
                    thumbsDown = !thumbsDown
                    if (thumbsDown) thumbsUp = false
                },
                thumbsUpSelected = thumbsUp,
                thumbsDownSelected = thumbsDown,
                onRetry = onRegenerate,
                onFlag = {
                    uriHandler.openUri("https://form.jotform.com/250014908169355")
                },
            )
        }
    }
}

/**
 * ChatGPT / DeepSeek / Claude style Thinking Process block.
 * Features:
 * - Glowing thought badge (🧠 Thought process)
 * - Monospace styling with dark canvas (#0B0F17)
 * - Single-line preview when collapsed
 * - Smooth expand/collapse transition
 */
@Composable
private fun ReasoningBlockquote(
    segments: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val preview = remember(segments) {
        segments.lastOrNull()
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() }
            .orEmpty()
    }

    val blockShape = RoundedCornerShape(10.dp)
    val accentCyan = com.dorai.ai.ui.theme.DesignTokens.PrimaryCyan
    val containerBg = com.dorai.ai.ui.theme.DesignTokens.SurfaceDark
    val borderColor = com.dorai.ai.ui.theme.DesignTokens.BorderSubtle
    val textSlate = LocalDesignColorTokens.current.textSecondary
    val textMuted = LocalDesignColorTokens.current.textMuted

    Column(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clip(blockShape)
            .background(containerBg)
            .border(1.dp, borderColor, blockShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .handCursor()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(accentCyan.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = accentCyan,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.bot_message_thinking_label),
                        style = KaiTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        fontFamily = FontFamily.Monospace,
                        color = textSlate,
                        fontSize = 12.sp,
                    )
                    if (segments.size > 1) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentCyan.copy(alpha = 0.12f))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "${segments.size} steps",
                                style = KaiTypography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = accentCyan,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
                if (!expanded && preview.isNotEmpty()) {
                    Text(
                        text = preview,
                        style = KaiTypography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.bot_message_thinking_expand_content_description),
                    tint = if (expanded) accentCyan else textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .background(com.dorai.ai.ui.theme.DesignTokens.TerminalBackground)
                    .border(
                        width = 1.dp,
                        color = borderColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                segments.forEachIndexed { index, segment ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = borderColor.copy(alpha = 0.6f),
                            thickness = 1.dp,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "STEP ${index + 1}",
                            style = KaiTypography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = accentCyan.copy(alpha = 0.75f),
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp,
                        )
                        SelectionContainer {
                            Text(
                                text = segment,
                                color = textSlate,
                                style = KaiTypography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
