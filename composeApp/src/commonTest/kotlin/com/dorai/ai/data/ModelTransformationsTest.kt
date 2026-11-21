package com.dorai.ai.data

import com.dorai.ai.ui.settings.ModelSortOption
import com.dorai.ai.ui.settings.SettingsModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelTransformationsTest {

    // --- Unified newest-first sort ---

    @Test
    fun `newestFirstComparator sorts by release date then context window`() {
        val models = listOf(
            SettingsModel(id = "older-small", subtitle = "", releaseDate = "2024-01", contextWindow = 8_000),
            SettingsModel(id = "newest-small", subtitle = "", releaseDate = "2025-11", contextWindow = 8_000),
            SettingsModel(id = "newest-large", subtitle = "", releaseDate = "2025-11", contextWindow = 200_000),
            SettingsModel(id = "no-date", subtitle = ""),
        )
        val sorted = models.sortedWith(newestFirstComparator)
        // same date -> larger ctx wins
        assertEquals("newest-large", sorted[0].id)
        assertEquals("newest-small", sorted[1].id)
        assertEquals("older-small", sorted[2].id)
        // null date goes last
        assertEquals("no-date", sorted[3].id)
    }

    // --- Model picker sort: Alpha (free first, then A-Z) ---

    @Test
    fun `alphaSort puts free models first then sorts A-Z by id within each group`() {
        val models = listOf(
            SettingsModel(id = "z-paid", subtitle = "", isFreeTier = false),
            SettingsModel(id = "a-free", subtitle = "", isFreeTier = true),
            SettingsModel(id = "m-paid", subtitle = "", isFreeTier = false),
            SettingsModel(id = "b-free", subtitle = "", isFreeTier = true),
        )
        val sorted = models.sortedWith(ModelSortOption.Alpha.comparator)
        assertEquals(listOf("a-free", "b-free", "m-paid", "z-paid"), sorted.map { it.id })
    }

    @Test
    fun `alphaSort sorts A-Z by id when all models share the same free tier`() {
        val models = listOf(
            SettingsModel(id = "zeta", subtitle = "", isFreeTier = true),
            SettingsModel(id = "alpha", subtitle = "", isFreeTier = true),
            SettingsModel(id = "mid", subtitle = "", isFreeTier = true),
        )
        val sorted = models.sortedWith(ModelSortOption.Alpha.comparator)
        assertEquals(listOf("alpha", "mid", "zeta"), sorted.map { it.id })
    }

    @Test
    fun `alphaSort uses displayName as tiebreaker when ids are equal`() {
        val models = listOf(
            SettingsModel(id = "same-id", subtitle = "", displayName = "Zebra", isFreeTier = false),
            SettingsModel(id = "same-id", subtitle = "", displayName = "Alpha", isFreeTier = false),
        )
        val sorted = models.sortedWith(ModelSortOption.Alpha.comparator)
        assertEquals("Alpha", sorted[0].displayName)
        assertEquals("Zebra", sorted[1].displayName)
    }

    // --- ensureSelectedModelPresent ---

    @Test
    fun `ensureSelectedModelPresent prepends custom id missing from list`() {
        val models = listOf(
            SettingsModel(id = "listed-a", subtitle = "", isSelected = false),
            SettingsModel(id = "listed-b", subtitle = "", isSelected = true),
        )
        val result = ensureSelectedModelPresent(models, selectedModelId = "glm-4.7-flash")
        assertEquals(3, result.size)
        assertEquals("glm-4.7-flash", result[0].id)
        assertTrue(result[0].isSelected)
        assertTrue(result[0].isManualEntry)
        assertTrue(result.drop(1).none { it.isSelected })
        assertTrue(result.drop(1).none { it.isManualEntry })
    }

    @Test
    fun `ensureSelectedModelPresent replaces prior manual draft instead of accumulating`() {
        val models = listOf(
            SettingsModel(id = "a", subtitle = "", isSelected = false, isManualEntry = true),
            SettingsModel(id = "as", subtitle = "", isSelected = true, isManualEntry = true),
            SettingsModel(id = "listed", subtitle = "", isSelected = false),
        )
        val result = ensureSelectedModelPresent(models, selectedModelId = "asd")
        assertEquals(2, result.size)
        assertEquals("asd", result[0].id)
        assertTrue(result[0].isSelected)
        assertTrue(result[0].isManualEntry)
        assertEquals("listed", result[1].id)
        assertFalse(result[1].isManualEntry)
        assertFalse(result[1].isSelected)
    }

    @Test
    fun `ensureSelectedModelPresent marks existing id selected without duplicating`() {
        val models = listOf(
            SettingsModel(id = "m1", subtitle = "", isSelected = true),
            SettingsModel(id = "m2", subtitle = "", isSelected = false),
        )
        val result = ensureSelectedModelPresent(models, selectedModelId = "m2")
        assertEquals(2, result.size)
        assertFalse(result.first { it.id == "m1" }.isSelected)
        assertTrue(result.first { it.id == "m2" }.isSelected)
        assertTrue(result.none { it.isManualEntry })
    }

    @Test
    fun `ensureSelectedModelPresent drops manual entry when provider list contains the id`() {
        val models = listOf(
            SettingsModel(id = "m1", subtitle = "", isSelected = true, isManualEntry = true),
            SettingsModel(id = "m1", subtitle = "", isSelected = false),
        )
        // After filter, provider has m1 — should not keep a second manual row.
        val result = ensureSelectedModelPresent(
            models.filter { !it.isManualEntry } +
                listOf(SettingsModel(id = "m1", subtitle = "", isManualEntry = true)),
            selectedModelId = "m1",
        )
        assertEquals(1, result.count { it.id == "m1" })
        assertTrue(result.first { it.id == "m1" }.isSelected)
        assertFalse(result.first { it.id == "m1" }.isManualEntry)
    }

    @Test
    fun `ensureSelectedModelPresent with empty id clears selection and manual drafts`() {
        val models = listOf(
            SettingsModel(id = "draft", subtitle = "", isSelected = true, isManualEntry = true),
            SettingsModel(id = "m1", subtitle = "", isSelected = false),
            SettingsModel(id = "m2", subtitle = "", isSelected = false),
        )
        val result = ensureSelectedModelPresent(models, selectedModelId = "")
        assertEquals(2, result.size)
        assertTrue(result.none { it.isSelected })
        assertTrue(result.none { it.isManualEntry })
    }

    @Test
    fun `ensureSelectedModelPresent injects into empty list`() {
        val result = ensureSelectedModelPresent(emptyList(), selectedModelId = "custom-only")
        assertEquals(1, result.size)
        assertEquals("custom-only", result[0].id)
        assertTrue(result[0].isSelected)
        assertTrue(result[0].isManualEntry)
    }
}
