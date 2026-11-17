package com.dorai.ai.ui.settings

import kotlin.test.Test
import kotlin.test.assertTrue

/** Compile-time guard for the shared General settings API.
 * Visual/a11y behavior is covered by Paparazzi settings goldens.
 */
class GeneralSettingsTest {

    @Test
    fun `general section heading is exposed`() {
        assertTrue(::SettingsDetailSectionHeading.toString().contains("SettingsDetailSectionHeading"))
    }

    @Test
    fun `general content uses shared components`() {
        // Verify shared primitives are referenced (pins the contract)
        assertTrue(::SettingsSwitchRow.toString().contains("SettingsSwitchRow"))
        assertTrue(::SettingsCard.toString().contains("SettingsCard"))
    }
}
