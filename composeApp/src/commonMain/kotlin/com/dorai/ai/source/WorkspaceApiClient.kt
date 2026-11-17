package com.dorai.ai.source

import com.dorai.ai.httpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlin.time.Clock

private const val GITHUB_API_BASE = "https://api.github.com"
private const val MAX_PAGES = 10

/**
 * Thin wrapper over the GitHub REST API. Read-only by default; the only write endpoint is
 * [createPullRequest], which refuses to run unless `writeEnabled = true` is passed explicitly
 * (the maintainer opt-in — see `WorkspaceIntegrationState.writeOperationsEnabled`).
 *
 * Every request sends `Authorization: Bearer <accessToken>` from [GitHubTokenVault],
 * `Accept: application/vnd.github+json`, `X-GitHub-Api-Version`, and a User-Agent.
 * Tokens are only ever placed in the `Authorization` header; Ktor's global request
 * logging (`HttpLogSanitizer`) redacts that header before it reaches stdout/logcat,
 * so tokens never appear in tool args, logs, or the agent timeline.
 */
open class WorkspaceApiClient(
    private val vault: GitHubTokenVault,
    private val httpClient: HttpClient = httpClient(),
) {

    private val json = Json { ignoreUnknownKeys = true }

    open suspend fun getAuthenticatedUser(): WorkspaceApiResult<GitHubUser> = request("$GITHUB_API_BASE/user", GitHubUser.serializer())

    open suspend fun listInstallations(): WorkspaceApiResult<List<GitHubInstallation>> = requestAllPages("$GITHUB_API_BASE/user/installations", GitHubInstallation.serializer())

    open suspend fun listInstallationRepositories(installationId: Long): WorkspaceApiResult<List<GitHubRepository>> = requestAllPages("$GITHUB_API_BASE/installations/$installationId/repositories", GitHubRepository.serializer())

    open suspend fun getRepository(owner: String, repo: String): WorkspaceApiResult<GitHubRepository> = request("$GITHUB_API_BASE/repos/$owner/$repo", GitHubRepository.serializer())

    /**
     * Creates a pull request (`POST /repos/{owner}/{repo}/pulls`).
     *
     * Refused — no HTTP call is made — unless `writeEnabled = true` (the maintainer opt-in).
     * The caller must additionally have confirmed `Pull requests: Write` installation
     * permission before invoking; this method does not re-check it.
     */
    open suspend fun createPullRequest(
        owner: String,
        repo: String,
        title: String,
        head: String,
        base: String,
        body: String = "",
        writeEnabled: Boolean = false,
    ): WorkspaceApiResult<GitHubPullRequest> {
        if (!writeEnabled) return WorkspaceApiResult.Forbidden
        if (title.isBlank() || head.isBlank() || base.isBlank()) return WorkspaceApiResult.Failed
        val token = vault.readAccessToken() ?: return WorkspaceApiResult.Unauthorized
        return postAuthenticated(
            "$GITHUB_API_BASE/repos/$owner/$repo/pulls",
            CreatePullRequestBody(title = title, head = head, base = base, body = body),
            CreatePullRequestBody.serializer(),
            token,
            GitHubPullRequest.serializer(),
        )
    }

    private suspend fun <T : Any> request(
        url: String,
        serializer: DeserializationStrategy<T>,
        etag: String? = null,
    ): WorkspaceApiResult<T> {
        val token = vault.readAccessToken() ?: return WorkspaceApiResult.Unauthorized
        return requestAuthenticated(url, token, serializer, etag)
    }

    private suspend fun <T : Any> requestAuthenticated(
        url: String,
        token: String,
        serializer: DeserializationStrategy<T>,
        etag: String? = null,
    ): WorkspaceApiResult<T> {
        val response = httpClient.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header("X-GitHub-Api-Version", GitHubAppConfig.API_VERSION)
            header("User-Agent", GitHubAppConfig.USER_AGENT_PRODUCT)
            etag?.let { header("If-None-Match", it) }
        }
        return mapResponse(response, serializer)
    }

    private suspend fun <B : Any, T : Any> postAuthenticated(
        url: String,
        body: B,
        bodySerializer: SerializationStrategy<B>,
        token: String,
        serializer: DeserializationStrategy<T>,
    ): WorkspaceApiResult<T> {
        val response = httpClient.post(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header("X-GitHub-Api-Version", GitHubAppConfig.API_VERSION)
            header("User-Agent", GitHubAppConfig.USER_AGENT_PRODUCT)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(bodySerializer, body))
        }
        return mapResponse(response, serializer)
    }

    private suspend fun <T : Any> mapResponse(
        response: io.ktor.client.statement.HttpResponse,
        serializer: DeserializationStrategy<T>,
    ): WorkspaceApiResult<T> = when {
        response.status == HttpStatusCode.Unauthorized -> WorkspaceApiResult.Unauthorized

        response.status == HttpStatusCode.NotFound -> WorkspaceApiResult.NotFound

        response.status == HttpStatusCode.Forbidden -> rateLimitedOrForbidden(response)

        response.status == HttpStatusCode.NotModified -> WorkspaceApiResult.Failed

        !response.status.isSuccess() -> WorkspaceApiResult.Failed

        else -> {
            val body = response.bodyAsText()
            runCatching { json.decodeFromString(serializer, body) }
                .map { WorkspaceApiResult.Success(it) }
                .getOrElse { WorkspaceApiResult.Failed }
        }
    }

    /**
     * Fetches every page of a list endpoint by following the `Link: rel="next"` header,
     * capped at [MAX_PAGES] pages. Any auth/rate-limit failure short-circuits.
     */
    private suspend fun <T : Any> requestAllPages(
        url: String,
        serializer: DeserializationStrategy<T>,
    ): WorkspaceApiResult<List<T>> {
        val token = vault.readAccessToken() ?: return WorkspaceApiResult.Unauthorized
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
                response.status == HttpStatusCode.Unauthorized -> return WorkspaceApiResult.Unauthorized
                response.status == HttpStatusCode.NotFound -> return WorkspaceApiResult.NotFound
                response.status == HttpStatusCode.Forbidden -> return rateLimitedOrForbidden(response)
                !response.status.isSuccess() -> return WorkspaceApiResult.Failed
            }
            val array = json.decodeFromString<JsonArray>(response.bodyAsText())
            for (element in array) {
                collected += json.decodeFromString(serializer, element.toString())
            }
            nextUrl = nextPageUrl(response.headers["Link"])
        }
        return WorkspaceApiResult.Success(collected)
    }

    private fun rateLimitedOrForbidden(response: io.ktor.client.statement.HttpResponse): WorkspaceApiResult<Nothing> {
        val remaining = response.headers["X-RateLimit-Remaining"]?.toIntOrNull()
        val resetSeconds = response.headers["X-RateLimit-Reset"]?.toLongOrNull()
        return if (remaining == 0) {
            val resetEpochMs = if (resetSeconds != null) resetSeconds * 1000 else Clock.System.now().toEpochMilliseconds()
            WorkspaceApiResult.RateLimited(resetEpochMs)
        } else {
            WorkspaceApiResult.Forbidden
        }
    }

    private fun nextPageUrl(linkHeader: String?): String? = linkHeader?.let {
        Regex("<([^>]+)>\\s*;\\s*rel=\"next\"").find(it)?.groupValues?.getOrNull(1)
    }
}
