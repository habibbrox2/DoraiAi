package com.dorai.ai.ui.adaptive

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoraiAdaptivePolicyTest {

    @Test
    fun `breakpoint mapping for compact medium expanded large`() {
        assertEquals(DoraiWindowWidthClass.Compact, calculateDoraiWindowWidthClass(390.dp))
        assertEquals(DoraiWindowWidthClass.Compact, calculateDoraiWindowWidthClass(599.dp))

        assertEquals(DoraiWindowWidthClass.Medium, calculateDoraiWindowWidthClass(600.dp))
        assertEquals(DoraiWindowWidthClass.Medium, calculateDoraiWindowWidthClass(768.dp))
        assertEquals(DoraiWindowWidthClass.Medium, calculateDoraiWindowWidthClass(839.dp))

        assertEquals(DoraiWindowWidthClass.Expanded, calculateDoraiWindowWidthClass(840.dp))
        assertEquals(DoraiWindowWidthClass.Expanded, calculateDoraiWindowWidthClass(960.dp))
        assertEquals(DoraiWindowWidthClass.Expanded, calculateDoraiWindowWidthClass(1199.dp))

        assertEquals(DoraiWindowWidthClass.Large, calculateDoraiWindowWidthClass(1200.dp))
        assertEquals(DoraiWindowWidthClass.Large, calculateDoraiWindowWidthClass(1440.dp))
    }

    @Test
    fun `pane width dimensions per width class`() {
        assertEquals(0.dp, DoraiWindowWidthClass.Compact.listPaneWidth)
        assertEquals(250.dp, DoraiWindowWidthClass.Medium.listPaneWidth)
        assertEquals(270.dp, DoraiWindowWidthClass.Expanded.listPaneWidth)
        assertEquals(300.dp, DoraiWindowWidthClass.Large.listPaneWidth)

        assertEquals(0.dp, DoraiWindowWidthClass.Compact.navigationWidth)
        assertEquals(78.dp, DoraiWindowWidthClass.Medium.navigationWidth)
        assertEquals(78.dp, DoraiWindowWidthClass.Expanded.navigationWidth)
        assertEquals(224.dp, DoraiWindowWidthClass.Large.navigationWidth)

        assertEquals(320.dp, DoraiWindowWidthClass.Compact.workspaceAgentPanelWidth)
        assertEquals(320.dp, DoraiWindowWidthClass.Medium.workspaceAgentPanelWidth)
        assertEquals(320.dp, DoraiWindowWidthClass.Expanded.workspaceAgentPanelWidth)
        assertEquals(380.dp, DoraiWindowWidthClass.Large.workspaceAgentPanelWidth)
    }

    @Test
    fun `destinations order and fromId resolution`() {
        assertEquals(
            listOf("home", "chat", "work", "settings"),
            DoraiDestination.entries.map { it.id },
        )
        assertEquals(DoraiDestination.Home, DoraiDestination.fromId("home"))
        assertEquals(DoraiDestination.Chat, DoraiDestination.fromId("chat"))
        assertEquals(DoraiDestination.Workspace, DoraiDestination.fromId("work"))
        assertEquals(DoraiDestination.Settings, DoraiDestination.fromId("settings"))
        assertEquals(DoraiDestination.Home, DoraiDestination.fromId("unknown"))
    }

    @Test
    fun `shell back stack navigates back step by step then to Home`() {
        val state = DoraiAppShellState(initialDestination = DoraiDestination.Home)
        assertEquals(DoraiDestination.Home, state.currentDestination)

        state.navigateTo(DoraiDestination.Chat)
        assertEquals(DoraiDestination.Chat, state.currentDestination)

        state.navigateTo(DoraiDestination.Workspace)
        assertEquals(DoraiDestination.Workspace, state.currentDestination)

        state.navigateTo(DoraiDestination.Settings)
        assertEquals(DoraiDestination.Settings, state.currentDestination)

        // Step 1: back from Settings -> Workspace
        assertTrue(state.handleBack())
        assertEquals(DoraiDestination.Workspace, state.currentDestination)

        // Step 2: back from Workspace -> Chat
        assertTrue(state.handleBack())
        assertEquals(DoraiDestination.Chat, state.currentDestination)

        // Step 3: back from Chat -> Home
        assertTrue(state.handleBack())
        assertEquals(DoraiDestination.Home, state.currentDestination)

        // Step 4: at Home, cannot go back further (allows system exit)
        assertFalse(state.handleBack())
    }

    @Test
    fun `shell back returns to Home if single top-level destination is non-home`() {
        val state = DoraiAppShellState(initialDestination = DoraiDestination.Settings)
        assertEquals(DoraiDestination.Settings, state.currentDestination)

        // Top-level Settings without history returns to Home first
        assertTrue(state.handleBack())
        assertEquals(DoraiDestination.Home, state.currentDestination)

        // At Home, exits
        assertFalse(state.handleBack())
    }
}
