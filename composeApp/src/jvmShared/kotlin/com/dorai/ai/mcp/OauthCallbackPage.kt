package com.dorai.ai.mcp

/**
 * The branded page the browser lands on after an MCP OAuth provider redirects back to the
 * loopback listener (`http://127.0.0.1:<port>/callback`).
 *
 * The page is fully self-contained: the D-Shape mark, the icon and every style are inlined,
 * so the browser never asks the listener for a second resource (a favicon request would
 * otherwise consume the one-shot socket the OAuth handshake is waiting on).
 *
 * The copy is intentionally English-only: it is a machine-facing hand-off page for a
 * third-party OAuth provider, not a localized app surface.
 */
internal object OauthCallbackPage {

    /** D-Shape mark, inlined from `docs/branding/dorai/assets/hexacore/svg/d-shape-gradient.svg`. */
    private const val LOGO_SVG = """
<svg class="logo" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="DORAI AI">
  <defs>
    <linearGradient id="dgrad" x1="84" y1="60" x2="440" y2="452" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#25E2F5"/><stop offset=".48" stop-color="#377CFF"/><stop offset="1" stop-color="#6F5BFF"/>
    </linearGradient>
    <linearGradient id="pgrad" x1="214" y1="200" x2="308" y2="312" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#25E2F5"/><stop offset="1" stop-color="#377CFF"/>
    </linearGradient>
  </defs>
  <path d="M118 60 H244 C352.25 60 440 147.75 440 256 C440 364.25 352.25 452 244 452 H118 C99 452 84 437 84 418 V94 C84 79 103 60 118 60 Z M164 140.0 V372.0 H244 C308.07 372.0 360.0 320.07 360.0 256 C360.0 191.93 308.07 140.0 244 140.0 Z" fill="url(#dgrad)" fill-rule="evenodd"/>
  <path d="M214.0 223.0 Q214 201 233.0 212.1 L289.0 244.9 Q308 256 289.0 267.1 L233.0 299.9 Q214 311 214.0 289.0 Z" fill="url(#pgrad)"/>
</svg>
"""

    /** The same mark, used as the in-page favicon so the browser asks for nothing else. */
    private const val FAVICON_SVG = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath d='M118 60H244c108.25 0 196 87.75 196 196s-87.75 196-196 196h-126c-19 0-34-15-34-34V94c0-15 19-34 34-34Zm46 80v232h80c64.07 0 116-51.93 116-116s-51.93-116-116-116h-80Z' fill='%23377CFF' fill-rule='evenodd'/%3E%3Cpath d='M214 223q0-22 19-10.9l56 32.8q19 11.1 0 22.2l-56 32.8Q214 311 214 289v-66Z' fill='%2325E2F5'/%3E%3C/svg%3E"

    private const val STYLES = """
<style>
  :root{
    --bg:#050A1C; --cyan:#25E2F5; --blue:#377CFF; --violet:#6F5BFF;
    --text:#F1F8FF; --muted:#93A4C4; --danger:#FF8A8A;
  }
  *{box-sizing:border-box}
  html,body{height:100%}
  body{
    margin:0;padding:24px;
    background:
      radial-gradient(900px 520px at 50% -12%, rgba(37,226,245,.20), transparent 62%),
      radial-gradient(760px 500px at 50% 112%, rgba(111,91,255,.24), transparent 64%),
      var(--bg);
    color:var(--text);
    font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Inter,Helvetica,Arial,sans-serif;
    display:flex;align-items:center;justify-content:center;
    -webkit-font-smoothing:antialiased;
  }
  .card{
    width:100%;max-width:460px;text-align:center;
    padding:40px 32px 26px;
    border-radius:22px;
    background:linear-gradient(180deg, rgba(13,28,56,.94), rgba(8,19,43,.94));
    border:1px solid rgba(37,226,245,.20);
    box-shadow:0 30px 80px -32px rgba(0,0,0,.95), 0 1px 0 rgba(255,255,255,.04) inset;
    animation:rise .55s cubic-bezier(.2,.8,.2,1) both;
  }
  .logo{width:72px;height:72px;display:block;margin:0 auto 22px;filter:drop-shadow(0 10px 26px rgba(55,124,255,.45))}
  .badge{
    width:60px;height:60px;margin:0 auto 18px;border-radius:50%;
    display:flex;align-items:center;justify-content:center;
    animation:pop .5s cubic-bezier(.2,.9,.25,1.15) .12s both;
  }
  .badge svg{width:30px;height:30px}
  .ok{background:linear-gradient(140deg, rgba(37,226,245,.22), rgba(111,91,255,.22));border:1px solid rgba(37,226,245,.45);box-shadow:0 0 0 8px rgba(37,226,245,.06)}
  .bad{background:linear-gradient(140deg, rgba(255,138,138,.20), rgba(255,138,138,.06));border:1px solid rgba(255,138,138,.45);box-shadow:0 0 0 8px rgba(255,138,138,.06)}
  .ok svg{stroke:var(--cyan)} .bad svg{stroke:var(--danger)}
  h1{margin:0 0 10px;font-size:22px;line-height:1.3;font-weight:700;letter-spacing:-.01em}
  .lead{margin:0 auto 22px;max-width:42ch;font-size:15px;line-height:1.55;color:var(--muted);text-wrap:balance}
  .lead b{color:var(--text);font-weight:600}
  .hint{
    display:flex;align-items:center;justify-content:center;gap:8px;
    padding:13px 16px;border-radius:14px;font-size:14px;color:var(--text);
    background:rgba(19,40,74,.55);border:1px solid rgba(255,255,255,.07);
  }
  .hint .dot{width:7px;height:7px;border-radius:50%;background:var(--cyan);box-shadow:0 0 10px var(--cyan);animation:pulse 1.8s ease-in-out infinite}
  .cta-row{display:flex;justify-content:center;margin-bottom:10px}
  .cta{
    display:inline-flex;align-items:center;justify-content:center;
    padding:13px 24px;border-radius:999px;font-size:14.5px;font-weight:700;
    text-decoration:none;color:#04121c;
    background:linear-gradient(120deg,var(--cyan) 0%,var(--blue) 55%,var(--violet) 100%);
    box-shadow:0 12px 30px -12px rgba(55,124,255,.85);
    transition:transform .15s ease,box-shadow .15s ease;
  }
  .cta:hover{transform:translateY(-1px);box-shadow:0 16px 34px -12px rgba(55,124,255,.95)}
  .cta:active{transform:translateY(0)}
  .sub{margin:0;font-size:12.5px;color:rgba(147,164,196,.8)}
  .foot{margin-top:22px;padding-top:16px;border-top:1px solid rgba(255,255,255,.07);
    font-size:11.5px;letter-spacing:.16em;text-transform:uppercase;color:rgba(147,164,196,.75)}
  .accent{height:3px;width:64px;margin:0 auto 22px;border-radius:99px;background:linear-gradient(90deg,var(--cyan),var(--blue),var(--violet))}
  @keyframes rise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}
  @keyframes pop{from{opacity:0;transform:scale(.72)}to{opacity:1;transform:none}}
  @keyframes pulse{0%,100%{opacity:1}50%{opacity:.35}}
  @media (prefers-reduced-motion: reduce){*{animation:none !important}}
</style>
"""

    private const val CHECK_SVG = """
<svg viewBox="0 0 24 24" fill="none" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
  <path d="M20 6 9 17l-5-5"/>
</svg>
"""

    private const val CROSS_SVG = """
<svg viewBox="0 0 24 24" fill="none" stroke-width="2.6" stroke-linecap="round" aria-hidden="true">
  <path d="M18 6 6 18M6 6l12 12"/>
</svg>
"""

    /**
     * Renders the hand-off page.
     *
     * @param serverName the MCP server that was authorized; shown when known.
     * @param error the OAuth error code, when the provider rejected the request.
     * @param errorDescription the provider's human-readable error detail, when present.
     * @param returnUri a deep link that re-opens the app (e.g. `dorai://oauth/done`). Null
     *   when the platform has no handler for it (desktop) — the page then only tells the
     *   user to switch back.
     */
    fun render(
        serverName: String?,
        error: String? = null,
        errorDescription: String? = null,
        returnUri: String? = null,
    ): String {
        val failed = !error.isNullOrBlank()
        val title = if (failed) "Authorization failed" else "Authorization complete"
        val subject = serverName?.takeIf { it.isNotBlank() }?.let { escape(it) }
        val lead = when {
            failed -> {
                val detail = errorDescription?.takeIf { it.isNotBlank() } ?: error.orEmpty()
                "DORAI AI could not finish the connection. " +
                    escape(detail.ifBlank { "The authorization was not completed." })
            }

            subject != null -> "<b>$subject</b> is now connected to DORAI AI."

            else -> "The connector is now connected to DORAI AI."
        }
        val badgeClass = if (failed) "bad" else "ok"
        val badgeIcon = if (failed) CROSS_SVG else CHECK_SVG

        val action = if (returnUri != null) {
            """
    <div class="cta-row">
      <a class="cta" href="${escape(returnUri)}">Return to DORAI AI</a>
    </div>
    <p class="sub">You can close this tab.</p>
            """.trimIndent()
        } else {
            """
    <div class="hint"><span class="dot"></span><span>${
                if (failed) {
                    "Return to DORAI AI and try connecting again."
                } else {
                    "Return to DORAI AI — you can close this tab."
                }
            }</span></div>
            """.trimIndent()
        }

        // The jump back into the app is attempted automatically; browsers that refuse an
        // unsolicited custom-scheme navigation simply leave the user on the page, where the
        // button is the explicit way back.
        val autoJump = if (returnUri != null && !failed) {
            """
<script>
  window.setTimeout(function () {
    try { window.location.href = ${jsString(returnUri)}; } catch (e) { /* the button stays */ }
  }, 900);
</script>
            """.trimIndent()
        } else {
            ""
        }

        return """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="dark">
<meta name="robots" content="noindex">
<title>DORAI AI &mdash; $title</title>
<link rel="icon" href="$FAVICON_SVG">
$STYLES
</head>
<body>
  <main class="card">
    $LOGO_SVG
    <div class="accent"></div>
    <div class="badge $badgeClass">$badgeIcon</div>
    <h1>$title</h1>
    <p class="lead">$lead</p>
$action
    <div class="foot">DORAI AI</div>
  </main>
$autoJump
</body>
</html>
        """.trimIndent()
    }

    /** Escapes the characters that would otherwise break out of HTML text or an attribute. */
    private fun escape(value: String): String = buildString(value.length) {
        for (char in value) {
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    /** Renders a string as a JavaScript literal. */
    private fun jsString(value: String): String = "'" + escape(value).replace("\\", "\\\\") + "'"
}
