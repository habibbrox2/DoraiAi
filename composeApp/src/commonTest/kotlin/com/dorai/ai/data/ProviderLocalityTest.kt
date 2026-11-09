package com.dorai.ai.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression coverage for provider execution-location classification — the
 * Local/Cloud badge must reflect where inference actually runs, decided by the
 * endpoint the user configured (OpenAI-Compatible) or the service itself.
 */
class ProviderLocalityTest {

    // ------------------------------------------------------------------
    // Service-declared locations
    // ------------------------------------------------------------------

    @Test
    fun `litert is always local regardless of base url`() {
        assertEquals(
            ExecutionLocation.LOCAL,
            Service.resolveExecutionLocation(Service.LiteRT, "https://attacker.example.com"),
        )
        assertEquals(
            ExecutionLocation.LOCAL,
            Service.resolveExecutionLocation(Service.LiteRT, null),
        )
    }

    @Test
    fun `hosted providers are always cloud`() {
        listOf(
            Service.OpenRouter to "http://localhost:11434/v1",
            Service.Gemini to "http://127.0.0.1:8080",
            Service.Anthropic to "http://[::1]:9000",
            Service.Free to null,
        ).forEach { (service, baseUrl) ->
            assertEquals(
                ExecutionLocation.CLOUD,
                Service.resolveExecutionLocation(service, baseUrl),
                "${service.id} with $baseUrl must not be classified local",
            )
        }
    }

    // ------------------------------------------------------------------
    // OpenAI-Compatible: the endpoint decides
    // ------------------------------------------------------------------

    @Test
    fun `openai-compatible loopback endpoints are local`() {
        listOf(
            null, // default falls back to the built-in Ollama localhost URL
            "",
            "   ",
            "http://localhost:11434/v1",
            "http://127.0.0.1:11434/v1",
            "http://[::1]:11434/v1",
            "http://192.168.1.50:1234/v1", // LAN LM Studio
            "http://10.0.0.8:8080/v1",
            "http://printer.local/v1",
        ).forEach { baseUrl ->
            assertEquals(
                ExecutionLocation.LOCAL,
                Service.resolveExecutionLocation(Service.OpenAICompatible, baseUrl),
                "base URL '$baseUrl' must classify as local",
            )
        }
    }

    @Test
    fun `openai-compatible public endpoints are cloud`() {
        listOf(
            "https://api.openai.com/v1",
            "https://gateway.example.com/v1",
            "http://93.184.216.34:11434/v1",
            "not a url", // malformed — must NOT silently inherit the localhost default
        ).forEach { baseUrl ->
            assertEquals(
                ExecutionLocation.CLOUD,
                Service.resolveExecutionLocation(Service.OpenAICompatible, baseUrl),
                "base URL '$baseUrl' must classify as cloud",
            )
        }
    }

    // ------------------------------------------------------------------
    // ServiceEntry propagation
    // ------------------------------------------------------------------

    @Test
    fun `service entries default to cloud so stale constructors stay safe`() {
        val entry = ServiceEntry(
            instanceId = "i1",
            serviceId = "openrouter",
            serviceName = "OpenRouter",
            modelId = "auto",
            icon = Service.OpenRouter.icon,
        )
        assertEquals(ExecutionLocation.CLOUD, entry.executionLocation)
    }

    @Test
    fun `service ids resolve to their services`() {
        assertEquals(Service.Free, Service.fromIdOrNull("free"))
        assertEquals(Service.OpenAICompatible, Service.fromIdOrNull("openai-compatible"))
        assertEquals(null, Service.fromIdOrNull("totally-unknown-provider"))
        // Legacy behavior preserved: fromId falls back to Free.
        assertEquals(Service.Free, Service.fromId("totally-unknown-provider"))
    }

    @Test
    fun `all services declare a valid execution location`() {
        Service.all.forEach { service ->
            when (service.executionLocation) {
                ExecutionLocation.LOCAL -> assertTrue(service.isOnDevice, "${service.id} claims LOCAL but is not on-device")
                ExecutionLocation.CLOUD, ExecutionLocation.USER_CONFIGURED -> assertFalse(service.isOnDevice, "${service.id} is on-device but not LOCAL")
            }
        }
    }
}
