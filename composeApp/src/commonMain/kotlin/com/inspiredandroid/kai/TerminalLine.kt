@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package com.inspiredandroid.kai

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.concurrent.atomics.AtomicLong

/**
 * Unique id source for terminal lines. The transcript is rendered with
 * `key = { it.id }` in a LazyColumn, so every line needs a distinct id — a
 * shared default (0) would make all keys collide. Process output can arrive
 * from several threads, hence an atomic counter.
 */
private val terminalLineIdCounter = AtomicLong(0L)

private fun nextTerminalLineId(): Long {
    while (true) {
        val current = terminalLineIdCounter.load()
        if (terminalLineIdCounter.compareAndSet(current, current + 1)) return current + 1
    }
}

@Immutable
@Serializable
sealed interface TerminalLine {
    val text: String
    val id: Long

    fun withText(newText: String): TerminalLine = when (this) {
        is Command -> Command(newText, id)
        is Output -> Output(newText, id)
        is Error -> Error(newText, id)
    }

    @Serializable
    @SerialName("command")
    data class Command(override val text: String, override val id: Long = nextTerminalLineId()) : TerminalLine

    @Serializable
    @SerialName("output")
    data class Output(override val text: String, override val id: Long = nextTerminalLineId()) : TerminalLine

    @Serializable
    @SerialName("error")
    data class Error(override val text: String, override val id: Long = nextTerminalLineId()) : TerminalLine
}
