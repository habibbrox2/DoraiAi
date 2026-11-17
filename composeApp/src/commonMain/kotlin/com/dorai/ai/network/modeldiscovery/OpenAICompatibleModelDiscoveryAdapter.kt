package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.network.OpenAICompatibleGenericException
import com.dorai.ai.network.OpenAICompatibleInvalidApiKeyException
import com.dorai.ai.network.ServiceCredentials
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleModelResponseDto
import com.dorai.ai.toIsoDate
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Validates a service's credentials before discovery runs. Probes throw the provider's API
 * exception on failure (e.g. [OpenAICompatibleInvalidApiKeyException] for a rejected key);
 * a probe that returns normally means the credentials were accepted.
 */
fun interface KeyProbe {
    suspend fun validate(credentials: ServiceCredentials)
}

/**
 * Model discovery for every provider that speaks the OpenAI-compatible `GET /models`
 * listing — a `{ data: [...] }` envelope, or a bare JSON array for the [responseIsArray]
 * providers (Together). Ported from the generic branch of the legacy
 * `Requests.getOpenAICompatibleModels` listing path; Cloudflare (paged v4 search), Gemini,
 * Anthropic, and Perplexity have their own adapters/paths.
 */
class OpenAICompatibleModelDiscoveryAdapter(
    override val service: Service,
    private val httpClient: HttpClient,
    /** Some providers (Together) answer with a bare JSON array instead of `{ data: [...] }`. */
    private val responseIsArray: Boolean = false,
    /**
     * Optional credentials check that runs before discovery when a key was provided. Services
     * that accept requests without a key (OpenRouter, Kilo, OpenCode, OpenAI-Compatible) must
     * still discover models with a blank key, so the probe is skipped for them.
     */
    private val keyProbe: KeyProbe? = null,
    /**
     * Gateways (Requesty) answer an invalid key on `/models` with 403, which the shared error
     * mapping would report as content moderation — surface it as an invalid-key error instead.
     */
    private val forbiddenMeansInvalidKey: Boolean = false,
) : ModelDiscoveryAdapter {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun discoverModels(credentials: ServiceCredentials): List<DiscoveredModel> {
        if (credentials.apiKey.isNotEmpty()) {
            keyProbe?.validate(credentials)
        }
        val modelsUrl = service.modelsUrl
            ?: throw OpenAICompatibleGenericException("Models URL not configured for ${service.displayName}")
        val apiKey = DiscoveryHttp.apiKeyFor(service, credentials, required = service.requiresApiKey)
        val response = httpClient.get(DiscoveryHttp.resolveUrl(service, credentials, modelsUrl)) {
            apiKey?.let { bearerAuth(it) }
            DiscoveryHttp.sessionHeaders(service, credentials, sessionId = null).forEach { (name, value) ->
                header(name, value)
            }
            // No-op for the services routed to this adapter (Cloudflare has its own adapter);
            // kept so the request shape matches the legacy listing path exactly.
            DiscoveryHttp.cloudflareGatewayHeader(service, credentials)?.let { (name, value) ->
                header(name, value)
            }
        }
        if (!response.status.isSuccess()) {
            if (forbiddenMeansInvalidKey && response.status.value == 403) {
                throw OpenAICompatibleInvalidApiKeyException()
            }
            DiscoveryHttp.mapOpenAICompatibleFailure(service, credentials, response)
        }
        val models: List<OpenAICompatibleModelResponseDto.Model> = if (responseIsArray) {
            json.decodeFromString(response.bodyAsText())
        } else {
            json.decodeFromString<OpenAICompatibleModelResponseDto>(response.bodyAsText()).data
        }
        if (models.isEmpty()) {
            throw OpenAICompatibleGenericException("${service.displayName} returned no models...")
        }
        return models.map { model ->
            DiscoveredModel(
                id = model.id,
                displayName = model.name,
                contextWindow = model.context_window ?: model.context_length,
                releaseDate = model.created?.toIsoDate(),
                isActive = model.isActive,
                modelType = model.type,
            )
        }
    }
}
