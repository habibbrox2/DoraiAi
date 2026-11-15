package com.dorai.ai.cloud

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoraiCloudClientTest {

    @Test
    fun `me returns the profile and sends the bearer token`() = runTest {
        var authorization: String? = null
        var path: String? = null
        val httpClient = HttpClient(
            MockEngine { request ->
                authorization = request.headers["Authorization"]
                path = request.url.encodedPath
                respond(
                    content = """{"user_id":"u-1","email":"a@b.c","plan":"free","usage":{"embedding":3,"memory_write":1,"vault_write":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        val cloud = DoraiCloudClient(httpClient = httpClient, baseUrl = "https://cloud.test")

        val profile = cloud.me("token-abc").getOrThrow()

        assertEquals("u-1", profile.userId)
        assertEquals("free", profile.plan)
        assertEquals(3, profile.usage.embedding)
        assertEquals("Bearer token-abc", authorization)
        assertEquals("/v1/me", path)
        httpClient.close()
    }

    @Test
    fun `upsertMemory sends ciphertext and never plaintext`() = runTest {
        var body: String? = null
        val httpClient = HttpClient(
            MockEngine { request ->
                body = (request.body as io.ktor.http.content.TextContent).text
                respond(
                    content = """{"id":"m-1","key":"k","category":"LEARNINGS","content_ciphertext":"Y3Q=","content_iv":"aXY="}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        val cloud = DoraiCloudClient(httpClient = httpClient, baseUrl = "https://cloud.test")

        val memory = cloud.upsertMemory(
            token = "t",
            key = "k",
            category = "LEARNINGS",
            contentCiphertext = "Y3Q=",
            contentIv = "aXY=",
            embedding = listOf(0.1f, 0.2f),
        ).getOrThrow()

        assertEquals("m-1", memory.id)
        val sent = body.orEmpty()
        assertTrue(sent.contains("\"content_ciphertext\":\"Y3Q=\""), sent)
        assertTrue(sent.contains("\"content_iv\":\"aXY=\""), sent)
        assertFalse(sent.contains("plaintext"), sent)
        httpClient.close()
    }

    @Test
    fun `searchMemories returns the decrypted-envelope rows`() = runTest {
        val httpClient = HttpClient(
            MockEngine {
                respond(
                    content = """{"memories":[{"key":"k","category":"LEARNINGS","content_ciphertext":"Y3Q=","content_iv":"aXY=","similarity":0.87}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        val cloud = DoraiCloudClient(httpClient = httpClient, baseUrl = "https://cloud.test")

        val memories = cloud.searchMemories("t", listOf(0.1f), limit = 3, threshold = 0.4f).getOrThrow()

        assertEquals(1, memories.size)
        assertEquals(0.87f, memories.first().similarity)
        assertEquals("Y3Q=", memories.first().contentCiphertext)
        httpClient.close()
    }

    @Test
    fun `unauthorized responses surface as failures`() = runTest {
        val httpClient = HttpClient(
            MockEngine {
                respond(content = """{"error":"unauthorized"}""", status = HttpStatusCode.Unauthorized)
            },
        )
        val cloud = DoraiCloudClient(httpClient = httpClient, baseUrl = "https://cloud.test")

        val result = cloud.me("bad-token")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("401") == true)
        httpClient.close()
    }

    @Test
    fun `embed returns the vector`() = runTest {
        val httpClient = HttpClient(
            MockEngine {
                respond(
                    content = """{"embedding":[0.5,0.25],"model":"@cf/baai/bge-small-en-v1.5","dimensions":2}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        val cloud = DoraiCloudClient(httpClient = httpClient, baseUrl = "https://cloud.test")

        val embedding = cloud.embed("t", "hello").getOrThrow()

        assertEquals(listOf(0.5f, 0.25f), embedding)
        httpClient.close()
    }
}
