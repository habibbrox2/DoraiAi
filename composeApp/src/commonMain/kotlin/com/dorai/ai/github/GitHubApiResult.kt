package com.dorai.ai.github

/**
 * Outcome of a GitHub REST call. `Success` carries the decoded payload; every other
 * subclass is a terminal, token-free signal so callers can decide whether to re-auth,
 * wait for a rate-limit reset, or surface a generic failure.
 */
sealed class GitHubApiResult<out T : Any> {

    data class Success<out T : Any>(val data: T) : GitHubApiResult<T>()

    /** 401 Unauthorized — the access token is missing, expired, or revoked. */
    object Unauthorized : GitHubApiResult<Nothing>()

    /** 404 Not Found. */
    object NotFound : GitHubApiResult<Nothing>()

    /** 403 Forbidden without a rate-limit reset signal. */
    object Forbidden : GitHubApiResult<Nothing>()

    /**
     * 403 Forbidden with `X-RateLimit-Remaining: 0`. [resetEpochMs] is epoch
     * milliseconds when the rate-limit window resets (GitHub sends epoch seconds;
     * this class stores milliseconds).
     */
    data class RateLimited(val resetEpochMs: Long) : GitHubApiResult<Nothing>()

    /** Any other non-success status or network/decoding error. */
    object Failed : GitHubApiResult<Nothing>()
}
