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
         * Builds the production registry. Adapters are not wired yet — coverage is added in
         * Task 8, so this returns an empty registry for now.
         */
        fun create(httpClient: HttpClient): ModelDiscoveryRegistry = ModelDiscoveryRegistry(emptyList())
    }
}
