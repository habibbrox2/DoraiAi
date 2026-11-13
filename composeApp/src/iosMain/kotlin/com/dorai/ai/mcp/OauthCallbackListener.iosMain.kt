package com.dorai.ai.mcp

internal actual fun createOauthCallbackListener(serverName: String?): OauthCallbackListener = object : OauthCallbackListener {
    override val port: Int = 0
    override fun start() = Unit
    override suspend fun awaitHandshake(timeoutSeconds: Long): OauthCallbackRequest = OauthCallbackRequest(code = null, state = null, error = "unsupported_platform", errorDescription = "OAuth is not available on this platform")
    override fun stop() = Unit
}
