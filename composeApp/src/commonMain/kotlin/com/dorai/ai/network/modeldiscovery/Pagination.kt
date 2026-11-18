package com.dorai.ai.network.modeldiscovery

/**
 * One page of a paged provider model listing, normalized for [fetchAllPages].
 *
 * [totalPages] is the provider-reported page count, or `null` when the provider does not
 * report one (in which case [fetchAllPages] infers continuation from the page size).
 */
internal data class ProviderPage(
    val models: List<DiscoveredModel>,
    val totalPages: Int?,
)

/**
 * Fetches every page of a paged model listing by repeatedly calling [fetchPage].
 *
 * Ported from the pagination loop of the legacy `Requests.getOpenAICompatibleModels`
 * Cloudflare branch (`while (page <= totalPages && page <= 20)`), generalized for all
 * paged adapters:
 * - Stops at [maxPages] so a misbehaving provider cannot loop forever (Cloudflare: 20).
 * - Stops when [ProviderPage.totalPages] is known and the loop passed it.
 * - When the provider does not report a total, a page with fewer than [perPage] models
 *   (or an empty one) is the last page.
 * - Dedupes by [DiscoveredModel.id] at the end, matching the legacy
 *   `distinctBy { it.id }` behavior for providers whose ids repeat across pages.
 */
internal suspend fun fetchAllPages(
    perPage: Int = 100,
    maxPages: Int = 20,
    fetchPage: suspend (page: Int) -> ProviderPage,
): List<DiscoveredModel> {
    val allModels = mutableListOf<DiscoveredModel>()
    var page = 1
    var totalPages = 1
    while (page <= totalPages && page <= maxPages) {
        val result = fetchPage(page)
        allModels += result.models
        totalPages = result.totalPages ?: if (result.models.size >= perPage) page + 1 else page
        page++
    }
    return allModels.distinctBy { it.id }
}
