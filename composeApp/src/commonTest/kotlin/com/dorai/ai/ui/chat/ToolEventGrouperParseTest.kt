package com.dorai.ai.ui.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToolEventGrouperParseTest {

    @Test
    fun `stdout with escaped quotes and newlines is decoded`() {
        val content = """{"stdout":"line1\nline \"quoted\" end","exit_code":0,"success":true}"""
        val parsed = parseToolExecutionOutput(content)
        assertEquals("line1\nline \"quoted\" end", parsed.stdout)
        assertFalse(parsed.isFailure)
    }

    @Test
    fun `unicode escapes are decoded including Bengali`() {
        val content = """{"stdout":"\u0986\u09AE\u09BF","exit_code":0,"success":true}"""
        val parsed = parseToolExecutionOutput(content)
        assertEquals("আমি", parsed.stdout)
    }

    @Test
    fun `success false with spaces is detected`() {
        val content = """{"success": false,"stdout":"partial","exit_code": 1}"""
        val parsed = parseToolExecutionOutput(content)
        assertTrue(parsed.isFailure)
        assertEquals(1, parsed.exitCode)
        // No error/stderr fields: the whole payload is surfaced as the failure detail (matches legacy behavior).
        assertEquals(content, parsed.stderr)
    }

    @Test
    fun `error field surfaces as failure`() {
        val content = """{"stdout":"","error":"permission denied"}"""
        val parsed = parseToolExecutionOutput(content)
        assertTrue(parsed.isFailure)
        assertEquals("permission denied", parsed.stderr)
    }

    @Test
    fun `nested JSON in stdout is not truncated`() {
        val content = """{"stdout":"{\"a\":\"b\"}","exit_code":0}"""
        val parsed = parseToolExecutionOutput(content)
        assertEquals("""{"a":"b"}""", parsed.stdout)
    }

    @Test
    fun `non-JSON content falls back to raw stdout`() {
        val parsed = parseToolExecutionOutput("just plain output")
        assertEquals("just plain output", parsed.stdout)
        assertFalse(parsed.isFailure)
    }

    @Test
    fun `non-JSON failure content goes to stderr`() {
        val parsed = parseToolExecutionOutput("tool_error: boom")
        assertTrue(parsed.isFailure)
        assertEquals("tool_error: boom", parsed.stderr)
        assertNull(parsed.stdout)
    }

    @Test
    fun `exitCode field name wins over exit_code`() {
        val parsed = parseToolExecutionOutput("""{"exitCode": 2}""")
        assertEquals(2, parsed.exitCode)
        assertTrue(parsed.isFailure)
    }

    @Test
    fun `command text is extracted from JSON args`() {
        assertEquals("ls -la /tmp", parseToolCommandText("""{"command":"ls -la /tmp","timeout":1000}"""))
        assertEquals("grep \"a b\" file.txt", parseToolCommandText("""{"command":"grep \"a b\" file.txt"}"""))
        assertEquals("not json at all", parseToolCommandText("not json at all"))
    }
}
