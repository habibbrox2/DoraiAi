package com.dorai.ai.ui.components.v2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.data.imageExtensions
import com.dorai.ai.theme.engine.doraiEdgeStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.composer_attach_content_description
import dorai.composeapp.generated.resources.composer_input_hint
import dorai.composeapp.generated.resources.composer_mic_content_description
import dorai.composeapp.generated.resources.composer_remove_file
import dorai.composeapp.generated.resources.composer_send_content_description
import dorai.composeapp.generated.resources.composer_stop
import dorai.composeapp.generated.resources.composer_stop_voice
import dorai.composeapp.generated.resources.ic_file
import dorai.composeapp.generated.resources.ic_image
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * True when the Enter key must send the composer content.
 *
 * - Shift+Enter inserts a newline instead of sending.
 * - While an IME is still composing (Bengali, CJK, …) the key must not send: the word is
 *   only half written, and sending it would post a broken prompt.
 * - An empty composer has nothing to send, and while a turn is streaming the stop
 *   button owns the keyboard.
 */
internal fun shouldSendOnEnter(
    isEnter: Boolean,
    isShiftPressed: Boolean,
    isComposing: Boolean,
    text: String,
    isStreaming: Boolean,
): Boolean = isEnter && !isShiftPressed && !isComposing && text.isNotBlank() && !isStreaming

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoraiComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    onStopStreaming: (() -> Unit)? = null,
    onAttach: (() -> Unit)? = null,
    onMicClick: (() -> Unit)? = null,
    /** True while a voice turn is capturing speech; the mic button becomes a stop button. */
    isVoiceRecording: Boolean = false,
    /** Short status line shown under the input while a voice turn is running. */
    voiceStatusText: String? = null,
    files: ImmutableList<PlatformFile> = persistentListOf(),
    onRemoveFile: ((PlatformFile) -> Unit)? = null,
    placeholder: String = stringResource(Res.string.composer_input_hint),
    maxContextTokens: Int? = null,
    /**
     * Optional [FocusRequester] attached to the text field so callers can focus the
     * composer (prefill open, return from a dismissed modal, refocus after send).
     */
    focusRequester: FocusRequester? = null,
) {
    val colors = LocalDoraiColors.current
    val boxShape = RoundedCornerShape(26.dp)

    // The field keeps its own TextFieldValue so the IME composition region stays observable.
    // Enter-to-send must not fire while a Bengali/CJK IME is still composing — otherwise a
    // half-written word is sent instead of being committed.
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = fieldValue.copy(text = value, selection = TextRange(value.length))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(boxShape)
            .background(colors.surface)
            .border(1.dp, colors.line, boxShape)
            .doraiEdgeStyle(shape = boxShape, cornerRadius = 26.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        if (files.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DoraiSpacing.SM, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(DoraiSpacing.XS),
            ) {
                for (file in files) {
                    val isImg = file.extension.lowercase() in imageExtensions
                    val icon = if (isImg) Res.drawable.ic_image else Res.drawable.ic_file
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.line),
                    ) {
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(role = Role.Button) { onRemoveFile?.invoke(file) }
                                .padding(horizontal = DoraiSpacing.SM, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.XS),
                        ) {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = file.name,
                                color = colors.text,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(Res.string.composer_remove_file),
                                tint = colors.muted,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Attach (+) Button
            IconButton(
                onClick = { onAttach?.invoke() },
                modifier = Modifier
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.composer_attach_content_description),
                    tint = colors.muted,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Multiline Input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 132.dp)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = colors.muted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }

                BasicTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        fieldValue = newValue
                        onValueChange(newValue.text)
                    },
                    textStyle = TextStyle(
                        color = colors.text,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { }
                        .then(
                            if (focusRequester != null) {
                                Modifier.focusRequester(focusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .onPreviewKeyEvent { event ->
                            if (
                                shouldSendOnEnter(
                                    isEnter = event.key == Key.Enter,
                                    isShiftPressed = event.isShiftPressed,
                                    isComposing = fieldValue.composition != null,
                                    text = fieldValue.text,
                                    isStreaming = isStreaming,
                                )
                            ) {
                                onSend()
                                true
                            } else {
                                false
                            }
                        },
                )
            }

            // Right Button: Stop if streaming, Send if non-empty, Mic if empty
            if (isStreaming) {
                IconButton(
                    onClick = { onStopStreaming?.invoke() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.err),
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(Res.string.composer_stop),
                        tint = colors.text,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else if (value.isNotBlank() || files.isNotEmpty()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(Res.string.composer_send_content_description),
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                // The mic button turns into a stop button while a voice turn is running.
                // The modifier chain is left untouched when idle so the resting state
                // renders exactly as before.
                val micModifier = if (isVoiceRecording) {
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.err)
                } else {
                    Modifier.size(48.dp)
                }
                IconButton(
                    onClick = { onMicClick?.invoke() },
                    modifier = micModifier,
                ) {
                    Icon(
                        imageVector = if (isVoiceRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = stringResource(
                            if (isVoiceRecording) {
                                Res.string.composer_stop_voice
                            } else {
                                Res.string.composer_mic_content_description
                            },
                        ),
                        tint = if (isVoiceRecording) colors.text else colors.muted,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        if (isVoiceRecording && voiceStatusText != null) {
            Text(
                text = voiceStatusText,
                color = colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
            )
        }

        if (value.isNotBlank() && (value.length > 120 || (maxContextTokens != null && maxContextTokens > 0))) {
            val estimatedTokens = ((value.length + 3) / 4).coerceAtLeast(1)
            val tokenText = if (maxContextTokens != null && maxContextTokens > 0) {
                val limitDisplay = if (maxContextTokens >= 1_000_000) {
                    "${maxContextTokens / 1_000_000}M"
                } else if (maxContextTokens >= 1_000) {
                    "${maxContextTokens / 1_000}k"
                } else {
                    "$maxContextTokens"
                }
                "~$estimatedTokens / $limitDisplay tokens"
            } else {
                "~$estimatedTokens tokens"
            }
            Text(
                text = tokenText,
                color = colors.muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
            )
        }
    }
}
