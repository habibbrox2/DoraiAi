package com.dorai.ai.mcp

import com.dorai.ai.DeepLinks

internal actual fun createOauthCallbackListener(serverName: String?): OauthCallbackListener = JvmOauthCallbackListener(serverName, DeepLinks.OAUTH_RETURN_URI)
