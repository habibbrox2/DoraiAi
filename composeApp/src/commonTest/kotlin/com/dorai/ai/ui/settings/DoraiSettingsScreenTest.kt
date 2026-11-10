package com.dorai.ai.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class DoraiSettingsScreenTest {

    @Test
    fun `eight categories are strictly ordered`() {
        val expected = listOf(
            "general",
            "appearance",
            "agent",
            "providers",
            "tools",
            "mcp",
            "github",
            "account",
        )
        assertEquals(expected, SettingsCategory.entries.map { it.id })
    }
}
