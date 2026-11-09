package com.dorai.ai.ui.chat

import com.dorai.ai.data.Service
import com.dorai.ai.data.ServiceEntry
import com.dorai.ai.ui.components.v2.ToolExecutionState
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolEventGrouperTest {

    private val sampleService = ServiceEntry(
        instanceId = "gemini-1",
        serviceId = Service.Gemini.id,
        serviceName = Service.Gemini.displayName,
        modelId = "gemini-1.5-pro",
        icon = Service.Gemini.icon,
    )

    @Test
    fun testSingleToolExecutionConsolidation() {
        val history = listOf(
            History(
                id = "u1",
                role = History.Role.USER,
                content = "List files in directory",
            ),
            History(
                id = "a1",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(
                    ToolCallInfo(
                        id = "call-1",
                        name = "execute_shell_command",
                        arguments = """{"command":"ls -la"}""",
                    ),
                ),
            ),
            History(
                id = "e1",
                role = History.Role.TOOL_EXECUTING,
                content = "ls -la",
                toolCallId = "call-1",
                toolName = "execute_shell_command",
            ),
            History(
                id = "r1",
                role = History.Role.TOOL,
                content = """{"stdout":"file1.txt\nfile2.txt","exit_code":0,"success":true}""",
                toolCallId = "call-1",
                toolName = "execute_shell_command",
            ),
            History(
                id = "a2",
                role = History.Role.ASSISTANT,
                content = "I found 2 files: file1.txt and file2.txt",
            ),
        )

        val messages = groupHistoryToChatMessages(
            history = history,
            pendingToolConfirmations = emptyList(),
            reasoningByAssistantId = emptyMap(),
            isSpeaking = false,
            isSpeakingContentId = null,
            availableServices = listOf(sampleService),
        )

        // Should have 1 USER message and 1 ASSISTANT message (NOT 5 messages!)
        assertEquals(2, messages.size)

        val userMsg = messages[0]
        assertTrue(userMsg.isUser)
        assertEquals("List files in directory", userMsg.text)

        val asstMsg = messages[1]
        assertFalse(asstMsg.isUser)
        assertEquals("I found 2 files: file1.txt and file2.txt", asstMsg.text)

        // Exactly 1 consolidated tool execution (NO duplicate cards!)
        assertEquals(1, asstMsg.toolExecutions.size)
        val tool = asstMsg.toolExecutions[0]
        assertEquals("call-1", tool.toolCallId)
        assertEquals("execute_shell_command", tool.toolName)
        assertEquals("ls -la", tool.commandText)
        assertEquals(ToolExecutionState.Completed, tool.state)
        assertEquals(0, tool.exitCode)
        assertEquals("file1.txt\nfile2.txt", tool.outputText)
    }

    @Test
    fun testToolPendingApprovalConsolidation() {
        val history = listOf(
            History(
                id = "u1",
                role = History.Role.USER,
                content = "Edit Settings.kt",
            ),
            History(
                id = "a1",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(
                    ToolCallInfo(
                        id = "call-edit",
                        name = "edit_file",
                        arguments = """{"path":"Settings.kt"}""",
                    ),
                ),
            ),
            History(
                id = "c1",
                role = History.Role.TOOL_CONFIRMATION,
                content = """{"path":"Settings.kt"}""",
                toolCallId = "call-edit",
                toolName = "edit_file",
            ),
        )

        val pendingConfirmations = listOf(
            PendingToolConfirmation(
                toolCallId = "call-edit",
                toolName = "edit_file",
                arguments = """{"path":"Settings.kt"}""",
            ),
        )

        val messages = groupHistoryToChatMessages(
            history = history,
            pendingToolConfirmations = pendingConfirmations,
            reasoningByAssistantId = emptyMap(),
            isSpeaking = false,
            isSpeakingContentId = null,
            availableServices = listOf(sampleService),
        )

        assertEquals(2, messages.size)
        val agentMsg = messages[1]
        assertEquals(1, agentMsg.toolExecutions.size)

        val tool = agentMsg.toolExecutions[0]
        assertEquals("call-edit", tool.toolCallId)
        assertEquals(ToolExecutionState.PendingApproval, tool.state)
    }

    @Test
    fun testToolFailureConsolidation() {
        val history = listOf(
            History(
                id = "u1",
                role = History.Role.USER,
                content = "Run bad command",
            ),
            History(
                id = "e1",
                role = History.Role.TOOL_EXECUTING,
                content = "bad_cmd",
                toolCallId = "call-fail",
                toolName = "execute_shell_command",
            ),
            History(
                id = "r1",
                role = History.Role.TOOL,
                content = """{"error":"command not found: bad_cmd","exit_code":127,"success":false}""",
                toolCallId = "call-fail",
                toolName = "execute_shell_command",
            ),
        )

        val messages = groupHistoryToChatMessages(
            history = history,
            pendingToolConfirmations = emptyList(),
            reasoningByAssistantId = emptyMap(),
            isSpeaking = false,
            isSpeakingContentId = null,
            availableServices = listOf(sampleService),
        )

        assertEquals(2, messages.size)
        val agentMsg = messages[1]
        assertEquals(1, agentMsg.toolExecutions.size)

        val tool = agentMsg.toolExecutions[0]
        assertEquals("call-fail", tool.toolCallId)
        assertEquals(ToolExecutionState.Failed, tool.state)
        assertEquals(127, tool.exitCode)
        assertEquals("command not found: bad_cmd", tool.errorText)
    }

    @Test
    fun testMultipleSequentialToolsInTurn() {
        val history = listOf(
            History(
                id = "u1",
                role = History.Role.USER,
                content = "Search and read files",
            ),
            History(
                id = "a1",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(
                    ToolCallInfo(id = "c1", name = "search", arguments = "ThemeState"),
                ),
            ),
            History(
                id = "r1",
                role = History.Role.TOOL,
                content = "Found ThemeState.kt",
                toolCallId = "c1",
                toolName = "search",
            ),
            History(
                id = "a2",
                role = History.Role.ASSISTANT,
                content = "",
                toolCalls = persistentListOf(
                    ToolCallInfo(id = "c2", name = "read_file", arguments = "ThemeState.kt"),
                ),
            ),
            History(
                id = "r2",
                role = History.Role.TOOL,
                content = "class ThemeState { val isDark = true }",
                toolCallId = "c2",
                toolName = "read_file",
            ),
            History(
                id = "a3",
                role = History.Role.ASSISTANT,
                content = "ThemeState contains the dark theme flag.",
            ),
        )

        val messages = groupHistoryToChatMessages(
            history = history,
            pendingToolConfirmations = emptyList(),
            reasoningByAssistantId = emptyMap(),
            isSpeaking = false,
            isSpeakingContentId = null,
            availableServices = listOf(sampleService),
        )

        assertEquals(2, messages.size)
        val agentMsg = messages[1]
        assertEquals("ThemeState contains the dark theme flag.", agentMsg.text)

        // Both c1 and c2 are in this agent message's activity
        assertEquals(2, agentMsg.toolExecutions.size)
        assertEquals("c1", agentMsg.toolExecutions[0].toolCallId)
        assertEquals("search", agentMsg.toolExecutions[0].toolName)
        assertEquals(ToolExecutionState.Completed, agentMsg.toolExecutions[0].state)

        assertEquals("c2", agentMsg.toolExecutions[1].toolCallId)
        assertEquals("read_file", agentMsg.toolExecutions[1].toolName)
        assertEquals(ToolExecutionState.Completed, agentMsg.toolExecutions[1].state)
    }

    // ------------------------------------------------------------------
    // Local/Cloud propagation (who-row badge must match the active service)
    // ------------------------------------------------------------------

    private fun assistantTextHistory(text: String) = listOf(
        History(id = "u1", role = History.Role.USER, content = "hi"),
        History(id = "a1", role = History.Role.ASSISTANT, content = text),
    )

    @Test
    fun `messages inherit the active service's execution location`() {
        val localEntry = ServiceEntry(
            instanceId = "litert-1",
            serviceId = Service.LiteRT.id,
            serviceName = Service.LiteRT.displayName,
            modelId = "gemma-3-1b",
            icon = Service.LiteRT.icon,
            executionLocation = com.dorai.ai.data.ExecutionLocation.LOCAL,
        )
        val local = groupHistoryToChatMessages(
            history = assistantTextHistory("on-device answer"),
            pendingToolConfirmations = emptyList(),
            reasoningByAssistantId = emptyMap(),
            isSpeaking = false,
            isSpeakingContentId = null,
            availableServices = listOf(localEntry),
        )
        val localMsg = local.first { !it.isUser }
        assertTrue(localMsg.isModelLocal, "LOCAL service must propagate isModelLocal to assistant messages")

        val cloud = groupHistoryToChatMessages(
            history = assistantTextHistory("cloud answer"),
            pendingToolConfirmations = emptyList(),
            reasoningByAssistantId = emptyMap(),
            isSpeaking = false,
            isSpeakingContentId = null,
            availableServices = listOf(sampleService),
        )
        val cloudMsg = cloud.first { !it.isUser }
        assertFalse(cloudMsg.isModelLocal, "CLOUD service must not be flagged local")
        assertEquals(sampleService.icon, cloudMsg.serviceIcon, "assistant messages carry the provider icon for the who-row")
    }

    @Test
    fun `fallback-answered messages are cloud even when the active service is local`() {
        val localEntry = ServiceEntry(
            instanceId = "litert-1",
            serviceId = Service.LiteRT.id,
            serviceName = Service.LiteRT.displayName,
            modelId = "gemma-3-1b",
            icon = Service.LiteRT.icon,
            executionLocation = com.dorai.ai.data.ExecutionLocation.LOCAL,
        )
        val history = listOf(
            History(id = "u1", role = History.Role.USER, content = "hi"),
            History(id = "a1", role = History.Role.ASSISTANT, content = "fallback answer", fallbackServiceName = "Kilo"),
        )
        val messages = groupHistoryToChatMessages(
            history = history,
            pendingToolConfirmations = emptyList(),
            reasoningByAssistantId = emptyMap(),
            isSpeaking = false,
            isSpeakingContentId = null,
            availableServices = listOf(localEntry),
        )
        val msg = messages.first { !it.isUser }
        assertFalse(msg.isModelLocal, "keyless fallback targets are cloud-hosted; badge must say Cloud")
    }
}
