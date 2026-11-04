package com.dorai.ai.ui.markdown

import com.dorai.ai.ui.dynamicui.AlertNode
import com.dorai.ai.ui.dynamicui.ColumnNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DoraiUiBlockIntegrationTest {

    @Test
    fun `kai-ui fence produces DoraiUiBlock`() {
        val md = """
            ```kai-ui
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
    fun `malformed kai-ui fence produces DoraiUiError`() {
        val md = """
            ```kai-ui
            not json at all
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is DoraiUiError)
    }

    @Test
    fun `ndjson multi-line kai-ui wraps children in a column`() {
        val md = """
            ```kai-ui
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
    fun `kai-ui block surrounded by markdown produces three blocks`() {
        val md = """
            Before

            ```kai-ui
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
    fun `split-block pattern with json fence is treated as kai-ui`() {
        val md = """
            kai-ui
            ```json
            {"type":"alert","message":"hi"}
            ```
        """.trimIndent()
        val block = parseMarkdown(md).blocks.single()
        assertTrue(block is DoraiUiBlock)
    }

    @Test
    fun `kai-ui block speakable text walks the node tree`() {
        val md = """
            Intro.

            ```kai-ui
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
