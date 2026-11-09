package com.dorai.ai.github

import com.dorai.ai.httpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlin.time.Clock

private const val GITHUB_API_BASE = "https://api.github.com"
private const val MAX_PAGES = 10

/**
 * Thin, read-only wrapper over the GitHub REST API.
 *
 * Every request sends `Authorization: Bearer <accessToken>` from [GitHubTokenVault],
 * `Accept: application/vnd.github+json`, `X-GitHub-Api-Version`, and a User-Agent.
 * Tokens are only ever placed in the `Authorization` header; Ktor's global request
 * logging (`HttpLogSanitizer`) redacts that header before it reaches stdout/logcat,
 * so tokens never appear in tool args, logs, or the agent timeline.
 */
class GitHubApiClient(
    private val vault: GitHubTokenVault,
    private val httpClient: HttpClient = httpClient(),
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAuthenticatedUser(): GitHubApiResult<GitHubUser> = request("$GITHUB_API_BASE/user", GitHubUser.serializer())

    suspend fun listInstallations(): GitHubApiResult<List<GitHubInstallation>> = requestAllPages("$GITHUB_API_BASE/user/installations", GitHubInstallation.serializer())

    suspend fun listInstallationRepositories(installationId: Long): GitHubApiResult<List<GitHubRepository>> = requestAllPages("$GITHUB_API_BASE/installations/$installationId/repositories", GitHubRepository.serializer())

    suspend fun getRepository(owner: String, repo: String): GitHubApiResult<GitHubRepository> = request("$GITHUB_API_BASE/repos/$owner/$repo", GitHubRepository.serializer())

    private suspend fun <T : Any> request(
        url: String,
        serializer: DeserializationStrategy<T>,
        etag: String? = null,
    ): GitHubApiResult<T> {
        val token = vault.readAccessToken() ?: return GitHubApiResult.Unauthorized
        return requestAuthenticated(url, token, serializer, etag)
    }

    private suspend fun <T : Any> requestAuthenticated(
        url: String,
        token: String,
        serializer: DeserializationStrategy<T>,
        etag: String? = null,
    ): GitHubApiResult<T> {
        val response = httpClient.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header("X-GitHub-Api-Version", GitHubAppConfig.API_VERSION)
            header("User-Agent", GitHubAppConfig.USER_AGENT_PRODUCT)
            etag?.let { header("If-None-Match", it) }
        }
        return when {
            response.status == HttpStatusCode.Unauthorized -> GitHubApiResult.Unauthorized

            response.status == HttpStatusCode.NotFound -> GitHubApiResult.NotFound

            response.status == HttpStatusCode.Forbidden -> rateLimitedOrForbidden(response)

            response.status == HttpStatusCode.NotModified -> GitHubApiResult.Failed

            !response.status.isSuccess() -> GitHubApiResult.Failed

            else -> {
                val body = response.bodyAsText()
                runCatching { json.decodeFromString(serializer, body) }
                    .map { GitHubApiResult.Success(it) }
                    .getOrElse { GitHubApiResult.Failed }
            }
        }
    }

    /**
     * Fetches every page of a list endpoint by following the `Link: rel="next"` header,
     * capped at [MAX_PAGES] pages. Any auth/rate-limit failure short-circuits.
     */
    private suspend fun <T : Any> requestAllPages(
        url: String,
        serializer: DeserializationStrategy<T>,
    ): GitHubApiResult<List<T>> {
        val token = vault.readAccessToken() ?: return GitHubApiResult.Unauthorized
        val collected = mutableListOf<T>()
        var nextUrl: String? = url
        var pages = 0
        while (nextUrl != null && pages < MAX_PAGES) {
            pages++
            val response = httpClient.get(nextUrl) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header("X-GitHub-Api-Version", GitHubAppConfig.API_VERSION)
                header("User-Agent", GitHubAppConfig.USER_AGENT_PRODUCT)
            }
            when {
                response.status == HttpStatusCode.Unauthorized -> return GitHubApiResult.Unauthorized
                response.status == HttpStatusCode.NotFound -> return GitHubApiResult.NotFound
                response.status == HttpStatusCode.Forbidden -> return rateLimitedOrForbidden(response)
                !response.status.isSuccess() -> return GitHubApiResult.Failed
            }
            val array = json.decodeFromString<JsonArray>(response.bodyAsText())
            for (element in array) {
                collected += json.decodeFromString(serializer, element.toString())
            }
            nextUrl = nextPageUrl(response.headers["Link"])
        }
        return GitHubApiResult.Success(collected)
    }

    private fun rateLimitedOrForbidden(response: io.ktor.client.statement.HttpResponse): GitHubApiResult<Nothing> {
        val remaining = response.headers["X-RateLimit-Remaining"]?.toIntOrNull()
        val resetSeconds = response.headers["X-RateLimit-Reset"]?.toLongOrNull()
        return if (remaining == 0) {
            val resetEpochMs = if (resetSeconds != null) resetSeconds * 1000 else Clock.System.now().toEpochMilliseconds()
            GitHubApiResult.RateLimited(resetEpochMs)
        } else {
            GitHubApiResult.Forbidden
        }
    }

    private fun nextPageUrl(linkHeader: String?): String? = linkHeader?.let {
        Regex("<([^>]+)>\\s*;\\s*rel=\"next\"").find(it)?.groupValues?.getOrNull(1)
    }
}
