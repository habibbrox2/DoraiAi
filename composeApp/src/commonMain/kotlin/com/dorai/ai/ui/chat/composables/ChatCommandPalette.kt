package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.chat.ChatConversationSummary
import com.dorai.ai.ui.handCursor
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.palette_hint
import dorai.composeapp.generated.resources.palette_new_chat
import dorai.composeapp.generated.resources.palette_no_results
import dorai.composeapp.generated.resources.palette_open_settings
import dorai.composeapp.generated.resources.palette_switch_model
import org.jetbrains.compose.resources.stringResource

private data class PaletteItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val run: () -> Unit,
)

/**
 * Command palette (Ctrl/Cmd+K): jump actions + conversation search in one place.
 * Arrow keys move the selection, Enter runs it, Escape or an outside tap dismisses.
 */
@Composable
internal fun ChatCommandPalette(
    conversations: List<ChatConversationSummary>,
    currentModelName: String,
    onDismiss: () -> Unit,
    onNewChat: () -> Unit,
    onOpenModelPicker: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
    onOpenConversation: (String) -> Unit,
) {
    val colors = LocalDoraiColors.current
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val newChatLabel = stringResource(Res.string.palette_new_chat)
    val switchModelLabel = stringResource(Res.string.palette_switch_model)
    val settingsLabel = stringResource(Res.string.palette_open_settings)

    val q = query.trim().lowercase()
    val items: List<PaletteItem> = remember(
        q,
        conversations,
        currentModelName,
        newChatLabel,
        switchModelLabel,
        settingsLabel,
    ) {
        buildList {
            if (q.isEmpty() || "new chat".contains(q) || "new".contains(q)) {
                add(PaletteItem("new-chat", newChatLabel, null, Icons.Default.Add, onNewChat))
            }
            if (onOpenModelPicker != null &&
                (
                    q.isEmpty() || "switch model".contains(q) || "model".contains(q) ||
                        currentModelName.lowercase().contains(q)
                    )
            ) {
                add(
                    PaletteItem(
                        "switch-model",
                        switchModelLabel,
                        currentModelName,
                        Icons.Default.SwapHoriz,
                        onOpenModelPicker,
                    ),
                )
            }
            if (onOpenSettings != null &&
                (q.isEmpty() || "settings".contains(q) || "open settings".contains(q))
            ) {
                add(PaletteItem("settings", settingsLabel, null, Icons.Default.Settings, onOpenSettings))
            }
            conversations
                .filter { q.isEmpty() || it.title.lowercase().contains(q) }
                .take(5)
                .forEach { conv ->
                    add(
                        PaletteItem(
                            id = "conv-${conv.id}",
                            title = conv.title,
                            subtitle = conv.preview.takeIf { it.isNotBlank() },
                            icon = Icons.Default.ChatBubbleOutline,
                            run = { onOpenConversation(conv.id) },
                        ),
                    )
                }
        }
    }

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            selectedIndex = selectedIndex.coerceIn(0, items.size - 1)
        } else {
            selectedIndex = 0
        }
    }

    fun runItem(item: PaletteItem) {
        item.run()
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 560.dp)
                .onPreviewKeyEvent { event ->
                    when {
                        event.key == Key.Escape -> {
                            onDismiss()
                            true
                        }

                        event.key == Key.DirectionUp -> {
                            if (items.isNotEmpty()) {
                                selectedIndex = (selectedIndex - 1 + items.size) % items.size
                            }
                            true
                        }

                        event.key == Key.DirectionDown -> {
                            if (items.isNotEmpty()) {
                                selectedIndex = (selectedIndex + 1) % items.size
                            }
                            true
                        }

                        event.key == Key.Enter && event.type == KeyEventType.KeyDown -> {
                            items.getOrNull(selectedIndex)?.let(::runItem)
                            true
                        }

                        else -> false
                    }
                },
        ) {
            Column(Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        selectedIndex = 0
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = colors.muted)
                    },
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.palette_hint),
                            color = colors.muted,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                if (items.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.palette_no_results),
                        color = colors.muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            val selected = index == selectedIndex
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) colors.surface2 else colors.surface)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = item.title,
                                    ) { runItem(item) }
                                    .semantics { this.selected = selected }
                                    .handCursor()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column {
                                    Text(
                                        text = item.title,
                                        color = colors.text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    item.subtitle?.let { subtitle ->
                                        Text(
                                            text = subtitle,
                                            color = colors.muted,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
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
