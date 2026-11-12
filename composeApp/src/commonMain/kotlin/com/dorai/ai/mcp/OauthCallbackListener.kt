package com.dorai.ai.mcp

/**
 * A one-shot loopback HTTP listener used during the OAuth authorization-code flow.
 *
 * The browser opens `$authorizationEndpoint?...redirect_uri=$port/callback`; the listener
 * captures the `code`/`state` query parameters from the single request that hits `/callback`
 * and resolves the waiting authorization flow.
 */
internal expect fun createOauthCallbackListener(): OauthCallbackListener

internal interface OauthCallbackListener {
    /** The ephemeral port the listener is bound to (0 until started). */
    val port: Int

    /** Binds 127.0.0.1:0 and returns the chosen ephemeral port. */
    fun start()

    /** Suspends until a `GET /callback?code=...&state=...` request arrives or it times out. */
    suspend fun awaitHandshake(timeoutSeconds: Long): OauthCallbackRequest

    /** Stops the listener and releases the port. */
    fun stop()
}

internal data class OauthCallbackRequest(
    val code: String?,
    val state: String?,
    val error: String?,
    val errorDescription: String?,
) {
    val isSuccess get() = error == null && !code.isNullOrBlank()
}
