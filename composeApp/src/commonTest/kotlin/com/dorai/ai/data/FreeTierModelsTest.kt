package com.dorai.ai.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression coverage for the keyless free-tier allowlists (the runtime source
 * of truth for Free badges, mirrored in `docs/knowledge/free-tier/`).
 */
class FreeTierModelsTest {

    @Test
    fun `openrouter allowlist has the curated seventeen ids`() {
        val count = FreeTierModels.openRouterFreeCount
        assertEquals(17, count, "OpenRouter free id count drifted — update the OKF bundle docs/knowledge/free-tier/ in the same change")
    }

    @Test
    fun `routing meta ids are free on openrouter`() {
        assertTrue(FreeTierModels.isFreeTier(Service.OpenRouter, "openrouter/auto"))
        assertTrue(FreeTierModels.isFreeTier(Service.OpenRouter, "openrouter/free"))
        assertTrue(FreeTierModels.isFreeTier(Service.OpenRouter, "auto"))
        // Case-insensitive matching for provider-returned ids.
        assertTrue(FreeTierModels.isFreeTier(Service.OpenRouter, "OpenRouter/Auto"))
    }

    @Test
    fun `free-ness is service-scoped`() {
        // The same weights are not free elsewhere.
        assertTrue(!FreeTierModels.isFreeTier(Service.Gemini, "auto"))
        assertTrue(!FreeTierModels.isFreeTier(Service.OllamaCloud, "openrouter/auto"))
    }

    @Test
    fun `ollama cloud allowlist has three models and normalizes cloud tags`() {
        assertTrue(FreeTierModels.isFreeTier(Service.OllamaCloud, "gpt-oss:20b"))
        assertTrue(FreeTierModels.isFreeTier(Service.OllamaCloud, "gemma4:31b"))
        assertTrue(FreeTierModels.isFreeTier(Service.OllamaCloud, "nemotron-3-nano:30b"))
        // Library-style aliases normalize onto API-style entries.
        assertTrue(FreeTierModels.isFreeTier(Service.OllamaCloud, "gpt-oss:20b-cloud"))
        assertTrue(FreeTierModels.isFreeTier(Service.OllamaCloud, "gemma4:31b-cloud"))
        assertTrue(!FreeTierModels.isFreeTier(Service.OllamaCloud, "llama4:latest"))
    }

    @Test
    fun `normalizeOllamaId strips cloud suffixes`() {
        assertEquals("gpt-oss:20b", FreeTierModels.normalizeOllamaId("gpt-oss:20b-cloud"))
        assertEquals("gemma4", FreeTierModels.normalizeOllamaId("gemma4:cloud"))
        assertEquals("nemotron-3-nano:30b", FreeTierModels.normalizeOllamaId("NEMOTRON-3-NANO:30B"))
        // Ids without a suffix survive unchanged.
        assertEquals("gemma4", FreeTierModels.normalizeOllamaId("gemma4"))
    }
}
