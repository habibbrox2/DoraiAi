package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ServiceDiscoveryCoverageTest {
    @Test
    fun `every service resolves to an adapter except Free and LiteRT`() {
        val registry = ModelDiscoveryRegistry.create(io.ktor.client.HttpClient())
        for (service in Service.all) {
            if (service == Service.Free || service == Service.LiteRT) {
                assertNull(registry.adapterFor(service), "${service.id} should have no adapter")
            } else {
                assertNotNull(registry.adapterFor(service), "${service.id} has no adapter")
            }
        }
    }
}