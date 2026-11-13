package com.dorai.ai.workspace.terminal

import com.dorai.ai.text.graphemeClusterCount
import com.dorai.ai.text.graphemeSafeDeleteCount
import com.dorai.ai.text.graphemeSafeTake

/**
 * Buffers IME composing updates until the keyboard commits them.
 *
 * Terminals cannot expose an editable composing region, so only committed
 * text is sent to the PTY. Some IMEs finish a composition without a final
 * commitText call; finishComposingText() flushes that last composed value.
 */
internal class TerminalImeTextBuffer {
    private val boundedTail = 128
    private var composingText: String = ""
    private var committedTail: String = ""

    fun updateComposingText(text: CharSequence?) {
        composingText = text?.toString().orEmpty()
    }

    /**
     * Best-effort rolling copy of the most recently committed text. The PTY owns the real
     * line buffer, so this can drift when the user edits with control keys; it exists only
     * so IME-initiated deletes don't split a grapheme cluster. Callers must treat the value
     * as advisory.
     */
    fun commitText(text: CharSequence?): String {
        composingText = ""
        return text?.toString().orEmpty().also { rememberCommitted(it) }
    }

    fun finishComposingText(): String {
        val committed = composingText
        composingText = ""
        return committed.also { rememberCommitted(it) }
    }

    /**
     * Number of leading backspaces the PTY should receive to cover [beforeLength] UTF-16
     * code units without splitting a grapheme cluster. Falls back to [beforeLength] when
     * the tracked tail is too short to analyse.
     */
    fun backspacesFor(beforeLength: Int): Int {
        if (beforeLength <= 0) return 0
        if (committedTail.isEmpty()) return beforeLength
        return graphemeSafeDeleteCount(committedTail, beforeLength)
    }

    /** Drop the final [count] grapheme clusters (or everything, if fewer are tracked) from the tail. */
    fun dropTailClusters(count: Int) {
        if (count <= 0 || committedTail.isEmpty()) return
        val total = graphemeClusterCount(committedTail)
        committedTail = graphemeSafeTake(committedTail, (total - count).coerceAtLeast(0))
    }

    private fun rememberCommitted(text: String) {
        if (text.isEmpty()) return
        committedTail = (committedTail + text).takeLast(boundedTail)
    }
}
