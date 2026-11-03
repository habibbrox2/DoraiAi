package com.inspiredandroid.kai.ui.chat.activities

import com.inspiredandroid.kai.ui.chat.History
import com.inspiredandroid.kai.ui.chat.ToolCallInfo
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ActivityMapperTest {
    @Test
    fun concurrentSameNameToolCallsAreCorrelatedByToolCallId() {
        val history = listOf(
            History(
                id = "assistant-run",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(
                    ToolCallInfo(id = "call-a", name = "search", arguments = """{"query":"alpha"}"""),
                    ToolCallInfo(id = "call-b", name = "search", arguments = """{"query":"beta"}"""),
                ),
            ),
            History(id = "exec-a", role = History.Role.TOOL_EXECUTING, content = "search", toolCallId = "call-a", toolName = "search"),
            History(id = "exec-b", role = History.Role.TOOL_EXECUTING, content = "search", toolCallId = "call-b", toolName = "search"),
            History(id = "result-b", role = History.Role.TOOL, content = """{"success":true,"value":"beta-result"}""", toolCallId = "call-b", toolName = "search"),
            History(id = "result-a", role = History.Role.TOOL, content = """{"success":true,"value":"alpha-result"}""", toolCallId = "call-a", toolName = "search"),
        )

        val activities = ActivityMapper.historyToActivities(history)

        assertEquals(2, activities.size)
        val alpha = activities.single { it.id == "call-a" }
        val beta = activities.single { it.id == "call-b" }
        assertEquals("assistant-run", alpha.runId)
        assertEquals(ActivityStatus.COMPLETED, alpha.status)
        assertEquals(ActivityStatus.COMPLETED, beta.status)
        assertTrue((alpha.details as ActivityDetails.ToolCall).result.orEmpty().contains("alpha-result"))
        assertTrue((beta.details as ActivityDetails.ToolCall).result.orEmpty().contains("beta-result"))
    }

    @Test
    fun failedToolResultIsMarkedFailed() {
        val history = listOf(
            History(
                id = "assistant-run",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(ToolCallInfo(id = "call-failed", name = "search", arguments = "{}")),
            ),
            History(
                id = "failed-result",
                role = History.Role.TOOL,
                content = """{"success":false,"error":"unavailable"}""",
                toolCallId = "call-failed",
                toolName = "search",
            ),
        )

        val activity = ActivityMapper.historyToActivities(history).single()

        assertEquals(ActivityStatus.FAILED, activity.status)
        val details = assertIs<ActivityDetails.ToolCall>(activity.details)
        assertTrue(details.error.orEmpty().contains("unavailable"))
    }

    @Test
    fun shellCommandIsMappedToTerminalActivity() {
        val history = listOf(
            History(
                id = "assistant-run",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(
                    ToolCallInfo(id = "shell-call", name = "execute_shell_command", arguments = """{"command":"pwd","working_dir":"/workspace"}"""),
                ),
            ),
        )

        val activity = ActivityMapper.historyToActivities(history).single()

        assertEquals(ActivityType.SHELL_COMMAND, activity.type)
        val details = assertIs<ActivityDetails.ShellCommand>(activity.details)
        assertEquals("pwd", details.command)
        assertEquals("/workspace", details.workingDirectory)
    }

    @Test
    fun fileOperationArgumentsDecodeBeforeAfterAndPatch() {
        val history = listOf(
            History(
                id = "assistant-file-run",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(
                    ToolCallInfo(
                        id = "file-call",
                        name = "edit_file",
                        arguments = """{"path":"src/Main.kt","old_content":"fun main() {}","new_content":"fun main() { println(1) }","diff":"@@ -1 +1 @@\\n-fun main() {}\\n+fun main() { println(1) }"}""",
                    ),
                ),
            ),
        )
        val details = assertIs<ActivityDetails.FileOperation>(ActivityMapper.historyToActivities(history).single().details)
        assertEquals("src/Main.kt", details.path)
        assertEquals("fun main() {}", details.oldContent)
        assertEquals("fun main() { println(1) }", details.newContent)
        assertTrue(details.diff.orEmpty().contains("@@ -1 +1 @@"))
    }

    @Test
    fun fileOperationResultCanSupplyBeforeAndAfterSnapshots() {
        val history = listOf(
            History(
                id = "assistant-file-run",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(ToolCallInfo(id = "file-call", name = "write_file", arguments = """{"path":"README.md"}""")),
            ),
            History(
                id = "file-result",
                role = History.Role.TOOL,
                content = """{"success":true,"old_content":"before","new_content":"after","summary":"Updated README"}""",
                toolCallId = "file-call",
                toolName = "write_file",
            ),
        )
        val details = assertIs<ActivityDetails.FileOperation>(ActivityMapper.historyToActivities(history).single().details)
        assertEquals("before", details.oldContent)
        assertEquals("after", details.newContent)
        assertTrue(details.changeSummary.orEmpty().contains("Updated README"))
    }
}
