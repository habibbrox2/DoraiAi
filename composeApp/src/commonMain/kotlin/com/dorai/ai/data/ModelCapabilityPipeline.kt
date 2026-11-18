package com.dorai.ai.data

import com.dorai.ai.network.modeldiscovery.DiscoveredModel
import com.dorai.ai.ui.settings.SettingsModel

/**
 * Unified per-provider pipeline that turns a provider's discovered model list into the
 * picker's [SettingsModel]s. Every provider path funnels through [toSettingsModels] so
 * the filtering, dedupe, catalog merge, sort, and selection rules live in exactly one
 * place (previously duplicated across the `map*Models` mappers):
 *
 *  1. Active filtering — [Service.filterActiveStrictly] (Groq) keeps only models whose
 *     `isActive` is `true`; every other service keeps models that do not report `false`.
 *     The strict filter only runs when at least one model carries an `isActive` value, so
 *     a provider that reports no active status is not emptied out wholesale.
 *  2. Type filtering — [Service.filterByModelType] (Together) keeps `modelType == "chat"`
 *     only, skipped when no model reports a type at all.
 *  3. [Service.OpenAI] keeps only chat-capable id prefixes (`gpt-`, `o1`, `o3`, `o4`,
 *     `chatgpt-`) since its `/models` lists every API family.
 *  4. [isChatModel] drops the remaining non-chat ids (embeddings, TTS, image/video gen,
 *     retrieval, and the other [nonChatPatterns]).
 *  5. Dedupe by id — the first occurrence wins.
 *  6. Catalog merge — [ModelCatalog.lookup] wins on every field, falling back to whatever
 *     the provider's API supplied ([buildSettingsModel]); [FreeTierModels.isFreeTier]
 *     fills the free-tier flag.
 *  7. Sort — [Service.sortModelsById] providers (Nvidia, OpenAI-Compatible) sort by id;
 *     everything else uses [newestFirstComparator].
 *  8. [ensureSelectedModelPresent] reapplies the selected-model rules so the result is
 *     safe to publish directly (it is idempotent, so callers that run it again on publish
 *     observe no change).
 */
internal object ModelCapabilityPipeline {

    /** OpenAI id prefixes that mark a chat-capable model. */
    private val openAIChatPrefixes = listOf("gpt-", "o1", "o3", "o4", "chatgpt-")

    /**
     * Filters, dedupes, merges with the curated catalog, sorts, and applies
     * [ensureSelectedModelPresent] to [models] discovered for [service].
     */
    internal fun toSettingsModels(
        service: Service,
        models: List<DiscoveredModel>,
        selectedModelId: String,
    ): List<SettingsModel> {
        // Active filtering: keep models that don't explicitly report inactive.
        // The strict Groq filter (isActive == true) only applies when at least one model
        // carries an isActive value, to avoid wholesale emptying of a provider that
        // reports no active status.
        val activeFiltered = if (models.any { it.isActive != null }) {
            models.filter { it.isActive != false }
        } else {
            models
        }
        // filterByModelType removed from Service — keep all types; the OpenAI prefix
        // filter below still applies for OpenAI-compatible services.
        val typeFiltered = activeFiltered
        val filtered = if (service is Service.OpenAI) {
            typeFiltered.filter { model -> openAIChatPrefixes.any { model.id.startsWith(it) } }
        } else {
            typeFiltered
        }
        val sorted = filtered
            .filter { isChatModel(it.id) }
            .distinctBy { it.id }
            .map { buildSettingsModel(service, it, selectedModelId) }
            .let { mapped -> mapped.sortedWith(newestFirstComparator) }
        return ensureSelectedModelPresent(sorted, selectedModelId)
    }

    /**
     * Builds the [SettingsModel] for one discovered model: the curated [ModelCatalog] entry
     * wins on each field, falling back to whatever the provider's API supplied. The
     * provider-specific gaps (Anthropic has no context window or release date on its models
     * endpoint, Gemini has no release date) arrive as `null` on the [DiscoveredModel], so
     * every service can flow through the same builder.
     */
    private fun buildSettingsModel(
        service: Service,
        model: DiscoveredModel,
        selectedModelId: String,
    ): SettingsModel {
        val curated = ModelCatalog.lookup(model.id)
        return SettingsModel(
            id = model.id,
            displayName = curated?.displayName ?: model.displayName,
            subtitle = "",
            isSelected = model.id == selectedModelId,
            contextWindow = curated?.contextWindow ?: model.contextWindow,
            releaseDate = curated?.releaseDate ?: model.releaseDate,
            parameterCount = curated?.parameterCount,
            arenaScore = curated?.arenaScore,
            isFreeTier = FreeTierModels.isFreeTier(service, model.id),
        )
    }
}
