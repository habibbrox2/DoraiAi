package com.dorai.ai.workspace

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkspaceGitLogParserTest {

    @Test
    fun `parses git log oneline output`() {
        val raw = "abc123 first subject\ndef456 second subject\n"
        val entries = parseGitLogOneline(raw)
        assertEquals(listOf("abc123", "def456"), entries.map { it.hash })
        assertEquals(listOf("first subject", "second subject"), entries.map { it.subject })
    }

    @Test
    fun `returns empty list for blank input`() {
        assertEquals(emptyList(), parseGitLogOneline(""))
        assertEquals(emptyList(), parseGitLogOneline("   "))
        assertEquals(emptyList(), parseGitLogOneline("\n\n"))
    }

    @Test
    fun `skips malformed lines without a space`() {
        val raw = "abc123 valid subject\nnospace\n"
        val entries = parseGitLogOneline(raw)
        assertEquals(listOf("abc123"), entries.map { it.hash })
    }
}
