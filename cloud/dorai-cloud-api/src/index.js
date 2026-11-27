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
const FIREBASE_PROJECT_ID = "dorai-ai-app";

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
      await ensureFirebaseAuthenticatedRole(session, env);
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

const jwksCache = new Map();

async function verifyJwt(token, env) {
  const parts = token.split(".");
  if (parts.length !== 3) throw new Error("malformed token");

  const header = JSON.parse(base64UrlDecode(parts[0]));
  const claims = JSON.parse(base64UrlDecode(parts[1]));
  const firebaseProject = env.FIREBASE_PROJECT_ID || FIREBASE_PROJECT_ID;
  const firebaseIssuer = `https://securetoken.google.com/${firebaseProject}`;
  const supabaseIssuer = `${env.SUPABASE_URL}/auth/v1`;
  const isFirebase = String(claims.iss || "") === firebaseIssuer;
  const isSupabase = String(claims.iss || "").startsWith(supabaseIssuer);
  if (!isFirebase && !isSupabase) throw new Error("invalid issuer");
  if (isFirebase && claims.aud !== firebaseProject) throw new Error("invalid audience");

  const key = await resolveKey(header.kid, header.alg, env, isFirebase ? "firebase" : "supabase");

  // Supabase may issue ES256 (ECDSA P-256) or RS256; Firebase issues RS256.
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
  if (typeof claims.iat === "number" && claims.iat > now + 300) throw new Error("token issued in future");
  if (isFirebase && claims.auth_time && Number(claims.auth_time) > now + 300) throw new Error("invalid auth_time");
  if (!claims.sub) throw new Error("token has no subject");
  return {
    sub: String(claims.sub),
    token,
    email: claims.email || null,
    provider: isFirebase ? "firebase" : "supabase",
    role: claims.role || null,
  };
}

async function resolveKey(kid, alg, env, provider) {
  const cacheKey = provider;
  const now = Date.now();
  const cached = jwksCache.get(cacheKey);
  if (!cached || now > cached.expiresAt) {
    const url =
      provider === "firebase"
        ? "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
        : `${env.SUPABASE_URL}/auth/v1/.well-known/jwks.json`;
    const response = await fetch(url, {
      headers: provider === "firebase" ? {} : { apikey: env.SUPABASE_ANON_KEY || "" },
    });
    if (!response.ok) throw new Error("jwks fetch failed");
    const body = await response.json();
    jwksCache.set(cacheKey, { keys: body.keys || [], expiresAt: now + 10 * 60 * 1000 });
  }
  const entry = jwksCache.get(cacheKey);
  const jwk = entry.keys.find((k) => k.kid === kid) || (provider === "firebase" ? null : entry.keys[0]);
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

async function ensureFirebaseAuthenticatedRole(session, env) {
  if (session.provider !== "firebase" || session.role === "authenticated") return;
  const raw = env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw) return;
  try {
    const sa = JSON.parse(raw);
    const accessToken = await getGoogleAccessToken(sa);
    const body = JSON.stringify({ localId: session.sub, customAttributes: JSON.stringify({ role: "authenticated" }) });
    await fetch(`https://identitytoolkit.googleapis.com/v1/projects/${sa.project_id}/accounts:update`, {
      method: "POST",
      headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
      body,
    });
  } catch {
    // Provisioning is best-effort; request should still proceed.
  }
}

let googleAccessTokenCache = { token: null, expiresAt: 0 };

async function getGoogleAccessToken(sa) {
  const now = Math.floor(Date.now() / 1000);
  if (googleAccessTokenCache.token && now < googleAccessTokenCache.expiresAt - 60) return googleAccessTokenCache.token;
  const header = base64UrlEncodeBytes(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const payload = base64UrlEncodeBytes(new TextEncoder().encode(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/identitytoolkit https://www.googleapis.com/auth/cloud-platform",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  })));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(sa.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${payload}`));
  const jwt = `${header}.${payload}.${base64UrlEncodeBytes(new Uint8Array(sig))}`;
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  if (!res.ok) throw new Error("google token failed");
  const data = await res.json();
  googleAccessTokenCache = { token: data.access_token, expiresAt: now + Number(data.expires_in || 3600) };
  return data.access_token;
}

function base64UrlEncodeBytes(bytes) {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function pemToArrayBuffer(pem) {
  const b64 = String(pem).replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

function log(entry) {
  console.log(JSON.stringify({ app: "dorai-cloud-api", ...entry }));
}
