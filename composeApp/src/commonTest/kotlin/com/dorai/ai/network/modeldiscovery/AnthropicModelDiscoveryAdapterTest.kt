package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.network.AnthropicInvalidApiKeyException
import com.dorai.ai.network.ServiceCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnthropicModelDiscoveryAdapterTest {
    @Test
    fun `sends x-api-key and anthropic-version headers`() = runTest {
        val engine = MockEngine { req ->
            assertEquals("k", req.headers["x-api-key"])
            assertEquals("2023-06-01", req.headers["anthropic-version"])
            respond("""{"data":[{"id":"claude-sonnet-4-5","display_name":"Claude Sonnet 4.5"}]}""", HttpStatusCode.OK)
        }
        val adapter = AnthropicModelDiscoveryAdapter(HttpClient(engine))
        val models = adapter.discoverModels(ServiceCredentials(apiKey = "k"))
        assertEquals(listOf("claude-sonnet-4-5"), models.map { it.id })
        assertEquals("Claude Sonnet 4.5", models.first().displayName)
    }

    @Test
    fun `bad key maps to AnthropicInvalidApiKeyException`() = runTest {
        val engine = MockEngine { respond("{}", HttpStatusCode.Unauthorized) }
        val adapter = AnthropicModelDiscoveryAdapter(HttpClient(engine))
        try {
            adapter.discoverModels(ServiceCredentials(apiKey = "bad"))
            throw AssertionError("expected throw")
        } catch (e: AnthropicInvalidApiKeyException) {
            // expected
        }
    }
}
