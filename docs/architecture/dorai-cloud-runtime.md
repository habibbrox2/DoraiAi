# DORAI AI Cloud Runtime Architecture

**Status:** Proposed (v1 scope agreed 2026-11-16)
**Scope:** First-party cloud backend for the Dorai AI runtime apps (Android, iOS, Desktop, Web)
**Related:** [`cloud-services-integration-blueprint.md`](cloud-services-integration-blueprint.md) (provider blueprint), [`../features/hybrid-rag-vector-memory.md`](../features/hybrid-rag-vector-memory.md), [`../features/supabase-e2ee-cloud-sync.md`](../features/supabase-e2ee-cloud-sync.md)

## 1. Goal

Two integrations that must stay separate:

| | Development MCP | Runtime Cloud Integration |
|---|---|---|
| Users | Claude Code, Codex, VS Code, Copilot | Dorai AI app end-users (Android/iOS/Desktop/Web) |
| Purpose | Operate Cloudflare, Supabase, GitHub, Firebase projects | Store and manage the end-user's own data in the first-party backend |
| Credentials | MCP OAuth, stored outside the repo | End-user session JWT; no service secrets on device |
| Scope | Infrastructure | Product data (memories, vault, settings, devices) |

The MCP side is already wired (`.opencode/opencode.json`). This document covers the runtime side.

## 2. Decisions (2026-11-16)

| Decision | Choice | Why |
|---|---|---|
| Identity | **Supabase Auth** (email + password, magic link) | Managed, proven, emits JWTs that RLS can consume directly |
| API surface | **Dorai Cloud API façade** (versioned `/v1/*`, edge-hosted) | Key rotation, quotas, rate limiting, observability, and the freedom to move the store later |
| v1 data | **Memories + E2EE Vault + Settings snapshot** | Conversation sync stays in the encrypted vault archive for v1 (largest privacy surface) |
| Cryptography | **Zero-knowledge** | The server never holds plaintext user content |

### Zero-knowledge vs. retrieval

Searchable memory and zero-knowledge are compatible because **the client computes the embedding**:

- The client encrypts the memory content (AES-256-GCM) and stores `content_ciphertext` + `content_iv`.
- The client asks the façade for an embedding of its query, and the façade (holding the Cloudflare Workers AI token) returns the vector.
- The server stores the vector and searches it with pgvector; the client decrypts only the top-k results it receives.

Consequence: the server can rank matches but cannot read a single memory. Metadata that stays in plaintext is limited to `key`, `category`, `source`, timestamps and the vector itself.

## 3. Components

```text
Dorai AI app (KMP)
  ├─ DoraiCloudClient (Ktor, commonMain)          ── bearer: end-user JWT
  │     ├─ /v1/me                                   profile + quota
  │     ├─ /v1/embeddings                           query vector (server holds Workers AI token)
  │     ├─ /v1/memories (POST/GET/DELETE)           ciphertext + embedding
  │     ├─ /v1/vault                                encrypted backup envelope
  │     ├─ /v1/settings                             encrypted snapshot
  │     └─ /v1/devices                              device registry / push tokens
  └─ Supabase Auth (sign-in, refresh, magic link)

Dorai Cloud API (edge façade, Cloudflare Worker)
  ├─ verify Supabase JWT (JWKS)
  ├─ enforce quota + rate limit per user
  ├─ service-role calls to Supabase PostgREST (server-side only)
  └─ never log plaintext bodies, tokens or ciphertext

Supabase (authoritative store, project "dorai-ai-app")
  └─ Postgres + RLS: every row scoped to auth.uid()
```

## 4. Data model (Supabase, project `dorai-ai-app`)

All tables are `user_id`-scoped with RLS policies of the form
`using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id)`.

| Table | Purpose | Notable columns |
|---|---|---|
| `profiles` | One row per account | `id` (= `auth.users.id`), `plan`, `display_name`, timestamps |
| `devices` | Device registry and push tokens | `platform`, `label`, `push_token`, `last_seen_at` |
| `memories` | Long-term semantic memory | `key`, `category`, `content_ciphertext`, `content_iv`, `embedding vector(384)`, `hit_count` |
| `vault_records` | E2EE backup envelopes | `device_id`, `payload_version`, `iv`, `ciphertext`, `checksum`, `updated_at` |
| `settings_snapshots` | E2EE settings snapshot | `iv`, `ciphertext`, `checksum`, `updated_at` (one row per user) |
| `usage_events` | Quota accounting | `kind`, `units`, `created_at` |

Server-side tables never receive plaintext end-user content; the pre-existing permissive
`dorai_memories` / `dorai_sync_vault` tables (BYO-key era) are removed from the first-party project.

## 5. Security model

- **Auth**: Supabase Auth issues the JWT; the façade verifies signature, expiry and audience.
- **Isolation**: RLS is the isolation boundary. The service role exists only inside the façade.
- **Transport**: TLS everywhere; the façade strips and never forwards client-supplied auth headers.
- **Secrets**: Workers AI token, service-role key live only in Worker secrets / Supabase env — never in the app, never in the repo.
- **Build/CI**: release builds receive public endpoints and keys via GitHub Secrets, never committed.
- **Logging**: request metadata only (route, status, latency, user id hash). No bodies, no ciphertext, no tokens.

## 6. Deployment status (2026-11-16)

| Piece | Where | State |
|---|---|---|
| Supabase project | `dorai-ai-app` (ap-south-1, free tier, `ACTIVE_HEALTHY`) | live |
| Schema | migration `dorai_cloud_runtime_schema` | applied (`profiles`, `devices`, `memories`, `vault_records`, `settings_snapshots`, `usage_events`, RLS on all) |
| Search RPC | `match_user_memories(query_embedding, match_threshold, match_count)` | applied, `SECURITY INVOKER` so RLS filters the caller |
| Edge façade | `dorai-cloud-api` Worker on `https://dorai-cloud-api.hrhabib.workers.dev` | live, all `/v1` routes verified end-to-end |
| Worker source | `cloud/dorai-cloud-api/src/index.js` | PR #187 |

Façade notes proven during the smoke test:

- Supabase Auth issues **ES256** (ECDSA P-256) tokens, not RS256 — the Worker verifies both.
- The Worker needs **no service-role key**: it forwards the caller's access token to PostgREST, so RLS is the only isolation boundary.
- Workers AI through the **AI binding** returns `{ shape, data }` where `data` is a wrapped vector (`data[0]`), not a flat array.
- The `devices`/`vault_records` tables are keyed per user; a session's user id comes only from the verified token.

## 7. v1 plan

1. **Schema + auth**: user-scoped tables and RLS (this branch), Supabase Auth email provider enabled, `profiles` auto-provisioning on first call.
2. **Façade**: Worker with `/v1/*` routes, JWT verification, quota counters, `/v1/embeddings` proxy to Workers AI.
3. **App**: `DoraiCloudClient`, sign-in UI (Settings → Account), sign-out, session refresh.
4. **Memory**: write-through on `memory_store`/`memory_learn`, retrieval on chat prompts with the existing 1.5 s budget, decrypt top-k client-side.
5. **Vault + settings**: encrypted backup/restore through the façade instead of direct PostgREST.
6. **Devices**: registration and push-token storage for background alerts.

## 8. Open items

- Email provider configuration (console or SQL) and magic-link redirect URLs per platform.
- Quota policy per plan (free tier: e.g. N memories, M syncs/day) and abuse handling.
- Whether the FOSS flavor may talk to the first-party backend at all (policy decision).
- Migration path for users who already entered their own Supabase/Cloudflare keys (BYO mode stays as an expert option).
