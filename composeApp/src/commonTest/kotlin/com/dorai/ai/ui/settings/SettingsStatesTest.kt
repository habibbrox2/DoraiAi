package com.dorai.ai.ui.settings

import kotlin.test.Test
import kotlin.test.assertTrue

/** Compile-time guards for the shared settings state components. */
class SettingsStatesTest {

    @Test
    fun `status card is exposed`() {
        assertTrue(::SettingsStatusCard.toString().contains("SettingsStatusCard"))
    }

    @Test
    fun `info card is exposed`() {
        assertTrue(::SettingsInfoCard.toString().contains("SettingsInfoCard"))
    }

    @Test
    fun `empty state is exposed`() {
        assertTrue(::SettingsEmptyState.toString().contains("SettingsEmptyState"))
    }

    @Test
    fun `error state is exposed`() {
        assertTrue(::SettingsErrorState.toString().contains("SettingsErrorState"))
    }

    @Test
    fun `loading state is exposed`() {
        assertTrue(::SettingsLoadingState.toString().contains("SettingsLoadingState"))
    }
}
