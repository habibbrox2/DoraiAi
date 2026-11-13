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

    // ---------- grapheme-aware delete (IME deleteSurroundingText path) ----------

    @Test
    fun backspaceCoversWholeBengaliClusterInsteadOfCodeUnits() {
        val buffer = TerminalImeTextBuffer()
        // ক + ্ + ষ is one grapheme cluster but three UTF-16 code units.
        buffer.commitText("ক্ষ")

        // An IME asking to delete one code unit must still remove the whole cluster.
        assertEquals(1, buffer.backspacesFor(1))
        // Asking for the whole cluster's code-unit length is still one cluster.
        assertEquals(1, buffer.backspacesFor(3))
    }

    @Test
    fun backspaceCountsClustersForMixedBengaliAndLatinText() {
        val buffer = TerminalImeTextBuffer()
        buffer.commitText("ক্ষাa")

        // The trailing Latin "a" is one cluster; the cluster before it is "ক্ষা".
        assertEquals(1, buffer.backspacesFor(1))
        assertEquals(2, buffer.backspacesFor(4))
    }

    @Test
    fun backspaceKeepsZwjEmojiSequenceIntact() {
        val buffer = TerminalImeTextBuffer()
        // 👩‍💻 is one cluster (surrogate pairs + ZWJ).
        buffer.commitText("👩‍💻")

        assertEquals(1, buffer.backspacesFor(1))
        assertEquals(1, buffer.backspacesFor(5))
    }

    @Test
    fun droppingClustersRemovesThemFromTheTrackedTail() {
        val buffer = TerminalImeTextBuffer()
        // "ক্ষা" is a single cluster (ক + ্ + ষ + া) and "b" is a second one.
        buffer.commitText("ক্ষাb")

        buffer.dropTailClusters(1)

        // Only the Bengali cluster is tracked now, so one backspace still covers it whole.
        assertEquals(1, buffer.backspacesFor(1))
        assertEquals(1, buffer.backspacesFor(3))
    }

    @Test
    fun droppingMoreClustersThanTrackedClearsTheTail() {
        val buffer = TerminalImeTextBuffer()
        buffer.commitText("ক্ষা")

        buffer.dropTailClusters(5)

        // Nothing is tracked any more: the raw requested length is used as a safe fallback.
        assertEquals(3, buffer.backspacesFor(3))
    }

    @Test
    fun backspaceFallsBackToRawLengthWhenNothingWasCommitted() {
        val buffer = TerminalImeTextBuffer()

        // Nothing tracked: fall back to the requested length rather than dropping the delete.
        assertEquals(4, buffer.backspacesFor(4))
        assertEquals(0, buffer.backspacesFor(0))
        assertEquals(0, buffer.backspacesFor(-1))
    }
}
