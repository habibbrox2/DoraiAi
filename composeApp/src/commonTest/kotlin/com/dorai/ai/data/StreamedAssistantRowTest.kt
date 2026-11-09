package com.dorai.ai.data

import com.dorai.ai.ui.chat.History
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreamedAssistantRowTest {

    private fun historyFlow() = MutableStateFlow<List<History>>(emptyList())

    @Test
    fun `deltas grow a single assistant row in place`() {
        val history = historyFlow()
        val stream = StreamedAssistantRow(history)

        stream.append("Hel")
        stream.append("lo")
        stream.flush()

        val entryId = assertNotNull(stream.takeEntryId())
        val row = history.value.single()
        assertEquals(entryId, row.id)
        assertEquals(History.Role.ASSISTANT, row.role)
        assertEquals("Hello", row.content)
    }

    @Test
    fun `row keeps its identity while content grows`() {
        val history = historyFlow()
        val stream = StreamedAssistantRow(history)

        stream.append("first")
        stream.flush()
        val firstId = history.value.single().id

        stream.append(" second")
        stream.flush()
        assertEquals(1, history.value.size)
        assertEquals(firstId, history.value.single().id)
        assertEquals("first second", history.value.single().content)
    }

    @Test
    fun `a delta after takeEntryId starts a new row`() {
        val history = historyFlow()
        val stream = StreamedAssistantRow(history)

        stream.append("answer")
        assertNotNull(stream.takeEntryId())
        stream.append("next round")

        assertEquals(2, history.value.size)
        assertEquals("next round", history.value.last().content)
    }

    @Test
    fun `discard removes the partial row and its pending text`() {
        val history = historyFlow()
        val stream = StreamedAssistantRow(history)

        stream.append("partial")
        stream.append(" tail")
        stream.discard()

        assertTrue(history.value.isEmpty())
        assertNull(stream.takeEntryId())
    }

    @Test
    fun `no deltas means no row and no entry id`() {
        val history = historyFlow()
        val stream = StreamedAssistantRow(history)

        assertNull(stream.takeEntryId())
        assertTrue(history.value.isEmpty())
    }
}
