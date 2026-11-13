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
 * Pins the MCP-only approval policy: NO non-MCP tool asks the user, regardless
 * of its risk classification or stored permission scope. Only MCP connector
 * tools prompt (verified manually / via integration coverage — a network-bound
 * McpServerManager is not mockable here).
 */
class GitHubToolPermissionsTest {

    private fun freshManager(): ToolPermissionManager = ToolPermissionManager(AppSettings(MapSettings()))

    @Test
    fun `github push runs without confirmation`() {
        assertFalse(
            freshManager().shouldAskForPermission("github_push", mapOf("repo" to "a/b")),
        )
    }

    @Test
    fun `github commit runs without confirmation`() {
        assertFalse(
            freshManager().shouldAskForPermission("github_commit", mapOf("repo" to "a/b")),
        )
    }

    @Test
    fun `github create pull request runs without confirmation`() {
        assertFalse(
            freshManager().shouldAskForPermission("github_create_pull_request", mapOf("repo" to "a/b")),
        )
    }

    @Test
    fun `no non-MCP tool asks regardless of stored permission state`() {
        val settings = AppSettings(MapSettings())
        val manager = ToolPermissionManager(settings)
        val scope = manager.getScope("github_push", mapOf("fullName" to "owner/repo"))
        // ALLOW_ONCE stored absent / mismatched scope: still no prompt for non-MCP.
        assertFalse(manager.shouldAskForPermission("github_push", mapOf("fullName" to "owner/repo")))
        assertFalse(manager.shouldAskForPermission("execute_shell_command", emptyMap()))
        assertFalse(manager.shouldAskForPermission("write_file", mapOf("path" to "/tmp/x")))
        assertFalse(manager.shouldAskForPermission("delete_file", mapOf("path" to "/tmp/x")))
        assertFalse(manager.shouldAskForPermission("web_search", mapOf("query" to "x")))
        assertFalse(manager.shouldAskForPermission("dial_phone", mapOf("number" to "100")))
        // Even with an always-allow record scoped to a DIFFERENT repo, non-MCP asks nothing.
        manager.recordAlwaysAllow("github_push", "owner/other")
        assertFalse(manager.shouldAskForPermission("github_push", mapOf("fullName" to "owner/repo")))
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
    fun `stored permission policy round-trips`() {
        val settings = AppSettings(MapSettings())
        val scope = "owner/repo"
        settings.setToolPermission(
            com.dorai.ai.data.ToolPermission(
                toolName = "github_push",
                scope = scope,
                policy = ToolPermissionPolicy.ALWAYS_ALLOW,
            ),
        )

        val granted = settings.getToolPermission("github_push", scope)
        assertEquals(ToolPermissionPolicy.ALWAYS_ALLOW, granted?.policy)
    }
}
