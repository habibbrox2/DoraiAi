package com.dorai.ai.ui.components.v2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoraiComponentsTest {

    @Test
    fun `tool execution states and models work as expected`() {
        val running = ToolExecutionModel(
            toolName = "fetch_url",
            argsSummary = "https://example.com",
            state = ToolExecutionState.Running,
        )
        assertEquals(ToolExecutionState.Running, running.state)

        val completed = ToolExecutionModel(
            toolName = "search",
            argsSummary = "kotlin multiplatform",
            state = ToolExecutionState.Completed,
            durationText = "1.2s",
            outputText = "results found: 5",
        )
        assertEquals(ToolExecutionState.Completed, completed.state)
        assertEquals("1.2s", completed.durationText)

        val failed = ToolExecutionModel(
            toolName = "run_shell",
            argsSummary = "cat /invalid/path",
            state = ToolExecutionState.Failed,
            errorText = "No such file or directory",
        )
        assertEquals(ToolExecutionState.Failed, failed.state)

        val pending = ToolExecutionModel(
            toolName = "edit_file",
            argsSummary = "Main.kt (line 42)",
            state = ToolExecutionState.PendingApproval,
            diffText = "- old line\n+ new line",
        )
        assertEquals(ToolExecutionState.PendingApproval, pending.state)
    }

    @Test
    fun `default agent modes contain 4 modes`() {
        assertEquals(4, DefaultAgentModes.size)
        val modeIds = DefaultAgentModes.map { it.id }
        assertEquals(listOf("ask", "plan", "build", "review"), modeIds)
    }

    @Test
    fun `dynamic ui model handles options and lock state`() {
        val model = DynamicUiModel(
            title = "Pick a deployment option",
            options = listOf(
                DynamicUiOption("opt1", "Cloudflare Workers", "Serverless edge runtime"),
                DynamicUiOption("opt2", "Docker Container", "Standard container deployment"),
            ),
            selectedOptionId = "opt1",
            isLocked = false,
        )
        assertEquals(2, model.options.size)
        assertEquals("opt1", model.selectedOptionId)
        assertFalse(model.isLocked)

        val locked = model.copy(isLocked = true)
        assertTrue(locked.isLocked)
    }

    @Test
    fun `timeline items preserve ordering and node types`() {
        val items = listOf(
            TimelineItemModel("1", TimelineNodeType.Task, "Initialize project", state = TimelineNodeState.Completed),
            TimelineItemModel("2", TimelineNodeType.Thinking, "Analyzing requirements", state = TimelineNodeState.Completed),
            TimelineItemModel("3", TimelineNodeType.Tool, "generate_code", state = TimelineNodeState.Running),
            TimelineItemModel("4", TimelineNodeType.Note, "Review pending", state = TimelineNodeState.Pending),
        )
        assertEquals(4, items.size)
        assertEquals(TimelineNodeType.Task, items[0].type)
        assertEquals(TimelineNodeType.Thinking, items[1].type)
        assertEquals(TimelineNodeType.Tool, items[2].type)
        assertEquals(TimelineNodeType.Note, items[3].type)
        assertEquals(TimelineNodeState.Running, items[2].state)
    }
}
