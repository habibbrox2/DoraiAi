package com.dorai.ai.network.modeldiscovery

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PaginationTest {
    @Test
    fun `stops after totalPages`() = runTest {
        val pages = listOf(
            ProviderPage(listOf(DiscoveredModel("a")), totalPages = 2),
            ProviderPage(listOf(DiscoveredModel("b")), totalPages = 2),
        )
        val result = fetchAllPages(fetchPage = { pages[it - 1] })
        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun `continues when totalPages null and page full`() = runTest {
        val pages = listOf(
            ProviderPage(List(100) { DiscoveredModel("m$it") }, totalPages = null),
            ProviderPage(listOf(DiscoveredModel("last")), totalPages = null),
        )
        val result = fetchAllPages(fetchPage = { pages.getOrElse(it - 1) { ProviderPage(emptyList(), null) } })
        assertEquals(101, result.size)
    }

    @Test
    fun `caps at maxPages`() = runTest {
        // Brief's original ids ("p${it}-${it}") repeat across pages and collapse under the
        // dedupe-by-id requirement; page-distinct ids keep the 2x100 intent of this test.
        val result = fetchAllPages(
            maxPages = 2,
            fetchPage = { page -> ProviderPage(List(100) { DiscoveredModel("p$page-$it") }, totalPages = null) },
        )
        assertEquals(200, result.size)
    }
}
