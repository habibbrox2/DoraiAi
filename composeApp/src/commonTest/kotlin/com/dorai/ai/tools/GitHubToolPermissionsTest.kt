package com.dorai.ai.tools

import com.dorai.ai.data.AppSettings
import com.dorai.ai.data.ToolPermissionManager
import com.dorai.ai.data.ToolPermissionPolicy
import com.dorai.ai.data.ToolRiskLevel
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the default policy for the GitHub write tools: all three require an explicit
 * approval prompt (OTHER risk / ASK_ALWAYS), scoped to the target repository.
 */
class GitHubToolPermissionsTest {

    private fun freshManager(): ToolPermissionManager = ToolPermissionManager(AppSettings(MapSettings()))

    @Test
    fun `github push requires confirmation`() {
        assertTrue(
            freshManager().shouldAskForPermission("github_push", mapOf("repo" to "a/b")),
        )
    }

    @Test
    fun `github commit requires confirmation`() {
        assertTrue(
            freshManager().shouldAskForPermission("github_commit", mapOf("repo" to "a/b")),
        )
    }

    @Test
    fun `github create pull request requires confirmation`() {
        assertTrue(
            freshManager().shouldAskForPermission("github_create_pull_request", mapOf("repo" to "a/b")),
        )
    }

    @Test
    fun `github write tools classify as OTHER risk`() {
        val manager = freshManager()
        assertEquals(ToolRiskLevel.OTHER, manager.classifyToolRisk("github_commit"))
        assertEquals(ToolRiskLevel.OTHER, manager.classifyToolRisk("github_push"))
        assertEquals(ToolRiskLevel.OTHER, manager.classifyToolRisk("github_create_pull_request"))
    }

    @Test
    fun `github push scopes to the repository fullName`() {
        val manager = freshManager()
        assertEquals("owner/repo", manager.getScope("github_push", mapOf("fullName" to "owner/repo")))
        // `fullName` wins when both are provided
        assertEquals(
            "owner/repo",
            manager.getScope("github_push", mapOf("fullName" to "owner/repo", "repo" to "x/y")),
        )
        assertEquals("owner/repo", manager.getScope("github_push", mapOf("repo" to "owner/repo")))
    }

    @Test
    fun `stored always-allow for the exact repository scope suppresses the prompt`() {
        val settings = AppSettings(MapSettings())
        val manager = ToolPermissionManager(settings)
        val fullName = "owner/repo"
        val scope = manager.getScope("github_push", mapOf("fullName" to fullName))
        manager.recordAlwaysAllow("github_push", scope)

        assertFalse(manager.shouldAskForPermission("github_push", mapOf("fullName" to fullName)))
        // A different repository must still ask
        assertTrue(manager.shouldAskForPermission("github_push", mapOf("fullName" to "owner/other")))
    }

    @Test
    fun `stored permission policy round-trips`() {
        val settings = AppSettings(MapSettings())
        val scope = "owner/repo"
        settings.setToolPermission(
            com.dorai.ai.data.ToolPermission(
                toolName = "github_push",
                scope = scope,
                policy = ToolPermissionPolicy.ASK_ALWAYS,
            ),
        )
        assertEquals(ToolPermissionPolicy.ASK_ALWAYS, settings.getToolPermission("github_push", scope)?.policy)
    }
}
