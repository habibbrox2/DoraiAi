package com.dorai.ai.ui.chat.activities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentReviewModelsTest {
    @Test
    fun runSummaryCountsDistinctFilesAndObservableDuration() {
        val activities = listOf(
            AgentActivity(id = "tool", type = ActivityType.TOOL_CALL, title = "Search", status = ActivityStatus.COMPLETED, startTime = 100, endTime = 450),
            AgentActivity(id = "edit", type = ActivityType.FILE_EDIT, title = "Edit", status = ActivityStatus.COMPLETED, startTime = 120, endTime = 700, details = ActivityDetails.FileOperation("src/A.kt", "Edit")),
            AgentActivity(id = "write", type = ActivityType.FILE_WRITE, title = "Write", status = ActivityStatus.FAILED, startTime = 150, endTime = 800, details = ActivityDetails.FileOperation("src/A.kt", "Write")),
            AgentActivity(id = "shell", type = ActivityType.SHELL_COMMAND, title = "Terminal", status = ActivityStatus.RUNNING, startTime = 200, details = ActivityDetails.ShellCommand("pwd")),
        )
        val summary = summarizeAgentRun(activities, nowMs = 1_000)
        assertEquals(4, summary.total)
        assertEquals(2, summary.completed)
        assertEquals(1, summary.running)
        assertEquals(1, summary.failed)
        assertEquals(1, summary.fileCount)
        assertEquals(1, summary.terminalCount)
        assertEquals(900, summary.durationMs)
    }

    @Test
    fun sideBySideDiffAlignsChangedAndAddedLines() {
        val diff = computeSideBySideDiff("same\nold\nlast", "same\nnew\nextra\nlast")
        assertEquals(DiffLineKind.UNCHANGED, diff.lines.first().kind)
        val changed = diff.lines.single { it.kind == DiffLineKind.MODIFIED }
        assertEquals("old", changed.oldText)
        assertEquals("new", changed.newText)
        assertTrue(diff.lines.any { it.kind == DiffLineKind.ADDED && it.newText == "extra" })
        assertFalse(diff.truncated)
    }

    @Test
    fun sideBySideDiffCapsLargeInputs() {
        val diff = computeSideBySideDiff((1..300).joinToString("\n"), (1..301).joinToString("\n"))
        assertTrue(diff.truncated)
        assertTrue(diff.lines.size <= 501)
    }
}
