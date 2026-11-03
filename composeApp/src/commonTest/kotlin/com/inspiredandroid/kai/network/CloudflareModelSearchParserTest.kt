package com.inspiredandroid.kai.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudflareModelSearchParserTest {

    @Test
    fun `parses Cloudflare v4 result envelope and pagination metadata`() {
        val page = parseCloudflareModelSearchPage(
            """
            {
              "success": true,
              "result": [
                {
                  "name": "@cf/meta/llama-3.1-8b-instruct",
                  "description": "Text generation",
                  "source": "Meta"
                },
                {
                  "id": "@cf/mistral/mistral-7b-instruct-v0.2",
                  "task": "Text Generation"
                },
                { "description": "missing identifier" }
              ],
              "result_info": { "page": 1, "per_page": 100, "total_pages": 3 }
            }
            """.trimIndent(),
        )

        assertEquals(2, page.models.size)
        assertEquals("@cf/meta/llama-3.1-8b-instruct", page.models[0].id)
        assertEquals("Meta", page.models[0].owned_by)
        assertEquals("Text generation", page.models[0].description)
        assertEquals("@cf/mistral/mistral-7b-instruct-v0.2", page.models[1].id)
        assertEquals(3, page.totalPages)
    }

    @Test
    fun `rejects unsuccessful Cloudflare envelopes`() {
        val error = assertFailsWith<IllegalArgumentException> {
            parseCloudflareModelSearchPage(
                """{"success":false,"errors":[{"message":"Invalid API token"}],"result":[]}""",
            )
        }
        assertEquals("Invalid API token", error.message)
    }

    @Test
    fun `extracts Cloudflare API error messages from non-success envelopes`() {
        assertEquals(
            "Authentication error: invalid API token",
            parseCloudflareModelSearchError(
                """{"success":false,"errors":[{"code":10000,"message":"Authentication error: invalid API token"}]}""",
            ),
        )
        assertEquals(null, parseCloudflareModelSearchError("""{"success":true,"result":[]}"""))
        assertEquals(null, parseCloudflareModelSearchError("not-json"))
    }

    @Test
    fun `rejects malformed result shape`() {
        assertFailsWith<IllegalArgumentException> {
            parseCloudflareModelSearchPage("""{"success":true,"result":{}}""")
        }
    }

    @Test
    fun `validates Cloudflare account identifiers`() {
        assertTrue(isValidCloudflareAccountId("0123456789abcdef0123456789abcdef"))
        assertFalse(isValidCloudflareAccountId("not-an-account-id"))
        assertFalse(isValidCloudflareAccountId("0123456789abcdef"))
        assertFalse(isValidCloudflareAccountId("0123456789abcdef0123456789abcdef/extra"))
    }
}
