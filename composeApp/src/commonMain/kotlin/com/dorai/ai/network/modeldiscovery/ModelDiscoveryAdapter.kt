package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.network.ServiceCredentials

/**
 * A model fetched from a provider's model-discovery endpoint, before it is merged into the
 * app's [com.dorai.ai.data.ModelCatalog].
 *
 * Optional fields are `null` when the provider does not report them; adapters must not
 * invent values.
 */
data class DiscoveredModel(
    val id: String,
    val displayName: String? = null,
    val contextWindow: Long? = null,
    val releaseDate: String? = null,
    val isActive: Boolean? = null,
    val modelType: String? = null,
)

/**
 * Per-provider model discovery. Each adapter knows how to turn [ServiceCredentials] for
 * [service] into that provider's list of models, replacing the per-service branches that
 * previously lived in the shared listing path.
 */
interface ModelDiscoveryAdapter {
    /** The single [Service] this adapter resolves models for. */
    val service: Service

    /**
     * Fetches the provider's models using the supplied [credentials]. Implementations
     * perform their own network I/O with the client they were constructed with and return
     * an empty list when the provider exposes no discovery endpoint.
     */
    suspend fun discoverModels(credentials: ServiceCredentials): List<DiscoveredModel>
}
