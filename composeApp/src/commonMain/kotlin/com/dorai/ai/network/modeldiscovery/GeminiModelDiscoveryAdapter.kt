package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.data.isChatModel
import com.dorai.ai.network.GeminiGenericException
import com.dorai.ai.network.GeminiInvalidApiKeyException
import com.dorai.ai.network.ServiceCredentials
import com.dorai.ai.network.dtos.gemini.GeminiModelsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Model discovery for Gemini — `GET https://generativelanguage.googleapis.com/v1beta/models`
 * with the `x-goog-api-key` header (the endpoint is canonical; [Service.Gemini] does not
 * accept a custom base URL, so [DiscoveryHttp.resolveUrl] is not needed). Ported verbatim
 * from the legacy `Requests.getGeminiModels` listing path: a blank key and 400/403 map to
 * [GeminiInvalidApiKeyException], every other failure surfaces as [GeminiGenericException].
 *
 * Only `generateContent`-capable models survive; after the `models/` prefix strip the
 * [isChatModel] filter drops embeddings/imagen/veo and the other non-chat APIs. A response
 * whose models are all filtered out returns an empty list — surfacing that as an error is
 * the repository's job.
 */
class GeminiModelDiscoveryAdapter(
    private val httpClient: HttpClient,
    override val service: Service = Service.Gemini,
) : ModelDiscoveryAdapter {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun discoverModels(credentials: ServiceCredentials): List<DiscoveredModel> {
        val apiKey = credentials.apiKey.ifEmpty { throw GeminiInvalidApiKeyException() }
        val response = httpClient.get(MODELS_URL) {
            header("x-goog-api-key", apiKey)
        }
        if (!response.status.isSuccess()) {
            when (response.status.value) {
                400, 403 -> throw GeminiInvalidApiKeyException()
                else -> throw GeminiGenericException("Failed to fetch models: ${response.status}")
            }
        }
        val models = try {
            json.decodeFromString<GeminiModelsResponseDto>(response.bodyAsText()).models
        } catch (e: Exception) {
            throw GeminiGenericException(
                e.message?.takeIf { it.isNotBlank() } ?: "Gemini model list returned invalid data.",
                e,
            )
        }
        return models
            .filter { it.supportedGenerationMethods?.contains("generateContent") == true }
            .map { it to it.name.removePrefix("models/") }
            .filter { (_, modelId) -> isChatModel(modelId) }
            .map { (dto, modelId) ->
                DiscoveredModel(
                    id = modelId,
                    displayName = dto.displayName,
                    contextWindow = dto.inputTokenLimit,
                )
            }
    }

    private companion object {
        /** Gemini's canonical model-listing endpoint. */
        const val MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }
}
