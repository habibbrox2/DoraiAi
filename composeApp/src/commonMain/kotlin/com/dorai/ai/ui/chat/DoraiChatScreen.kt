package com.dorai.ai.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.engine.doraiSurfaceStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.DoraiListDetailPaneScaffold
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import com.dorai.ai.ui.components.v2.DoraiComposer
import com.dorai.ai.ui.components.v2.DoraiDynamicUiBlock
import com.dorai.ai.ui.components.v2.DoraiModalSheet
import com.dorai.ai.ui.components.v2.DoraiModeSelector
import com.dorai.ai.ui.components.v2.DoraiProviderBadge
import com.dorai.ai.ui.components.v2.DoraiStatusBadge
import com.dorai.ai.ui.components.v2.DoraiToolCard
import com.dorai.ai.ui.components.v2.DynamicUiModel
import com.dorai.ai.ui.components.v2.ToolExecutionModel
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.chat_copy_message
import dorai.composeapp.generated.resources.chat_history_title
import dorai.composeapp.generated.resources.chat_jump_to_latest
import dorai.composeapp.generated.resources.chat_model_picker_title
import dorai.composeapp.generated.resources.chat_regenerate_message
import dorai.composeapp.generated.resources.chat_tagline
import dorai.composeapp.generated.resources.chat_today
import dorai.composeapp.generated.resources.chat_yesterday
import dorai.composeapp.generated.resources.logo
import dorai.composeapp.generated.resources.nav_brand_title
import dorai.composeapp.generated.resources.nav_chat
import dorai.composeapp.generated.resources.new_chat_content_description
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class ChatMessageItem(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val modelName: String? = null,
    val isModelLocal: Boolean = false,
    val toolExecutions: List<ToolExecutionModel> = emptyList(),
    val dynamicUi: DynamicUiModel? = null,
)

data class ChatConversationSummary(
    val id: String,
    val title: String,
    val preview: String,
    val isToday: Boolean = true,
)

/**
 * UI v2 Chat screen featuring:
 * - Adaptive list-detail scaffold (History pane + Conversation pane)
 * - Empty state with mode-dependent quick starts
 * - Message timeline with tool cards and dynamic UI blocks
 * - Data-driven mode selector & multiline composer
 * - Model picker sheet
 */
@Composable
fun DoraiChatScreen(
    messages: List<ChatMessageItem>,
    conversations: List<ChatConversationSummary>,
    currentModelName: String,
    isModelLocal: Boolean,
    selectedModeId: String,
    onModeSelected: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    onStopStreaming: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onOpenModelPicker: (() -> Unit)? = null,
) {
    val widthClass = LocalDoraiWindowWidthClass.current
    val colors = LocalDoraiColors.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (widthClass.isCompact) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .background(colors.surface),
                ) {
                    ChatHistoryPane(
                        conversations = conversations,
                        onOpenConversation = { id ->
                            onOpenConversation(id)
                            scope.launch { drawerState.close() }
                        },
                        onNewChat = {
                            onNewChat()
                            scope.launch { drawerState.close() }
                        },
                    )
                }
            },
        ) {
            ChatDetailPane(
                messages = messages,
                currentModelName = currentModelName,
                isModelLocal = isModelLocal,
                selectedModeId = selectedModeId,
                onModeSelected = onModeSelected,
                onSendMessage = onSendMessage,
                onNewChat = onNewChat,
                isStreaming = isStreaming,
                onStopStreaming = onStopStreaming,
                onRegenerate = onRegenerate,
                onOpenModelPicker = onOpenModelPicker,
                onOpenDrawer = { scope.launch { drawerState.open() } },
            )
        }
    } else {
        DoraiListDetailPaneScaffold(
            listPane = {
                ChatHistoryPane(
                    conversations = conversations,
                    onOpenConversation = onOpenConversation,
                    onNewChat = onNewChat,
                )
            },
            detailPane = {
                ChatDetailPane(
                    messages = messages,
                    currentModelName = currentModelName,
                    isModelLocal = isModelLocal,
                    selectedModeId = selectedModeId,
                    onModeSelected = onModeSelected,
                    onSendMessage = onSendMessage,
                    onNewChat = onNewChat,
                    isStreaming = isStreaming,
                    onStopStreaming = onStopStreaming,
                    onRegenerate = onRegenerate,
                    onOpenModelPicker = onOpenModelPicker,
                    onOpenDrawer = null,
                )
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun ChatHistoryPane(
    conversations: List<ChatConversationSummary>,
    onOpenConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    var searchQuery by remember { mutableStateOf("") }

    val filtered = conversations.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.preview.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(DoraiSpacing.MD),
    ) {
        // Header Row: New Chat + Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.chat_history_title),
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onNewChat,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surface2),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.new_chat_content_description),
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        // Search box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search chats…", fontSize = 13.sp, color = colors.muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp)) },
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
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(DoraiSpacing.MD))

        // Conversation items
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filtered, key = { it.id }) { conv ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenConversation(conv.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = conv.title,
                        color = colors.text,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = conv.preview,
                        color = colors.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatDetailPane(
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
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current
    val listState = rememberLazyListState()
    var composerInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val isAtBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || lastVisible >= total - 2
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // Chat Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (widthClass.isCompact && onOpenDrawer != null) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "History",
                        tint = colors.text,
                    )
                }
            }

            // Model pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surface2)
                    .border(1.dp, colors.line, CircleShape)
                    .clickable { onOpenModelPicker?.invoke() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentModelName,
                        color = colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    DoraiProviderBadge(isLocal = isModelLocal, iconOnly = true)
                }
            }

            // Active mode badge (hidden on compact)
            if (!widthClass.isCompact) {
                Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                DoraiStatusBadge(text = selectedModeId.uppercase(), color = colors.primary)
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onNewChat) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.new_chat_content_description),
                    tint = colors.text,
                )
            }
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
                    onSelectQuickPrompt = { prompt ->
                        composerInput = prompt
                    },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = DoraiSpacing.MD),
                    verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageRow(
                            message = msg,
                            onRegenerate = onRegenerate,
                        )
                    }
                }
            }

            // Jump to latest button
            androidx.compose.animation.AnimatedVisibility(
                visible = !isAtBottom && messages.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DoraiSpacing.SM),
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                        }
                    },
                    containerColor = colors.surface2,
                    contentColor = colors.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = stringResource(Res.string.chat_jump_to_latest),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Composer Section at Bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
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
                    if (prompt.isNotEmpty()) {
                        composerInput = ""
                        onSendMessage(prompt)
                    }
                },
                isStreaming = isStreaming,
                onStopStreaming = onStopStreaming,
            )
        }
    }
}

@Composable
private fun ChatMessageRow(
    message: ChatMessageItem,
    onRegenerate: (() -> Unit)?,
) {
    val colors = LocalDoraiColors.current

    if (message.isUser) {
        // User message bubble on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                    .background(colors.surface2)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.text,
                    color = colors.text,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    } else {
        // AI message full-width, no bubble
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            // Who row: Model name + Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                Text(
                    text = message.modelName ?: stringResource(Res.string.nav_brand_title),
                    color = colors.muted,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                DoraiProviderBadge(isLocal = message.isModelLocal, iconOnly = true)
            }

            // Message text
            if (message.text.isNotEmpty()) {
                Text(
                    text = message.text,
                    color = colors.text,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                )
            }

            // Embedded Tool cards
            message.toolExecutions.forEach { tool ->
                Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                DoraiToolCard(model = tool)
            }

            // Embedded Dynamic UI block
            if (message.dynamicUi != null) {
                Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                DoraiDynamicUiBlock(
                    model = message.dynamicUi,
                    onOptionSelected = {},
                    onConfirm = {},
                )
            }

            // Action row (copy, regenerate)
            Row(
                modifier = Modifier
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(Res.string.chat_copy_message),
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (onRegenerate != null) {
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.chat_regenerate_message),
                            tint = colors.muted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    selectedModeId: String,
    onSelectQuickPrompt: (String) -> Unit,
) {
    val colors = LocalDoraiColors.current
    val widthClass = LocalDoraiWindowWidthClass.current

    val quickStarts = when (selectedModeId) {
        "plan" -> listOf(
            "Architect a full-stack Kotlin Multiplatform app" to "Design layers, modules, and protocols.",
            "Plan a secure database migration" to "Step-by-step schema upgrade plan.",
        )

        "build" -> listOf(
            "Create a new Compose Multiplatform component" to "Adaptive UI card with 3D elevation.",
            "Write a unit test suite for tokens" to "WCAG AA contrast checking test.",
        )

        "review" -> listOf(
            "Review pull request git diff" to "Inspect changes for potential regressions.",
            "Analyze memory usage and security gates" to "Identify non-sandboxed operations.",
        )

        else -> listOf(
            "How does Compose Multiplatform work?" to "Learn shared UI cross-platform mechanics.",
            "Explain Linux Sandbox isolation" to "Understand how tools run securely.",
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(DoraiSpacing.LG),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 680.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.height(DoraiSpacing.MD))

            Text(
                text = stringResource(Res.string.nav_brand_title),
                color = colors.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(Res.string.chat_tagline),
                color = colors.muted,
                fontSize = 14.sp,
            )

            Spacer(modifier = Modifier.height(DoraiSpacing.LG))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
            ) {
                quickStarts.forEach { (title, subtitle) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                            .clickable { onSelectQuickPrompt(title) }
                            .padding(DoraiSpacing.MD),
                    ) {
                        Column {
                            Text(
                                text = title,
                                color = colors.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = subtitle,
                                color = colors.muted,
                                fontSize = 12.5.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
