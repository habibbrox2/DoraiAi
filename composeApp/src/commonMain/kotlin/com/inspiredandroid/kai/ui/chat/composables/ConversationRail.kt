package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.chat.ChatSpacing
import com.inspiredandroid.kai.ui.chat.ChatUiState
import com.inspiredandroid.kai.ui.chat.ConversationSummary
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.theme.LocalDesignTokens
import kotlinx.collections.immutable.ImmutableList

/**
 * Desktop conversation rail shown on expanded windows (Phase 2 of the chat UI
 * upgrade plan). Lists saved conversations and lets the user switch or start a
 * new one. Presentation only; loading/deletion go through [ChatUiState.actions].
 */
@Composable
fun ConversationRail(
    uiState: ChatUiState,
    filteredConversations: ImmutableList<ConversationSummary>,
    onNewChat: () -> Unit,
    onConversationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDesignTokens.current
    val rowShape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.SurfaceDark)
            .padding(ChatSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChatSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChatSpacing.xs),
        ) {
            Text(
                text = "Conversations",
                style = KaiTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = tokens.PrimaryText,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onNewChat,
                modifier = Modifier.size(32.dp).handCursor(),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New conversation",
                    tint = tokens.PrimaryCyan,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(ChatSpacing.xxs))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(ChatSpacing.xxs),
        ) {
            items(filteredConversations, key = { it.id }) { conversation ->
                val selected = conversation.id == uiState.currentConversationId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .handCursor()
                        .clip(rowShape)
                        .clickable(onClick = { onConversationSelected(conversation.id) })
                        .background(
                            if (selected) tokens.SurfaceElevated else Color.Transparent,
                            rowShape,
                        )
                        .border(
                            if (selected) BorderStroke(1.dp, tokens.PrimaryCyan.copy(alpha = 0.35f)) else BorderStroke(0.dp, Color.Transparent),
                            rowShape,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(tokens.PrimaryCyan),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = if (selected) tokens.PrimaryCyan else tokens.MutedText,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = conversation.title.ifBlank { "Untitled" },
                            style = KaiTypography.bodySmall,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = if (selected) tokens.PrimaryText else tokens.SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
