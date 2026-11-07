package com.dorai.ai.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoraiHomeScreenTest {

    @Test
    fun `home ui state defaults and project summary work as expected`() {
        val state = HomeUiState(
            isWorkspaceSetupDone = true,
            activeProject = HomeProjectSummary("DoraiAi", "feature/ui-v2", 3),
            runningCount = 2,
            waitingApprovalCount = 1,
            currentModelName = "Free FAST",
            isModelLocal = false,
            isSandboxRunning = true,
            recentChats = listOf(
                HomeChatSummary("1", "Theme migration", "Updating semantic tokens"),
            ),
        )

        assertTrue(state.isWorkspaceSetupDone)
        assertEquals("DoraiAi", state.activeProject?.name)
        assertEquals(2, state.runningCount)
        assertEquals(1, state.waitingApprovalCount)
        assertEquals(1, state.recentChats.size)
        assertTrue(state.isSandboxRunning)
    }

    @Test
    fun `home ui state setup not done state`() {
        val state = HomeUiState(
            isWorkspaceSetupDone = false,
            activeProject = null,
        )
        assertFalse(state.isWorkspaceSetupDone)
        assertEquals(null, state.activeProject)
    }
}
