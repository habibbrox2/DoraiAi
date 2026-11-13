package com.dorai.ai.mcp

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The OAuth hand-off page is the only part of the app the user sees in a browser, so the
 * contract is checked here: it must brand itself, name the server that was authorized, and
 * never let a server-provided string break out of the markup.
 */
class OauthCallbackPageTest {

    @Test
    fun `success page carries the brand and the authorized server`() {
        val html = OauthCallbackPage.render(serverName = "Cloudflare")

        assertContains(html, "<title>DORAI AI &mdash; Authorization complete</title>")
        assertContains(html, "Authorization complete")
        assertContains(html, "<b>Cloudflare</b> is now connected to DORAI AI.")
        // The inlined D-Shape mark, gradient stops and all.
        assertContains(html, "id=\"dgrad\"")
        assertContains(html, "#25E2F5")
        assertContains(html, "id=\"pgrad\"")
    }

    @Test
    fun `success page tells the user to return to the app`() {
        val html = OauthCallbackPage.render(serverName = "Cloudflare")

        assertContains(html, "Return to DORAI AI")
        assertContains(html, "you can close this tab")
    }

    @Test
    fun `page is self contained so the one-shot socket is not spent on a second request`() {
        val html = OauthCallbackPage.render(serverName = "Cloudflare")

        // The favicon is inlined; an external reference would make the browser ask the
        // listener a second time and swallow the OAuth callback.
        assertContains(html, "rel=\"icon\" href=\"data:image/svg+xml,")
        assertFalse(html.contains("src=\"http"), "no external asset may be referenced")
        assertFalse(html.contains("href=\"http"), "no external asset may be referenced")
    }

    @Test
    fun `error page reports the failure instead of a success`() {
        val html = OauthCallbackPage.render(
            serverName = "Cloudflare",
            error = "access_denied",
            errorDescription = "The user denied the request",
        )

        assertContains(html, "Authorization failed")
        assertContains(html, "The user denied the request")
        assertFalse(html.contains("Authorization complete"))
    }

    @Test
    fun `server name is escaped so it cannot inject markup`() {
        val html = OauthCallbackPage.render(serverName = "<script>alert(1)</script>")

        assertContains(html, "&lt;script&gt;alert(1)&lt;/script&gt;")
        assertFalse(html.contains("<script>"))
    }

    @Test
    fun `page without a server name still reads as a completed authorization`() {
        val html = OauthCallbackPage.render(serverName = null)

        assertContains(html, "Authorization complete")
        assertContains(html, "The connector is now connected to DORAI AI.")
    }

    @Test
    fun `page renders the same shape for a blank server name`() {
        val html = OauthCallbackPage.render(serverName = "   ")

        assertTrue(html.contains("Authorization complete"))
        assertFalse(html.contains("<b></b>"))
    }

    @Test
    fun `a return uri turns the hint into a button back into the app`() {
        val html = OauthCallbackPage.render(serverName = "Cloudflare", returnUri = "dorai://oauth/done")

        assertContains(html, "<a class=\"cta\" href=\"dorai://oauth/done\">Return to DORAI AI</a>")
        assertContains(html, "You can close this tab.")
        // The page also tries the jump on its own — a browser that refuses it leaves the button.
        assertContains(html, "window.location.href = 'dorai://oauth/done'")
    }

    @Test
    fun `without a return uri the page only asks the user to switch back`() {
        val html = OauthCallbackPage.render(serverName = "Cloudflare")

        assertContains(html, "Return to DORAI AI")
        assertFalse(html.contains("class=\"cta\""))
        assertFalse(html.contains("window.location.href"))
    }

    @Test
    fun `the error page never jumps back on its own`() {
        val html = OauthCallbackPage.render(
            serverName = "Cloudflare",
            error = "access_denied",
            errorDescription = "The user denied the request",
            returnUri = "dorai://oauth/done",
        )

        // The button is still there, but the page must not steal focus while the user reads
        // why the authorization failed.
        assertContains(html, "class=\"cta\"")
        assertFalse(html.contains("window.location.href"))
    }
}
