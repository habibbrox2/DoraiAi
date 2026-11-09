package com.dorai.ai.network

import kotlin.test.Test
import kotlin.test.assertTrue

class HttpLogSanitizerTest {

    @Test
    fun `authorization headers are redacted`() {
        val logged = "--> GET https://api.example.com/v1/chat/completions\n" +
            "authorization: Bearer sk-live-abcdef1234567890\n" +
            "content-type: application/json"
        val sanitized = HttpLogSanitizer.sanitize(logged)
        assertTrue(!sanitized.contains("sk-live-abcdef1234567890"), "token leaked: $sanitized")
        assertTrue(sanitized.contains("authorization: [REDACTED]"), "header shape lost: $sanitized")
        assertTrue(sanitized.contains("content-type: application/json"), "non-sensitive header lost: $sanitized")
    }

    @Test
    fun `provider api key header variants are redacted`() {
        listOf(
            "x-goog-api-key: AIzaSyD-1234567890abcdef",
            "x-api-key: sk-ant-9876543210",
            "api-key: k1234567890abcdef",
            "cookie: session=supersecretvalue123",
            "set-cookie: sid=abc123; HttpOnly",
        ).forEach { line ->
            val sanitized = HttpLogSanitizer.sanitize(line)
            assertTrue(!sanitized.contains("supersecretvalue123") && !sanitized.contains("AIzaSyD") && !sanitized.contains("sk-ant"), line)
        }
    }

    @Test
    fun `inline bearer tokens outside headers are redacted`() {
        val sanitized = HttpLogSanitizer.sanitize("error: unauthorized for token Bearer sk-abc1234567890xyz")
        assertTrue(!sanitized.contains("sk-abc1234567890xyz"), "inline token leaked: $sanitized")
        assertTrue(sanitized.contains("Bearer [REDACTED]"), "marker missing: $sanitized")
    }

    @Test
    fun `normal log lines pass through untouched`() {
        val line = "--> POST https://api.openai.com/v1/chat/completions (1024-byte body)"
        assertTrue(HttpLogSanitizer.sanitize(line) == line)
    }
}
