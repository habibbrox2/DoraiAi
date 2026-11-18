package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import io.ktor.client.HttpClient

/**
 * Resolves the [ModelDiscoveryAdapter] that owns model discovery for a given [Service].
 * Adapters are registered explicitly; a service without an adapter resolves to `null` and
 * falls back to the legacy listing path.
 */
class ModelDiscoveryRegistry(
    private val adapters: List<ModelDiscoveryAdapter>,
) {
    /** Returns the adapter registered for [service], or `null` when none is registered. */
    fun adapterFor(service: Service): ModelDiscoveryAdapter? = adapters.firstOrNull { it.service == service }

    companion object {
        /**
         * Every service whose model listing speaks the OpenAI-compatible `GET /models` shape
         * and is therefore served by [OpenAICompatibleModelDiscoveryAdapter]. Bespoke providers
         * Gemini, Anthropic, and Perplexity get their own adapters in later tasks; Free and
         * LiteRT never list remote models.
         */
        private val openAICompatibleServices = listOf(
            Service.AtlasCloud,
            Service.Groq,
            Service.XAI,
            Service.OpenRouter,
            Service.Requesty,
            Service.Nvidia,
            Service.OpenAI,
            Service.DeepSeek,
            Service.Mistral,
            Service.Cerebras,
            Service.OllamaCloud,
            Service.LongCat,
            Service.Together,
            Service.HuggingFace,
            Service.Venice,
            Service.Moonshot,
            Service.Zai,
            Service.ZaiCodingPlan,
            Service.Minimax,
            Service.AiHubMix,
            Service.DeepInfra,
            Service.FireworksAI,
            Service.OpenCode,
            Service.Kilo,
            Service.PublicAI,
            Service.AIHorde,
            Service.OneMinAI,
            Service.OpenAICompatible,
        )

        /**
         * Builds the production registry. OpenAI-compatible services are wired to
         * [OpenAICompatibleModelDiscoveryAdapter]; Cloudflare/CloudflareAIGateway to
         * [CloudflareModelDiscoveryAdapter]; remaining bespoke adapters are added by later tasks.
         */
        fun create(httpClient: HttpClient): ModelDiscoveryRegistry = ModelDiscoveryRegistry(
            openAICompatibleServices.map { service ->
                OpenAICompatibleModelDiscoveryAdapter(
                    service = service,
                    httpClient = httpClient,
                    responseIsArray = service.modelsResponseIsArray,
                    keyProbe = when (service) {
                        // Same pre-discovery check the repository performed before delegating to
                        // the legacy listing path (skipped for a blank key).
                        Service.OpenRouter -> KeyProbe { DiscoveryHttp.validateOpenRouterApiKey(httpClient, it).getOrThrow() }

                        else -> null
                    },
                    // Requesty answers a bad key on /models with 403; see the adapter's KDoc.
                    forbiddenMeansInvalidKey = service == Service.Requesty,
                )
            } + listOf(
                CloudflareModelDiscoveryAdapter(httpClient = httpClient, service = Service.Cloudflare),
                CloudflareModelDiscoveryAdapter(httpClient = httpClient, service = Service.CloudflareAIGateway),
            ),
        )
    }
}
