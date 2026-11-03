package com.inspiredandroid.kai.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsTabScrollTest {

    @Test
    fun `visible tab does not move the scroll position`() {
        assertNull(
            settingsTabScrollTarget(
                currentScroll = 120,
                viewportWidth = 300,
                maxScroll = 600,
                itemStart = 160,
                itemEnd = 300,
            ),
        )
    }

    @Test
    fun `left clipped tab scrolls back by its clipped distance`() {
        assertEquals(
            80,
            settingsTabScrollTarget(
                currentScroll = 120,
                viewportWidth = 300,
                maxScroll = 600,
                itemStart = 80,
                itemEnd = 220,
            ) ?: error("Expected scroll target"),
        )
    }

    @Test
    fun `right clipped tab scrolls forward to its trailing edge`() {
        assertEquals(
            160,
            settingsTabScrollTarget(
                currentScroll = 120,
                viewportWidth = 300,
                maxScroll = 600,
                itemStart = 340,
                itemEnd = 460,
            ) ?: error("Expected scroll target"),
        )
    }

    @Test
    fun `scroll target is clamped to available range`() {
        assertEquals(
            150,
            settingsTabScrollTarget(
                currentScroll = 120,
                viewportWidth = 300,
                maxScroll = 150,
                itemStart = 340,
                itemEnd = 460,
            ) ?: error("Expected scroll target"),
        )
    }

    @Test
    fun `unmeasured viewport does not request scrolling`() {
        assertNull(
            settingsTabScrollTarget(
                currentScroll = 0,
                viewportWidth = 0,
                maxScroll = 500,
                itemStart = 200,
                itemEnd = 400,
            ),
        )
    }
}
