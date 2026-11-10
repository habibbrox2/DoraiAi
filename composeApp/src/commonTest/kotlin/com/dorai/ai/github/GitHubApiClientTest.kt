package com.dorai.ai.github

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GitHubApiClientTest {

    private class FakeVault(var token: String? = "access-token-123") : GitHubTokenVault {
        override fun readAccessToken(): String? = token
        override fun readRefreshToken(): String? = null
        override fun readLogin(): String? = "dorai-user"
        override fun store(
            accessToken: String,
            refreshToken: String?,
            expiresAtEpochMs: Long?,
            refreshExpiresAtEpochMs: Long?,
            login: String,
            installationIds: List<Long>,
        ) {
        }
        override fun clear() {
            token = null
        }
    }

    private fun clientFor(
        status: HttpStatusCode,
        body: String,
        responseHeaders: Map<String, String> = emptyMap(),
        captured: MutableList<Map<String, String>>,
    ): HttpClient = HttpClient(
        MockEngine { request ->
            val entry = mutableMapOf<String, String>()
            entry[HttpHeaders.Authorization] = request.headers[HttpHeaders.Authorization] ?: ""
            entry[HttpHeaders.Accept] = request.headers[HttpHeaders.Accept] ?: ""
            entry["User-Agent"] = request.headers["User-Agent"] ?: ""
            entry["X-GitHub-Api-Version"] = request.headers["X-GitHub-Api-Version"] ?: ""
            captured.add(entry)
            respond(
                content = body,
                status = status,
                headers = headersOf(*responseHeaders.map { it.key to listOf(it.value) }.toTypedArray()),
            )
        },
    )

    private fun noCallClient(): HttpClient = HttpClient(MockEngine { error("GitHubApiClient must not call the network without a token") })

    @Test
    fun `getAuthenticatedUser returns Success and sends bearer token`() = runTest {
        val captured = mutableListOf<Map<String, String>>()
        val vault = FakeVault("access-token-123")
        val client = GitHubApiClient(
            vault,
            clientFor(
                HttpStatusCode.OK,
                """
                {"login":"dorai-user","id":42,"name":"Dorai","avatar_url":"https://avatars.githubusercontent.com/u/42"}
                """.trimIndent(),
                captured = captured,
            ),
        )

        val result = client.getAuthenticatedUser()

        assertIs<GitHubApiResult.Success<*>>(result)
        val user = (result as GitHubApiResult.Success<GitHubUser>).data
        assertEquals("dorai-user", user.login)
        assertEquals(42L, user.id)
        assertEquals("Bearer access-token-123", captured.single()[HttpHeaders.Authorization])
        assertEquals("application/vnd.github+json", captured.single()[HttpHeaders.Accept])
        assertEquals("DORAI-AI", captured.single()["User-Agent"])
        assertEquals("2026-03-10", captured.single()["X-GitHub-Api-Version"])
    }

    @Test
    fun `getAuthenticatedUser returns Unauthorized when vault has no token`() = runTest {
        val vault = FakeVault(null)
        val client = GitHubApiClient(vault, noCallClient())
        assertSame(GitHubApiResult.Unauthorized, client.getAuthenticatedUser())
    }

    @Test
    fun `getAuthenticatedUser returns Unauthorized on 401`() = runTest {
        val vault = FakeVault("stale-token")
        val client = GitHubApiClient(vault, clientFor(HttpStatusCode.Unauthorized, "{}", captured = mutableListOf()))
        assertSame(GitHubApiResult.Unauthorized, client.getAuthenticatedUser())
    }

    @Test
    fun `getRepository returns NotFound on 404`() = runTest {
        val vault = FakeVault("access-token-123")
        val client = GitHubApiClient(vault, clientFor(HttpStatusCode.NotFound, "{}", captured = mutableListOf()))
        assertSame(GitHubApiResult.NotFound, client.getRepository("owner", "repo"))
    }

    @Test
    fun `listInstallations returns RateLimited with reset time on 403 and zero remaining`() = runTest {
        val vault = FakeVault("access-token-123")
        val nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val resetSeconds = (nowMs / 1000 + 3600).toString()
        val responseHeaders = mapOf("X-RateLimit-Remaining" to "0", "X-RateLimit-Reset" to resetSeconds)
        val client = GitHubApiClient(
            vault,
            clientFor(HttpStatusCode.Forbidden, "{}", responseHeaders, captured = mutableListOf()),
        )

        val result = client.listInstallations()

        assertIs<GitHubApiResult.RateLimited>(result)
        assertEquals(resetSeconds.toLong() * 1000, (result as GitHubApiResult.RateLimited).resetEpochMs)
    }

    @Test
    fun `listInstallations follows Link rel=next and merges pages`() = runTest {
        val vault = FakeVault("access-token-123")
        val captured = mutableListOf<Map<String, String>>()
        var calls = 0
        val page1 = "[{\"id\":1,\"account\":{\"id\":11,\"login\":\"org-a\",\"type\":\"Organization\"}}]"
        val page2 = "[{\"id\":2,\"account\":{\"id\":12,\"login\":\"org-b\",\"type\":\"Organization\"}}]"
        val link1 = "<https://api.github.com/user/installations?page=2>; rel=\"next\""
        val client = GitHubApiClient(
            vault,
            HttpClient(
                MockEngine { request ->
                    val entry = mutableMapOf<String, String>()
                    entry[HttpHeaders.Authorization] = request.headers[HttpHeaders.Authorization] ?: ""
                    entry[HttpHeaders.Accept] = request.headers[HttpHeaders.Accept] ?: ""
                    captured.add(entry)
                    calls++
                    when (calls) {
                        1 -> respond(
                            content = page1,
                            status = HttpStatusCode.OK,
                            headers = headersOf("Link" to listOf(link1)),
                        )

                        2 -> respond(content = page2, status = HttpStatusCode.OK)

                        else -> error("unexpected call $calls")
                    }
                },
            ),
        )

        val result = client.listInstallations()

        assertIs<GitHubApiResult.Success<*>>(result)
        val installations = (result as GitHubApiResult.Success<List<GitHubInstallation>>).data
        assertNotNull(installations)
        assertEquals(2, installations.size)
        assertEquals(1L, installations[0].id)
        assertEquals("org-a", installations[0].account.login)
        assertEquals(2L, installations[1].id)
        assertEquals("org-b", installations[1].account.login)
        assertEquals(2, captured.size)
        assertEquals("Bearer access-token-123", captured.first()[HttpHeaders.Authorization])
    }

    @Test
    fun `getRepository decodes repository payload`() = runTest {
        val vault = FakeVault("access-token-123")
        val client = GitHubApiClient(
            vault,
            clientFor(
                HttpStatusCode.OK,
                """
                {"id":7,"name":"cli","full_name":"owner/cli","owner":{"login":"owner","id":1,"type":"User"},
                 "private":false,"default_branch":"main","html_url":"https://github.com/owner/cli"}
                """.trimIndent(),
                captured = mutableListOf(),
            ),
        )

        val result = client.getRepository("owner", "cli")

        assertIs<GitHubApiResult.Success<*>>(result)
        val repo = (result as GitHubApiResult.Success<GitHubRepository>).data
        assertEquals("cli", repo.name)
        assertEquals("owner/cli", repo.fullName)
        assertEquals("owner", repo.owner.login)
        assertFalse(repo.`private`)
        assertEquals("main", repo.defaultBranch)
    }
}
