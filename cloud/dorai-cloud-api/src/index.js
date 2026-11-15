/**
 * DORAI AI Cloud API — edge facade in front of Supabase.
 *
 * Responsibilities:
 *  - verify the end-user's Supabase Auth JWT (RS256, JWKS)
 *  - expose a small, versioned surface (/v1/*) the app can call
 *  - keep the Workers AI credential server-side
 *  - enforce per-user quotas and never log bodies, tokens or ciphertext
 *
 * Authorization model: the Worker never holds a Supabase service-role key. It
 * forwards the caller's own access token to PostgREST, so RLS stays the single
 * isolation boundary and the database can never be reached with elevated rights
 * from this code path. The caller's user id is taken only from the verified
 * token — never from a request body.
 */

const API_VERSION = "v1";
const EMBEDDING_MODEL = "@cf/baai/bge-small-en-v1.5";
const EMBEDDING_DIMENSIONS = 384;

/** Free-tier guardrails (per user, rolling 24h). */
const QUOTA = {
  embedding: 2000,
  memory_write: 500,
  vault_write: 50,
};

const CORS_HEADERS = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization,content-type",
  "access-control-allow-methods": "GET,POST,PUT,DELETE,OPTIONS",
};

export default {
  async fetch(request, env) {
    const startedAt = Date.now();
    const requestId = request.headers.get("cf-ray") || crypto.randomUUID();
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    if (!url.pathname.startsWith(`/${API_VERSION}/`)) {
      return json({ error: "not_found" }, 404);
    }

    if (url.pathname === `/${API_VERSION}/health`) {
      return json({ ok: true, service: "dorai-cloud-api", version: API_VERSION }, 200);
    }

    let session;
    try {
      const token = extractBearerToken(request);
      session = await verifyJwt(token, env);
    } catch (error) {
      return json({ error: "unauthorized", message: String(error.message || error) }, 401);
    }

    try {
      const response = await route(request, url, env, session);
      log({
        requestId,
        userId: session.sub,
        path: url.pathname,
        method: request.method,
        status: response.status,
        ms: Date.now() - startedAt,
      });
      return response;
    } catch (error) {
      log({
        requestId,
        userId: session.sub,
        path: url.pathname,
        method: request.method,
        status: 500,
        ms: Date.now() - startedAt,
        error: String(error.message || error),
      });
      return json({ error: "internal_error" }, 500);
    }
  },
};

async function route(request, url, env, session) {
  const path = url.pathname.replace(`/${API_VERSION}/`, "");
  const userId = session.sub;

  if (path === "me" && request.method === "GET") return handleMe(env, session);
  if (path === "embeddings" && request.method === "POST") return handleEmbeddings(request, env, session);
  if (path === "memories" && request.method === "POST") return handleMemoryUpsert(request, env, session);
  if (path === "memories/search" && request.method === "POST") return handleMemorySearch(request, env, session);
  if (path === "memories" && request.method === "DELETE") return handleMemoryDelete(request, env, session);
  if (path === "vault" && request.method === "PUT") return handleVaultPut(request, env, session);
  if (path === "vault" && request.method === "GET") return handleVaultGet(env, session);
  if (path === "settings" && request.method === "PUT") return handleSettingsPut(request, env, session);
  if (path === "settings" && request.method === "GET") return handleSettingsGet(env, session);
  if (path === "devices" && request.method === "POST") return handleDeviceRegister(request, env, session);
  if (path === "usage" && request.method === "GET") return handleUsage(env, session);

  return json({ error: "not_found" }, 404);
}

/* ------------------------------------------------------------------ auth */

function extractBearerToken(request) {
  const header = request.headers.get("authorization") || "";
  const match = /^Bearer\s+(.+)$/i.exec(header.trim());
  if (!match) throw new Error("missing bearer token");
  return match[1];
}

let jwksCache = { keys: null, expiresAt: 0 };

async function verifyJwt(token, env) {
  const parts = token.split(".");
  if (parts.length !== 3) throw new Error("malformed token");

  const header = JSON.parse(base64UrlDecode(parts[0]));
  const claims = JSON.parse(base64UrlDecode(parts[1]));
  const key = await resolveKey(header.kid, header.alg, env);

  // Supabase issues ES256 (ECDSA P-256) by default; older projects use RS256.
  const algorithm =
    header.alg === "ES256"
      ? { name: "ECDSA", hash: "SHA-256" }
      : header.alg === "RS256"
        ? { name: "RSASSA-PKCS1-v1_5" }
        : null;
  if (!algorithm) throw new Error(`unsupported token alg: ${header.alg}`);

  const verified = await crypto.subtle.verify(
    algorithm,
    key,
    base64UrlToBytes(parts[2]),
    new TextEncoder().encode(`${parts[0]}.${parts[1]}`),
  );
  if (!verified) throw new Error("invalid signature");

  const now = Math.floor(Date.now() / 1000);
  if (typeof claims.exp === "number" && claims.exp < now) throw new Error("token expired");
  const issuer = `${env.SUPABASE_URL}/auth/v1`;
  if (claims.iss && !String(claims.iss).startsWith(issuer)) throw new Error("invalid issuer");
  if (!claims.sub) throw new Error("token has no subject");
  return { sub: claims.sub, token, email: claims.email || null };
}

async function resolveKey(kid, alg, env) {
  const now = Date.now();
  if (!jwksCache.keys || now > jwksCache.expiresAt) {
    const response = await fetch(`${env.SUPABASE_URL}/auth/v1/.well-known/jwks.json`, {
      headers: { apikey: env.SUPABASE_ANON_KEY || "" },
    });
    if (!response.ok) throw new Error("jwks fetch failed");
    const body = await response.json();
    jwksCache = { keys: body.keys || [], expiresAt: now + 10 * 60 * 1000 };
  }
  const jwk = jwksCache.keys.find((k) => k.kid === kid) || jwksCache.keys[0];
  if (!jwk) throw new Error("signing key not found");
  const importAlgorithm =
    alg === "ES256"
      ? { name: "ECDSA", namedCurve: "P-256" }
      : { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" };
  return crypto.subtle.importKey("jwk", jwk, importAlgorithm, false, ["verify"]);
}

function base64UrlToBytes(value) {
  const padded = value.replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(padded + "=".repeat((4 - (padded.length % 4)) % 4));
  return Uint8Array.from(binary, (c) => c.charCodeAt(0));
}

function base64UrlDecode(value) {
  return new TextDecoder().decode(base64UrlToBytes(value));
}

/* ---------------------------------------------------------------- routes */

async function handleMe(env, session) {
  const profiles = await supabase(env, session, "/rest/v1/profiles?on_conflict=id", {
    method: "POST",
    headers: { Prefer: "resolution=merge-duplicates,return=representation" },
    body: JSON.stringify({ id: session.sub, updated_at: new Date().toISOString() }),
  });
  const row = Array.isArray(profiles) ? profiles[0] : profiles;
  return json({
    user_id: session.sub,
    email: session.email,
    plan: row?.plan || "free",
    display_name: row?.display_name || null,
    created_at: row?.created_at || null,
    usage: await usageSummary(env, session),
  });
}

async function handleEmbeddings(request, env, session) {
  const body = await readJson(request);
  const text = typeof body?.text === "string" ? body.text.trim() : "";
  if (!text) return json({ error: "text_required" }, 400);
  if (text.length > 8000) return json({ error: "text_too_long" }, 400);

  const quota = await enforceDailyQuota(env, session, "embedding", QUOTA.embedding);
  if (!quota.ok) return json({ error: "quota_exceeded", limit: quota.limit }, 429);

  const embedding = await embed(env, text).catch((error) => {
    log({ event: "embedding_failed", detail: String(error?.message || error) });
    return null;
  });
  if (!embedding) return json({ error: "embedding_failed" }, 502);
  await recordUsage(env, session, "embedding");
  return json({ embedding, model: EMBEDDING_MODEL, dimensions: EMBEDDING_DIMENSIONS });
}

async function handleMemoryUpsert(request, env, session) {
  const body = await readJson(request);
  const key = typeof body?.key === "string" ? body.key.trim() : "";
  if (!key) return json({ error: "key_required" }, 400);
  if (!body?.content_ciphertext || !body?.content_iv) return json({ error: "encrypted_content_required" }, 400);

  const quota = await enforceDailyQuota(env, session, "memory_write", QUOTA.memory_write);
  if (!quota.ok) return json({ error: "quota_exceeded", limit: quota.limit }, 429);

  const now = Date.now();
  const row = {
    user_id: session.sub,
    key,
    category: typeof body.category === "string" ? body.category : "LEARNINGS",
    content_ciphertext: String(body.content_ciphertext),
    content_iv: String(body.content_iv),
    embedding: Array.isArray(body.embedding) ? body.embedding : null,
    hit_count: Number.isFinite(body.hit_count) ? body.hit_count : 1,
    source: typeof body.source === "string" ? body.source : null,
    created_at: now,
    updated_at: now,
  };

  const result = await supabase(env, session, "/rest/v1/memories?on_conflict=user_id,key", {
    method: "POST",
    headers: { Prefer: "resolution=merge-duplicates,return=representation" },
    body: JSON.stringify(row),
  });
  await recordUsage(env, session, "memory_write");
  return json({ ok: true, memory: Array.isArray(result) ? result[0] : result });
}

async function handleMemorySearch(request, env, session) {
  const body = await readJson(request);
  const embedding = Array.isArray(body?.embedding) ? body.embedding : null;
  if (!embedding || embedding.length !== EMBEDDING_DIMENSIONS) {
    return json({ error: "embedding_required" }, 400);
  }
  const limit = clamp(Number(body?.limit) || 5, 1, 20);
  const threshold = typeof body?.threshold === "number" ? body.threshold : 0.5;

  const rows = await supabase(env, session, "/rest/v1/rpc/match_user_memories", {
    method: "POST",
    body: JSON.stringify({
      p_query_embedding: embedding,
      p_match_threshold: threshold,
      p_match_count: limit,
    }),
  });
  return json({ memories: Array.isArray(rows) ? rows : [] });
}

async function handleMemoryDelete(request, env, session) {
  const url = new URL(request.url);
  const key = url.searchParams.get("key");
  if (!key) return json({ error: "key_required" }, 400);
  await supabase(env, session, `/rest/v1/memories?key=eq.${encodeURIComponent(key)}`, {
    method: "DELETE",
  });
  return json({ ok: true });
}

async function handleVaultPut(request, env, session) {
  const body = await readJson(request);
  if (!body?.iv || !body?.ciphertext || !body?.checksum) return json({ error: "envelope_required" }, 400);

  const quota = await enforceDailyQuota(env, session, "vault_write", QUOTA.vault_write);
  if (!quota.ok) return json({ error: "quota_exceeded", limit: quota.limit }, 429);

  const row = {
    user_id: session.sub,
    device_id: body.device_id || null,
    payload_version: Number(body.payload_version) || 1,
    iv: String(body.iv),
    ciphertext: String(body.ciphertext),
    checksum: String(body.checksum),
    updated_at: Date.now(),
  };
  await supabase(env, session, "/rest/v1/vault_records", {
    method: "POST",
    headers: { Prefer: "return=minimal" },
    body: JSON.stringify(row),
  });
  await recordUsage(env, session, "vault_write");
  return json({ ok: true, updated_at: row.updated_at });
}

async function handleVaultGet(env, session) {
  const rows = await supabase(
    env,
    session,
    "/rest/v1/vault_records?select=iv,ciphertext,checksum,payload_version,device_id,updated_at&order=updated_at.desc&limit=1",
  );
  const row = Array.isArray(rows) ? rows[0] : null;
  if (!row) return json({ found: false });
  return json({ found: true, envelope: row });
}

async function handleSettingsPut(request, env, session) {
  const body = await readJson(request);
  if (!body?.iv || !body?.ciphertext || !body?.checksum) return json({ error: "envelope_required" }, 400);
  await supabase(env, session, "/rest/v1/settings_snapshots?on_conflict=user_id", {
    method: "POST",
    headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
    body: JSON.stringify({
      user_id: session.sub,
      iv: String(body.iv),
      ciphertext: String(body.ciphertext),
      checksum: String(body.checksum),
      updated_at: Date.now(),
    }),
  });
  return json({ ok: true, updated_at: Date.now() });
}

async function handleSettingsGet(env, session) {
  const rows = await supabase(env, session, "/rest/v1/settings_snapshots?select=iv,ciphertext,checksum,updated_at&limit=1");
  const row = Array.isArray(rows) ? rows[0] : null;
  if (!row) return json({ found: false });
  return json({ found: true, snapshot: row });
}

async function handleDeviceRegister(request, env, session) {
  const body = await readJson(request);
  const platform = typeof body?.platform === "string" ? body.platform : "";
  if (!platform) return json({ error: "platform_required" }, 400);
  const label = typeof body?.label === "string" ? body.label : platform;
  const now = Date.now();
  const rows = await supabase(env, session, "/rest/v1/devices?on_conflict=user_id,platform,label", {
    method: "POST",
    headers: { Prefer: "resolution=merge-duplicates,return=representation" },
    body: JSON.stringify({
      user_id: session.sub,
      platform,
      label,
      push_token: typeof body?.push_token === "string" ? body.push_token : null,
      last_seen_at: now,
      created_at: now,
    }),
  });
  return json({ ok: true, device: Array.isArray(rows) ? rows[0] : rows });
}

async function handleUsage(env, session) {
  return json({ usage: await usageSummary(env, session), quota: QUOTA });
}

/* --------------------------------------------------------------- helpers */

async function embed(env, text) {
  const detail = {};

  // Workers AI binding. The response is { data, shape, pooling, usage } where
  // `data` is a flat vector for embedding models.
  const pickVector = (result) => {
    if (!result) return null;
    const data = result.data;
    if (Array.isArray(data)) {
      if (typeof data[0] === "number") return data;
      if (Array.isArray(data[0])) return data[0];
      if (data[0] && Array.isArray(data[0].embedding)) return data[0].embedding;
    }
    if (Array.isArray(result.embedding)) return result.embedding;
    return null;
  };

  if (env.AI && typeof env.AI.run === "function") {
    try {
      const result = await env.AI.run(EMBEDDING_MODEL, { text: [text] });
      const vector = pickVector(result);
      if (Array.isArray(vector)) return vector;
      detail.ai = "response had no embedding vector";
    } catch (error) {
      detail.ai = String(error?.message || error).slice(0, 160);
    }
  } else {
    detail.ai = "ai binding missing";
  }

  // Fallback: REST call with a scoped API token.
  if (env.CLOUDFLARE_ACCOUNT_ID && env.CLOUDFLARE_API_TOKEN) {
    const response = await fetch(
      `https://api.cloudflare.com/client/v4/accounts/${env.CLOUDFLARE_ACCOUNT_ID}/ai/run/${encodeURIComponent(EMBEDDING_MODEL)}`,
      {
        method: "POST",
        headers: {
          authorization: `Bearer ${env.CLOUDFLARE_API_TOKEN}`,
          "content-type": "application/json",
        },
        body: JSON.stringify({ text: [text] }),
      },
    );
    if (response.ok) {
      const body = await response.json();
      const vector = pickVector(body?.result);
      if (Array.isArray(vector)) return vector;
      detail.rest = "response had no embedding vector";
    } else {
      detail.rest = `http ${response.status}`;
    }
  } else {
    detail.rest = "no api token configured";
  }

  throw new Error(`embedding failed: ${JSON.stringify(detail)}`);
}

/** Calls PostgREST with the caller's own token so RLS applies. */
async function supabase(env, session, path, options = {}) {
  const response = await fetch(`${env.SUPABASE_URL}${path}`, {
    ...options,
    headers: {
      apikey: env.SUPABASE_ANON_KEY || "",
      authorization: `Bearer ${session.token}`,
      "content-type": "application/json",
      ...(options.headers || {}),
    },
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`supabase ${path} failed [${response.status}]: ${text.slice(0, 200)}`);
  }
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

async function enforceDailyQuota(env, session, kind, limit) {
  const summary = await usageSummary(env, session);
  const used = summary[kind] || 0;
  return { ok: used < limit, limit, used };
}

async function usageSummary(env, session) {
  const since = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
  const rows = await supabase(
    env,
    session,
    `/rest/v1/usage_events?select=kind,units&created_at=gte.${since}`,
  );
  const summary = { embedding: 0, memory_write: 0, vault_write: 0 };
  for (const row of Array.isArray(rows) ? rows : []) {
    summary[row.kind] = (summary[row.kind] || 0) + (row.units || 0);
  }
  return summary;
}

async function recordUsage(env, session, kind, units = 1) {
  try {
    await supabase(env, session, "/rest/v1/usage_events", {
      method: "POST",
      headers: { Prefer: "return=minimal" },
      body: JSON.stringify({ user_id: session.sub, kind, units }),
    });
  } catch {
    /* usage accounting must never break a request */
  }
}

async function readJson(request) {
  try {
    return await request.json();
  } catch {
    return null;
  }
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function json(payload, status) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "content-type": "application/json", ...CORS_HEADERS },
  });
}

function log(entry) {
  console.log(JSON.stringify({ app: "dorai-cloud-api", ...entry }));
}
