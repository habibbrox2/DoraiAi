package com.dorai.ai.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CloudflareEmbeddingTest {

    @Test
    fun `parses 2D array embeddings successfully`() {
        val json = """
            {
              "result": {
                "shape": [2, 3],
                "data": [
                  [0.12, -0.34, 0.56],
                  [0.78, 0.90, -0.11]
                ]
              },
              "success": true,
              "errors": [],
              "messages": []
            }
        """.trimIndent()

        val parsed = parseCloudflareEmbeddingResponse(json)
        assertEquals(2, parsed.size)
        assertEquals(listOf(0.12f, -0.34f, 0.56f), parsed[0])
        assertEquals(listOf(0.78f, 0.90f, -0.11f), parsed[1])
    }

    @Test
    fun `parses single 1D vector embeddings as 1-element list`() {
        val json = """
            {
              "result": {
                "shape": [3],
                "data": [0.42, -0.18, 0.99]
              },
              "success": true,
              "errors": []
            }
        """.trimIndent()

        val parsed = parseCloudflareEmbeddingResponse(json)
        assertEquals(1, parsed.size)
        assertEquals(listOf(0.42f, -0.18f, 0.99f), parsed[0])
    }

    @Test
    fun `throws error on unsuccessful Cloudflare response`() {
        val json = """
            {
              "success": false,
              "errors": [
                {
                  "code": 1000,
                  "message": "Model not found or rate limit exceeded"
                }
              ],
              "result": null
            }
        """.trimIndent()

        val error = assertFailsWith<IllegalArgumentException> {
            parseCloudflareEmbeddingResponse(json)
        }
        assertTrue(error.message?.contains("Model not found") == true)
    }

    @Test
    fun `handles empty data array gracefully`() {
        val json = """
            {
              "result": {
                "shape": [0],
                "data": []
              },
              "success": true,
              "errors": []
            }
        """.trimIndent()

        val parsed = parseCloudflareEmbeddingResponse(json)
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun `validates Cloudflare account ID format`() {
        val valid = "0123456789abcdef0123456789abcdef"
        val invalidShort = "0123456789abcdef"
        val invalidChars = "0123456789abcdef0123456789abcdeg" // 'g' is invalid hex

        assertTrue(isValidCloudflareAccountId(valid))
        assertTrue(!isValidCloudflareAccountId(invalidShort))
        assertTrue(!isValidCloudflareAccountId(invalidChars))
    }
}
