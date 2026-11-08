package com.dorai.ai.ui.markdown

import com.dorai.ai.ui.dynamicui.AlertNode
import com.dorai.ai.ui.dynamicui.ColumnNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DoraiUiBlockIntegrationTest {

    @Test
    fun `dorai-ui fence produces DoraiUiBlock`() {
        val md = """
            ```dorai-ui
            {"type":"alert","title":"Heads up","message":"Hello"}
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is DoraiUiBlock)
        val alert = block.node as AlertNode
        assertEquals("Heads up", alert.title)
        assertEquals("Hello", alert.message)
    }

    @Test
    fun `malformed dorai-ui fence produces DoraiUiError`() {
        val md = """
            ```dorai-ui
            not json at all
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is DoraiUiError)
    }

    @Test
    fun `ndjson multi-line dorai-ui wraps children in a column`() {
        val md = """
            ```dorai-ui
            {"type":"text","value":"a"}
            {"type":"text","value":"b"}
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is DoraiUiBlock)
        val col = block.node as ColumnNode
        assertEquals(2, col.children.size)
    }

    @Test
    fun `dorai-ui block surrounded by markdown produces three blocks`() {
        val md = """
            Before

            ```dorai-ui
            {"type":"alert","message":"hi"}
            ```

            After
        """.trimIndent()
        val blocks = parseMarkdown(md).blocks
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is Paragraph)
        assertTrue(blocks[1] is DoraiUiBlock)
        assertTrue(blocks[2] is Paragraph)
    }

    @Test
    fun `split-block pattern with json fence is treated as dorai-ui`() {
        val md = """
            dorai-ui
            ```json
            {"type":"alert","message":"hi"}
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is DoraiUiBlock)
    }

    @Test
    fun `legacy kai-ui fence produces DoraiUiBlock for backwards compatibility`() {
        val md = """
            ```kai-ui
            {"type":"alert","title":"Legacy","message":"Still works"}
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is DoraiUiBlock)
    }

    @Test
    fun `dorai-ui block speakable text walks the node tree`() {
        val md = """
            Intro.

            ```dorai-ui
            {"type":"alert","title":"Heads up","message":"Take care"}
            ```

            Outro.
        """.trimIndent()
        val spoken = parseMarkdown(md).toSpeakableText()
        assertTrue(spoken.contains("Intro"))
        assertTrue(spoken.contains("Heads up"))
        assertTrue(spoken.contains("Take care"))
        assertTrue(spoken.contains("Outro"))
    }
}
