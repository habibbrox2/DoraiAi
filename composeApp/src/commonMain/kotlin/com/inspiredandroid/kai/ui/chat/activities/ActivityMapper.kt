package com.inspiredandroid.kai.ui.chat.activities

import com.inspiredandroid.kai.ui.chat.History
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock

/**
 * Converts persisted conversation history into a stable execution trace.
 * Tool-call IDs are authoritative; name matching is only a legacy fallback.
 */
object ActivityMapper {
    fun historyToActivities(history: List<History>): List<AgentActivity> {
        val activities = mutableListOf<AgentActivity>()
        val indexesByToolCallId = mutableMapOf<String, Int>()
        var currentRunId: String? = null

        for (entry in history) {
            when (entry.role) {
                History.Role.USER,
                History.Role.TOOL_CONFIRMATION,
                -> Unit

                History.Role.ASSISTANT -> {
                    val calls = entry.toolCalls.orEmpty()
                    if (calls.isNotEmpty()) {
                        currentRunId = entry.id
                        calls.forEach { call ->
                            indexesByToolCallId[call.id] = activities.size
                            activities.add(createActivity(call.id, entry.id, call.name, call.arguments))
                        }
                    }
                }

                History.Role.TOOL_EXECUTING -> {
                    if (entry.isStatusMessage) continue
                    val isShell = entry.content == "execute_shell_command" || entry.toolName == "execute_shell_command"
                    val toolName = if (isShell) "execute_shell_command" else entry.toolName?.takeIf { it.isNotBlank() } ?: entry.content
                    val index = entry.toolCallId?.let(indexesByToolCallId::get)
                    if (index != null) {
                        activities[index] = activities[index].copy(
                            status = ActivityStatus.RUNNING,
                            title = activityTitle(activities[index].type, toolName),
                        )
                    } else {
                        val activity = createActivity(entry.toolCallId ?: entry.id, currentRunId, toolName, "")
                        val details = if (isShell) ActivityDetails.ShellCommand(command = entry.toolName.orEmpty()) else activity.details
                        if (entry.toolCallId != null) indexesByToolCallId[entry.toolCallId] = activities.size
                        activities.add(activity.copy(status = ActivityStatus.RUNNING, details = details))
                    }
                }

                History.Role.TOOL -> {
                    val toolName = entry.toolName.orEmpty()
                    val index = entry.toolCallId?.let(indexesByToolCallId::get)
                        ?: if (entry.toolCallId == null) {
                            activities.indexOfLast {
                                it.status in setOf(ActivityStatus.PENDING, ActivityStatus.RUNNING) && matchesTool(it, toolName)
                            }.takeIf { it >= 0 }
                        } else {
                            null
                        }
                    val completedAt = Clock.System.now().toEpochMilliseconds()
                    val failed = isFailure(entry.content)
                    if (index != null) {
                        val existing = activities[index]
                        activities[index] = existing.copy(
                            status = if (failed) ActivityStatus.FAILED else ActivityStatus.COMPLETED,
                            endTime = completedAt,
                            durationMs = (completedAt - existing.startTime).coerceAtLeast(0),
                            details = withResult(existing.details, entry.content, failed),
                        )
                    } else {
                        val activity = createActivity(entry.toolCallId ?: entry.id, currentRunId, toolName.ifBlank { "tool" }, "")
                        activities.add(
                            activity.copy(
                                status = if (failed) ActivityStatus.FAILED else ActivityStatus.COMPLETED,
                                endTime = completedAt,
                                durationMs = 0,
                                details = withResult(activity.details, entry.content, failed),
                            ),
                        )
                    }
                }
            }
        }
        return activities.toImmutableList()
    }

    private fun createActivity(id: String, runId: String?, toolName: String, arguments: String): AgentActivity {
        val type = activityType(toolName)
        val details = when (type) {
            ActivityType.SHELL_COMMAND -> parseShellCommand(arguments)

            ActivityType.FILE_READ,
            ActivityType.FILE_WRITE,
            ActivityType.FILE_EDIT,
            ActivityType.FILE_DELETE,
            -> parseFileOperation(toolName, arguments)

            else -> ActivityDetails.ToolCall(toolName = toolName, arguments = arguments)
        }
        return AgentActivity(id = id, runId = runId, type = type, title = activityTitle(type, toolName), status = ActivityStatus.PENDING, details = details)
    }

    private fun activityType(toolName: String): ActivityType {
        val name = toolName.lowercase()
        if (name == "execute_shell_command" || name.contains("shell") || name.contains("terminal")) return ActivityType.SHELL_COMMAND
        if (name == "propose_file_delete") return ActivityType.FILE_DELETE
        if (name == "propose_file_change") return ActivityType.FILE_EDIT
        if (name.contains("patch")) return ActivityType.FILE_EDIT
        if (name.contains("file") || name.contains("filesystem")) {
            return when {
                name.contains("delete") || name.contains("remove") -> ActivityType.FILE_DELETE
                name.contains("edit") || name.contains("patch") -> ActivityType.FILE_EDIT
                name.contains("write") || name.contains("create") -> ActivityType.FILE_WRITE
                else -> ActivityType.FILE_READ
            }
        }
        return ActivityType.TOOL_CALL
    }

    private fun activityTitle(type: ActivityType, toolName: String): String = when (type) {
        ActivityType.SHELL_COMMAND -> "Running terminal command"
        ActivityType.FILE_READ -> "Reading file"
        ActivityType.FILE_WRITE -> "Writing file"
        ActivityType.FILE_EDIT -> "Editing file"
        ActivityType.FILE_DELETE -> "Deleting file"
        else -> "Using ${toolName.ifBlank { "tool" }}"
    }

    private fun matchesTool(activity: AgentActivity, toolName: String): Boolean = when (val details = activity.details) {
        is ActivityDetails.ToolCall -> details.toolName == toolName
        is ActivityDetails.ShellCommand -> toolName == "execute_shell_command" || activity.type == ActivityType.SHELL_COMMAND
        is ActivityDetails.FileOperation -> toolName.contains("file", ignoreCase = true)
        else -> false
    }

    private fun isFailure(content: String): Boolean = content.contains("\"success\":false") || content.contains("\"error\"") || content.contains("tool_error", ignoreCase = true)

    private fun withResult(details: ActivityDetails, result: String, failed: Boolean): ActivityDetails = when (details) {
        is ActivityDetails.ToolCall -> details.copy(result = result, error = if (failed) result else null)

        is ActivityDetails.ShellCommand -> parseShellResult(details, result)

        is ActivityDetails.FileOperation -> details.copy(
            changeSummary = jsonString(result, "change_summary", "summary", "message") ?: result,
            oldContent = details.oldContent ?: jsonString(result, "old_content", "oldContent", "before"),
            newContent = details.newContent ?: jsonString(result, "new_content", "newContent", "content", "after"),
            diff = details.diff ?: jsonString(result, "diff", "patch", "unified_diff"),
            proposalId = details.proposalId ?: jsonString(result, "proposal_id", "proposalId"),
        )

        is ActivityDetails.Generic -> details.copy(content = result)

        is ActivityDetails.Skill -> details

        is ActivityDetails.Permission -> details
    }

    private fun parseFileOperation(toolName: String, args: String): ActivityDetails.FileOperation {
        val path = jsonString(args, "path", "file_path", "filePath", "filename", "file").orEmpty()
        val oldContent = jsonString(args, "old_content", "oldContent", "before")
        val newContent = jsonString(args, "new_content", "newContent", "content", "after")
        val diff = jsonString(args, "diff", "patch", "unified_diff")
        val operation = when (activityType(toolName)) {
            ActivityType.FILE_DELETE -> "Delete"
            ActivityType.FILE_EDIT -> "Edit"
            ActivityType.FILE_WRITE -> "Write"
            else -> "Read"
        }
        return ActivityDetails.FileOperation(
            path = path.ifBlank { "File operation" },
            operation = operation,
            oldContent = oldContent,
            newContent = newContent,
            diff = diff,
        )
    }

    private fun jsonString(source: String, vararg keys: String): String? {
        val parsed = runCatching { Json.parseToJsonElement(source) as? JsonObject }.getOrNull()
        return keys.firstNotNullOfOrNull { key ->
            val value = parsed?.get(key) ?: return@firstNotNullOfOrNull null
            when (value) {
                is JsonPrimitive -> value.content
                else -> value.toString()
            }
        }
    }

    internal fun parseShellCommand(args: String): ActivityDetails.ShellCommand = try {
        val command = Regex("\"command\"\\s*:\\s*\"([^\"]+)\"").find(args)?.groupValues?.get(1)
        val directory = Regex("\"working_dir\"\\s*:\\s*\"([^\"]+)\"").find(args)?.groupValues?.get(1)
        ActivityDetails.ShellCommand(command = command ?: args, workingDirectory = directory)
    } catch (_: Exception) {
        ActivityDetails.ShellCommand(command = args)
    }

    internal fun parseShellResult(details: ActivityDetails.ShellCommand, result: String): ActivityDetails.ShellCommand = try {
        val stdout = Regex("\"stdout\"\\s*:\\s*\"([^\"]*)\"").find(result)?.groupValues?.get(1)
        val stderr = Regex("\"stderr\"\\s*:\\s*\"([^\"]*)\"").find(result)?.groupValues?.get(1)
        val exitCode = Regex("\"(?:exitCode|exit_code)\"\\s*:\\s*(-?\\d+)").find(result)?.groupValues?.get(1)?.toIntOrNull()
        details.copy(stdout = stdout?.replace("\\n", "\n"), stderr = stderr?.replace("\\n", "\n"), exitCode = exitCode)
    } catch (_: Exception) {
        details.copy(stdout = result)
    }
}




