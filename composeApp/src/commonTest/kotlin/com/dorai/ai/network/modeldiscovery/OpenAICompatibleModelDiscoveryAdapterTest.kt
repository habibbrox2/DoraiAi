package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.network.OpenAICompatibleGenericException
import com.dorai.ai.network.OpenAICompatibleInvalidApiKeyException
import com.dorai.ai.network.ServiceCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAICompatibleModelDiscoveryAdapterTest {
    @Test
    fun `sends bearer token when key provided`() = runTest {
        val engine = MockEngine { req ->
            assertEquals("Bearer sk-test", req.headers[HttpHeaders.Authorization])
            respond("""{"data":[{"id":"m1"}]}""", HttpStatusCode.OK)
        }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.Groq, HttpClient(engine))
        val models = adapter.discoverModels(ServiceCredentials(apiKey = "sk-test"))
        assertEquals(listOf("m1"), models.map { it.id })
    }

    @Test
    fun `omits authorization header for optional-key service with blank key`() = runTest {
        val engine = MockEngine { req ->
            assertTrue(req.headers[HttpHeaders.Authorization] == null)
            respond("""{"data":[{"id":"m1"}]}""", HttpStatusCode.OK)
        }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.OpenRouter, HttpClient(engine))
        adapter.discoverModels(ServiceCredentials(apiKey = ""))
    }

    @Test
    fun `parses bare array envelope for responseIsArray services`() = runTest {
        val engine = MockEngine { respond("""[{"id":"t1"}]""", HttpStatusCode.OK) }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.Together, HttpClient(engine), responseIsArray = true)
        assertEquals(listOf("t1"), adapter.discoverModels(ServiceCredentials(apiKey = "k")).map { it.id })
    }

    @Test
    fun `maps 403 to InvalidApiKey when forbiddenMeansInvalidKey`() = runTest {
        val engine = MockEngine { respond("{}", HttpStatusCode.Forbidden) }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.Requesty, HttpClient(engine), forbiddenMeansInvalidKey = true)
        try {
            adapter.discoverModels(ServiceCredentials(apiKey = "bad"))
            throw AssertionError("expected throw")
        } catch (e: OpenAICompatibleInvalidApiKeyException) {
            // expected
        }
    }

    @Test
    fun `empty data array throws NoModels-style error`() = runTest {
        val engine = MockEngine { respond("""{"data":[]}""", HttpStatusCode.OK) }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.Groq, HttpClient(engine))
        try {
            adapter.discoverModels(ServiceCredentials(apiKey = "k"))
            throw AssertionError("expected throw")
        } catch (e: OpenAICompatibleGenericException) {
            // expected
        }
    }

    @Test
    fun `keyProbe runs before discovery when provided`() = runTest {
        var probeCalled = false
        val engine = MockEngine { respond("""{"data":[{"id":"m1"}]}""", HttpStatusCode.OK) }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(
            Service.OpenRouter,
            HttpClient(engine),
            keyProbe = KeyProbe { probeCalled = true },
        )
        adapter.discoverModels(ServiceCredentials(apiKey = "k"))
        assertTrue(probeCalled)
    }
}
