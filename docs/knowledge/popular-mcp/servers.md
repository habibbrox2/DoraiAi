---
type: Catalog
title: Popular MCP servers
description: Curated free Streamable HTTP MCP endpoints for DORAI AI’s one-tap add sheet.
tags: [mcp, popular-servers]
status: stable
stale_after: 2026-08-26
generated: { by: process:update-popular-mcp-servers, at: 2026-08-12T20:49:34Z }
verified: { by: process:desktopTest-PopularMcpServers, at: 2026-11-12T13:42:00Z }
sources:
  - id: selection-policy
    resource: /selection-policy.md
    title: Selection and probe policy
  - id: popular-mcp-playbook
    resource: /refresh-playbook.md
    title: Refresh popular MCP servers playbook
---

# Policy

Include a host when **all** of the following hold (see [selection-policy.md](selection-policy.md)):

1. HTTPS Streamable HTTP MCP (`initialize` + `tools/list` like the app client).
2. Free to add (optional free API key allowed).
3. A real end-user job, not a demo echo server.

A probe is **attested live** only when that initialize + tools/list handshake succeeds. Auth-optional hosts (Jina AI) need initialize without a key.

Replace this snapshot only via the [refresh playbook](refresh-playbook.md).

# Current set (attested live, no setup required)

Snapshot mirrored into `popularMcpServers` (runtime). 13 hosts. Plus Desktop Commander (OAuth preset, below).

| Name | URL | Auth | Probe | Tools |
|---|---|---|---|---|
| Context7 | `https://mcp.context7.com/mcp` | none | attested live | 2 — `resolve-library-id`, `query-docs` |
| MDN | `https://mcp.mdn.mozilla.net` | none | attested live | 3 — `get-compat`, `get-doc`, `search` |
| DeepWiki | `https://mcp.deepwiki.com/mcp` | none | attested live | 3 — `ask_question`, `read_wiki_contents`, `read_wiki_structure` |
| Parallel Search | `https://search.parallel.ai/mcp` | none | attested live | 2 — `web_search`, `web_fetch` |
| Yahoo Finance | `https://gateway.mcpservers.org/yahoo-finance/mcp` | none | attested live | 4 — `get_quote`, `search`, `get_chart`, `quote_summary` |
| CoinGecko | `https://mcp.api.coingecko.com/mcp` | none | attested live | 2 — `execute`, `search_docs` |
| Jina AI | `https://mcp.jina.ai/v1` | optional key | attested live (no key) | 22 — read/search/screenshot family |
| Open-Meteo Weather | `https://mcp.open-mcp.org/api/server/open-weather@latest/mcp` | none | attested live | 2 — `expandSchema`, `getweatherdata` |
| Kiwi.com | `https://mcp.kiwi.com` | none | attested live | 2 — `search-flight`, `feedback-to-devs` |
| Malwarebytes | `https://scamguard.malwarebytes.com/claude/mcp` | none | attested live | 6 — `reputation-check_link`, `reputation-check_phone`, `reputation-check_email`, `reputation-report`, `reputation-whois`, `reputation-scan_all` |
| tldraw | `https://tldraw-mcp-app.tldraw.workers.dev/mcp` | none | attested live | 6 — `search`, `exec`, `_exec_callback`, `_get_canvas_state`, `read_checkpoint`, `save_checkpoint` |
| Find-A-Domain | `https://api.findadomain.dev/mcp` | none | attested live | 2 — `check_domain`, `list_tlds` |
| SubwayInfo NYC | `https://subwayinfo.nyc/mcp` | none | attested live | 25 — MTA / bus / ferry arrivals and alerts |

# OAuth-capable presets (not probe-attested at runtime)

| Name | URL | Auth | Notes |
|---|---|---|---|
| Desktop Commander | `https://mcp.desktopcommander.app/mcp` | OAuth (PKCE + DCR) | Added 2026-11-12. App performs RFC 7591 DCR (public client), RFC 6749 authorization code grant with S256 PKCE, loopback callback at `127.0.0.1:<ephemeral>/callback`, stores tokens in the encrypted MCP prefs. Relies on the server parked behind a Cloudflare WAF that rejects non-browser UAs with **Error 1010**; the app's HTTP client sends a browser-shaped `User-Agent` for the OAuth handshake. Re-probe at the next `update-popular-mcp-servers` run. |

# Snapshot

| Field | Value |
|---|---|
| Probed | 2026-08-12T20:49:34Z (13 hosts), Desktop Commander OAuth wired 2026-11-12 (live handshake not attested end-to-end) |
| Handshake | RFC 6749 auth-code + PKCE (`authorization_code`), RFC 7591 DCR, RFC 9728 resource metadata, RFC 8414 AS metadata |
| Current | 13 attested live + Desktop Commander (preset, OAuth-required; not probe-attested) |
| Dropped this refresh | Manifold Markets (`https://api.manifold.markets/v0/mcp`) — Cloudflare **526** invalid origin TLS, `retryable: false` |

# Trust boundary

The OAuth workflow (dynamic client registration + authorization code + PKCE via loopback) is purpose-built for MCP hosts that require it — today only Desktop Commander. The authorization server lives on the resource's own origin (`mcp.desktopcommander.app`), tokens are stored encrypted per platform (EncryptedSharedPreferences on Android, DPAPI-backed AES-256-GCM on Desktop), and the browser's authorize page is responsible for user consent. The app never auto-approves; re-authorize after token expiry/rotation.

# Notes

- Jina AI still uses `requiresAuth: true` in Kotlin so the add sheet shows a key field. The probe listed 22 tools without a key; search tools still need a free jina.ai key.
- `remote.mcpservers.org` Fetch and Sequential Thinking stay out (removed earlier).
- Do not add replacement hosts without an explicit product decision.
