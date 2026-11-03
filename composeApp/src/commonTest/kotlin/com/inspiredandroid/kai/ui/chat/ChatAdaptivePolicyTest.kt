package com.inspiredandroid.kai.ui.chat

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatAdaptivePolicyTest {

    @Test
    fun `expanded layout keeps the rail when no side panel is open`() {
        assertFalse(ChatAdaptivePolicy.useExpandedLayout(839.dp, hasSidePanel = false))
        assertTrue(ChatAdaptivePolicy.useExpandedLayout(840.dp, hasSidePanel = false))
    }

    @Test
    fun `side panel requires enough width for rail panel and usable chat content`() {
        assertFalse(ChatAdaptivePolicy.useExpandedLayout(1119.dp, hasSidePanel = true))
        assertTrue(ChatAdaptivePolicy.useExpandedLayout(1120.dp, hasSidePanel = true))
        assertTrue(ChatAdaptivePolicy.useExpandedLayout(1280.dp, hasSidePanel = true))
    }

    @Test
    fun `expanded side panel threshold matches reserved widths`() {
        assertEquals(1120.dp, ChatAdaptivePolicy.expandedSidePanelMinWidth)
    }
}
