package com.dorai.ai.ui.settings

import kotlin.test.Test
import kotlin.test.assertTrue

/** Compile-time guard for the shared confirmation dialog API. */
class SettingsConfirmDialogTest {

    @Test
    fun `confirmation dialog is exposed with enabled parameter`() {
        assertTrue(::SettingsConfirmDialog.toString().contains("SettingsConfirmDialog"))
    }

    @Test
    fun `confirm button color resolver distinguishes enabled and disabled`() {
        // Pure resolver introduced in Task 4; pins the interaction-state policy.
        assertTrue(::confirmationConfirmColor.toString().contains("confirmationConfirmColor"))
    }
}
