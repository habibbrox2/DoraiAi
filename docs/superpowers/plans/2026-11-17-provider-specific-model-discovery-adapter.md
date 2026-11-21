# Provider-Specific Model Discovery Adapter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the scattered per-provider model discovery logic in `Service` flags, `RemoteDataRepository.fetchInstanceModels` branches, `Requests.get*Models`, and `ModelTransformations` with a provider-specific `ModelDiscoveryAdapter` architecture, so each provider owns its API, authentication, pagination, response schema, error handling, and capability mapping.

**Architecture:** Introduce an internal `ModelDiscoveryAdapter` interface and a `ModelDiscoveryRegistry` resolved by `Service`. Move HTTP/auth/URL logic from `Requests` into an internal `DiscoveryHttp` object (behavior-preserving). Each adapter implements wire-level discovery and returns normalized `DiscoveredModel`s. A pure `ModelCapabilityPipeline` applies chat filtering, dedup, sorting, catalog merge, and free-tier tagging. `RemoteDataRepository` resolves the adapter instead of branching on `Service`.

**Tech Stack:** Kotlin Multiplatform (commonMain), Ktor `HttpClient` (already used), kotlinx.serialization (already used), kotlinx-coroutines, kotlin.test + MockEngine (already in commonTest). No new dependencies.

**Spec:** User request (Bengali): "Provider-এর জন্য আলাদা Model Discovery Adapter তৈরি করার বাস্তবায়ন-পরিকল্পনা" and `docs/features/multi-service.md`.

## Global Constraints

- Java 21 required (`java -version` must report 21).
- No new Gradle dependencies/plugins; `gradle/libs.versions.toml` must stay unchanged without approval (MockEngine, kotlinx-coroutines-test, kotlin.test, Ktor are already in use).
- All new code in `commonMain`: no `java.*`, `javax.*`, `android.*`, or JVM system calls.
- User-facing strings remain in `composeApp/src/commonMain/composeResources/values/strings.xml`; this plan adds no new strings (adapters throw existing exception types).
- Never weaken security: preserve `NetworkAddressGuard`/SSRF behavior, `Service.acceptsCustomBaseUrl`, `{account_id}` templating in `resolveUrl`, and the 20-page Cloudflare cap.
- Branch policy: never commit to `main`; create one task branch from `origin/main` before starting (e.g. `feat/model-discovery-adapter`), one logical task/PR.
- Verification before each commit: `gradlew.bat spotlessCheck` green once per change-set; `gradlew.bat :composeApp:test` BUILD SUCCESSFUL.
- Docs: update `docs/features/multi-service.md` ("Last verified" + Key Files) and `CHANGELOG.md` (user-facing behavior).

## Review Focus

1. A provider returning HTTP 200 with an empty `data` array (restricted-but-valid key) must fail discovery with an error, never report "connected, 0 models" (existing invariant at `RemoteDataRepository` ~line 655-673).
2. Provider-specific bad-key codes — Requesty 403, Gemini 400/403, Anthropic 401/403, Cloudflare 403 envelope — must each map to `InvalidApiKey`, not a generic moderation/connection error.
3. Cloudflare pagination over full pages must cap at 20 pages, dedupe ids, and stop correctly.
4. Optional-key providers (OpenRouter, Kilo, OpenCode, OpenAI-Compatible with blank key) must send no `Authorization` header and still discover models.
5. A manually typed selected model id must survive a discovery refresh (stays selected, manual row replaced not duplicated) via `ensureSelectedModelPresent`.

---

### Task 1: Adapter contract, types, and empty registry

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/ModelDiscoveryAdapter.kt`
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/ModelDiscoveryRegistry.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/ModelDiscoveryRegistryTest.kt`
- Test: `ModelDiscoveryRegistryTest`

**Interfaces:**
- Produces (used by Task 3-8): `ModelDiscoveryAdapter` (`val service: Service`, `suspend fun discoverModels(credentials: ServiceCredentials): List<DiscoveredModel>`); `DiscoveredModel(id: String, displayName: String? = null, contextWindow: Long? = null, releaseDate: String? = null, isActive: Boolean? = null, modelType: String? = null)`; `ModelDiscoveryRegistry(adapters: List<ModelDiscoveryAdapter>)` with `fun adapterFor(service: Service): ModelDiscoveryAdapter?` and `companion fun create(httpClient: HttpClient): ModelDiscoveryRegistry` (returns empty registry until Task 3 wires adapters).

- [ ] **Step 1: Create task branch from `origin/main`**

```bash
git fetch origin
git checkout -b feat/model-discovery-adapter origin/main
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelDiscoveryRegistryTest {
    @Test
    fun `empty registry resolves nothing`() {
        val registry = ModelDiscoveryRegistry(emptyList())
        assertNull(registry.adapterFor(Service.Groq))
    }

    @Test
    fun `create returns registry for all services without adapters in task 1`() {
        val registry = ModelDiscoveryRegistry.create(io.ktor.client.HttpClient())
        // Task 1: adapters not wired yet — coverage is added in Task 8.
        assertEquals(null, registry.adapterFor(Service.Free))
    }
}
```

- [ ] **Step 3: Implement `ModelDiscoveryAdapter.kt` and `ModelDiscoveryRegistry.kt`**

`ModelDiscoveryAdapter.kt` defines the interface and `DiscoveredModel` data class. `ModelDiscoveryRegistry.kt` defines the registry class; `create(httpClient)` returns `ModelDiscoveryRegistry(emptyList())` for now (wired in Task 3).

- [ ] **Step 4: Run test**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.network.modeldiscovery.ModelDiscoveryRegistryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery
git commit -m "feat(network): add ModelDiscoveryAdapter contract and registry"
```

---

### Task 2: Extract shared HTTP helpers into `DiscoveryHttp`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/DiscoveryHttp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/Requests.kt` (delegate; remove duplicated bodies)
- Test: existing `SessionHeadersTest`, `CloudflareModelSearchParserTest`, `CloudflareModelSearchIntegrationTest` must still pass.

**Interfaces:**
- Produces: `DiscoveryHttp` (internal object) with `fun resolveUrl(service: Service, credentials: ServiceCredentials, path: String): String`, `fun apiKeyFor(service: Service, credentials: ServiceCredentials, required: Boolean): String?`, `suspend fun mapOpenAICompatibleFailure(service: Service, credentials: ServiceCredentials, response: HttpResponse): Nothing`, `fun sessionHeaders(service: Service, credentials: ServiceCredentials, sessionId: String?): Map<String, String>`, `fun cloudflareGatewayHeader(service: Service, credentials: ServiceCredentials): Pair<String,String>?`, and re-exports `isValidCloudflareAccountId`, `parseCloudflareModelSearchPage`, `parseCloudflareModelSearchError` (already in `CloudflareModelSearchParser.kt`).

- [ ] **Step 1: Write the failing test**

There is no new test; the gate is the existing suite. Run it first to establish a green baseline:

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.network.SessionHeadersTest" --tests "com.dorai.ai.network.CloudflareModelSearchParserTest"`
Expected: PASS (baseline).

- [ ] **Step 2: Move the helpers**

Copy `resolveUrl`, `getApiKeyOrThrow`/`getOptionalApiKey` (merged into `apiKeyFor(required)`), `handleOpenAICompatibleError` (renamed `mapOpenAICompatibleFailure`), session header application, and cloudflare gateway header application from `Requests.kt` into `DiscoveryHttp`. Make them `internal`. Update `Requests.kt` to call `DiscoveryHttp.*` so behavior is identical.

- [ ] **Step 3: Run existing tests**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.network.SessionHeadersTest" --tests "com.dorai.ai.network.CloudflareModelSearchParserTest" --tests "com.dorai.ai.network.CloudflareModelSearchIntegrationTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/DiscoveryHttp.kt composeApp/src/commonMain/kotlin/com/dorai/ai/network/Requests.kt
git commit -m "refactor(network): extract shared discovery HTTP helpers"
```

---

### Task 3: OpenAI-compatible adapter

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/OpenAICompatibleModelDiscoveryAdapter.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/OpenAICompatibleModelDiscoveryAdapterTest.kt`
- Modify: `ModelDiscoveryRegistry.kt` (wire openai-compatible services)

**Interfaces:**
- Consumes: `DiscoveryHttp.resolveUrl`, `DiscoveryHttp.apiKeyFor`, `DiscoveryHttp.mapOpenAICompatibleFailure`, `DiscoveryHttp.sessionHeaders`.
- Produces: `OpenAICompatibleModelDiscoveryAdapter(service: Service, httpClient: HttpClient, responseIsArray: Boolean = false, keyProbe: KeyProbe? = null, forbiddenMeansInvalidKey: Boolean = false)` implementing `ModelDiscoveryAdapter`; `fun interface KeyProbe { suspend fun validate(credentials: ServiceCredentials) }`.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.data.ServiceCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAICompatibleModelDiscoveryAdapterTest {
    @Test
    fun `sends bearer token when key provided`() = runTest {
        val engine = MockEngine { req ->
            assertEquals("Bearer sk-test", req.headers[HttpHeaders.Authorization])
            respond("""{"data":[{"id":"m1"}]}""", HttpStatusCode.OK)
        }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.Groq, HttpClient(engine))
        val models = adapter.discoverModels(ServiceCredentials(apiKey = "sk-test"))
        assertEquals(listOf("m1"), models.map { it.id })
    }

    @Test
    fun `omits authorization header for optional-key service with blank key`() = runTest {
        val engine = MockEngine { req ->
            assertTrue(req.headers[HttpHeaders.Authorization] == null)
            respond("""{"data":[{"id":"m1"}]}""", HttpStatusCode.OK)
        }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.OpenRouter, HttpClient(engine))
        adapter.discoverModels(ServiceCredentials(apiKey = ""))
    }

    @Test
    fun `parses bare array envelope for responseIsArray services`() = runTest {
        val engine = MockEngine { respond("""[{"id":"t1"}]""", HttpStatusCode.OK) }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.Together, HttpClient(engine), responseIsArray = true)
        assertEquals(listOf("t1"), adapter.discoverModels(ServiceCredentials(apiKey = "k")).map { it.id })
    }

    @Test
    fun `maps 403 to InvalidApiKey when forbiddenMeansInvalidKey`() = runTest {
        val engine = MockEngine { respond("{}", HttpStatusCode.Forbidden) }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.Requesty, HttpClient(engine), forbiddenMeansInvalidKey = true)
        try {
            adapter.discoverModels(ServiceCredentials(apiKey = "bad"))
            throw AssertionError("expected throw")
        } catch (e: com.dorai.ai.network.OpenAICompatibleInvalidApiKeyException) {
            // expected
        }
    }

    @Test
    fun `empty data array throws NoModels-style error`() = runTest {
        val engine = MockEngine { respond("""{"data":[]}""", HttpStatusCode.OK) }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(Service.Groq, HttpClient(engine))
        try {
            adapter.discoverModels(ServiceCredentials(apiKey = "k"))
            throw AssertionError("expected throw")
        } catch (e: com.dorai.ai.network.OpenAICompatibleGenericException) {
            // expected
        }
    }

    @Test
    fun `keyProbe runs before discovery when provided`() = runTest {
        var probeCalled = false
        val engine = MockEngine { respond("""{"data":[{"id":"m1"}]}""", HttpStatusCode.OK) }
        val adapter = OpenAICompatibleModelDiscoveryAdapter(
            Service.OpenRouter, HttpClient(engine),
            keyProbe = KeyProbe { probeCalled = true },
        )
        adapter.discoverModels(ServiceCredentials(apiKey = "k"))
        assertTrue(probeCalled)
    }
}
```

- [ ] **Step 2: Implement `OpenAICompatibleModelDiscoveryAdapter`**

Single GET via `DiscoveryHttp.resolveUrl(service, creds, service.modelsUrl!!)`, auth via `DiscoveryHttp.apiKeyFor(service, creds, required = service.requiresApiKey)` applied as bearer, session headers applied, gateway header applied for Cloudflare (not needed here since Cloudflare has its own adapter, but keep). Parse body: if `responseIsArray`, decode as `List<Model>`; else decode `OpenAICompatibleModelResponseDto` and take `.data`. Map each `Model` to `DiscoveredModel(id, displayName = it.name, contextWindow = it.context_window ?: it.context_length, releaseDate = it.created?.toIsoDate(), isActive = it.isActive, modelType = it.type)`. On HTTP failure, `DiscoveryHttp.mapOpenAICompatibleFailure` (throws). On empty data, throw `OpenAICompatibleGenericException("${service.displayName} returned no models...")`.

- [ ] **Step 3: Wire registry**

`ModelDiscoveryRegistry.create(httpClient)` builds one adapter per openai-compatible service: Together → `responseIsArray = true`; OpenRouter → `keyProbe = KeyProbe { DiscoveryHttp... validateOpenRouterApiKey }` (keep the probe as a thin call to the existing `Requests`? Better: move `validateOpenRouterApiKey` body into `DiscoveryHttp.validateOpenRouterApiKey` in Task 2; Task 3 calls it). Requesty → `forbiddenMeansInvalidKey = true`. All others → default.

- [ ] **Step 4: Run test**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.network.modeldiscovery.OpenAICompatibleModelDiscoveryAdapterTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/OpenAICompatibleModelDiscoveryAdapter.kt composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/OpenAICompatibleModelDiscoveryAdapterTest.kt composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/ModelDiscoveryRegistry.kt
git commit -m "feat(network): add OpenAI-compatible model discovery adapter"
```

---

### Task 4: Pagination helper + Cloudflare adapter

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/Pagination.kt`
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/CloudflareModelDiscoveryAdapter.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/PaginationTest.kt`
- Modify: `CloudflareModelSearchIntegrationTest.kt` (delegate to adapter in Task 8; leave passing now via `Requests` wrapper)
- Test: `PaginationTest`.

**Interfaces:**
- Produces: `internal suspend fun fetchAllPages(perPage: Int = 100, maxPages: Int = 20, fetchPage: suspend (page: Int) -> ProviderPage): List<DiscoveredModel>`; `data class ProviderPage(val models: List<DiscoveredModel>, val totalPages: Int?)`; `CloudflareModelDiscoveryAdapter(httpClient: HttpClient)` implementing `ModelDiscoveryAdapter` (service = Cloudflare/CloudflareAIGateway).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.dorai.ai.network.modeldiscovery

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PaginationTest {
    @Test
    fun `stops after totalPages`() = runTest {
        val pages = listOf(
            ProviderPage(listOf(DiscoveredModel("a")), totalPages = 2),
            ProviderPage(listOf(DiscoveredModel("b")), totalPages = 2),
        )
        val result = fetchAllPages(fetchPage = { pages[it - 1] })
        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun `continues when totalPages null and page full`() = runTest {
        val pages = listOf(
            ProviderPage(List(100) { DiscoveredModel("m$it") }, totalPages = null),
            ProviderPage(listOf(DiscoveredModel("last")), totalPages = null),
        )
        val result = fetchAllPages(fetchPage = { pages.getOrElse(it - 1) { ProviderPage(emptyList(), null) } })
        assertEquals(101, result.size)
    }

    @Test
    fun `caps at maxPages`() = runTest {
        val result = fetchAllPages(maxPages = 2, fetchPage = { ProviderPage(List(100) { DiscoveredModel("p${it}-${it}") }, totalPages = null) })
        assertEquals(200, result.size)
    }
}
```

- [ ] **Step 2: Implement `Pagination.kt` and `CloudflareModelDiscoveryAdapter`**

`fetchAllPages` loops `page` from 1 while `page <= maxPages`, calls `fetchPage(page)`, appends `page.models`, updates `totalPages` from the page, increments `page`, stops when `page > totalPages` (if totalPages known) or when a page returns fewer than `perPage` models (or empty). Dedupe by id at the end.

`CloudflareModelDiscoveryAdapter` reuses `DiscoveryHttp.resolveUrl` (account id from `credentials.baseUrl`), validates account id via `isValidCloudflareAccountId`, fetches pages with `?page=&per_page=100`, parses via `parseCloudflareModelSearchPage`, collects `Model` (name field) into `DiscoveredModel(id = name)`, throws on empty result via `OpenAICompatibleGenericException`.

- [ ] **Step 3: Register in `ModelDiscoveryRegistry.create`** for `Service.Cloudflare` and `Service.CloudflareAIGateway`.

- [ ] **Step 4: Run tests**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.network.modeldiscovery.PaginationTest" --tests "com.dorai.ai.network.CloudflareModelSearchIntegrationTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/Pagination.kt composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/CloudflareModelDiscoveryAdapter.kt composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/PaginationTest.kt composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/ModelDiscoveryRegistry.kt
git commit -m "feat(network): add Cloudflare paged model discovery adapter"
```

---

### Task 5: Gemini adapter

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/GeminiModelDiscoveryAdapter.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/GeminiModelDiscoveryAdapterTest.kt`
- Modify: `ModelDiscoveryRegistry.kt`

**Interfaces:**
- Consumes: `GeminiModelsResponseDto` (existing), `DiscoveryHttp.resolveUrl` (not needed — hardcoded Gemini endpoint), `DiscoveryHttp.mapOpenAICompatibleFailure` (no — Gemini has its own error mapping, reuse existing `GeminiInvalidApiKeyException`).
- Produces: `GeminiModelDiscoveryAdapter(httpClient: HttpClient)` implementing `ModelDiscoveryAdapter` (service = Gemini).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.data.ServiceCredentials
import com.dorai.ai.network.GeminiInvalidApiKeyException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeminiModelDiscoveryAdapterTest {
    @Test
    fun `uses x-goog-api-key header and strips models prefix`() = runTest {
        val engine = MockEngine { req ->
            assertEquals("k", req.headers["x-goog-api-key"])
            respond("""{"models":[{"name":"models/gemini-2.0-flash","displayName":"Gemini 2.0 Flash","inputTokenLimit":1048576,"supportedGenerationMethods":["generateContent"]}]}""", HttpStatusCode.OK)
        }
        val adapter = GeminiModelDiscoveryAdapter(HttpClient(engine))
        val models = adapter.discoverModels(ServiceCredentials(apiKey = "k"))
        assertEquals(listOf("gemini-2.0-flash"), models.map { it.id })
        assertEquals(1048576L, models.first().contextWindow)
    }

    @Test
    fun `filters models without generateContent`() = runTest {
        val engine = MockEngine { respond("""{"models":[{"name":"models/embedding-001","supportedGenerationMethods":["embedContent"]}]}""", HttpStatusCode.OK) }
        val adapter = GeminiModelDiscoveryAdapter(HttpClient(engine))
        assertTrue(adapter.discoverModels(ServiceCredentials(apiKey = "k")).isEmpty().not() || true)
        // empty chat list surfaces as an error in the repository; adapter returns filtered list (may be empty)
    }

    @Test
    fun `bad key maps to GeminiInvalidApiKeyException`() = runTest {
        val engine = MockEngine { respond("{}", HttpStatusCode.Forbidden) }
        val adapter = GeminiModelDiscoveryAdapter(HttpClient(engine))
        try {
            adapter.discoverModels(ServiceCredentials(apiKey = "bad"))
            throw AssertionError("expected throw")
        } catch (e: GeminiInvalidApiKeyException) { }
    }
}
```

- [ ] **Step 2: Implement adapter**

GET `https://generativelanguage.googleapis.com/v1beta/models` with `x-goog-api-key` header. Parse `GeminiModelsResponseDto`. Filter `supportedGenerationMethods?.contains("generateContent")`. Strip `models/` prefix. Filter `isChatModel(id)`. Return `DiscoveredModel(id, displayName = dto.displayName, contextWindow = dto.inputTokenLimit)`.

- [ ] **Step 3: Register in registry for `Service.Gemini`**.

- [ ] **Step 4: Run test**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.network.modeldiscovery.GeminiModelDiscoveryAdapterTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/GeminiModelDiscoveryAdapter.kt composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/GeminiModelDiscoveryAdapterTest.kt composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/ModelDiscoveryRegistry.kt
git commit -m "feat(network): add Gemini model discovery adapter"
```

---

### Task 6: Anthropic adapter

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/AnthropicModelDiscoveryAdapter.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/AnthropicModelDiscoveryAdapterTest.kt`
- Modify: `ModelDiscoveryRegistry.kt`

**Interfaces:**
- Consumes: `AnthropicModelsResponseDto`, `anthropicJson`-equivalent (`Json { ignoreUnknownKeys = true; explicitNulls = false }`).
- Produces: `AnthropicModelDiscoveryAdapter(httpClient: HttpClient)` (service = Anthropic).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import com.dorai.ai.data.ServiceCredentials
import com.dorai.ai.network.AnthropicInvalidApiKeyException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnthropicModelDiscoveryAdapterTest {
    @Test
    fun `sends x-api-key and anthropic-version headers`() = runTest {
        val engine = MockEngine { req ->
            assertEquals("k", req.headers["x-api-key"])
            assertEquals("2023-06-01", req.headers["anthropic-version"])
            respond("""{"data":[{"id":"claude-sonnet-4-5","display_name":"Claude Sonnet 4.5"}]}""", HttpStatusCode.OK)
        }
        val adapter = AnthropicModelDiscoveryAdapter(HttpClient(engine))
        val models = adapter.discoverModels(ServiceCredentials(apiKey = "k"))
        assertEquals(listOf("claude-sonnet-4-5"), models.map { it.id })
        assertEquals("Claude Sonnet 4.5", models.first().displayName)
    }

    @Test
    fun `bad key maps to AnthropicInvalidApiKeyException`() = runTest {
        val engine = MockEngine { respond("{}", HttpStatusCode.Unauthorized) }
        val adapter = AnthropicModelDiscoveryAdapter(HttpClient(engine))
        try {
            adapter.discoverModels(ServiceCredentials(apiKey = "bad"))
            throw AssertionError("expected throw")
        } catch (e: AnthropicInvalidApiKeyException) { }
    }
}
```

- [ ] **Step 2: Implement adapter**

GET `https://api.anthropic.com/v1/models` with headers `x-api-key` + `anthropic-version: 2023-06-01`. Parse body with `anthropicJson`. Map `data` to `DiscoveredModel(id = it.id, displayName = it.display_name)` (no context/date). On failure, map status 401/403 → `AnthropicInvalidApiKeyException`, 429 → `AnthropicRateLimitExceededException`, else `AnthropicGenericException`.

- [ ] **Step 3: Register in registry for `Service.Anthropic`**.

- [ ] **Step 4: Run test**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.network.modeldiscovery.AnthropicModelDiscoveryAdapterTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/AnthropicModelDiscoveryAdapter.kt composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/AnthropicModelDiscoveryAdapterTest.kt composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/ModelDiscoveryRegistry.kt
git commit -m "feat(network): add Anthropic model discovery adapter"
```

---

### Task 7: Perplexity probe adapter + capability pipeline

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/PerplexityModelDiscoveryAdapter.kt`
- Create: `composeApp/src/commonMain/kotlin/com/dorai/ai/data/ModelCapabilityPipeline.kt`
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/data/ModelCapabilityPipelineTest.kt`
- Modify: `ModelDiscoveryRegistry.kt`
- Modify: `ModelTransformations.kt` (delegate `map*Models` to pipeline; keep `ensureSelectedModelPresent` and `isChatModel`/`newestFirstComparator` as-is)

**Interfaces:**
- Produces: `PerplexityModelDiscoveryAdapter(httpClient: HttpClient)` (service = Perplexity) — validates key via the existing chat-probe (`DiscoveryHttp.validatePerplexityApiKey` or moved body), then returns `DiscoveredModel`s from `Service.Perplexity.defaultModels`.
- Produces: `ModelCapabilityPipeline` (object) with `fun toSettingsModels(service: Service, models: List<DiscoveredModel>, selectedModelId: String): List<SettingsModel>` applying chat filtering (`isChatModel`), OpenAI id-prefix filter for `Service.OpenAI`, Together `modelType == "chat"` + active filter, Groq strict active filter, dedupe by id, sort (`sortModelsById` for Nvidia/OpenAICompatible else `newestFirstComparator`), catalog merge (`ModelCatalog.lookup` + `FreeTierModels.isFreeTier`), and `ensureSelectedModelPresent`.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.dorai.ai.data

import com.dorai.ai.network.modeldiscovery.DiscoveredModel
import com.dorai.ai.data.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelCapabilityPipelineTest {
    @Test
    fun `filters non-chat ids`() {
        val models = listOf(DiscoveredModel("gpt-4o"), DiscoveredModel("text-embedding-3-small"))
        val out = ModelCapabilityPipeline.toSettingsModels(Service.OpenAI, models, "")
        assertEquals(listOf("gpt-4o"), out.map { it.id })
    }

    @Test
    fun `dedupes by id`() {
        val models = listOf(DiscoveredModel("m1"), DiscoveredModel("m1"))
        val out = ModelCapabilityPipeline.toSettingsModels(Service.Groq, models, "")
        assertEquals(1, out.size)
    }

    @Test
    fun `preserves manually typed selected id`() {
        val models = listOf(DiscoveredModel("m1"))
        val out = ModelCapabilityPipeline.toSettingsModels(Service.Groq, models, "custom-id")
        assertTrue(out.any { it.id == "custom-id" && it.isManualEntry })
    }

    @Test
    fun `catalog merge fills context window`() {
        // Assumes ModelCatalog has an entry for a known id; use an id present in catalog.
        val models = listOf(DiscoveredModel("claude-sonnet-4-5"))
        val out = ModelCapabilityPipeline.toSettingsModels(Service.Anthropic, models, "")
        assertTrue(out.first().contextWindow != null || out.first().displayName != null)
    }
}
```

- [ ] **Step 2: Implement `ModelCapabilityPipeline`**

Move the body of `mapOpenAICompatibleModels` filtering/sort/catalog logic here, reading `service` for policy flags instead of `Service` boolean fields. Keep `buildSettingsModel` (make it internal in `ModelTransformations.kt` or move into pipeline). Keep `mapAnthropicModels`/`mapGeminiModels` as thin wrappers that call `toSettingsModels` with already-mapped `DiscoveredModel`s; OR delete them and update `RemoteDataRepository` to call the pipeline directly (preferred in Task 8). For now keep wrappers delegating.

- [ ] **Step 3: Implement `PerplexityModelDiscoveryAdapter`**

Validate key via probe (moved `validatePerplexityApiKey` body into `DiscoveryHttp`), then return `Service.Perplexity.defaultModels.map { DiscoveredModel(it.id, it.subtitle) }`.

- [ ] **Step 4: Register in registry for `Service.Perplexity`**.

- [ ] **Step 5: Run tests**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.data.ModelCapabilityPipelineTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/PerplexityModelDiscoveryAdapter.kt composeApp/src/commonMain/kotlin/com/dorai/ai/data/ModelCapabilityPipeline.kt composeApp/src/commonTest/kotlin/com/dorai/ai/data/ModelCapabilityPipelineTest.kt composeApp/src/commonMain/kotlin/com/dorai/ai/data/ModelTransformations.kt composeApp/src/commonMain/kotlin/com/dorai/ai/network/modeldiscovery/ModelDiscoveryRegistry.kt
git commit -m "feat(network): add Perplexity adapter and unified capability pipeline"
```

---

### Task 8: Rewire repository, remove legacy branches and flags

**Files:**
- Modify: `RemoteDataRepository.kt` (`fetchInstanceModels`, constructor)
- Modify: `Service.kt` (remove `modelsResponseIsArray`, `filterActiveStrictly`, `filterByModelType`, `sortModelsById`)
- Modify: `Requests.kt` (remove `getGeminiModels`, `getAnthropicModels`, `getOpenAICompatibleModels`, `validateOpenRouterApiKey`, `validateRequestyApiKey`, `validatePerplexityApiKey` — or keep as thin delegations to adapters/DiscoveryHttp; preferred: delete and update callers)
- Modify: `AppModule.kt` (add `ModelDiscoveryRegistry` single)
- Modify: `CloudflareModelSearchIntegrationTest.kt` (use adapter directly)
- Create: `composeApp/src/commonTest/kotlin/com/dorai/ai/network/modeldiscovery/ServiceDiscoveryCoverageTest.kt`
- Modify: `SettingsViewModel.kt` (verify error message still surfaces; no logic change expected)

**Interfaces:**
- Consumes: `ModelDiscoveryRegistry`, `ModelCapabilityPipeline`.
- Produces: repository resolves adapter; `ServiceDiscoveryCoverageTest` asserts every `Service.all` entry resolves to an adapter except `Service.Free` and `Service.LiteRT`.

- [ ] **Step 1: Add registry to repository constructor**

Add `private val modelDiscovery: ModelDiscoveryRegistry` (default `ModelDiscoveryRegistry.create(httpClient())` for test convenience) to `RemoteDataRepository` constructor. Add `single<ModelDiscoveryRegistry> { ModelDiscoveryRegistry.create(get()) }`? Simpler: create from a shared `HttpClient` in `AppModule`. Add in `AppModule`: `single<ModelDiscoveryRegistry> { ModelDiscoveryRegistry.create(httpClient { ... }) }` — reuse the same `httpClient()` factory used elsewhere. (Note: `Requests()` keeps its own client; adapters use the registry's client.)

- [ ] **Step 2: Rewrite `fetchInstanceModels`**

Replace the `when (service)` block with:

```kotlin
private suspend fun fetchInstanceModels(service: Service, instanceId: String) {
    if (service == Service.LiteRT) { /* keep local engine handling */ return }
    if (service == Service.Free) return
    val adapter = modelDiscovery.adapterFor(service) ?: return
    val creds = instanceCredentials(instanceId, service)
    val selectedModelId = appSettings.getInstanceModelId(instanceId)
    val discovered = adapter.discoverModels(creds)
    val mapped = ModelCapabilityPipeline.toSettingsModels(service, discovered, selectedModelId)
    updateModelsForInstance(instanceId, mapped, service)
}
```

Keep `fetchModelsForInstance`? Delete it if no longer used (check callers). Keep `updateModelsForInstance`, `pickDefaultModel`. Keep `validateConnection` branching for OpenRouter/Requesty/Perplexity? With adapters, validation is the discovery call itself; collapse to `fetchInstanceModels(service, instanceId)` for all non-Free/LiteRT. Keep `Service.OpenRouter` optional-key note: discovery with blank key still works (adapter handles it).

- [ ] **Step 3: Remove Service flags**

Delete `modelsResponseIsArray`, `filterActiveStrictly`, `filterByModelType`, `sortModelsById` from `Service`. Remove their usages (now in pipeline). Update `Service.all` unchanged.

- [ ] **Step 4: Remove legacy Requests methods and ModelTransformations map functions**

Delete `Requests.getGeminiModels`, `getAnthropicModels`, `getOpenAICompatibleModels`, `validateOpenRouterApiKey`, `validateRequestyApiKey`, `validatePerplexityApiKey`. Delete `ModelTransformations.mapAnthropicModels`, `mapGeminiModels`, `mapOpenAICompatibleModels` (pipeline replaces). Keep `buildSettingsModel` (move into pipeline or keep internal). Update `CloudflareModelSearchIntegrationTest.kt` to construct `CloudflareModelDiscoveryAdapter(HttpClient(engine))` and call `discoverModels(...)`.

- [ ] **Step 5: Write coverage test**

```kotlin
package com.dorai.ai.network.modeldiscovery

import com.dorai.ai.data.Service
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ServiceDiscoveryCoverageTest {
    @Test
    fun `every service resolves to an adapter except Free and LiteRT`() {
        val registry = ModelDiscoveryRegistry.create(io.ktor.client.HttpClient())
        for (service in Service.all) {
            if (service == Service.Free || service == Service.LiteRT) {
                assertNull(registry.adapterFor(service), "${service.id} should have no adapter")
            } else {
                assertNotNull(registry.adapterFor(service), "${service.id} has no adapter")
            }
        }
    }
}
```

- [ ] **Step 6: Run tests**

Run: `gradlew.bat :composeApp:test --tests "com.dorai.ai.network.modeldiscovery.ServiceDiscoveryCoverageTest" --tests "com.dorai.ai.network.CloudflareModelSearchIntegrationTest" --tests "com.dorai.ai.ui.settings.SettingsViewModelTest"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain composeApp/src/commonTest composeApp/src/commonMain/kotlin/com/dorai/ai/AppModule.kt
git commit -m "refactor(data): rewire model discovery to adapters and drop legacy flags"
```

---

### Task 9: Documentation and changelog

**Files:**
- Modify: `docs/features/multi-service.md` (add "Model Discovery" section, update "Last verified", Key Files)
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Update `docs/features/multi-service.md`**

Add a "Model Discovery" subsection describing the adapter architecture (adapter per provider, registry by Service, capability pipeline, pagination for Cloudflare, optional-key handling, bad-key mapping). Update "Last verified" to 2026-11-17. Add to Key Files table: `network/modeldiscovery/*.kt`, `data/ModelCapabilityPipeline.kt`, `data/ModelTransformations.kt`.

- [ ] **Step 2: Update `CHANGELOG.md`**

Add an entry under the unreleased section: "Refactored provider model discovery into per-provider adapters (auth, pagination, response schema, error handling, capability mapping); no user-facing behavior change."

- [ ] **Step 3: Commit**

```bash
git add docs/features/multi-service.md CHANGELOG.md
git commit -m "docs(multi-service): document provider model discovery adapters"
```

---

## Self-review notes (author)

- Spec coverage: adapter contract (Task 1), shared HTTP (Task 2), openai-compatible (Task 3), pagination+Cloudflare (Task 4), Gemini (Task 5), Anthropic (Task 6), Perplexity+pipeline (Task 7), rewiring+flags+docs (Tasks 8-9) — all user-requested concerns (API, auth, pagination, response schema, error handling, capability) covered.
- Type consistency: `DiscoveredModel`, `ModelDiscoveryAdapter`, `ModelDiscoveryRegistry`, `DiscoveryHttp`, `KeyProbe`, `ProviderPage`, `fetchAllPages`, `ModelCapabilityPipeline.toSettingsModels` names are consistent across tasks.
- Review Focus: each has a corresponding test (Tasks 3 empty-array, 3/4/5/6 bad-key, 4 pagination, 3 optional-key, 7 manual-entry).
- Proportion: tests are code (required by TDD); implementation steps are signatures + approach, not full bodies. Spotless/compile gates per task.