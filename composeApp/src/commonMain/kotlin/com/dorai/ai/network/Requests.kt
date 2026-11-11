@file:OptIn(ExperimentalUuidApi::class)

package com.dorai.ai.network

import com.dorai.ai.Version
import com.dorai.ai.currentPlatform
import com.dorai.ai.data.Service
import com.dorai.ai.httpClient
import com.dorai.ai.isDebugBuild
import com.dorai.ai.network.dtos.anthropic.AnthropicChatRequestDto
import com.dorai.ai.network.dtos.anthropic.AnthropicChatResponseDto
import com.dorai.ai.network.dtos.anthropic.AnthropicModelsResponseDto
import com.dorai.ai.network.dtos.gemini.FunctionDeclaration
import com.dorai.ai.network.dtos.gemini.FunctionParameters
import com.dorai.ai.network.dtos.gemini.GeminiChatRequestDto
import com.dorai.ai.network.dtos.gemini.GeminiChatResponseDto
import com.dorai.ai.network.dtos.gemini.GeminiModelsResponseDto
import com.dorai.ai.network.dtos.gemini.GeminiTool
import com.dorai.ai.network.dtos.gemini.PropertySchema
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleChatChunkDto
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleChatRequestDto
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleChatResponseDto
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleCompletion
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleModelResponseDto
import com.dorai.ai.network.dtos.openairesponses.OpenAIResponsesRequestDto
import com.dorai.ai.network.dtos.openairesponses.OpenAIResponsesResponseDto
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
import io.ktor.http.content.TextContent
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
)

/** Applies a per-request override of the client-wide timeouts, when the caller supplied one. */
private fun HttpRequestBuilder.applyTimeout(requestTimeoutMs: Long?) {
    requestTimeoutMs ?: return
    timeout {
        requestTimeoutMillis = requestTimeoutMs
        socketTimeoutMillis = requestTimeoutMs
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
    sessionHeadersFor(service, sessionId, credentials.baseUrl).forEach { (k, v) -> header(k, v) }
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
 */
private inline fun <T> openAICompatibleResult(block: () -> Result<T>): Result<T> = try {
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

    suspend fun getGeminiModels(credentials: ServiceCredentials): Result<GeminiModelsResponseDto> = try {
        val apiKey = credentials.apiKey.ifEmpty { throw GeminiInvalidApiKeyException() }
        val response: HttpResponse =
            defaultClient.get("https://generativelanguage.googleapis.com/v1beta/models") {
                header("x-goog-api-key", apiKey)
            }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            when (response.status.value) {
                400, 403 -> throw GeminiInvalidApiKeyException()
                else -> throw GeminiGenericException("Failed to fetch models: ${response.status}")
            }
        }
    } catch (e: GeminiApiException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(GeminiGenericException("Connection failed", e))
    }

    suspend fun geminiChat(
        credentials: ServiceCredentials,
        messages: List<GeminiChatRequestDto.Content>,
        tools: List<Tool> = emptyList(),
        systemInstruction: String? = null,
        requestTimeoutMs: Long? = null,
    ): Result<GeminiChatResponseDto> = try {
        val apiKey = credentials.apiKey.ifEmpty { throw GeminiInvalidApiKeyException() }
        val selectedModelId = credentials.modelId

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
                429 -> throw GeminiRateLimitExceededException()

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
        val apiKey = getApiKeyOrThrow(service, credentials)
        val model = credentials.modelId.ifEmpty { null }
        val url = resolveUrl(service, credentials, service.chatUrl)
        val response: HttpResponse =
            defaultClient.post(url) {
                applyTimeout(requestTimeoutMs)
                contentType(ContentType.Application.Json)
                apiKey?.let { bearerAuth(it) }
                applySessionHeader(service, credentials, sessionId)
                customHeaders.forEach { (k, v) -> header(k, v) }
                if (service == Service.CloudflareAIGateway) {
                    header("cf-aig-gateway-id", "default")
                }
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
            handleOpenAICompatibleError(service, credentials, response)
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
        val apiKey = getApiKeyOrThrow(service, credentials)
        val model = credentials.modelId.ifEmpty { null }
        val url = resolveUrl(service, credentials, service.chatUrl)

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
            if (service == Service.CloudflareAIGateway) {
                header("cf-aig-gateway-id", "default")
            }
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
                handleOpenAICompatibleError(service, credentials, response)
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
                if (payload == "[DONE]") break

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
        val apiKey = getApiKeyOrThrow(service, credentials)
        val responsesUrl = service.responsesUrl
            ?: throw OpenAICompatibleGenericException("Responses URL not configured for ${service.displayName}")
        val url = resolveUrl(service, credentials, responsesUrl)
        val response: HttpResponse =
            defaultClient.post(url) {
                applyTimeout(requestTimeoutMs)
                contentType(ContentType.Application.Json)
                apiKey?.let { bearerAuth(it) }
                setBody(
                    OpenAIResponsesRequestDto(
                        input = input,
                        model = credentials.modelId.ifEmpty { null },
                        tools = tools.toRequestTools { it.toResponsesTool() },
                    ),
                )
            }
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            handleOpenAICompatibleError(service, credentials, response)
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

    suspend fun getOpenAICompatibleModels(
        service: Service,
        credentials: ServiceCredentials,
    ): Result<OpenAICompatibleModelResponseDto> = openAICompatibleResult {
        val modelsUrl = service.modelsUrl
            ?: return Result.failure(OpenAICompatibleGenericException("Models URL not configured for ${service.displayName}"))
        val apiKey = if (service == Service.Cloudflare || service == Service.CloudflareAIGateway) {
            getApiKeyOrThrow(service, credentials)
        } else {
            getOptionalApiKey(service, credentials)
        }

        // Cloudflare Workers AI uses the Cloudflare v4 envelope (result/result_info), not
        // the OpenAI-compatible { data: [...] } model-list response. Fetch all pages explicitly.
        if (service == Service.Cloudflare || service == Service.CloudflareAIGateway) {
            if (credentials.baseUrl.isBlank()) {
                return Result.failure(
                    OpenAICompatibleGenericException("Cloudflare Account ID is required to discover Workers AI models."),
                )
            }
            val accountId = credentials.baseUrl.trim()
                .substringAfter("/accounts/", missingDelimiterValue = credentials.baseUrl.trim())
                .substringBefore("/")
            if (!isValidCloudflareAccountId(accountId)) {
                return Result.failure(OpenAICompatibleGenericException("Enter a valid 32-character Cloudflare Account ID."))
            }

            val allModels = mutableListOf<OpenAICompatibleModelResponseDto.Model>()
            var page = 1
            var totalPages = 1
            val perPage = 100
            while (page <= totalPages && page <= 20) {
                val pageUrl = "${resolveUrl(service, credentials, modelsUrl)}?page=$page&per_page=$perPage"
                val response = defaultClient.get(pageUrl) {
                    apiKey?.let { bearerAuth(it) }
                    applySessionHeader(service, credentials, sessionId = null)
                    if (service == Service.CloudflareAIGateway) {
                        header("cf-aig-gateway-id", "default")
                    }
                }
                if (!response.status.isSuccess()) {
                    if (service == Service.Cloudflare || service == Service.CloudflareAIGateway) {
                        val responseBody = response.bodyAsText()
                        val detail = parseCloudflareModelSearchError(responseBody)
                            ?: "HTTP ${response.status.value}"
                        throw OpenAICompatibleGenericException("Cloudflare model discovery failed: $detail")
                    }
                    handleOpenAICompatibleError(service, credentials, response)
                }
                val parsedPage = try {
                    parseCloudflareModelSearchPage(response.bodyAsText())
                } catch (e: Exception) {
                    throw OpenAICompatibleGenericException(
                        e.message?.takeIf { it.isNotBlank() } ?: "Cloudflare model search returned invalid data.",
                        e,
                    )
                }
                allModels += parsedPage.models
                totalPages = parsedPage.totalPages
                    ?: if (parsedPage.models.size == perPage) page + 1 else page
                page++
            }
            return Result.success(OpenAICompatibleModelResponseDto(data = allModels.distinctBy { it.id }))
        }

        val url = resolveUrl(service, credentials, modelsUrl)
        val response: HttpResponse = defaultClient.get(url) {
            apiKey?.let { bearerAuth(it) }
            applySessionHeader(service, credentials, sessionId = null)
        }
        if (response.status.isSuccess()) {
            if (service.modelsResponseIsArray) {
                val models: List<OpenAICompatibleModelResponseDto.Model> = response.body()
                Result.success(OpenAICompatibleModelResponseDto(data = models))
            } else {
                Result.success(response.body())
            }
        } else {
            handleOpenAICompatibleError(service, credentials, response)
        }
    }

    suspend fun validateOpenRouterApiKey(credentials: ServiceCredentials): Result<Unit> = openAICompatibleResult {
        val apiKey = credentials.apiKey.ifEmpty { throw OpenAICompatibleInvalidApiKeyException() }
        val response: HttpResponse = defaultClient.get("https://openrouter.ai/api/v1/auth/key") {
            bearerAuth(apiKey)
        }
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            when (response.status.value) {
                401, 403 -> throw OpenAICompatibleInvalidApiKeyException()
                else -> throw OpenAICompatibleGenericException("Failed to validate OpenRouter API key: ${response.status}")
            }
        }
    }

    /**
     * Requesty answers a bad key on `/models` with 403, which the shared error mapping would
     * report as content moderation. Check the key here so the settings screen shows "invalid key".
     */
    suspend fun validateRequestyApiKey(credentials: ServiceCredentials): Result<Unit> = openAICompatibleResult {
        val apiKey = credentials.apiKey.ifEmpty { throw OpenAICompatibleInvalidApiKeyException() }
        val response: HttpResponse = defaultClient.get("https://router.requesty.ai/v1/models") {
            bearerAuth(apiKey)
        }
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            when (response.status.value) {
                401, 403 -> throw OpenAICompatibleInvalidApiKeyException()
                else -> throw OpenAICompatibleGenericException("Failed to validate Requesty API key: ${response.status}")
            }
        }
    }

    /**
     * Perplexity Sonar has no authenticated models endpoint, so key checks go through the
     * chat completions URL with an intentionally incomplete body. Auth is evaluated first:
     * invalid keys return 401/403; a valid key typically yields 400/422 on the empty messages
     * array — which we treat as connected without spending tokens on a real completion.
     */
    suspend fun validatePerplexityApiKey(credentials: ServiceCredentials): Result<Unit> = openAICompatibleResult {
        val apiKey = credentials.apiKey.ifEmpty { throw OpenAICompatibleInvalidApiKeyException() }
        val response: HttpResponse = defaultClient.post(Service.Perplexity.chatUrl) {
            bearerAuth(apiKey)
            // TextContent avoids ContentNegotiation re-encoding a raw String as a JSON string.
            setBody(TextContent("""{"model":"sonar","messages":[]}""", ContentType.Application.Json))
        }
        when (response.status.value) {
            401, 403 -> throw OpenAICompatibleInvalidApiKeyException()

            // A bad request means the key was accepted and the probe payload rejected —
            // the key itself is valid. Only treat 2xx as success; other 4xx (rate limit,
            // not found) and 5xx must not silently report "Connected".
            200, 400, 404, 422 -> Result.success(Unit)

            else -> throw OpenAICompatibleGenericException("Failed to validate Perplexity API key: ${response.status}")
        }
    }

    // endregion

    private val anthropicJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // region Anthropic

    suspend fun getAnthropicModels(credentials: ServiceCredentials): Result<AnthropicModelsResponseDto> = try {
        val apiKey = credentials.apiKey.ifEmpty { throw AnthropicInvalidApiKeyException() }
        val response: HttpResponse =
            defaultClient.get("https://api.anthropic.com/v1/models") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
            }
        val responseBody = response.bodyAsText()
        if (response.status.isSuccess()) {
            val dto = anthropicJson.decodeFromString(AnthropicModelsResponseDto.serializer(), responseBody)
            Result.success(dto)
        } else {
            throwAnthropicError(response.status.value, responseBody)
        }
    } catch (e: AnthropicApiException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AnthropicGenericException("Anthropic: ${e.message}", e))
    }

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
            throwAnthropicError(response.status.value, responseBody)
        }
    } catch (e: AnthropicApiException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AnthropicGenericException("Anthropic: ${e.message}", e))
    }

    private fun throwAnthropicError(statusCode: Int, responseBody: String): Nothing {
        when (statusCode) {
            401, 403 -> throw AnthropicInvalidApiKeyException()
            429 -> throw AnthropicRateLimitExceededException()
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

    private fun resolveUrl(service: Service, credentials: ServiceCredentials, path: String): String {
        val customBase = credentials.baseUrl.trim()
        if (!Service.acceptsCustomBaseUrl(service)) {
            // Hosted providers own their canonical endpoints. A custom base URL entered
            // for one of them must never be spliced into the URL — it would produce a
            // malformed request like "https://myhost/https://api.groq.com/...". Ignore it
            // (the settings UI no longer offers the field for these services; this guard
            // covers stale values persisted before that change and any other writer).
            return path.replace("{account_id}", "")
        }
        if (customBase.isNotEmpty()) {
            if (path.contains("{account_id}") && customBase.isNotEmpty()) {
                // Try to extract account ID from custom base URL or use as-is if it contains account ID
                val accountId = customBase
                    .substringAfter("/accounts/", missingDelimiterValue = "")
                    .substringBefore("/", missingDelimiterValue = customBase)
                    .takeIf { it.isNotEmpty() && !it.contains(":") && !it.startsWith("http") } ?: ""
                val resolved = if (accountId.isNotEmpty() && path.contains("{account_id}")) {
                    path.replace("{account_id}", accountId)
                } else if (path.contains("{account_id}")) {
                    // Maybe customBase is just the account ID
                    path.replace("{account_id}", customBase)
                } else {
                    path
                }
                return resolved
            }
            val base = Service.normalizeOpenAICompatibleBaseUrl(customBase)
            val relPath = if (path.startsWith("/")) path else "/$path"
            return "$base$relPath"
        }
        return path.replace("{account_id}", "")
    }

    private fun getApiKeyOrThrow(service: Service, credentials: ServiceCredentials): String? {
        if (!service.requiresApiKey && !service.supportsOptionalApiKey) return null
        val key = credentials.apiKey
        if (service.requiresApiKey && key.isEmpty()) throw OpenAICompatibleInvalidApiKeyException()
        return key.ifEmpty { null }
    }

    private fun getOptionalApiKey(service: Service, credentials: ServiceCredentials): String? {
        if (!service.requiresApiKey && !service.supportsOptionalApiKey) return null
        return credentials.apiKey.ifEmpty { null }
    }

    private suspend fun handleOpenAICompatibleError(
        service: Service,
        credentials: ServiceCredentials,
        response: HttpResponse,
    ): Nothing {
        val responseBody = response.bodyAsText()
        val parsed = parseOpenAICompatibleErrorDetail(responseBody)
        val moderationDetail = parsed.moderationDetail()
        when (response.status.value) {
            400 -> {
                if (parsed.looksLikeContentPolicyViolation()) {
                    throw OpenAICompatibleContentModerationException(moderationDetail)
                }
                throw OpenAICompatibleBadRequestException(parsed.message)
            }

            401 -> throw OpenAICompatibleInvalidApiKeyException()

            402 -> throw OpenAICompatibleQuotaExhaustedException()

            403 -> {
                // OpenCode answers unauthenticated requests with 403 (not 401), so a missing or
                // rejected key looks like a moderation block if mapped generically. Surface the
                // real cause as an invalid-key error so the UI (and the Free fallback) react right.
                if (service == Service.OpenCode || parsed.looksLikeAuthRejection()) {
                    throw OpenAICompatibleInvalidApiKeyException()
                }
                throw OpenAICompatibleContentModerationException(moderationDetail)
            }

            404 -> throw OpenAICompatibleModelNotFoundException()

            408, 504 -> throw OpenAICompatibleTimeoutException()

            413 -> throw requestTooLargeException(service, parsed.message)

            429 -> throw OpenAICompatibleRateLimitExceededException()

            500, 502 -> throw OpenAICompatibleProviderErrorException(parsed.message)

            503 -> throw OpenAICompatibleServiceUnavailableException()

            else -> {
                val haystack = parsed.message ?: responseBody
                if (haystack.contains("credit", ignoreCase = true) ||
                    haystack.contains("exhausted", ignoreCase = true) ||
                    haystack.contains("spending limit", ignoreCase = true) ||
                    haystack.contains("quota", ignoreCase = true) ||
                    haystack.contains("subscription", ignoreCase = true) ||
                    haystack.contains("upgrade", ignoreCase = true)
                ) {
                    throw OpenAICompatibleQuotaExhaustedException()
                }
                val detail = parsed.message ?: "${response.status}"
                throw OpenAICompatibleGenericException("${service.displayName}: $detail")
            }
        }
    }

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

    private data class OpenAICompatibleErrorDetail(
        val message: String?,
        val code: String?,
        val type: String?,
        val reasons: List<String>,
    ) {
        fun looksLikeContentPolicyViolation(): Boolean {
            if (code?.equals("content_policy_violation", ignoreCase = true) == true) return true
            if (type?.contains("content_filter", ignoreCase = true) == true) return true
            if (type?.contains("content_policy", ignoreCase = true) == true) return true
            val msg = message ?: return false
            return msg.contains("content policy", ignoreCase = true) ||
                msg.contains("moderation", ignoreCase = true) ||
                msg.contains("flagged", ignoreCase = true)
        }

        /**
         * Some gateways answer an invalid/revoked key with 403 instead of 401 (documented
         * for Requesty, observed elsewhere). Distinguish an auth-shaped 403 from a
         * moderation one before reporting "content flagged" to the user.
         */
        fun looksLikeAuthRejection(): Boolean {
            if (code?.contains("invalid_api_key", ignoreCase = true) == true) return true
            if (type?.contains("auth", ignoreCase = true) == true) return true
            val msg = message ?: return false
            return msg.contains("api key", ignoreCase = true) ||
                msg.contains("apikey", ignoreCase = true) ||
                msg.contains("unauthorized", ignoreCase = true) ||
                msg.contains("authentication", ignoreCase = true) ||
                msg.contains("invalid key", ignoreCase = true)
        }

        fun moderationDetail(): String? {
            val reasonText = reasons.takeIf { it.isNotEmpty() }
                ?.joinToString(", ")
                ?.let { "flagged for '$it'" }
            return reasonText ?: message
        }
    }

    private fun parseOpenAICompatibleErrorDetail(responseBody: String): OpenAICompatibleErrorDetail = try {
        val error = anthropicJson.parseToJsonElement(responseBody).jsonObject["error"]
        when {
            error == null -> OpenAICompatibleErrorDetail(null, null, null, emptyList())

            error is JsonPrimitive -> OpenAICompatibleErrorDetail(
                message = error.content.takeIf { it.isNotBlank() },
                code = null,
                type = null,
                reasons = emptyList(),
            )

            else -> {
                val obj = error.jsonObject
                val message = obj["message"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                val code = (obj["code"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
                val type = (obj["type"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
                val reasons = (obj["metadata"] as? JsonObject)?.get("reasons")?.let { it as? JsonArray }
                    ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() } }
                    .orEmpty()
                OpenAICompatibleErrorDetail(message, code, type, reasons)
            }
        }
    } catch (_: Exception) {
        OpenAICompatibleErrorDetail(null, null, null, emptyList())
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
