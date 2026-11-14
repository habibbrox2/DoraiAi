package com.dorai.ai.ui.settings

import com.dorai.ai.github.GitHubConnectionStatus
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_github_no_repositories
import dorai.composeapp.generated.resources.settings_github_no_repositories_hint
import dorai.composeapp.generated.resources.settings_github_repositories_error
import dorai.composeapp.generated.resources.settings_github_repositories_not_connected
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * The repository-list empty copy follows the connection status: only a list that actually
 * loaded may read as "no repositories found". While not connected there is nothing to find
 * yet, and after a failed connection that copy contradicts the "Connection failed" card —
 * the screen used to blame the account for what is really a connection error.
 */
class GitHubSectionTest {

    @Test
    fun `connected but empty explains the missing installation`() {
        assertEquals(
            Res.string.settings_github_no_repositories_hint,
            repositoryEmptyMessageRes(GitHubConnectionStatus.Connected),
        )
    }

    @Test
    fun `error state points at retry instead of claiming no repositories`() {
        assertEquals(
            Res.string.settings_github_repositories_error,
            repositoryEmptyMessageRes(GitHubConnectionStatus.Error),
        )
    }

    @Test
    fun `error state never reuses the empty-list copy`() {
        assertNotEquals(
            Res.string.settings_github_no_repositories,
            repositoryEmptyMessageRes(GitHubConnectionStatus.Error),
        )
    }

    @Test
    fun `not connected tells the user to connect first`() {
        assertEquals(
            Res.string.settings_github_repositories_not_connected,
            repositoryEmptyMessageRes(GitHubConnectionStatus.NotConnected),
        )
    }

    @Test
    fun `waiting for device authorization keeps the connect-first copy`() {
        assertEquals(
            Res.string.settings_github_repositories_not_connected,
            repositoryEmptyMessageRes(GitHubConnectionStatus.WaitingForDevice),
        )
    }
}
