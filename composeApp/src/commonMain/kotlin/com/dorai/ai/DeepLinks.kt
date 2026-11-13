package com.dorai.ai

/**
 * Custom-scheme deep links the app answers to.
 *
 * The MCP OAuth hand-off page uses [oauthReturnUri] to bring the user back into the app
 * after the authorization server redirects the browser to the loopback listener.
 *
 * The link carries **no credential**: the authorization code travels over the loopback
 * socket, not through the URI. The deep link only asks the app to show the Integrations /
 * MCP settings, so a hostile web page cannot do more than open that screen.
 *
 * The Android manifest must match — `android:scheme` / `android:host` in
 * `androidApp/src/main/AndroidManifest.xml`.
 */
object DeepLinks {
    /** Custom scheme of the app. */
    const val SCHEME = "dorai"

    /** Host of the MCP OAuth hand-off link. */
    const val OAUTH_HOST = "oauth"

    /** Path of the MCP OAuth hand-off link. */
    const val OAUTH_PATH = "/done"

    /** `dorai://oauth/done` — brings the user back to the Integrations / MCP settings. */
    const val OAUTH_RETURN_URI = "$SCHEME://$OAUTH_HOST$OAUTH_PATH"

    /** Host of the GitHub authorization link. */
    const val GITHUB_HOST = "github"

    /** Path of the GitHub authorization link. */
    const val GITHUB_PATH = "/done"

    /** `dorai://github/done` — brings the user back to the GitHub settings. */
    const val GITHUB_RETURN_URI = "$SCHEME://$GITHUB_HOST$GITHUB_PATH"
}
