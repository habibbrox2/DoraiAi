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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
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
import dorai.composeapp.generated.resources.composer_send_content_description
import dorai.composeapp.generated.resources.ic_file
import dorai.composeapp.generated.resources.ic_image
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
    files: ImmutableList<PlatformFile> = persistentListOf(),
    onRemoveFile: ((PlatformFile) -> Unit)? = null,
    placeholder: String = stringResource(Res.string.composer_input_hint),
) {
    val colors = LocalDoraiColors.current
    val boxShape = RoundedCornerShape(26.dp)

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
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                                .clickable { onRemoveFile?.invoke(file) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove file",
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
                    .size(40.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
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
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = colors.text,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Enter && !event.isShiftPressed && value.isNotBlank() && !isStreaming) {
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.err),
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = colors.text,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else if (value.isNotBlank() || files.isNotEmpty()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(40.dp)
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
                IconButton(
                    onClick = { onMicClick?.invoke() },
                    modifier = Modifier
                        .size(40.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(Res.string.composer_mic_content_description),
                        tint = colors.muted,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
