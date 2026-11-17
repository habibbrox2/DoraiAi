package com.dorai.ai.source

/**
 * Outcome of a GitHub REST call. `Success` carries the decoded payload; every other
 * subclass is a terminal, token-free signal so callers can decide whether to re-auth,
 * wait for a rate-limit reset, or surface a generic failure.
 */
sealed class WorkspaceApiResult<out T : Any> {

    data class Success<out T : Any>(val data: T) : WorkspaceApiResult<T>()

    /** 401 Unauthorized — the access token is missing, expired, or revoked. */
    object Unauthorized : WorkspaceApiResult<Nothing>()

    /** 404 Not Found. */
    object NotFound : WorkspaceApiResult<Nothing>()

    /** 403 Forbidden without a rate-limit reset signal. */
    object Forbidden : WorkspaceApiResult<Nothing>()

    /**
     * 403 Forbidden with `X-RateLimit-Remaining: 0`. [resetEpochMs] is epoch
     * milliseconds when the rate-limit window resets (GitHub sends epoch seconds;
     * this class stores milliseconds).
     */
    data class RateLimited(val resetEpochMs: Long) : WorkspaceApiResult<Nothing>()

    /** Any other non-success status or network/decoding error. */
    object Failed : WorkspaceApiResult<Nothing>()
}
