package com.dorai.ai.network

import com.dorai.ai.data.Service
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleChatRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenAICompatibleChatStreamTest {
    private fun client(body: String, contentType: ContentType = ContentType.Text.EventStream): HttpClient = HttpClient(
        MockEngine {
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, contentType.toString()),
            )
        },
    ) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                },
            )
        }
        install(HttpTimeout)
    }

    private val messages = listOf(
        OpenAICompatibleChatRequestDto.Message(role = "user", content = JsonPrimitive("hello")),
    )
    private val credentials = ServiceCredentials(apiKey = "test-key", modelId = "test-model")

    @Test
    fun `SSE content fragments are delivered incrementally and finish reason is retained`() = runTest {
        val http = client(
            """
            : keep-alive
            data: {"choices":[{"delta":{"role":"assistant","content":"Hel"}}]}

            data: {"choices":[{"delta":{"content":"lo"}}]}

            data: {"choices":[{"delta":{},"finish_reason":"stop"}]}

            data: [DONE]

            """.trimIndent(),
        )
        try {
            val deltas = mutableListOf<String>()
            val result = Requests(defaultClient = http).openAICompatibleChatStream(
                service = Service.Groq,
                credentials = credentials,
                messages = messages,
                onDelta = { deltas += it },
            ).getOrThrow()

            assertEquals(listOf("Hel", "lo"), deltas)
            assertEquals("Hello", result.content)
            assertEquals("stop", result.finishReason)
        } finally {
            http.close()
        }
    }

    @Test
    fun `reasoning deltas are accumulated separately from visible answer`() = runTest {
        val http = client(
            """
            data: {"choices":[{"delta":{"reasoning_content":"think "}}]}

            data: {"choices":[{"delta":{"reasoning_content":"carefully","content":"Answer"}}]}

            data: [DONE]
            """.trimIndent(),
        )
        try {
            val deltas = mutableListOf<String>()
            val result = Requests(defaultClient = http).openAICompatibleChatStream(
                Service.Groq,
                credentials,
                messages,
                onDelta = { deltas += it },
            ).getOrThrow()

            assertEquals(listOf("Answer"), deltas)
            assertEquals("Answer", result.content)
            assertEquals("think carefully", result.reasoning)
        } finally {
            http.close()
        }
    }

    @Test
    fun `tool call fragments are joined by index without producing visible text`() = runTest {
        val http = client(
            """
            data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1","type":"function","function":{"name":"get_","arguments":"{"}}]}}]}
            data: {"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"name":"weather","arguments":"}"}}]}}]}

            data: {"choices":[{"delta":{},"finish_reason":"tool_calls"}]}

            data: [DONE]
            """.trimIndent(),
        )
        try {
            val deltas = mutableListOf<String>()
            val result = Requests(defaultClient = http).openAICompatibleChatStream(
                Service.Groq,
                credentials,
                messages,
                onDelta = { deltas += it },
            ).getOrThrow()

            assertTrue(deltas.isEmpty())
            val call = result.toolCalls.single()
            assertEquals("call_1", call.id)
            assertEquals("get_weather", call.function.name)
            assertEquals("{}", call.function.arguments)
            assertEquals("tool_calls", result.finishReason)
        } finally {
            http.close()
        }
    }

    @Test
    fun `provider error event fails instead of returning an empty success`() = runTest {
        val http = client("data: {\"error\":{\"message\":\"model overloaded\"}}\n\n")
        try {
            val result = Requests(defaultClient = http).openAICompatibleChatStream(
                Service.Groq,
                credentials,
                messages,
                onDelta = {},
            )
            assertTrue(result.isFailure)
            assertNotNull(result.exceptionOrNull())
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("model overloaded"))
        } finally {
            http.close()
        }
    }

    @Test
    fun nonSseJsonResponseIsDeliveredAsOneCompatibilityDelta() = runTest {
        val http = client(
            """{"choices":[{"message":{"role":"assistant","content":"Buffered answer"}}]}""",
            contentType = ContentType.Application.Json,
        )
        try {
            val deltas = mutableListOf<String>()
            val result = Requests(defaultClient = http).openAICompatibleChatStream(
                Service.Groq,
                credentials,
                messages,
                onDelta = { deltas += it },
            ).getOrThrow()

            assertEquals(listOf("Buffered answer"), deltas)
            val content = assertNotNull(result.content)
            assertEquals("Buffered answer", content)
            assertFalse(content.isEmpty())
        } finally {
            http.close()
        }
    }
}
