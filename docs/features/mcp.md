# MCP Servers

**Last verified:** 2026-11-14

DORAI AI supports external tool servers via the [Model Context Protocol](https://modelcontextprotocol.io/) (MCP). Users can connect to remote MCP servers using Streamable HTTP transport and use their tools alongside native tools.

## Concepts

### MCP Server

A remote service that exposes tools via the MCP JSON-RPC protocol. Each server has a name, URL, optional authentication headers, and an enabled state. Server configurations are persisted as JSON in app settings. If the saved list ever becomes unreadable, it is backed up to a separate settings entry before the next save replaces it, so a corrupt blob is never silently lost.

### MCP Tool

A tool discovered from a connected MCP server. Each one is wrapped so it behaves like a built-in tool and integrates seamlessly with the existing tool executor and AI request pipeline. Each MCP tool has an ID of `mcp_{serverId}_{toolName}` and can be individually toggled.

### Popular Servers

A curated list of verified free MCP endpoints. Displayed as quick-add shortcuts in the add server bottom sheet. Selection criteria: free, Streamable HTTP transport, practically useful, reasonably stable. Most require no API key and one-tap add. Jina AI prefills the form with an optional API key field (stored as `Authorization: Bearer …` when provided); the server can still be added without a key, but search tools need a free key from jina.ai. Existing user-defined headers are never overwritten. The current names live in the runtime popular-server list in code; selection policy, last probe results, and a mirrored snapshot live in the knowledge bundle under `docs/knowledge/popular-mcp/`. Both are refreshed together with the `update-popular-mcp-servers` skill. Dead hosts (`remote.mcpservers.org` Fetch and Sequential Thinking) were removed earlier.

One popular entry requires OAuth instead of an API key: **Desktop Commander** (`https://mcp.desktopcommander.app/mcp`). It authorizes via PKCE and a loopback callback on `127.0.0.1` (see [OAuth for MCP Servers](https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization)). After adding Desktop Commander, tap **Authorize** on its card to open the browser; the server's authorization page handles login/consent. The app stores the token in the platform's encrypted settings (Android Keystore / Desktop DPAPI-backed AES-256-GCM).

## Adding a Server

MCP servers are managed in **Settings > Tools > Connectors**. The tab is a single scroll with two sections: **Your connectors** (the servers you configured, each with its enable switch and status) and **Top connectors** (the curated catalog with one-tap Add). A search bar filters both sections, and the Add button opens the server dialog. Each section heading carries its own count. The previous **Yours / Discover** toggle was removed — it read like a second tab row inside the category chips and split the library in two. Users can:

- Tap "Add MCP Server" to open a bottom sheet
- Enter a name, URL, and any number of custom headers manually (e.g., `Authorization`, plus additional vendor-specific headers); rows can be added or removed individually
- Or pick from the popular servers list: no-auth servers one-tap add; auth-optional servers (Jina AI) prefill name/URL and show an optional API key field (Add works without a key)

MCP server configurations are included in the settings export/import feature, so the full set of servers (and their headers) can be moved between devices.

## Connection Flow

When a server is added or enabled:

1. DORAI AI creates an MCP client for the server URL and headers
2. Sends an `initialize` JSON-RPC request with client capabilities
3. Sends a `notifications/initialized` notification
4. Calls `tools/list` to discover available tools
5. Registers discovered tools with their metadata (name, description, input schema)
6. The server appears as connected (green dot) in settings

When the chat screen first opens, all enabled MCP servers are reconnected in the background in parallel. The first time the settings screen becomes visible in a session, the same connect sweep runs once (alongside connection validation for services); later returns to settings do not automatically re-connect failed servers. Servers also reconnect when the user enables a server, taps refresh, or imports MCP settings. Connection state is protected by a mutex to prevent data races from concurrent connections, and individual server failures do not block other servers from connecting.

## Server Management

Each server card in settings shows:

- A status dot (green=connected, orange=connecting, red=error, grey=unknown), an enable/disable toggle, and a dropdown chevron
- Clicking anywhere on the card expands/collapses it
- When expanded: the OAuth state, the discovered tools with individual toggles, a refresh button, and a remove button (removal is deferred with a snackbar "Undo" option before permanent deletion)
- Disabling a server disconnects it immediately and the status dot reflects the change

Tool descriptions are clamped to two lines with a tap-to-expand row: a single MCP tool can ship a
paragraph-long usage guide (the Cloudflare documentation tool does), and rendering it in full pushed
the rest of the card out of view. The tool count and the number of enabled tools sit above the list.

The UI uses the same card style, status dot colors, and spacing as the Services tab for visual consistency.

## Transport

Only Streamable HTTP transport is supported:

- POST requests with `Content-Type: application/json` and `Accept: application/json, text/event-stream`
- The client tracks `Mcp-Session-Id` headers for session management
- Both direct JSON responses and SSE (Server-Sent Events) responses are handled
- No stdio transport support

## Authentication

MCP servers authenticate with custom headers (e.g., `Authorization: Bearer <token>`), sent on every request. Auth-optional popular servers (Jina AI) collect an optional API key in the add sheet and, when provided, store it as an Authorization header (`Bearer` is prefixed automatically when missing). The server can be added with no key.

A subset of servers is **OAuth-native**: the client must obtain a token via the OAuth authorization-code flow. DORAI supports this for MCP servers that publish Protected Resource Metadata (RFC 9728) and Authorization Server Metadata (RFC 8414) — today this is Desktop Commander. The flow is:

1. Tapping **Authorize** on the server card triggers `McpOAuthFlow.authorize` (`mcp/McpOAuthFlow.kt`).
2. The flow discovers endpoints: `.well-known/oauth-protected-resource` on the resource, then Authorization Server Metadata via `.well-known/oauth-authorization-server`.
3. The app dynamically registers itself as a public OAuth client with the authorization server (RFC 7591 Dynamic Client Registration), using a **loopback redirect URI** (`http://127.0.0.1:<ephemeral>/callback`).
4. The browser opens the authorization endpoint with the standard `response_type=code`, `S256` PKCE challenge, the discovered scopes (or `mcp:tools` when none are advertised), and the `resource` parameter.
5. After consent, the server redirects back to the loopback URL; the app captures `code` (and `state`, verified), exchanges the code for `access_token`/`refresh_token` at the token endpoint.
6. Tokens are stored in the encrypted MCP config (`AppSettings` — AES-256-GCM / Keystore) and refreshed automatically on expiry. They are never logged or attached to exports as raw strings — but `docs/features/settings-export-import.md` still applies to the full MCP config blob (treat exports with the same care as settings containing API keys).

OAuth tokens are refreshed lazily at connection time (before initialize + tools/list); a failed refresh surfaces a clear "re-authorize" message. **Revoke OAuth** on the server card clears the stored session without deleting the server — re-authorizing replaces it.

### The OAuth hand-off page

After consent, the authorization server redirects the browser to the loopback listener, which
answers with a branded page instead of a bare line of text: the D-Shape mark, a success (or
failure) badge, the name of the server that was authorized, and a "Return to DORAI AI" hint. The
page is self-contained — the mark, the icon and the favicon are inlined — so the browser never
spends a second request on the one-shot socket the handshake is waiting on, and a stray
`/favicon.ico` probe is answered with a 404 instead of swallowing the callback. The copy is
English-only: it is a machine-facing hand-off for a third-party provider, not a localized app
surface. Implementation: `mcp/OauthCallbackPage.kt` (page) and `mcp/JvmOauthCallbackListener`
(loopback listener, shared by the Android and desktop targets).

### Coming back to the app

On Android the hand-off page also offers a **Return to DORAI AI** button that opens the app
through the `dorai://oauth/done` deep link, and it attempts the same jump on its own after a
short delay. Browsers show their own "open this app?" confirmation, so the jump is one tap;
the button stays available when a browser refuses the automatic attempt. The deep link carries
**no credential** — the authorization code travels over the loopback socket — so all it does is
ask the app to show the Integrations / MCP settings (`DeepLinks.OAUTH_RETURN_URI`, handled by
`MainActivity` and routed by the app shell). The Android manifest registers the `dorai` scheme
for the `oauth` and `github` hosts; the desktop has no handler for it and simply asks the user
to switch back to the window.

After the handshake completes — whether the follow-up reconnect succeeds or fails — the settings MCP list rebuilds from the persisted server config, so the card always reflects the stored session (authorized badge + **Revoke OAuth**) instead of a stale "Authorization required" state. If the reconnect fails after a successful handshake, the card shows the connection failure alongside the still-present session; the user can retry (refresh) or revoke, without needing to re-authorize blindly.

## Integration with Tools

MCP tools are automatically available to the AI — no changes needed to the tool executor or request serialization. The platform's list of available tools includes every enabled tool from connected MCP servers. MCP tools have a 60-second timeout (vs 30s default for native tools). MCP tools are only shown within their server's expanded card in settings, not in the native tools list.

Tool calls to MCP servers go through the same execution pipeline as native tools: the tool executor finds the tool by name, the MCP tool wrapper sends a `tools/call` JSON-RPC request to the server, and the result is returned to the AI.

Every tool must have a unique name in a request. An MCP tool whose name is already taken — by a built-in tool or by another server's tool — is offered to the AI as `<server id>_<tool name>` (then `_2`, `_3`, … if needed, limited to 64 characters of `A–Z a–z 0–9 _ -`). Calls to the renamed tool still invoke the server's original tool name; the per-tool enable switches in settings are unaffected. Stopping a run while an MCP tool call is in flight cancels it rather than returning a failed result.

## Limitations

- HTTP/SSE transport only (no stdio)
- CORS may block MCP server requests on the web platform
- MCP tool parameters preserve full JSON Schema (including nested `items`, `properties`, `enum`) for accurate API serialization
- OAuth is supported for MCP servers that publish RFC 9728 + RFC 8414 metadata and perform RFC 7591 DCR. Desktop Commander is the shipped target. Servers without DCR or loopback-friendly redirect URIs may require a manual Authorization header instead.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../mcp/McpClient.kt` | MCP JSON-RPC client over HTTP/SSE |
| `composeApp/src/commonMain/.../mcp/McpServerManager.kt` | Server lifecycle, connection, tool discovery, OAuth authorize/refresh/revoke entry points |
| `composeApp/src/commonMain/.../mcp/McpOAuth.kt` | RFC 9728 / RFC 8414 discovery + RFC 7591 DCR + token-exchange/refresh plumbing |
| `composeApp/src/commonMain/.../mcp/McpOAuthFlow.kt` | User-facing OAuth flow (browser → loopback callback → token storage) |
| `composeApp/src/commonMain/.../mcp/OauthCallbackListener.kt` | Loopback callback listener interface + per-platform actuals |
| `composeApp/src/jvmShared/.../mcp/JvmOauthCallbackListener.jvmShared.kt` | JVM loopback listener (127.0.0.1 ephemeral port) |
| `composeApp/src/commonMain/.../mcp/McpOAuthCrypto.kt` | base64url + random-url-safe helpers (PKCE) |
| `composeApp/src/commonMain/.../mcp/McpCryptoPlatform.kt` | expect fun sha256 / secureRandomBytes (jvmShared actuals) |
| `composeApp/src/commonMain/.../mcp/McpServerManager.kt` | Server lifecycle, connection, tool discovery |
| `composeApp/src/commonMain/.../data/SettingsJson.kt` | Shared settings-backed JSON persistence: decode-or-default, encode-and-write, locked read-modify-write |
| `composeApp/src/commonMain/.../mcp/McpTool.kt` | Wraps MCP tools as native Tool implementations |
| `composeApp/src/commonMain/.../mcp/McpServerConfig.kt` | Server configuration data model |
| `composeApp/src/commonMain/.../mcp/McpModels.kt` | JSON-RPC DTOs and MCP-specific models |
| `composeApp/src/commonMain/.../mcp/PopularMcpServers.kt` | Curated list of verified MCP endpoints |
| `docs/knowledge/popular-mcp/` | OKF bundle: selection policy, last probe snapshot, refresh playbook |
| `composeApp/src/commonMain/.../ui/settings/McpSection.kt` | Connectors tab: Yours/Discover lists, search, server card UI, add-server bottom sheet |
| `composeApp/src/commonMain/.../ui/settings/ToolsSettings.kt` | Merged Tools tab shell hosting Connectors / Skills / Plugins |
| `composeApp/src/commonMain/.../ui/settings/SettingsViewModel.kt` | MCP connection management and UI state |
| `composeApp/src/commonMain/.../ui/settings/SettingsUiState.kt` | McpServerUiState, McpConnectionStatus |
| `composeApp/src/commonMain/.../data/AppSettings.kt` | MCP server config persistence, backup of undecodable JSON |
