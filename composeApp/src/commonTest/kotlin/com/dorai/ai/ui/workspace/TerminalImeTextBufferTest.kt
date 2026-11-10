package com.dorai.ai.ui.workspace

import com.dorai.ai.workspace.terminal.TerminalImeTextBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalImeTextBufferTest {

    @Test
    fun emitsOnlyLatestComposingValueWhenCompositionFinishes() {
        val buffer = TerminalImeTextBuffer()

        buffer.updateComposingText("ক")
        buffer.updateComposingText("কি")
        buffer.updateComposingText("ক্ষ")

        assertEquals("ক্ষ", buffer.finishComposingText())
        assertEquals("", buffer.finishComposingText())
    }

    @Test
    fun committedTextReplacesCompositionAndIsNotEmittedTwice() {
        val buffer = TerminalImeTextBuffer()

        buffer.updateComposingText("ক")
        buffer.updateComposingText("কি")

        assertEquals("ক্ষ", buffer.commitText("ক্ষ"))
        assertEquals("", buffer.finishComposingText())
    }

    @Test
    fun preservesBengaliConjunctsDiacriticsAndEmoji() {
        val text = "বাংলা: ক্ষ্ম, ক্ত, শ্র, কি, 👨‍👩‍👧‍👦"
        val buffer = TerminalImeTextBuffer()

        buffer.updateComposingText("বাংলা")
        assertEquals(text, buffer.commitText(text))
    }

    @Test
    fun clearingCompositionDoesNotSendStaleText() {
        val buffer = TerminalImeTextBuffer()

        buffer.updateComposingText("অসম্পূর্ণ")
        buffer.updateComposingText("")

        assertEquals("", buffer.finishComposingText())
    }
}
