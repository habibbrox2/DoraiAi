package com.dorai.ai.ui.settings

import com.dorai.ai.ui.components.v2.ActionButtonVariant
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compile-time guards for the shared settings row primitives.
 * Visual/a11y behavior (role, 48dp target, semantics) is covered by Paparazzi
 * settings goldens; these tests pin the public API that detail screens consume.
 */
class SettingsRowsTest {

    @Test
    fun `shared preference row is exposed`() {
        assertTrue(::SettingsPreferenceRow.toString().contains("SettingsPreferenceRow"))
    }

    @Test
    fun `shared switch row is exposed`() {
        assertTrue(::SettingsSwitchRow.toString().contains("SettingsSwitchRow"))
    }

    @Test
    fun `shared action row is exposed with action variant`() {
        assertTrue(::SettingsActionRow.toString().contains("SettingsActionRow"))
        // The variant type is consumed by SettingsActionRow; referencing it pins the contract.
        assertTrue(ActionButtonVariant.SECONDARY.name == "SECONDARY")
    }
}
