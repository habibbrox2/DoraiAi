package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelDiscoveryRegistryTest {
    @Test
    fun `empty registry resolves nothing`() {
        val registry = ModelDiscoveryRegistry(emptyList())
        assertNull(registry.adapterFor(Service.Groq))
    }

    @Test
    fun `create returns registry for all services without adapters in task 1`() {
        val registry = ModelDiscoveryRegistry.create(io.ktor.client.HttpClient())
        // Task 1: adapters not wired yet — coverage is added in Task 8.
        assertEquals(null, registry.adapterFor(Service.Free))
    }
}
