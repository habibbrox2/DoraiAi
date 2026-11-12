package com.dorai.ai.mcp

internal actual fun createOauthCallbackListener(): OauthCallbackListener = object : OauthCallbackListener {
    override val port: Int = 0
    override suspend fun awaitHandshake(timeoutSeconds: Long): OauthCallbackRequest = OauthCallbackRequest(code = null, state = null, error = "unsupported_platform", errorDescription = "OAuth is not available on this platform")
    override fun stop() = Unit
}
