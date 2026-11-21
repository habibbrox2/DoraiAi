package com.dorai.ai.ui.settings

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compile-time guard for the responsive Settings detail shell.
 *
 * Behavior (compact vs medium+ layout) is verified by Paparazzi screenshot tests;
 * this test pins the public API that screens adopt for responsive layout.
 */
class SettingsResponsiveShellTest {

    @Test
    fun `responsive shell is exposed for detail screens`() {
        // Pinning the signature proves the symbol exists and is usable from other screens
        assertTrue(::SettingsDetailShell.toString().contains("SettingsDetailShell"))
    }
}
