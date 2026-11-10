package com.dorai.ai.workspace.terminal

/**
 * Buffers IME composing updates until the keyboard commits them.
 *
 * Terminals cannot expose an editable composing region, so only committed
 * text is sent to the PTY. Some IMEs finish a composition without a final
 * commitText call; finishComposingText() flushes that last composed value.
 */
internal class TerminalImeTextBuffer {
    private var composingText: String = ""

    fun updateComposingText(text: CharSequence?) {
        composingText = text?.toString().orEmpty()
    }

    fun commitText(text: CharSequence?): String {
        composingText = ""
        return text?.toString().orEmpty()
    }

    fun finishComposingText(): String {
        val committed = composingText
        composingText = ""
        return committed
    }
}
