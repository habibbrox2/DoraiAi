package com.dorai.ai.ui.adaptive

import com.dorai.ai.ui.settings.SettingsCategory
import com.dorai.ai.ui.workspace.WorkspaceProjectTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoraiUiV2ReachabilityTest {

    @Test
    fun `app shell state initializes at home destination`() {
        val state = DoraiAppShellState(initialDestination = DoraiDestination.Home)
        assertEquals(DoraiDestination.Home, state.currentDestination)
        assertEquals(listOf(DoraiDestination.Home), state.backStack)
    }

    @Test
    fun `app shell state navigates across primary destinations`() {
        val state = DoraiAppShellState(initialDestination = DoraiDestination.Home)

        state.navigateTo(DoraiDestination.Chat)
        assertEquals(DoraiDestination.Chat, state.currentDestination)

        state.navigateTo(DoraiDestination.Workspace)
        assertEquals(DoraiDestination.Workspace, state.currentDestination)

        state.navigateTo(DoraiDestination.Settings)
        assertEquals(DoraiDestination.Settings, state.currentDestination)

        // Back navigation traverses backstack
        assertTrue(state.handleBack())
        assertEquals(DoraiDestination.Workspace, state.currentDestination)

        assertTrue(state.handleBack())
        assertEquals(DoraiDestination.Chat, state.currentDestination)

        assertTrue(state.handleBack())
        assertEquals(DoraiDestination.Home, state.currentDestination)

        // At Home, cannot go back further (exits)
        assertFalse(state.handleBack())
        assertEquals(DoraiDestination.Home, state.currentDestination)
    }

    @Test
    fun `back navigation from non-home single destination returns to home first`() {
        val state = DoraiAppShellState(initialDestination = DoraiDestination.Settings)
        assertEquals(DoraiDestination.Settings, state.currentDestination)

        assertTrue(state.handleBack())
        assertEquals(DoraiDestination.Home, state.currentDestination)

        assertFalse(state.handleBack())
    }

    @Test
    fun `settings categories are strictly 8 ordered categories`() {
        val expected = listOf(
            "general",
            "appearance",
            "agent",
            "providers",
            "tools",
            "mcp",
            "github",
            "account",
        )
        assertEquals(expected, SettingsCategory.entries.map { it.id })
    }

    @Test
    fun `workspace tabs contain editor terminal git`() {
        val expected = listOf("Editor", "Terminal", "Git")
        assertEquals(expected, WorkspaceProjectTab.entries.map { it.name })
    }

    @Test
    fun `chat message item supports rich attachments reasoning and speaking state`() {
        val item = com.dorai.ai.ui.chat.ChatMessageItem(
            id = "test-msg",
            isUser = false,
            text = "Hello world",
            reasoningSegments = listOf("Thinking step 1", "Thinking step 2"),
            isThinking = false,
            isSpeaking = true,
            fallbackServiceName = "Backup Provider",
        )
        assertEquals("test-msg", item.id)
        assertFalse(item.isUser)
        assertEquals(2, item.reasoningSegments.size)
        assertTrue(item.isSpeaking)
        assertEquals("Backup Provider", item.fallbackServiceName)
    }
}
