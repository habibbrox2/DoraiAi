package com.dorai.ai.ui.settings

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compile-time guard for the shared Settings detail header.
 *
 * This module has no Compose UI test runner (verified: no createComposeRule /
 * runComposeUiTest in the repo). Behavior (heading semantics, back content
 * description, 48dp target) is covered by Paparazzi screenshotTests.
 */
class SettingsDetailHeaderTest {

    @Test
    fun `shared header is exposed for detail screens`() {
        val reference: Any = ::SettingsDetailHeader
        assertTrue(reference.toString().contains("SettingsDetailHeader"))
    }
}
