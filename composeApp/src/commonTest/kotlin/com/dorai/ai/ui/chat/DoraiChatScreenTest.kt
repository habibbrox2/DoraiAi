package com.dorai.ai.ui.chat

import com.dorai.ai.ui.components.v2.ToolExecutionModel
import com.dorai.ai.ui.components.v2.ToolExecutionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoraiChatScreenTest {

    @Test
    fun `chat message model binds user and ai messages correctly`() {
        val userMsg = ChatMessageItem(
            id = "1",
            isUser = true,
            text = "Hello DORAI",
        )
        assertTrue(userMsg.isUser)
        assertEquals("Hello DORAI", userMsg.text)

        val aiMsg = ChatMessageItem(
            id = "2",
            isUser = false,
            text = "Hello! How can I assist you with your project today?",
            modelName = "Free EXPERT",
            isModelLocal = false,
            toolExecutions = listOf(
                ToolExecutionModel("search", "knowledge base", ToolExecutionState.Completed),
            ),
        )
        assertFalse(aiMsg.isUser)
        assertEquals(1, aiMsg.toolExecutions.size)
        assertEquals("Free EXPERT", aiMsg.modelName)
    }

    @Test
    fun `chat conversation summary group handles today and yesterday`() {
        val convToday = ChatConversationSummary("1", "Setup project", "Start by configuring gradle", isToday = true)
        val convOlder = ChatConversationSummary("2", "Review diff", "Checked tokens contrast", isToday = false)

        assertTrue(convToday.isToday)
        assertFalse(convOlder.isToday)
    }
}
