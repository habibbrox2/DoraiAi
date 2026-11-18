package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.network.AnthropicGenericException
import com.dorai.ai.network.AnthropicInvalidApiKeyException
import com.dorai.ai.network.AnthropicRateLimitExceededException
import com.dorai.ai.network.ServiceCredentials
import com.dorai.ai.network.dtos.anthropic.AnthropicModelsResponseDto
import com.dorai.ai.network.parseRetryAfterSeconds
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Model discovery for Anthropic — `GET https://api.anthropic.com/v1/models` with the
 * `x-api-key` and `anthropic-version: 2023-06-01` headers (the endpoint is canonical;
 * [Service.Anthropic] does not accept a custom base URL, so [DiscoveryHttp.resolveUrl]
 * is not needed). Ported from the legacy `Requests.getAnthropicModels` listing path:
 * a blank key and 401/403 map to [AnthropicInvalidApiKeyException], 429 maps to
 * [AnthropicRateLimitExceededException] (carrying the server's `Retry-After` hint when
 * present), and every other failure surfaces as [AnthropicGenericException].
 *
 * Each `data` entry maps to a [DiscoveredModel] with only the id and display name —
 * Anthropic's listing reports no context window or release date, and the adapter must
 * not invent values.
 */
class AnthropicModelDiscoveryAdapter(
    private val httpClient: HttpClient,
    override val service: Service = Service.Anthropic,
) : ModelDiscoveryAdapter {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun discoverModels(credentials: ServiceCredentials): List<DiscoveredModel> {
        val apiKey = credentials.apiKey.ifEmpty { throw AnthropicInvalidApiKeyException() }
        val response = httpClient.get(MODELS_URL) {
            header("x-api-key", apiKey)
            header("anthropic-version", ANTHROPIC_VERSION)
        }
        if (!response.status.isSuccess()) {
            when (response.status.value) {
                401, 403 -> throw AnthropicInvalidApiKeyException()
                429 -> throw AnthropicRateLimitExceededException(parseRetryAfterSeconds(response.headers["Retry-After"]))
                else -> throw AnthropicGenericException("${service.displayName}: ${response.status}")
            }
        }
        return try {
            json.decodeFromString<AnthropicModelsResponseDto>(response.bodyAsText()).data.map {
                DiscoveredModel(id = it.id, displayName = it.display_name)
            }
        } catch (e: Exception) {
            throw AnthropicGenericException(
                e.message?.takeIf { it.isNotBlank() } ?: "Anthropic model list returned invalid data.",
                e,
            )
        }
    }

    private companion object {
        /** Anthropic's canonical model-listing endpoint. */
        const val MODELS_URL = "https://api.anthropic.com/v1/models"

        /** The pinned API version the legacy listing path sent. */
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
