package com.inspiredandroid.kai.network

import com.inspiredandroid.kai.data.Service
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudflareModelSearchIntegrationTest {

    private val accountId = "0123456789abcdef0123456789abcdef"

    @Test
    fun `fetches all Cloudflare model pages with account scoped URL and bearer token`() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                assertEquals("Bearer test-cloudflare-token", request.headers[HttpHeaders.Authorization])
                when (request.url.parameters["page"]) {
                    "1" -> respond(
                        content = """
                            {
                              "success": true,
                              "result": [{"name": "@cf/meta/llama-3.1-8b-instruct"}],
                              "result_info": {"page": 1, "per_page": 100, "total_pages": 2}
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                    )

                    "2" -> respond(
                        content = """
                            {
                              "success": true,
                              "result": [{"name": "@cf/mistral/mistral-7b-instruct-v0.2"}],
                              "result_info": {"page": 2, "per_page": 100, "total_pages": 2}
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                    )

                    else -> error("Unexpected Cloudflare model search page: ${request.url}")
                }
            },
        )

        try {
            val result = Requests(client).getOpenAICompatibleModels(
                Service.Cloudflare,
                ServiceCredentials(
                    apiKey = "test-cloudflare-token",
                    baseUrl = accountId,
                ),
            )

            assertTrue(result.isSuccess, result.exceptionOrNull()?.message ?: "Cloudflare model discovery failed")
            assertEquals(
                listOf("@cf/meta/llama-3.1-8b-instruct", "@cf/mistral/mistral-7b-instruct-v0.2"),
                result.getOrThrow().data.map { it.id },
            )
            assertEquals(2, requestedUrls.size)
            assertTrue(
                requestedUrls.all {
                    it.startsWith("https://api.cloudflare.com/client/v4/accounts/$accountId/ai/models/search?")
                },
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun `surfaces Cloudflare API error details for failed model discovery`() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"success":false,"errors":[{"code":10000,"message":"API token lacks Workers AI read permission"}]}""",
                    status = HttpStatusCode.Forbidden,
                )
            },
        )

        try {
            val result = Requests(client).getOpenAICompatibleModels(
                Service.Cloudflare,
                ServiceCredentials(
                    apiKey = "test-cloudflare-token",
                    baseUrl = accountId,
                ),
            )

            assertTrue(result.isFailure)
            assertContains(
                result.exceptionOrNull()?.message.orEmpty(),
                "API token lacks Workers AI read permission",
            )
        } finally {
            client.close()
        }
    }
}
