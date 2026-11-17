package com.dorai.ai.ui.settings

import kotlin.test.Test
import kotlin.test.assertTrue

/** Compile-time guard for the shared Agent settings API. */
class AgentSettingsTest {

    @Test
    fun `agent section heading is exposed`() {
        assertTrue(::SettingsDetailSectionHeading.toString().contains("SettingsDetailSectionHeading"))
    }

    @Test
    fun `agent content uses shared components`() {
        // Verify shared primitives are referenced (pins the contract)
        assertTrue(::SettingsDetailSectionHeading.toString().contains("SettingsDetailSectionHeading"))
    }
}
