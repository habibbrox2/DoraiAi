package com.dorai.ai.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * "Auto" selects every model in the Model List so a request falls back across all of them.
 * The marker is a selector, never a model id that could reach a provider.
 */
class AutoModelSelectionTest {

    @Test
    fun `auto off keeps the request pinned to the stored selection`() {
        val ordered = AutoModelSelection.ordered(
            selectedModelId = "gpt-4o-mini",
            autoEnabled = false,
            listedModelIds = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1"),
        )
        assertEquals(listOf("gpt-4o-mini"), ordered)
    }

    @Test
    fun `auto walks the whole model list starting with the selection`() {
        val ordered = AutoModelSelection.ordered(
            selectedModelId = "gpt-4o",
            autoEnabled = true,
            listedModelIds = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1"),
        )
        assertEquals(listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1"), ordered)
    }

    @Test
    fun `auto still tries the selection when it is not in the list`() {
        val ordered = AutoModelSelection.ordered(
            selectedModelId = "my-custom-model",
            autoEnabled = true,
            listedModelIds = listOf("listed-a", "listed-b"),
        )
        assertEquals(listOf("my-custom-model", "listed-a", "listed-b"), ordered)
    }

    @Test
    fun `auto falls back to list order when nothing is selected yet`() {
        val ordered = AutoModelSelection.ordered(
            selectedModelId = "",
            autoEnabled = true,
            listedModelIds = listOf("listed-a", "listed-b"),
        )
        assertEquals(listOf("listed-a", "listed-b"), ordered)
    }

    @Test
    fun `auto with no exposed list behaves like a pinned selection`() {
        val ordered = AutoModelSelection.ordered(
            selectedModelId = "gpt-4o-mini",
            autoEnabled = true,
            listedModelIds = emptyList(),
        )
        assertEquals(listOf("gpt-4o-mini"), ordered)
    }

    @Test
    fun `auto never repeats a model and ignores blank ids`() {
        val ordered = AutoModelSelection.ordered(
            selectedModelId = " listed-a ",
            autoEnabled = true,
            listedModelIds = listOf("listed-a", "", "  ", "listed-b", "listed-a"),
        )
        assertEquals(listOf("listed-a", "listed-b"), ordered)
    }

    @Test
    fun `the auto marker is not a model id`() {
        // The walk must never hand the marker itself to a provider as a model name.
        assertFalse(AutoModelSelection.ID.isBlank())
        assertTrue(AutoModelSelection.ID.startsWith("__"), "opaque marker, unlikely to collide with a real model id")
    }
}
