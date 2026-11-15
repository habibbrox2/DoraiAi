# DORAI AI Cloud API

Edge facade (Cloudflare Worker) in front of Supabase for the **runtime** app path.
The *development* path (Claude Code / Codex / VS Code / Copilot managing Cloudflare,
Supabase, GitHub) is a separate concern — see `docs/architecture/dorai-cloud-runtime.md`.

## Why a facade instead of the app talking to Supabase directly

- The Supabase service-role key and the Workers AI token never reach the device.
- One versioned surface (`/v1/*`) to change later without shipping an app update.
- Per-user quotas and rate limits in one place.
- Request logs contain metadata only (route, status, latency, user id) — never bodies,
  tokens or ciphertext.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/v1/health` | Liveness (no auth) |
| GET | `/v1/me` | Profile (auto-provisioned) + usage |
| POST | `/v1/embeddings` | `{text}` → 384-dim vector (Workers AI, server-side token) |
| POST | `/v1/memories` | Upsert an encrypted memory (`content_ciphertext` + `content_iv` + `embedding`) |
| POST | `/v1/memories/search` | `{embedding, limit, threshold}` → top-k encrypted rows |
| DELETE | `/v1/memories?key=…` | Forget one memory |
| PUT | `/v1/vault` | Store the E2EE backup envelope |
| GET | `/v1/vault` | Latest E2EE backup envelope |
| PUT | `/v1/settings` | Store the E2EE settings snapshot |
| GET | `/v1/settings` | Latest E2EE settings snapshot |
| POST | `/v1/devices` | Register / refresh a device and its push token |
| GET | `/v1/usage` | Usage counters + free-tier quotas |

All `/v1/*` routes except `/v1/health` require `Authorization: Bearer <supabase-jwt>`.

## Security model

- The JWT is verified against the project's JWKS (RS256). The caller's user id comes
  **only** from the verified token, never from a request body.
- The service role bypasses RLS, so every query filters on that user id explicitly.
- The `match_user_memories` RPC is `security definer` and executable only by
  `service_role`, so a client cannot read another user's rows.
- Memory, vault and settings content is encrypted on the device (AES-256-GCM). The
  server stores ciphertext plus the embedding vector used for ranking.

## Configuration

| Name | Kind | Purpose |
|---|---|---|
| `SUPABASE_URL` | var | `https://<project-ref>.supabase.co` |
| `SUPABASE_ANON_KEY` | secret | Used only to fetch the JWKS document |
| `SUPABASE_SERVICE_ROLE_KEY` | secret | Server-side data access |
| `CLOUDFLARE_ACCOUNT_ID` | var | Workers AI account |
| `CLOUDFLARE_API_TOKEN` | secret | Workers AI call token |

## Local development

```sh
pnpm install
pnpm exec wrangler dev          # needs .dev.vars with the values above
pnpm exec wrangler deploy
```

The package is standalone on purpose: it is not part of the Gradle build and not part
of the `frontend/` pnpm workspace.
