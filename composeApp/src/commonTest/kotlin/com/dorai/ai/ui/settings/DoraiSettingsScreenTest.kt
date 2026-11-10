package com.dorai.ai.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class DoraiSettingsScreenTest {

    @Test
    fun `nine categories are strictly ordered`() {
        val expected = listOf(
            "general",
            "appearance",
            "agent",
            "providers",
            "tools",
            "mcp",
            "github",
            "sandbox",
            "account",
        )
        assertEquals(expected, SettingsCategory.entries.map { it.id })
    }
}
