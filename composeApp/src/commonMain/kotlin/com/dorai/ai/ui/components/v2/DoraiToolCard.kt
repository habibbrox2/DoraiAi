package com.dorai.ai.ui.components.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.engine.doraiSurfaceStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.rememberCopyToClipboard
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_copy
import dorai.composeapp.generated.resources.a11y_copy_details
import dorai.composeapp.generated.resources.chat_collapse_all
import dorai.composeapp.generated.resources.chat_collapse_output
import dorai.composeapp.generated.resources.chat_command_failed_with_exit
import dorai.composeapp.generated.resources.chat_exit_code
import dorai.composeapp.generated.resources.chat_expand_all
import dorai.composeapp.generated.resources.chat_expand_output
import dorai.composeapp.generated.resources.chat_lines_label
import dorai.composeapp.generated.resources.chat_tool_done
import dorai.composeapp.generated.resources.chat_tool_input_label
import dorai.composeapp.generated.resources.state_collapsed
import dorai.composeapp.generated.resources.state_expanded
import dorai.composeapp.generated.resources.tool_action_ask_fix
import dorai.composeapp.generated.resources.tool_action_retry
import dorai.composeapp.generated.resources.tool_card_command_header
import dorai.composeapp.generated.resources.tool_card_exit_format
import dorai.composeapp.generated.resources.tool_card_more_lines_format
import dorai.composeapp.generated.resources.tool_card_output_header
import dorai.composeapp.generated.resources.tool_execution_error
import dorai.composeapp.generated.resources.tool_status_completed
import dorai.composeapp.generated.resources.tool_status_failed
import dorai.composeapp.generated.resources.tool_status_running
import dorai.composeapp.generated.resources.tool_status_waiting_approval
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource

enum class ToolExecutionState {
    Running,
    Completed,
    Failed,
    PendingApproval,
}

data class ToolExecutionModel(
    val toolName: String,
    val argsSummary: String = "",
    val state: ToolExecutionState = ToolExecutionState.Running,
    val durationText: String? = null,
    val commandText: String? = null,
    val outputText: String? = null,
    val errorText: String? = null,
    val diffText: String? = null,
    val toolCallId: String = "",
    val exitCode: Int? = null,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
    val subAgents: List<SubAgentExecution> = emptyList(),
)

/**
 * One sub-agent execution inside a parent agent tool run (e.g. `agent.spawn`).
 *
 * @property role Agent role label (e.g. "Writer", "Critic").
 * @property task Task description the sub-agent was asked to run.
 * @property status Lifecycle status of the sub-agent (e.g. "draft", "review").
 * @property durationText Human-readable sub-agent duration (e.g. "১.০s").
 * @property resultSummary Short human-readable result summary.
 * @property state Latest known lifecycle state.
 */
@Serializable
data class SubAgentExecution(
    val role: String,
    val task: String? = null,
    val status: String? = null,
    val durationText: String? = null,
    val resultSummary: String? = null,
    val state: ToolExecutionState = ToolExecutionState.Running,
)

/**
 * Wire payload for agent-style tools (e.g. `agent.spawn`): the sub-agents to run.
 * Unknown keys are tolerated via the lenient JSON config.
 */
@Serializable
data class SubAgentSpawnArgs(
    val agents: List<SubAgentSpawnAgent> = emptyList(),
)

@Serializable
data class SubAgentSpawnAgent(
    val role: String = "Agent",
    val task: String? = null,
    val status: String? = null,
)

/**
 * Wire payload for key-value tool arguments (web.search query, code.run snippet).
 * Any of the known argument names is accepted as the human-facing input line.
 */
@Serializable
data class KeyValueArgsWire(
    val query: String? = null,
    val code: String? = null,
    val command: String? = null,
    val source: String? = null,
    val input: String? = null,
) {
    /** First non-blank argument in a stable priority order. */
    fun firstInput(): String? = listOfNotNull(query, code, command, source, input)
        .firstOrNull { it.isNotBlank() }
}

/** Wire payload for `web_search` results: a small list of title/url pairs plus an optional message. */
@Serializable
data class WebSearchResultWire(
    val title: String? = null,
    val url: String? = null,
)

@Serializable
data class WebSearchOutputWire(
    val results: List<WebSearchResultWire> = emptyList(),
    val message: String? = null,
)

private val keyValueJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Tools whose argument is a query/code snippet and whose result reads as text rather than a
 * command transcript (e.g. `web_search`, `code.run`). They render Input/Output rows instead of
 * the command/output blocks.
 */
private fun isKeyValueTool(toolName: String): Boolean {
    val name = toolName.lowercase()
    return name.contains("web_search") || name.contains("web.search") ||
        name.contains("code.run") || name.contains("code_run")
}

/**
 * Human-facing input line for a key-value tool row: the tool's primary argument, or the raw
 * arguments string when the payload shape is unknown.
 */
private fun parseKeyValueInput(toolName: String, args: String): String {
    if (args.isBlank()) return ""
    val parsed = when {
        isKeyValueTool(toolName) ->
            runCatching { keyValueJson.decodeFromString<KeyValueArgsWire>(args) }.getOrNull()

        else -> null
    }
    return parsed?.firstInput()?.takeIf { it.isNotBlank() } ?: args
}

/** Parses a `web_search` JSON result payload; null when the payload is not web-search shaped. */
private fun parseWebSearchOutput(output: String): WebSearchOutputWire? = runCatching { keyValueJson.decodeFromString<WebSearchOutputWire>(output) }.getOrNull()

/** Input/Output label-and-value row used by key-value tool rows (prototype `.kv`). */
@Composable
private fun ToolKeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalDoraiColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = colors.muted.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.background)
                .border(1.dp, colors.line.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = value,
                color = colors.muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

/**
 * Maps tool name to a semantic icon.
 */
fun getToolCategoryIcon(toolName: String): ImageVector {
    val name = toolName.lowercase()
    return when {
        name.contains("shell") || name.contains("terminal") || name.contains("bash") ||
            name.contains("sh") || name.contains("cmd") || name.contains("exec") -> Icons.Default.Terminal

        name.contains("edit") || name.contains("patch") || name.contains("modify") ||
            name.contains("write") -> Icons.Default.Edit

        name.contains("read") || name.contains("cat") || name.contains("view") ||
            name.contains("file") || name.contains("fs") -> Icons.AutoMirrored.Filled.InsertDriveFile

        name.contains("search") || name.contains("grep") || name.contains("find") ||
            name.contains("glob") -> Icons.Default.Search

        name.contains("git") -> Icons.Default.Code

        name.contains("mcp") || name.contains("plugin") -> Icons.Default.Extension

        else -> Icons.Default.Build
    }
}

/**
 * Formats tool name into a compact, human-readable identifier.
 */
fun formatToolDisplayName(toolName: String): String = when (toolName) {
    "execute_shell_command" -> "bash"
    "read_file" -> "read_file"
    "edit_file", "propose_file_change" -> "edit_file"
    "search", "web_search" -> "search"
    else -> toolName.removePrefix("tool_")
}

/**
 * Collapse-all footer for a group of tool cards (prototype `.tfoot`): mirrors the tool row
 * container's open/close-all action that expands or collapses every card in the group at once.
 * Renders only for groups larger than one card, matching the prototype's `rows.length > 1` gate.
 */
@Composable
internal fun DoraiToolCardCollapseAll(
    expanded: Boolean,
    onToggleAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.background)
            .border(1.dp, colors.line.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onToggleAll),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(if (expanded) Res.string.chat_collapse_all else Res.string.chat_expand_all),
            color = colors.muted,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/**
 * Production-grade Agent Tool Execution Card:
 * - Compact tool header with semantic icon, tool name, arguments preview, and metadata.
 * - Semantic status badge (Running with indicator, Completed, Failed, Pending Approval).
 * - Execution metadata: duration and explicit exit code badge.
 * - Expandable command block with copy action.
 * - Collapsible stdout output with line-count badge to prevent vertical bloat.
 * - Error-focused panel with reddish highlight, stderr output, copy error, and retry actions.
 * - Unified diff view for file modifications.
 * - Blocking approval actions for security-sensitive executions.
 */
@Composable
fun DoraiToolCard(
    model: ToolExecutionModel,
    modifier: Modifier = Modifier,
    onAllowOnce: (() -> Unit)? = null,
    onAllowSession: (() -> Unit)? = null,
    onDeny: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onAskFix: (() -> Unit)? = null,
    expandedOverride: Boolean? = null,
    onExpansionChange: ((Boolean) -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val copyToClipboard = rememberCopyToClipboard()

    val isAgentTool = model.toolName.contains("agent", ignoreCase = true)

    // Default expanded state: Completed is collapsed by default to save space; Failed, Running, and
    // PendingApproval expand. Keyed on the tool-call identity rather than the state: keying on
    // state would reset the user's manual toggle whenever the tool transitions Running → Completed.
    var internalExpanded by remember(model.toolCallId) {
        mutableStateOf(model.state != ToolExecutionState.Completed)
    }
    var isTouched by remember(model.toolCallId) { mutableStateOf(false) }

    // When an external owner controls the value (e.g. the collapse-all footer), follow it instead of
    // tracking local toggles.
    val isExpanded: Boolean = expandedOverride ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = { value ->
        if (expandedOverride == null) internalExpanded = value
        onExpansionChange?.invoke(value)
    }

    // Auto-collapse agent sub-agent timelines shortly after completion when the user has not
    // manually toggled the card (prototype: 800ms auto-collapse).
    LaunchedEffect(model.toolCallId, model.state) {
        if (model.state == ToolExecutionState.Completed && isAgentTool && model.subAgents.isNotEmpty() && !isTouched) {
            delay(800)
            setExpanded(false)
        }
    }

    val borderColor = when (model.state) {
        ToolExecutionState.Running -> colors.info.copy(alpha = 0.5f)
        ToolExecutionState.Completed -> colors.line
        ToolExecutionState.Failed -> colors.err
        ToolExecutionState.PendingApproval -> colors.warn
    }

    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surface)
            .border(1.dp, borderColor, cardShape)
            .doraiSurfaceStyle(shape = cardShape)
            .padding(DoraiSpacing.SM),
    ) {
        // Compact Tool Header Row (min-height 44dp)
        val expandedDescription = stringResource(if (isExpanded) Res.string.state_expanded else Res.string.state_collapsed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button) {
                    isTouched = true
                    setExpanded(!isExpanded)
                }
                .semantics {
                    stateDescription = expandedDescription
                }
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tool Icon Badge (28x28 dp container)
            val iconTint = when (model.state) {
                ToolExecutionState.Running -> colors.info
                ToolExecutionState.Completed -> colors.ok
                ToolExecutionState.Failed -> colors.err
                ToolExecutionState.PendingApproval -> colors.warn
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (model.state == ToolExecutionState.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = colors.info,
                    )
                } else {
                    Icon(
                        imageVector = getToolCategoryIcon(model.toolName),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(DoraiSpacing.SM))

            // Tool Name (compact bold monospace)
            Text(
                text = formatToolDisplayName(model.toolName),
                color = colors.text,
                fontSize = 13.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )

            // Agent role chips (agent.spawn and friends) instead of raw arguments preview.
            if (isAgentTool && model.subAgents.isNotEmpty()) {
                Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                Row(horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.XS)) {
                    model.subAgents.forEach { agent ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.accent.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = agent.role,
                                color = colors.accent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }

            // Arguments / Command Preview (key-value tools show their primary argument)
            val previewText = remember(model.argsSummary, model.commandText, model.toolName) {
                if (isKeyValueTool(model.toolName)) {
                    parseKeyValueInput(model.toolName, model.argsSummary)
                } else {
                    model.argsSummary.ifBlank { model.commandText.orEmpty() }
                }
            }
            if (previewText.isNotBlank() && !isAgentTool) {
                Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                Text(
                    text = previewText,
                    color = colors.muted,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Exit Code Badge (Explicit Result Metadata)
            if (model.exitCode != null) {
                val exitColor = if (model.exitCode == 0) colors.ok else colors.err
                Text(
                    text = stringResource(Res.string.tool_card_exit_format, model.exitCode),
                    color = exitColor,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(exitColor.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
                Spacer(modifier = Modifier.width(DoraiSpacing.XS))
            }

            // Duration Metadata
            if (model.durationText != null) {
                Text(
                    text = model.durationText,
                    color = colors.muted,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.surface2)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
                Spacer(modifier = Modifier.width(DoraiSpacing.XS))
            }

            // Semantic Status Badge
            when (model.state) {
                ToolExecutionState.Running -> {
                    DoraiStatusBadge(
                        text = stringResource(Res.string.tool_status_running),
                        color = colors.info,
                    )
                }

                ToolExecutionState.Completed -> {
                    DoraiStatusBadge(
                        text = stringResource(Res.string.tool_status_completed),
                        color = colors.ok,
                    )
                }

                ToolExecutionState.Failed -> {
                    DoraiStatusBadge(
                        text = stringResource(Res.string.tool_status_failed),
                        color = colors.err,
                    )
                }

                ToolExecutionState.PendingApproval -> {
                    DoraiStatusBadge(
                        text = stringResource(Res.string.tool_status_waiting_approval),
                        color = colors.warn,
                    )
                }
            }

            Spacer(modifier = Modifier.width(DoraiSpacing.XS))

            // Expand / Collapse Chevron
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(if (isExpanded) Res.string.chat_collapse_output else Res.string.chat_expand_output),
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }

        // Expanded Body
        AnimatedVisibility(
            visible = isExpanded || prefersReducedMotion(),
            enter = if (prefersReducedMotion()) tween(durationMillis = 0) else expandVertically(),
            exit = if (prefersReducedMotion()) tween(durationMillis = 0) else shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DoraiSpacing.SM),
            ) {
                // Input/Output key-value rows (web.search, code.run — prototype `.kv` tool rows).
                if (isKeyValueTool(model.toolName)) {
                    val inputText = remember(model.argsSummary, model.toolName) {
                        parseKeyValueInput(model.toolName, model.argsSummary)
                    }
                    val outputDisplay = remember(model.outputText) {
                        val titles = model.outputText
                            ?.let { parseWebSearchOutput(it) }
                            ?.results
                            ?.mapNotNull { it.title?.takeIf { title -> title.isNotBlank() } }
                            .orEmpty()
                        if (titles.isNotEmpty()) {
                            titles.joinToString("\n") { "• $it" }
                        } else {
                            model.outputText.orEmpty()
                        }
                    }
                    if (inputText.isNotBlank()) {
                        ToolKeyValueRow(
                            label = stringResource(Res.string.chat_tool_input_label),
                            value = inputText,
                        )
                    }
                    if (outputDisplay.isNotBlank()) {
                        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                        ToolKeyValueRow(
                            label = stringResource(Res.string.tool_card_output_header),
                            value = outputDisplay,
                        )
                    }
                }

                // Sub-agent timeline (agent.spawn and friends): one row per spawned agent.
                if (isAgentTool && model.subAgents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.XS),
                    ) {
                        model.subAgents.forEach { agent ->
                            val agentAccent = if (agent.state == ToolExecutionState.Failed) colors.err else colors.accent
                            val agentStatusColor = when (agent.state) {
                                ToolExecutionState.Running -> colors.info
                                ToolExecutionState.Completed -> colors.ok
                                ToolExecutionState.Failed -> colors.err
                                ToolExecutionState.PendingApproval -> colors.warn
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surface2)
                                    .border(1.dp, agentAccent.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                                    .padding(DoraiSpacing.SM),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier.size(7.dp).clip(CircleShape).background(agentStatusColor),
                                    )
                                    Spacer(modifier = Modifier.width(DoraiSpacing.XS))
                                    Text(
                                        text = agent.role,
                                        color = agentAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                                    Text(
                                        text = agent.task.orEmpty(),
                                        color = colors.text,
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                                    if (agent.durationText != null) {
                                        Text(
                                            text = agent.durationText,
                                            color = colors.muted,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    DoraiStatusBadge(
                                        text = agent.status ?: stringResource(Res.string.tool_status_completed),
                                        color = when (agent.state) {
                                            ToolExecutionState.Running -> colors.info
                                            ToolExecutionState.Completed -> colors.ok
                                            ToolExecutionState.Failed -> colors.err
                                            ToolExecutionState.PendingApproval -> colors.warn
                                        },
                                    )
                                    if (!agent.resultSummary.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                                        Text(
                                            text = agent.resultSummary,
                                            color = colors.muted,
                                            fontSize = 11.5.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Command Block (Expandable code block with Copy button)
                if (!model.commandText.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.codeBackground)
                            .padding(DoraiSpacing.SM),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(Res.string.tool_card_command_header),
                                    color = colors.muted.copy(alpha = 0.7f),
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                IconButton(
                                    onClick = { copyToClipboard(model.commandText) },
                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(Res.string.a11y_copy),
                                        tint = colors.muted,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    text = "$ ${model.commandText}",
                                    color = colors.info,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }
                }

                // 2. Error-Focused Panel
                if (model.state == ToolExecutionState.Failed || !model.errorText.isNullOrEmpty()) {
                    val errorContent = model.errorText
                        ?: model.exitCode?.let { stringResource(Res.string.chat_command_failed_with_exit, it) }
                        ?: stringResource(Res.string.tool_status_failed)
                    Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.err.copy(alpha = 0.08f))
                            .border(1.dp, colors.err.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(DoraiSpacing.SM),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = colors.err,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val errorTitle = listOfNotNull(
                                        stringResource(Res.string.tool_execution_error),
                                        model.exitCode?.let { stringResource(Res.string.chat_exit_code, it) },
                                    ).joinToString(" · ")
                                    Text(
                                        text = errorTitle,
                                        color = colors.err,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                IconButton(
                                    onClick = { copyToClipboard(errorContent) },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(Res.string.a11y_copy_details),
                                        tint = colors.err.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorContent,
                                color = colors.err,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                            )

                            // Error Action Buttons: Retry + Ask agent to fix
                            if (onRetry != null || onAskFix != null) {
                                Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
                                ) {
                                    if (onRetry != null) {
                                        Button(
                                            onClick = onRetry,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.surface2,
                                                contentColor = colors.text,
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(Res.string.tool_action_retry),
                                                fontSize = 12.5.sp,
                                            )
                                        }
                                    }
                                    if (onAskFix != null) {
                                        Button(
                                            onClick = onAskFix,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.primary,
                                                contentColor = colors.onPrimary,
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.tool_action_ask_fix),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Collapsible Output (stdout) — key-value tools render their own Input/Output rows
                if (!model.outputText.isNullOrEmpty() && !isKeyValueTool(model.toolName)) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                    val outputLines = remember(model.outputText) { model.outputText.lines() }
                    var isOutputExpanded by remember(model.toolCallId) {
                        mutableStateOf(model.state == ToolExecutionState.Running || outputLines.size <= 4)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.codeBackground)
                            .padding(DoraiSpacing.SM),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(Res.string.tool_card_output_header),
                                        color = colors.muted.copy(alpha = 0.7f),
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(Res.string.chat_lines_label, outputLines.size),
                                        color = colors.muted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.surface2)
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (outputLines.size > 4) {
                                        Text(
                                            text = stringResource(if (isOutputExpanded) Res.string.chat_collapse_output else Res.string.chat_expand_output),
                                            color = colors.info,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .clickable(role = Role.Button) { isOutputExpanded = !isOutputExpanded }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(
                                        onClick = { copyToClipboard(model.outputText) },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(Res.string.a11y_copy),
                                            tint = colors.muted,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            if (!isOutputExpanded && outputLines.size > 4) {
                                val preview = outputLines.take(3).joinToString("\n")
                                Text(
                                    text = preview,
                                    color = colors.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                )
                                Text(
                                    text = stringResource(Res.string.tool_card_more_lines_format, outputLines.size - 3),
                                    color = colors.info.copy(alpha = 0.85f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier
                                        .clickable(role = Role.Button) { isOutputExpanded = true }
                                        .padding(top = 2.dp),
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState()),
                                ) {
                                    Text(
                                        text = model.outputText,
                                        color = colors.text,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Diff Block
                if (!model.diffText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                    DoraiDiffBlock(diff = model.diffText)
                }

                // 5. Pending Approval Action Row
                if (model.state == ToolExecutionState.PendingApproval) {
                    Spacer(modifier = Modifier.height(DoraiSpacing.MD))
                    DoraiApprovalActions(
                        onAllowOnce = onAllowOnce,
                        onAllowSession = onAllowSession,
                        onDeny = onDeny,
                    )
                }
            }
        }
    }
}
