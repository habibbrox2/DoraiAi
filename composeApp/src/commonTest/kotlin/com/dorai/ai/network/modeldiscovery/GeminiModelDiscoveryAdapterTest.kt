package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.network.GeminiInvalidApiKeyException
import com.dorai.ai.network.ServiceCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeminiModelDiscoveryAdapterTest {
    @Test
    fun `uses x-goog-api-key header and strips models prefix`() = runTest {
        val engine = MockEngine { req ->
            assertEquals("k", req.headers["x-goog-api-key"])
            respond(
                """{"models":[{"name":"models/gemini-2.0-flash","displayName":"Gemini 2.0 Flash","inputTokenLimit":1048576,"supportedGenerationMethods":["generateContent"]}]}""",
                HttpStatusCode.OK,
            )
        }
        val adapter = GeminiModelDiscoveryAdapter(HttpClient(engine))
        val models = adapter.discoverModels(ServiceCredentials(apiKey = "k"))
        assertEquals(listOf("gemini-2.0-flash"), models.map { it.id })
        assertEquals(1048576L, models.first().contextWindow)
    }

    @Test
    fun `filters models without generateContent`() = runTest {
        val engine = MockEngine {
            respond(
                """{"models":[{"name":"models/embedding-001","supportedGenerationMethods":["embedContent"]}]}""",
                HttpStatusCode.OK,
            )
        }
        val adapter = GeminiModelDiscoveryAdapter(HttpClient(engine))
        // An embedding-only model (no generateContent) must be filtered out; the adapter
        // returns the filtered list (may be empty) and the repository decides how to surface it.
        assertTrue(adapter.discoverModels(ServiceCredentials(apiKey = "k")).isEmpty())
    }

    @Test
    fun `bad key maps to GeminiInvalidApiKeyException`() = runTest {
        val engine = MockEngine { respond("{}", HttpStatusCode.Forbidden) }
        val adapter = GeminiModelDiscoveryAdapter(HttpClient(engine))
        try {
            adapter.discoverModels(ServiceCredentials(apiKey = "bad"))
            throw AssertionError("expected throw")
        } catch (e: GeminiInvalidApiKeyException) {
            // expected
        }
    }
}
