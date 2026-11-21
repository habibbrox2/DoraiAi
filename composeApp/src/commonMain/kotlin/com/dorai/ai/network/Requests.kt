@file:OptIn(ExperimentalUuidApi::class)

package com.dorai.ai.network

import com.dorai.ai.Version
import com.dorai.ai.currentPlatform
import com.dorai.ai.data.Service
import com.dorai.ai.httpClient
import com.dorai.ai.isDebugBuild
import com.dorai.ai.network.dtos.anthropic.AnthropicChatRequestDto
import com.dorai.ai.network.dtos.anthropic.AnthropicChatResponseDto
import com.dorai.ai.network.dtos.gemini.FunctionDeclaration
import com.dorai.ai.network.dtos.gemini.FunctionParameters
import com.dorai.ai.network.dtos.gemini.GeminiChatRequestDto
import com.dorai.ai.network.dtos.gemini.GeminiChatResponseDto
import com.dorai.ai.network.dtos.gemini.GeminiTool
import com.dorai.ai.network.dtos.gemini.PropertySchema
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleChatChunkDto
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleChatRequestDto
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleChatResponseDto
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleCompletion
import com.dorai.ai.network.dtos.openairesponses.OpenAIResponsesRequestDto
import com.dorai.ai.network.dtos.openairesponses.OpenAIResponsesResponseDto
import com.dorai.ai.network.modeldiscovery.DiscoveryHttp
import com.dorai.ai.network.tools.Tool
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ServiceCredentials(
    val apiKey: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    /** Cloudflare AI Gateway id for Service.CloudflareAIGateway — blank means the account's "default" gateway. */
    val cloudflareGatewayId: String = "",
)

/** Applies a per-request override of the client-wide timeouts, when the caller supplied one. */
private fun HttpRequestBuilder.applyTimeout(requestTimeoutMs: Long?) {
    requestTimeoutMs ?: return
    timeout {
        requestTimeoutMillis = requestTimeoutMs
        socketTimeoutMillis = requestTimeoutMs
        connectTimeoutMillis = 30.seconds.inWholeMilliseconds
    }
}

/**
 * Session id for provider requests that belong to no conversation — the model list, and the
 * silent asks that run before a chat exists. Stable for the lifetime of the process.
 */
private val processSessionId: String by lazy { Uuid.random().toString() }

/**
 * Streaming needs an overall deadline long enough for a slow model to finish, while the socket
 * (idle) timeout stays short so a stalled connection fails instead of hanging the turn forever.
 */
private const val STREAM_REQUEST_TIMEOUT_MS = 15 * 60 * 1000L
private const val STREAM_SOCKET_TIMEOUT_MS = 120_000L

/** Tolerant parser for SSE payloads: providers add fields the DTO does not model. */
private val streamingJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * OpenCode identifies the calling client by an `x-opencode-session` header and rejects
 * requests that omit it. One id per conversation, so a whole chat reads as a single session
 * upstream; [sessionId] is the conversation id, or null for requests outside any conversation.
 * An OpenAI-Compatible instance pointed at `opencode.ai` (e.g. the OpenCode Go gateway) gets the
 * header too. No other provider is sent a session header.
 */
internal fun sessionHeadersFor(service: Service, sessionId: String?, baseUrl: String = ""): Map<String, String> = if (
    service == Service.OpenCode ||
    (service == Service.OpenAICompatible && baseUrl.contains("opencode.ai", ignoreCase = true))
) {
    mapOf("x-opencode-session" to (sessionId?.takeIf { it.isNotBlank() } ?: processSessionId))
} else {
    emptyMap()
}

/**
 * 413 is also how token-per-minute caps reject an oversized prompt (Groq's free tier answers
 * "Request too large for model … on tokens per minute"), so the provider's own message is shown
 * unless it is missing or actually about an image.
 */
internal fun requestTooLargeException(service: Service, detail: String?): OpenAICompatibleApiException = if (detail.isNullOrBlank() || detail.contains("image", ignoreCase = true)) {
    OpenAICompatibleRequestTooLargeException()
} else {
    OpenAICompatibleGenericException("${service.displayName}: $detail")
}

private fun HttpRequestBuilder.applySessionHeader(service: Service, credentials: ServiceCredentials, sessionId: String?) {
    DiscoveryHttp.sessionHeaders(service, credentials, sessionId).forEach { (k, v) -> header(k, v) }
}

/**
 * Parses a `Retry-After` delta-seconds value (capped at 5 minutes). HTTP-date values are not
 * parsed in common code — callers fall back to their normal backoff in that case.
 */
internal fun parseRetryAfterSeconds(value: String?): Long? = value?.trim()?.toLongOrNull()?.coerceIn(0, 300)

/** Gateway header for Service.CloudflareAIGateway — blank gateway id means the account's auto-created "default" gateway. */
private fun HttpRequestBuilder.applyCloudflareGatewayHeader(service: Service, credentials: ServiceCredentials) {
    DiscoveryHttp.cloudflareGatewayHeader(service, credentials)?.let { (k, v) -> header(k, v) }
}

/**
 * Converts declared tools into a provider's request shape, dropping any whose schema can't be
 * represented rather than failing the whole request, and returning null for an empty list so the
 * field is omitted from the payload entirely.
 */
private fun <T> List<Tool>.toRequestTools(convert: (Tool) -> T): List<T>? = mapNotNull { runCatching { convert(it) }.getOrNull() }.ifEmpty { null }

/**
 * Shared failure mapping for the OpenAI-compatible endpoints that have no bespoke error handling:
 * API exceptions propagate as-is, anything else (I/O, serialization) becomes a connection error.
 * Kept separate from [Requests.openAICompatibleChat], which additionally distinguishes timeouts.
 * Shared with [com.dorai.ai.network.modeldiscovery.DiscoveryHttp]'s key-validation probes.
 */
internal inline fun <T> openAICompatibleResult(block: () -> Result<T>): Result<T> = try {
    block()
} catch (e: OpenAICompatibleApiException) {
    Result.failure(e)
} catch (e: CancellationException) {
    // Stop/timeout must cancel the caller, not surface as a connection or API error.
    throw e
} catch (e: Exception) {
    Result.failure(OpenAICompatibleConnectionException())
}

class Requests(
    private val defaultClient: HttpClient = httpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                },
            )
        }
        install(UserAgent) {
            agent = "DORAI-AI/${Version.appVersion} (${currentPlatform.displayName})"
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 180.seconds.inWholeMilliseconds
            socketTimeoutMillis = 180.seconds.inWholeMilliseconds
            connectTimeoutMillis = 30.seconds.inWholeMilliseconds
        }
        install(Logging) {
            if (isDebugBuild) {
                logger = DebugKtorLogger()
                level = LogLevel.BODY
            } else {
                logger = Logger.EMPTY
                level = LogLevel.NONE
            }
        }
    },
) {

    class DebugKtorLogger : Logger {
        override fun log(message: String) {
            // Redact credential headers before they reach logcat/stdout. Debug builds log
            // bodies too, but provider credentials travel in headers, never in bodies.
            println("[KTOR] ${HttpLogSanitizer.sanitize(message)}")
        }
    }

    // region Gemini

    suspend fun geminiChat(
        credentials: ServiceCredentials,
        messages: List<GeminiChatRequestDto.Content>,
        tools: List<Tool> = emptyList(),
        systemInstruction: String? = null,
        requestTimeoutMs: Long? = null,
    ): Result<GeminiChatResponseDto> = try {
        val apiKey = credentials.apiKey.ifEmpty { throw GeminiInvalidApiKeyException() }
        val selectedModelId = credentials.modelId.trim()
            .removePrefix("models/")
            .removeSuffix(":free")

        val systemContent = systemInstruction?.let {
            GeminiChatRequestDto.Content(
                parts = listOf(GeminiChatRequestDto.Part(text = it)),
            )
        }

        val response: HttpResponse =
            defaultClient.post("${Service.Gemini.chatUrl}$selectedModelId:generateContent") {
                header("x-goog-api-key", apiKey)
                applyTimeout(requestTimeoutMs)
                contentType(ContentType.Application.Json)
                setBody(
                    GeminiChatRequestDto(
                        contents = messages,
                        tools = tools.toRequestTools { it.toGeminiTool() },
                        systemInstruction = systemContent,
                    ),
                )
            }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            when (response.status.value) {
                404 -> throw GeminiModelNotFoundException(selectedModelId)

                429 -> throw GeminiRateLimitExceededException(parseRetryAfterSeconds(response.headers["Retry-After"]))

                403 -> throw GeminiInvalidApiKeyException()

                else -> {
                    val responseBody = response.bodyAsText()
                    if (responseBody.contains("API_KEY_INVALID", ignoreCase = true)) {
                        throw GeminiInvalidApiKeyException()
                    } else {
                        throw GeminiGenericException("Chat request failed: ${response.status}")
                    }
                }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // endregion

    // region OpenAI-compatible (unified)

    suspend fun openAICompatibleChat(
        service: Service,
        credentials: ServiceCredentials,
        messages: List<OpenAICompatibleChatRequestDto.Message>,
        tools: List<Tool> = emptyList(),
        customHeaders: Map<String, String> = emptyMap(),
        sessionId: String? = null,
        requestTimeoutMs: Long? = null,
    ): Result<OpenAICompatibleChatResponseDto> = try {
        val apiKey = DiscoveryHttp.apiKeyFor(service, credentials, required = true)
        val model = credentials.modelId.trim().let { id ->
            if (service == Service.Nvidia) id.removeSuffix(":free") else id
        }.ifEmpty { null }
        val url = DiscoveryHttp.resolveUrl(service, credentials, service.chatUrl)
        val response: HttpResponse =
            defaultClient.post(url) {
                applyTimeout(requestTimeoutMs)
                contentType(ContentType.Application.Json)
                apiKey?.let { bearerAuth(it) }
                applySessionHeader(service, credentials, sessionId)
                customHeaders.forEach { (k, v) -> header(k, v) }
                applyCloudflareGatewayHeader(service, credentials)
                setBody(
                    OpenAICompatibleChatRequestDto(
                        messages = messages,
                        model = model,
                        tools = tools.toRequestTools { it.toRequestTool() },
                    ),
                )
            }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            DiscoveryHttp.mapOpenAICompatibleFailure(service, credentials, response)
        }
    } catch (e: OpenAICompatibleApiException) {
        Result.failure(e)
    } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
        Result.failure(OpenAICompatibleConnectionException())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(mapOpenAICompatibleException(e))
    }

    /**
     * Streaming variant of [openAICompatibleChat] over server-sent events (`"stream": true`).
     *
     * Answer deltas are handed to [onDelta] the moment they arrive so the chat can render the
     * reply in place instead of waiting for the whole turn. Protocol details:
     *
     * - `delta.content` is the visible answer; `delta.reasoning_content` / `delta.reasoning` is
     *   the thinking trace, accumulated but never passed to [onDelta] (it renders separately).
     * - `delta.tool_calls` arrive as fragments joined by their `index`; the assembled calls are
     *   returned so the existing tool loop keeps working unchanged.
     * - `data: [DONE]` ends the stream; `:` comment lines and undecodable keep-alive payloads are
     *   skipped.
     *
     * Providers that ignore `stream` (or sit behind a buffering proxy) answer with a plain JSON
     * body; that message is delivered as a single [onDelta] call, so callers need no fallback
     * branch of their own.
     *
     * Deliberately **not** retried on failure: a replay after tokens were already rendered would
     * duplicate text in the transcript. The caller moves on to the next provider instead.
     */
    suspend fun openAICompatibleChatStream(
        service: Service,
        credentials: ServiceCredentials,
        messages: List<OpenAICompatibleChatRequestDto.Message>,
        tools: List<Tool> = emptyList(),
        sessionId: String? = null,
        requestTimeoutMs: Long? = null,
        onDelta: suspend (String) -> Unit,
    ): Result<OpenAICompatibleCompletion> = try {
        val apiKey = DiscoveryHttp.apiKeyFor(service, credentials, required = true)
        val model = credentials.modelId.trim().let { id ->
            if (service == Service.Nvidia) id.removeSuffix(":free") else id
        }.ifEmpty { null }
        val url = DiscoveryHttp.resolveUrl(service, credentials, service.chatUrl)

        val completion = defaultClient.preparePost(url) {
            timeout {
                requestTimeoutMillis = requestTimeoutMs ?: STREAM_REQUEST_TIMEOUT_MS
                socketTimeoutMillis = STREAM_SOCKET_TIMEOUT_MS
            }
            contentType(ContentType.Application.Json)
            // Ask intermediaries not to buffer the event stream.
            header("Accept", "text/event-stream")
            apiKey?.let { bearerAuth(it) }
            applySessionHeader(service, credentials, sessionId)
            applyCloudflareGatewayHeader(service, credentials)
            setBody(
                OpenAICompatibleChatRequestDto(
                    messages = messages,
                    model = model,
                    tools = tools.toRequestTools { it.toRequestTool() },
                    stream = true,
                ),
            )
        }.execute { response ->
            if (!response.status.isSuccess()) {
                DiscoveryHttp.mapOpenAICompatibleFailure(service, credentials, response)
            }

            if (response.contentType()?.withoutParameters() != ContentType.Text.EventStream) {
                // Streaming was ignored: the body is a normal completion document.
                val text = response.bodyAsText()
                val message = runCatching {
                    streamingJson.decodeFromString(OpenAICompatibleChatResponseDto.serializer(), text)
                }.getOrNull()?.choices?.firstOrNull()?.message
                    ?: throw OpenAICompatibleEmptyResponseException()
                message.effectiveContent?.takeIf { it.isNotEmpty() }?.let { onDelta(it) }
                return@execute OpenAICompatibleCompletion(
                    content = message.effectiveContent.orEmpty(),
                    reasoning = message.effectiveReasoning,
                    toolCalls = message.toolCalls.orEmpty(),
                )
            }

            val answer = StringBuilder()
            val reasoning = StringBuilder()
            val toolCalls = linkedMapOf<Int, StreamingToolCall>()
            var finishReason: String? = null
            var sawEvent = false
            var sawDone = false

            val channel = response.bodyAsChannel()
            while (true) {
                // readUTF8Line caps at Int.MAX_VALUE by default: a provider may send a whole
                // tool-call argument blob (or a large content delta) as one `data:` line.
                val line = channel.readUTF8Line() ?: break
                if (line.isEmpty() || line.startsWith(":")) continue
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty()) continue
                sawEvent = true
                if (payload == "[DONE]") {
                    sawDone = true
                    break
                }

                val chunk = runCatching {
                    streamingJson.decodeFromString(OpenAICompatibleChatChunkDto.serializer(), payload)
                }.getOrNull()
                if (chunk == null || chunk.choices.isEmpty()) {
                    // Some providers stream their errors as an event instead of an HTTP status.
                    val errorMessage = runCatching {
                        streamingJson.parseToJsonElement(payload).jsonObject["error"]?.jsonObject
                            ?.get("message")?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    if (errorMessage != null) {
                        throw OpenAICompatibleGenericException("${service.displayName}: $errorMessage")
                    }
                    continue
                }

                val choice = chunk.choices.firstOrNull() ?: continue
                choice.finishReason?.let { finishReason = it }
                val delta = choice.delta ?: continue

                delta.reasoningContent?.let { reasoning.append(it) }
                if (delta.reasoningContent == null) delta.reasoning?.let { reasoning.append(it) }
                delta.content?.takeIf { it.isNotEmpty() }?.let { piece ->
                    answer.append(piece)
                    onDelta(piece)
                }
                delta.toolCalls.orEmpty().forEach { fragment ->
                    val call = toolCalls.getOrPut(fragment.index) { StreamingToolCall() }
                    fragment.id?.takeIf { it.isNotBlank() }?.let { call.id = it }
                    fragment.function?.name?.takeIf { it.isNotBlank() }?.let { piece ->
                        call.name = call.name.orEmpty() + piece
                    }
                    fragment.function?.arguments?.let { call.arguments.append(it) }
                }
            }

            if (!sawEvent) throw OpenAICompatibleEmptyResponseException()

            if (!sawDone && finishReason == null) {
                // The server hung up without a [DONE] terminator — a truncated stream is not a
                // complete completion. Surface it as an error (not a silent partial success) so
                // the caller can retry or fall back to the next provider.
                throw OpenAICompatibleTruncatedStreamException()
            }

            OpenAICompatibleCompletion(
                content = answer.toString(),
                reasoning = reasoning.toString().takeIf { it.isNotBlank() },
                toolCalls = toolCalls.entries.sortedBy { it.key }.map { entry ->
                    val index = entry.key
                    val call = entry.value
                    OpenAICompatibleChatResponseDto.ToolCall(
                        id = call.id ?: "stream-$index",
                        function = OpenAICompatibleChatResponseDto.FunctionCall(
                            name = call.name.orEmpty(),
                            arguments = call.arguments.toString().ifEmpty { "{}" },
                        ),
                    )
                },
                finishReason = finishReason,
            )
        }

        Result.success(completion)
    } catch (e: OpenAICompatibleApiException) {
        Result.failure(e)
    } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
        Result.failure(OpenAICompatibleConnectionException())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(mapOpenAICompatibleException(e))
    }

    /** Mutable accumulator for one `delta.tool_calls[index]` spread over several chunks. */
    private class StreamingToolCall {
        var id: String? = null
        var name: String? = null
        val arguments = StringBuilder()
    }

    /**
     * OpenAI Responses API (`POST /v1/responses`). Used for the model families whose function
     * calling chat completions rejects — see `requiresResponsesApi`. Auth, URL resolution and
     * error mapping are shared with [openAICompatibleChat]; only the body and result shape differ.
     */
    suspend fun openAIResponses(
        service: Service,
        credentials: ServiceCredentials,
        input: List<JsonObject>,
        tools: List<Tool> = emptyList(),
        requestTimeoutMs: Long? = null,
    ): Result<OpenAIResponsesResponseDto> = try {
        val apiKey = DiscoveryHttp.apiKeyFor(service, credentials, required = true)
        val responsesUrl = service.responsesUrl
            ?: throw OpenAICompatibleGenericException("Responses URL not configured for ${service.displayName}")
        val url = DiscoveryHttp.resolveUrl(service, credentials, responsesUrl)
        val response: HttpResponse =
            defaultClient.post(url) {
                applyTimeout(requestTimeoutMs)
                contentType(ContentType.Application.Json)
                apiKey?.let { bearerAuth(it) }
                setBody(
                    OpenAIResponsesRequestDto(
                        input = input,
                        model = credentials.modelId.trim().let { id ->
                            if (service == Service.Nvidia) id.removeSuffix(":free") else id
                        }.ifEmpty { null },
                        tools = tools.toRequestTools { it.toResponsesTool() },
                    ),
                )
            }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            DiscoveryHttp.mapOpenAICompatibleFailure(service, credentials, response)
        }
    } catch (e: OpenAICompatibleApiException) {
        Result.failure(e)
    } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
        Result.failure(OpenAICompatibleConnectionException())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(mapOpenAICompatibleException(e))
    }

    // endregion

    private val anthropicJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // region Anthropic

    suspend fun anthropicChat(
        credentials: ServiceCredentials,
        messages: List<AnthropicChatRequestDto.Message>,
        tools: List<Tool> = emptyList(),
        systemInstruction: String? = null,
        requestTimeoutMs: Long? = null,
    ): Result<AnthropicChatResponseDto> = try {
        val apiKey = credentials.apiKey.ifEmpty { throw AnthropicInvalidApiKeyException() }
        val response: HttpResponse =
            defaultClient.post(Service.Anthropic.chatUrl) {
                applyTimeout(requestTimeoutMs)
                contentType(ContentType.Application.Json)
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                setBody(
                    AnthropicChatRequestDto(
                        model = credentials.modelId,
                        messages = messages,
                        max_tokens = 8192,
                        system = systemInstruction,
                        tools = tools.toRequestTools { it.toAnthropicTool() },
                    ),
                )
            }
        val responseBody = response.bodyAsText()
        if (response.status.isSuccess()) {
            val dto = anthropicJson.decodeFromString(AnthropicChatResponseDto.serializer(), responseBody)
            Result.success(dto)
        } else {
            throwAnthropicError(response.status.value, responseBody, response.headers["Retry-After"])
        }
    } catch (e: AnthropicApiException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AnthropicGenericException("Anthropic: ${e.message}", e))
    }

    private fun throwAnthropicError(statusCode: Int, responseBody: String, retryAfter: String? = null): Nothing {
        when (statusCode) {
            401, 403 -> throw AnthropicInvalidApiKeyException()
            429 -> throw AnthropicRateLimitExceededException(parseRetryAfterSeconds(retryAfter))
            529 -> throw AnthropicOverloadedException()
        }
        val errorMessage = parseAnthropicErrorMessage(responseBody)
        if (errorMessage != null && errorMessage.contains("credit balance", ignoreCase = true)) {
            throw AnthropicInsufficientCreditsException()
        }
        throw AnthropicGenericException(errorMessage ?: "Anthropic: $statusCode $responseBody")
    }

    private fun parseAnthropicErrorMessage(responseBody: String): String? = try {
        val json = anthropicJson.parseToJsonElement(responseBody)
        val errorObj = json.jsonObject["error"]?.jsonObject
        errorObj?.get("message")?.jsonPrimitive?.content
    } catch (_: Exception) {
        null
    }

    // endregion

    // region Helpers

    // Distinguish genuine network/I/O failures (preserve the "Cannot connect to
    // server" UX and the settings-screen ErrorConnectionFailed status) from
    // client-side exceptions (schema/serialization bugs, etc.) which would
    // otherwise be silently misclassified as connection failures.
    private fun mapOpenAICompatibleException(e: Exception): OpenAICompatibleApiException {
        val name = e::class.simpleName.orEmpty()
        val looksLikeNetworkFailure = name.endsWith("IOException") ||
            name.contains("Timeout", ignoreCase = true) ||
            name.contains("ConnectException", ignoreCase = true) ||
            name.contains("UnknownHost", ignoreCase = true) ||
            name.contains("NoRoute", ignoreCase = true) ||
            name.contains("SocketException", ignoreCase = true) ||
            name.contains("Unresolved", ignoreCase = true)
        return if (looksLikeNetworkFailure) {
            OpenAICompatibleConnectionException()
        } else {
            OpenAICompatibleGenericException(
                e.message?.takeIf { it.isNotBlank() } ?: "Unexpected error: $name",
                e,
            )
        }
    }

    private fun Tool.toRequestTool(): OpenAICompatibleChatRequestDto.Tool = OpenAICompatibleChatRequestDto.Tool(
        function = OpenAICompatibleChatRequestDto.Function(
            name = schema.name,
            description = schema.description,
            parameters = OpenAICompatibleChatRequestDto.Parameters(
                properties = propertySchemas(OpenAISchemaDialect),
                required = requiredParameterNames(),
            ),
        ),
    )

    /** Same schema as [toRequestTool], flattened — the Responses API tags function tools inline. */
    private fun Tool.toResponsesTool(): OpenAIResponsesRequestDto.Tool = OpenAIResponsesRequestDto.Tool(
        name = schema.name,
        description = schema.description,
        parameters = OpenAICompatibleChatRequestDto.Parameters(
            properties = propertySchemas(OpenAISchemaDialect),
            required = requiredParameterNames(),
        ),
    )

    private fun Tool.toAnthropicTool(): AnthropicChatRequestDto.Tool = AnthropicChatRequestDto.Tool(
        name = schema.name,
        description = schema.description,
        input_schema = AnthropicChatRequestDto.InputSchema(
            properties = propertySchemas(AnthropicSchemaDialect),
            required = requiredParameterNames(),
        ),
    )

    private fun Tool.toGeminiTool(): GeminiTool = GeminiTool(
        functionDeclarations = listOf(
            FunctionDeclaration(
                name = schema.name,
                description = schema.description,
                parameters = FunctionParameters(
                    properties = propertySchemas(GeminiSchemaDialect),
                    required = requiredParameterNames(),
                ),
            ),
        ),
    )

    // endregion
}

/**
 * Builds one provider's property-schema node. The three chat APIs accept the same JSON Schema
 * shape under three unrelated DTOs, so the traversal in [toPropertySchema] is written once and
 * the dialect only decides which type each node becomes. Fields a provider doesn't model (e.g.
 * Anthropic and Gemini have no `additionalProperties`) are dropped by its implementation.
 */
private interface SchemaDialect<T> {
    /** Element type used when an `array` node declares no `items`. */
    val defaultStringItems: T

    fun property(
        type: String,
        description: String? = null,
        enum: List<String>? = null,
        items: T? = null,
        properties: Map<String, T>? = null,
        required: List<String>? = null,
        additionalProperties: Boolean? = null,
    ): T
}

private object OpenAISchemaDialect : SchemaDialect<OpenAICompatibleChatRequestDto.PropertySchema> {
    override val defaultStringItems = OpenAICompatibleChatRequestDto.PropertySchema(type = "string")

    override fun property(
        type: String,
        description: String?,
        enum: List<String>?,
        items: OpenAICompatibleChatRequestDto.PropertySchema?,
        properties: Map<String, OpenAICompatibleChatRequestDto.PropertySchema>?,
        required: List<String>?,
        additionalProperties: Boolean?,
    ) = OpenAICompatibleChatRequestDto.PropertySchema(
        type = type,
        description = description,
        enum = enum,
        items = items,
        properties = properties,
        required = required,
        additionalProperties = additionalProperties,
    )
}

private object AnthropicSchemaDialect : SchemaDialect<AnthropicChatRequestDto.PropertySchema> {
    override val defaultStringItems = AnthropicChatRequestDto.PropertySchema(type = "string")

    override fun property(
        type: String,
        description: String?,
        enum: List<String>?,
        items: AnthropicChatRequestDto.PropertySchema?,
        properties: Map<String, AnthropicChatRequestDto.PropertySchema>?,
        required: List<String>?,
        additionalProperties: Boolean?,
    ) = AnthropicChatRequestDto.PropertySchema(
        type = type,
        description = description,
        enum = enum,
        items = items,
        properties = properties,
        required = required,
    )
}

private object GeminiSchemaDialect : SchemaDialect<PropertySchema> {
    override val defaultStringItems = PropertySchema(type = "string")

    override fun property(
        type: String,
        description: String?,
        enum: List<String>?,
        items: PropertySchema?,
        properties: Map<String, PropertySchema>?,
        required: List<String>?,
        additionalProperties: Boolean?,
    ) = PropertySchema(
        type = type,
        description = description,
        enum = enum,
        items = items,
        properties = properties,
        required = required,
    )
}

/** Declared parameters of this tool, converted to [dialect]'s node type. */
private fun <T> Tool.propertySchemas(dialect: SchemaDialect<T>): Map<String, T> = schema.parameters.mapValues { (_, param) ->
    param.rawSchema?.toPropertySchema(dialect)
        ?: dialect.property(type = param.type, description = param.description)
}

private fun Tool.requiredParameterNames(): List<String> = schema.parameters.filter { it.value.required }.keys.toList()

// JSON Schema dialects vary: `type` may be a string or an array (e.g.
// ["string","null"]); enum/required entries may be non-string primitives;
// nested objects may be malformed. MCP tool servers (especially proxies like
// litellm /toolset/<category>/mcp) emit any of these shapes, so the converter
// falls back to safe defaults instead of throwing — a throw here would bubble
// to setBody() and surface as a misleading "Cannot connect to server" error.

private fun <T> JsonObject.toPropertySchema(dialect: SchemaDialect<T>): T {
    val type = schemaType()
    val items = (this["items"] as? JsonObject)?.toPropertySchema(dialect)
    val properties = schemaObjectMap("properties")?.mapValues { it.value.toPropertySchema(dialect) }
    val additionalProperties = (this["additionalProperties"] as? JsonPrimitive)
        ?.contentOrNull?.toBooleanStrictOrNull()
    return dialect.property(
        type = type,
        description = schemaString("description"),
        enum = schemaStringList("enum"),
        items = items ?: if (type == "array") dialect.defaultStringItems else null,
        properties = properties,
        required = schemaStringList("required"),
        additionalProperties = additionalProperties,
    )
}

private fun JsonObject.schemaType(): String = when (val t = this["type"]) {
    is JsonPrimitive -> t.contentOrNull ?: "string"

    is JsonArray -> t.asSequence()
        .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        .firstOrNull { it != "null" }
        ?: "string"

    else -> "string"
}

private fun JsonObject.schemaString(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.schemaStringList(key: String): List<String>? = (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    ?.takeIf { it.isNotEmpty() }

private fun JsonObject.schemaObjectMap(key: String): Map<String, JsonObject>? = (this[key] as? JsonObject)?.mapNotNull { (k, v) -> (v as? JsonObject)?.let { k to it } }
    ?.toMap()
    ?.takeIf { it.isNotEmpty() }
