package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.Attachment
import com.inspiredandroid.kai.decodeToImageBitmap
import com.inspiredandroid.kai.ui.KaiShapes
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.chat.ChatSpacing
import com.inspiredandroid.kai.ui.components.LocalShowFullScreenImage
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.theme.DesignTokens
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.ic_file
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 2027 Premium AI Agent Workspace User Message Bubble.
 *
 * Implements Phase 4 specifications from docs/ui-upgrade-plan-7-premium-chat.md.
 * Features calm dark obsidian / elevated surface background, subtle border,
 * clean typography, and neat attachment chips.
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun UserMessage(
    message: String,
    attachments: ImmutableList<Attachment> = persistentListOf(),
) {
    val showFullScreen = LocalShowFullScreenImage.current

    SelectionContainer {
        Row(
            Modifier
                .padding(horizontal = ChatSpacing.lg, vertical = ChatSpacing.sm)
                .fillMaxWidth(),
        ) {
            Spacer(Modifier.weight(1f))
            val userBubbleShape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 4.dp,
            )
            Column(
                modifier = Modifier
                    .widthIn(min = 48.dp, max = 680.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = userBubbleShape,
                        ambientColor = Color(0x33000000),
                        spotColor = Color(0x33000000),
                    )
                    .clip(userBubbleShape)
                    .background(DesignTokens.SurfaceElevated, userBubbleShape)
                    .border(
                        width = 1.dp,
                        color = DesignTokens.BorderSubtle,
                        shape = userBubbleShape,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                val images = attachments.filter { it.mimeType.startsWith("image/") }
                val others = attachments.filter { !it.mimeType.startsWith("image/") }
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
                            contentDescription = "Image attachment",
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .clip(KaiShapes.xs)
                                .handCursor()
                                .clickable { showFullScreen(imageBitmap) },
                            contentScale = ContentScale.FillWidth,
                        )
                        Spacer(Modifier.height(ChatSpacing.sm))
                    }
                }
                if (others.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(ChatSpacing.xs),
                    ) {
                        for (att in others) {
                            key("${att.fileName ?: att.mimeType}_${att.data.take(8)}") {
                                val chipColors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = DesignTokens.SurfaceRaised,
                                    labelColor = LocalDesignColorTokens.current.textPrimary,
                                )
                                SuggestionChip(
                                    onClick = {},
                                    colors = chipColors,
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderColor = DesignTokens.BorderSubtle,
                                    ),
                                    icon = {
                                        Icon(
                                            modifier = Modifier.size(ChatSpacing.lg),
                                            painter = painterResource(Res.drawable.ic_file),
                                            contentDescription = "Attachment file",
                                            tint = DesignTokens.PrimaryCyan,
                                        )
                                    },
                                    label = { Text(truncateFileName(att.fileName ?: att.mimeType)) },
                                )
                            }
                        }
                    }
                    if (message.isNotEmpty()) {
                        Spacer(Modifier.height(ChatSpacing.sm))
                    }
                }
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        color = LocalDesignColorTokens.current.textPrimary,
                        style = KaiTypography.bodyLarge.copy(color = LocalDesignColorTokens.current.textPrimary),
                    )
                }
            }
        }
    }
}




