package com.dorai.ai.ui.settings

import com.dorai.ai.ui.components.CapabilityCountCard
import kotlin.test.Test
import kotlin.test.assertTrue

/** Compile-time guard for the shared Providers (AI Services) settings API. */
class ServicesSettingsTest {

    @Test
    fun `services section heading is exposed`() {
        assertTrue(::SettingsDetailSectionHeading.toString().contains("SettingsDetailSectionHeading"))
    }

    @Test
    fun `services content uses shared components`() {
        assertTrue(::SettingsEmptyState.toString().contains("SettingsEmptyState"))
        assertTrue(::CapabilityCountCard.toString().contains("CapabilityCountCard"))
    }
}
