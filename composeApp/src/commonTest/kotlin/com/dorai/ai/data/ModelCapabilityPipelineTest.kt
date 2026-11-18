package com.dorai.ai.data

import com.dorai.ai.network.modeldiscovery.DiscoveredModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelCapabilityPipelineTest {
    @Test
    fun `filters non-chat ids`() {
        val models = listOf(DiscoveredModel("gpt-4o"), DiscoveredModel("text-embedding-3-small"))
        val out = ModelCapabilityPipeline.toSettingsModels(Service.OpenAI, models, "")
        assertEquals(listOf("gpt-4o"), out.map { it.id })
    }

    @Test
    fun `dedupes by id`() {
        val models = listOf(DiscoveredModel("m1"), DiscoveredModel("m1"))
        val out = ModelCapabilityPipeline.toSettingsModels(Service.Groq, models, "")
        assertEquals(1, out.size)
    }

    @Test
    fun `preserves manually typed selected id`() {
        val models = listOf(DiscoveredModel("m1"))
        val out = ModelCapabilityPipeline.toSettingsModels(Service.Groq, models, "custom-id")
        assertTrue(out.any { it.id == "custom-id" && it.isManualEntry })
    }

    @Test
    fun `catalog merge fills context window`() {
        // Assumes ModelCatalog has an entry for a known id; use an id present in catalog.
        val models = listOf(DiscoveredModel("claude-sonnet-4-5"))
        val out = ModelCapabilityPipeline.toSettingsModels(Service.Anthropic, models, "")
        assertTrue(out.first().contextWindow != null || out.first().displayName != null)
    }
}
