package com.inspiredandroid.kai.ui.chat.activities

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Immutable
data class AgentActivity(
    val id: String = Uuid.random().toString(),
    val runId: String? = null,
    val type: ActivityType,
    val title: String,
    val description: String? = null,
    val status: ActivityStatus = ActivityStatus.PENDING,
    val startTime: Long = Clock.System.now().toEpochMilliseconds(),
    val endTime: Long? = null,
    val durationMs: Long? = null,
    val details: ActivityDetails = ActivityDetails.Generic(),
    val isExpandable: Boolean = true,
    val isExpanded: Boolean = false,
    val parentId: String? = null,
    val children: ImmutableList<AgentActivity> = persistentListOf(),
)

enum class ActivityType {
    AGENT_STARTED,
    THOUGHT,
    TOOL_CALL,
    SHELL_COMMAND,
    SKILL_DISCOVERY,
    SKILL_LOADED,
    SKILL_EXECUTION,
    FILE_READ,
    FILE_WRITE,
    FILE_EDIT,
    FILE_DELETE,
    PERMISSION_REQUEST,
    PERMISSION_RESOLVED,
    AGENT_PROGRESS,
    AGENT_COMPLETED,
    AGENT_FAILED,
    AGENT_CANCELLED,
}

enum class ActivityStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    DENIED,
    TIMEOUT,
}

@Immutable
sealed class ActivityDetails {
    @Immutable
    data class Generic(
        val content: String? = null,
    ) : ActivityDetails()

    @Immutable
    data class ToolCall(
        val toolName: String,
        val toolDisplayName: String? = null,
        val arguments: String? = null,
        val result: String? = null,
        val error: String? = null,
    ) : ActivityDetails()

    @Immutable
    data class ShellCommand(
        val command: String,
        val workingDirectory: String? = null,
        val stdout: String? = null,
        val stderr: String? = null,
        val exitCode: Int? = null,
        val environment: String? = null,
    ) : ActivityDetails()

    @Immutable
    data class Skill(
        val skillId: String,
        val skillName: String,
        val description: String? = null,
        val stage: String? = null,
    ) : ActivityDetails()

    @Immutable
    data class FileOperation(
        val path: String,
        val operation: String,
        val changeSummary: String? = null,
        val oldContent: String? = null,
        val newContent: String? = null,
        val diff: String? = null,
    ) : ActivityDetails()

    @Immutable
    data class Permission(
        val action: String,
        val target: String,
        val scope: String? = null,
        val allowed: Boolean? = null,
        val persistent: Boolean = false,
    ) : ActivityDetails()
}
