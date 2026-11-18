package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.network.ServiceCredentials
import io.ktor.client.HttpClient

/**
 * Model discovery for Perplexity — Sonar exposes no authenticated `/models` list (the
 * public one is Agent-API only), so the picker is populated from the curated
 * [Service.Perplexity.defaultModels] definitions. Auth is evaluated first via the
 * chat-completions key probe ([DiscoveryHttp.validatePerplexityApiKey]): an invalid or
 * blank key throws before any model is returned, mirroring the legacy repository path
 * that validated the key and then seeded the list from the curated defaults.
 */
class PerplexityModelDiscoveryAdapter(
    private val httpClient: HttpClient,
    override val service: Service = Service.Perplexity,
) : ModelDiscoveryAdapter {

    override suspend fun discoverModels(credentials: ServiceCredentials): List<DiscoveredModel> {
        DiscoveryHttp.validatePerplexityApiKey(httpClient, credentials).getOrThrow()
        return service.defaultModels.map { definition ->
            DiscoveredModel(id = definition.id, displayName = definition.subtitle)
        }
    }
}
