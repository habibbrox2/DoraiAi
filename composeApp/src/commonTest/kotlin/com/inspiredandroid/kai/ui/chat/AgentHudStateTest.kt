package com.inspiredandroid.kai.ui.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.chat.composables.isCompactAgentHud
import com.inspiredandroid.kai.ui.theme.DesignTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentHudStateTest {

    private fun resolveOrbColor(
        isGenerating: Boolean,
        isShellExecuting: Boolean,
        isSpeaking: Boolean,
    ): Color = when {
        isShellExecuting -> DesignTokens.Warning
        isGenerating -> DesignTokens.AgentViolet
        isSpeaking -> DesignTokens.PrimaryCyan
        else -> DesignTokens.BrandCyan
    }

    @Test
    fun `idle state returns brand cyan color`() {
        val color = resolveOrbColor(isGenerating = false, isShellExecuting = false, isSpeaking = false)
        assertEquals(DesignTokens.BrandCyan, color)
    }

    @Test
    fun `generating state prioritizes agent violet color`() {
        val color = resolveOrbColor(isGenerating = true, isShellExecuting = false, isSpeaking = false)
        assertEquals(DesignTokens.AgentViolet, color)
    }

    @Test
    fun `shell executing state prioritizes warning amber color`() {
        val color = resolveOrbColor(isGenerating = false, isShellExecuting = true, isSpeaking = false)
        assertEquals(DesignTokens.Warning, color)
    }

    @Test
    fun `speaking state uses primary cyan color`() {
        val color = resolveOrbColor(isGenerating = false, isShellExecuting = false, isSpeaking = true)
        assertEquals(DesignTokens.PrimaryCyan, color)
    }

    @Test
    fun `agent HUD switches to compact layout below 720dp`() {
        assertTrue(isCompactAgentHud(719.dp))
        assertFalse(isCompactAgentHud(720.dp))
    }
}
