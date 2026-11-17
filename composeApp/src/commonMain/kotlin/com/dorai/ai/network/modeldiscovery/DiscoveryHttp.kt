package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.network.OpenAICompatibleBadRequestException
import com.dorai.ai.network.OpenAICompatibleContentModerationException
import com.dorai.ai.network.OpenAICompatibleGenericException
import com.dorai.ai.network.OpenAICompatibleInvalidApiKeyException
import com.dorai.ai.network.OpenAICompatibleModelNotFoundException
import com.dorai.ai.network.OpenAICompatibleProviderErrorException
import com.dorai.ai.network.OpenAICompatibleQuotaExhaustedException
import com.dorai.ai.network.OpenAICompatibleRateLimitExceededException
import com.dorai.ai.network.OpenAICompatibleServiceUnavailableException
import com.dorai.ai.network.OpenAICompatibleTimeoutException
import com.dorai.ai.network.ServiceCredentials
import com.dorai.ai.network.isValidCloudflareAccountId
import com.dorai.ai.network.openAICompatibleResult
import com.dorai.ai.network.parseRetryAfterSeconds
import com.dorai.ai.network.requestTooLargeException
import com.dorai.ai.network.sessionHeadersFor
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared HTTP plumbing for per-provider model discovery (the adapters registered in
 * [ModelDiscoveryRegistry]). Moved verbatim out of the `Requests` class so adapters and
 * the legacy listing path execute exactly the same URL resolution, auth, header and
 * error-mapping logic.
 */
internal object DiscoveryHttp {

    private val errorJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * Resolves the request URL for [service]:
     * - Hosted providers that don't accept a custom base URL get their canonical [path]
     *   with any `{account_id}` template emptied.
     * - Services with a custom base URL either splice it in front of [path] or use it as
     *   the Cloudflare account id for a `{account_id}` path template.
     */
    internal fun resolveUrl(service: Service, credentials: ServiceCredentials, path: String): String {
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
                // Accept either the raw account id or a full dashboard URL containing
                // "/accounts/<id>/" — extract the id when it is a well-formed 32-hex value,
                // and otherwise produce a deterministic request Cloudflare rejects instead
                // of splicing arbitrary user input into the URL path.
                val accountId = customBase.substringAfter("/accounts/", missingDelimiterValue = customBase)
                    .substringBefore("/")
                    .trim()
                if (isValidCloudflareAccountId(accountId)) {
                    return path.replace("{account_id}", accountId)
                }
                return path.replace("{account_id}", "")
            }
            val base = Service.normalizeOpenAICompatibleBaseUrl(customBase)
            val relPath = if (path.startsWith("/")) path else "/$path"
            return "$base$relPath"
        }
        return path.replace("{account_id}", "")
    }

    /**
     * Returns the bearer-token API key for [service], or `null` when the request should go
     * out unauthenticated. With [required] set, a missing key on a service that mandates
     * one throws [OpenAICompatibleInvalidApiKeyException]; with [required] unset, a missing
     * key simply returns `null`.
     */
    internal fun apiKeyFor(service: Service, credentials: ServiceCredentials, required: Boolean): String? {
        if (!service.requiresApiKey && !service.supportsOptionalApiKey) return null
        if (required && service.requiresApiKey && credentials.apiKey.isEmpty()) {
            throw OpenAICompatibleInvalidApiKeyException()
        }
        return credentials.apiKey.ifEmpty { null }
    }

    /**
     * Session id headers for [service] — only OpenCode (and an OpenAI-Compatible instance
     * pointed at `opencode.ai`) receives them.
     */
    internal fun sessionHeaders(service: Service, credentials: ServiceCredentials, sessionId: String?): Map<String, String> = sessionHeadersFor(service, sessionId, credentials.baseUrl)

    /** Gateway header for Service.CloudflareAIGateway — blank gateway id means the account's auto-created "default" gateway. */
    internal fun cloudflareGatewayHeader(service: Service, credentials: ServiceCredentials): Pair<String, String>? = if (service == Service.CloudflareAIGateway) {
        "cf-aig-gateway-id" to credentials.cloudflareGatewayId.ifBlank { "default" }
    } else {
        null
    }

    /**
     * Shared failure mapping for the OpenAI-compatible endpoints that have no bespoke error
     * handling. Always throws an [com.dorai.ai.network.OpenAICompatibleApiException] shaped
     * like the legacy `Requests` behavior.
     */
    internal suspend fun mapOpenAICompatibleFailure(
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

            429 -> throw OpenAICompatibleRateLimitExceededException(parseRetryAfterSeconds(response.headers["Retry-After"]))

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
        val error = errorJson.parseToJsonElement(responseBody).jsonObject["error"]
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

    // region Key-validation probes

    suspend fun validateOpenRouterApiKey(httpClient: HttpClient, credentials: ServiceCredentials): Result<Unit> = openAICompatibleResult {
        val apiKey = credentials.apiKey.ifEmpty { throw OpenAICompatibleInvalidApiKeyException() }
        val response: HttpResponse = httpClient.get("https://openrouter.ai/api/v1/auth/key") {
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
     * Perplexity Sonar has no authenticated models endpoint, so key checks go through the
     * chat completions URL with an intentionally incomplete body. Auth is evaluated first:
     * invalid keys return 401/403; a valid key typically yields 400/422 on the empty messages
     * array — which we treat as connected without spending tokens on a real completion.
     */
    suspend fun validatePerplexityApiKey(httpClient: HttpClient, credentials: ServiceCredentials): Result<Unit> = openAICompatibleResult {
        val apiKey = credentials.apiKey.ifEmpty { throw OpenAICompatibleInvalidApiKeyException() }
        val response: HttpResponse = httpClient.post(Service.Perplexity.chatUrl) {
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
}
