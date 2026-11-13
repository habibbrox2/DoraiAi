package com.dorai.ai.mcp

internal actual fun createOauthCallbackListener(serverName: String?): OauthCallbackListener = JvmOauthCallbackListener(serverName, null)
