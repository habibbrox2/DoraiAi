package com.dorai.ai.ui.chat

import com.dorai.ai.data.ExecutionLocation
import com.dorai.ai.data.ServiceEntry
import com.dorai.ai.ui.components.v2.DynamicUiModel
import com.dorai.ai.ui.components.v2.DynamicUiOption
import com.dorai.ai.ui.components.v2.ToolExecutionModel
import com.dorai.ai.ui.components.v2.ToolExecutionState

/**
 * Parsed details of a tool execution output.
 */
data class ParsedToolOutput(
    val stdout: String?,
    val stderr: String?,
    val exitCode: Int?,
    val isFailure: Boolean,
)

/**
 * Extracts stdout, stderr, exit code, and failure flag from tool output string.
 */
fun parseToolExecutionOutput(content: String): ParsedToolOutput {
    val stdout = Regex("\"stdout\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1)?.replace("\\n", "\n")
    val stderr = Regex("\"stderr\"\\s*:\\s*\"([^\"]*)\"").find(content)?.groupValues?.get(1)?.replace("\\n", "\n")
    val exitCode = Regex("\"(?:exitCode|exit_code)\"\\s*:\\s*(-?\\d+)").find(content)?.groupValues?.get(1)?.toIntOrNull()
    val hasSuccessFalse = content.contains("\"success\":false")
    val errorField = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1)
    val isFailure = hasSuccessFalse || (exitCode != null && exitCode != 0) || !errorField.isNullOrBlank() ||
        content.contains("tool_error", ignoreCase = true)

    val finalOutput = stdout ?: if (!isFailure) content else null
    val finalError = errorField ?: stderr ?: if (isFailure) content else null

    return ParsedToolOutput(
        stdout = finalOutput,
        stderr = finalError,
        exitCode = exitCode,
        isFailure = isFailure,
    )
}

/**
 * Parses command text from tool arguments.
 */
fun parseToolCommandText(args: String): String {
    val command = Regex("\"command\"\\s*:\\s*\"([^\"]+)\"").find(args)?.groupValues?.get(1)
    return command ?: args
}

/**
 * Helper builder to consolidate lifecycle events for a single tool call ID.
 */
private class ConsolidatedToolBuilder(
    val toolCallId: String,
    var toolName: String = "Tool",
    var argsSummary: String = "",
    var commandText: String? = null,
    var outputText: String? = null,
    var errorText: String? = null,
    var diffText: String? = null,
    var exitCode: Int? = null,
    var durationText: String? = null,
    var state: ToolExecutionState = ToolExecutionState.Running,
) {
    fun toModel(): ToolExecutionModel = ToolExecutionModel(
        toolName = toolName,
        argsSummary = argsSummary,
        state = state,
        durationText = durationText,
        commandText = commandText,
        outputText = outputText,
        errorText = errorText,
        diffText = diffText,
        toolCallId = toolCallId,
        exitCode = exitCode,
    )
}

/**
 * Groups chat history events into UI presentation messages.
 *
 * Consolidates multiple backend lifecycle events for the same tool execution
 * ([History.Role.TOOL_CONFIRMATION], [History.Role.TOOL_EXECUTING], [History.Role.TOOL])
 * into a single unified [ToolExecutionModel] without mutating backend events, execution IDs,
 * or ordering.
 */
fun groupHistoryToChatMessages(
    history: List<History>,
    pendingToolConfirmations: List<PendingToolConfirmation>,
    reasoningByAssistantId: Map<String, List<String>>,
    isSpeaking: Boolean,
    isSpeakingContentId: String?,
    availableServices: List<ServiceEntry>,
): List<ChatMessageItem> {
    // Hidden system turns ([SYSTEM]-prefixed interactive retries) stay in the provider context
    // but never render as user bubbles.
    val visibleHistory = history.filterNot { it.hiddenFromUi }
    val result = mutableListOf<ChatMessageItem>()
    val currentTurnEntries = mutableListOf<History>()
    val activeServiceIsLocal = availableServices.firstOrNull()?.executionLocation == ExecutionLocation.LOCAL

    fun flushAgentTurn() {
        if (currentTurnEntries.isEmpty()) return

        // Wall-clock span of this agent turn, when every relevant entry carries a timestamp.
        val timestamps = currentTurnEntries.mapNotNull { it.sentAt }
        val turnDurationMs = if (timestamps.size >= 2) {
            val span = timestamps.max() - timestamps.min()
            span.takeIf { it > 0 }
        } else {
            null
        }

        // 1. Consolidate tool executions within this turn by toolCallId
        val toolBuilders = linkedMapOf<String, ConsolidatedToolBuilder>()

        for (entry in currentTurnEntries) {
            // A. Tool calls initiated by ASSISTANT
            entry.toolCalls?.forEach { tc ->
                val builder = toolBuilders.getOrPut(tc.id) { ConsolidatedToolBuilder(tc.id) }
                builder.toolName = tc.name
                builder.argsSummary = tc.arguments
                if (tc.name.contains("shell") || tc.name.contains("cmd") || tc.name == "execute_shell_command") {
                    builder.commandText = parseToolCommandText(tc.arguments)
                }
                val isPending = pendingToolConfirmations.any { it.toolCallId == tc.id }
                if (isPending) {
                    builder.state = ToolExecutionState.PendingApproval
                }
            }

            // B. Tool confirmation prompt
            if (entry.role == History.Role.TOOL_CONFIRMATION) {
                val callId = entry.toolCallId ?: entry.id
                val builder = toolBuilders.getOrPut(callId) { ConsolidatedToolBuilder(callId) }
                if (!entry.toolName.isNullOrBlank()) builder.toolName = entry.toolName
                if (entry.content.isNotBlank()) {
                    builder.argsSummary = entry.content
                    if (builder.commandText == null && (builder.toolName.contains("shell") || builder.toolName == "execute_shell_command")) {
                        builder.commandText = parseToolCommandText(entry.content)
                    }
                }
                builder.state = ToolExecutionState.PendingApproval
            }

            // C. Tool actively executing
            if (entry.role == History.Role.TOOL_EXECUTING) {
                val callId = entry.toolCallId ?: entry.id
                val builder = toolBuilders.getOrPut(callId) { ConsolidatedToolBuilder(callId) }
                if (!entry.toolName.isNullOrBlank() && entry.toolName != "execute_shell_command") {
                    builder.toolName = entry.toolName
                } else if (builder.toolName == "Tool") {
                    builder.toolName = entry.content.takeIf { it.isNotBlank() } ?: "Tool"
                }
                if (entry.content.isNotBlank() && builder.argsSummary.isBlank()) {
                    builder.argsSummary = entry.content
                    if (builder.commandText == null && (builder.toolName.contains("shell") || builder.toolName == "execute_shell_command")) {
                        builder.commandText = parseToolCommandText(entry.content)
                    }
                }
                val isPending = entry.pendingToolConfirmation || pendingToolConfirmations.any { it.toolCallId == callId }
                if (isPending) {
                    builder.state = ToolExecutionState.PendingApproval
                } else if (builder.state != ToolExecutionState.Completed && builder.state != ToolExecutionState.Failed) {
                    builder.state = ToolExecutionState.Running
                }
            }

            // D. Tool completed or failed
            if (entry.role == History.Role.TOOL) {
                val callId = entry.toolCallId ?: entry.id
                val builder = toolBuilders.getOrPut(callId) { ConsolidatedToolBuilder(callId) }
                if (!entry.toolName.isNullOrBlank()) builder.toolName = entry.toolName

                val parsed = parseToolExecutionOutput(entry.content)
                builder.exitCode = parsed.exitCode
                if (parsed.isFailure) {
                    builder.state = ToolExecutionState.Failed
                    builder.errorText = parsed.stderr
                    builder.outputText = parsed.stdout
                } else {
                    builder.state = ToolExecutionState.Completed
                    builder.outputText = parsed.stdout
                    builder.errorText = parsed.stderr
                }
            }
        }

        val consolidatedTools = toolBuilders.values.map { it.toModel() }

        // 2. Identify ASSISTANT messages in this turn
        val assistantEntries = currentTurnEntries.filter { it.role == History.Role.ASSISTANT }
        val entriesWithContent = assistantEntries.filter { it.content.isNotEmpty() }

        if (entriesWithContent.isNotEmpty()) {
            // Distribute consolidated tools to the assistant message(s)
            entriesWithContent.forEachIndexed { index, asst ->
                val isLast = index == entriesWithContent.lastIndex
                val toolsForThisMessage = if (isLast) consolidatedTools else emptyList()
                val dynamicUi = asst.uiSubmission?.let { sub ->
                    DynamicUiModel(
                        title = "Form Submission",
                        description = sub.sourceContent.takeIf { it.isNotBlank() },
                        options = sub.values.map { (k, v) -> DynamicUiOption(id = k, title = "$k: $v") },
                        isLocked = true,
                    )
                }
                val reasoning: List<String> = reasoningByAssistantId[asst.id] ?: emptyList()
                val isSpeakingThis = isSpeaking && isSpeakingContentId == asst.id

                result.add(
                    ChatMessageItem(
                        id = asst.id,
                        isUser = false,
                        text = asst.content,
                        modelName = asst.fallbackServiceName ?: availableServices.firstOrNull()?.serviceName,
                        // Fallback answers always come from the cloud-hosted keyless targets,
                        // even when the primary configured service is local.
                        isModelLocal = activeServiceIsLocal && asst.fallbackServiceName == null,
                        serviceIcon = availableServices.firstOrNull()?.icon,
                        toolExecutions = toolsForThisMessage,
                        dynamicUi = dynamicUi,
                        attachments = asst.attachments,
                        reasoningSegments = reasoning,
                        isThinking = asst.isThinking,
                        isSpeaking = isSpeakingThis,
                        fallbackServiceName = asst.fallbackServiceName,
                        sentAt = asst.sentAt,
                        agentWorkedDurationMs = turnDurationMs,
                    ),
                )
            }
        } else {
            // No assistant text yet; tools are executing, waiting for approval, or assistant is thinking
            val latestEntry = currentTurnEntries.last()
            val reasoning: List<String> = assistantEntries.firstNotNullOfOrNull { reasoningByAssistantId[it.id] } ?: emptyList()
            val isThinking = assistantEntries.any { it.isThinking }

            if (consolidatedTools.isNotEmpty() || isThinking || reasoning.isNotEmpty()) {
                result.add(
                    ChatMessageItem(
                        id = latestEntry.id,
                        isUser = false,
                        text = "",
                        modelName = availableServices.firstOrNull()?.serviceName ?: "Agent",
                        isModelLocal = activeServiceIsLocal,
                        serviceIcon = availableServices.firstOrNull()?.icon,
                        toolExecutions = consolidatedTools,
                        reasoningSegments = reasoning,
                        isThinking = isThinking,
                        sentAt = assistantEntries.firstNotNullOfOrNull { it.sentAt },
                        agentWorkedDurationMs = turnDurationMs,
                    ),
                )
            }
        }

        currentTurnEntries.clear()
    }

    for (item in visibleHistory) {
        if (item.role == History.Role.USER) {
            flushAgentTurn()
            result.add(
                ChatMessageItem(
                    id = item.id,
                    isUser = true,
                    text = item.content,
                    attachments = item.attachments,
                    sentAt = item.sentAt,
                ),
            )
        } else {
            currentTurnEntries.add(item)
        }
    }

    flushAgentTurn()
    return result
}
