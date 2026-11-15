# Hybrid RAG & Vector Memory

**Last verified:** 2026-11-16

DORAI AI provides hybrid retrieval-augmented generation (RAG) by combining immediate local on-device memory storage with cloud semantic search powered by Cloudflare Workers AI embeddings and Supabase pgvector long-term persistence.

## Behavior

### Settings & Configuration
- Gated under **Settings > Agent**:
  - Cloudflare Account ID and API Token (with optional Cloudflare AI Gateway ID for observability and caching).
  - Supabase URL and Anon Key.
  - **Vector Memory (pgvector)** toggle (default enabled when credentials configured).
- If credentials are not configured or the feature toggle is disabled, the system operates purely on local memory with zero remote network requests.

### Embedding Generation
- Text embeddings are generated using Cloudflare Workers AI free tier model `@cf/baai/bge-small-en-v1.5`, producing 384-dimensional dense floating-point vector representations.
- When an AI Gateway ID is configured, requests are routed through Cloudflare AI Gateway with `cf-aig-gateway-id` header for edge caching, rate-limit protection, and telemetry.

### Write-Through Learning
- Whenever the AI agent or user calls memory tools (`memory_store`, `memory_learn`) or reinforces knowledge, the record is immediately committed to local on-device memory.
- In the background, a non-blocking asynchronous task generates the text embedding via Cloudflare and upserts the vector item into the Supabase `dorai_memories` table.
- Deleting a memory via `memory_forget` similarly dispatches an asynchronous removal call to Supabase.

### Live Context Retrieval & Prompt Injection
- When the user sends a chat message, the system evaluates semantic relevance:
  - Generates a query vector for the user's prompt.
  - Queries Supabase via the `match_memories` remote procedure call (RPC) using cosine similarity.
  - Enforces a bounded 1.5-second timeout (`withTimeoutOrNull(1500)`).
- Semantically relevant memories above the similarity threshold are appended to the agent's system prompt under `## Retrieved Relevant Context (Semantic Search)` formatted with key, content, and relevance percentage.
- The bounded timeout ensures that chat response generation is never blocked or delayed by slow network connections.

### Offline-First & Privacy Guarantees
- Local device memory remains authoritative at all times.
- If network drops, timeout expires, or remote servers return errors, the app falls back silently to local memory without displaying crashes or interruption banners.
- No user conversation history is shared with Supabase or Cloudflare beyond explicit memory facts approved by the agent or user.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/CloudflareEmbeddingClient.kt` | Ktor client for Cloudflare Workers AI text embeddings and AI Gateway routing |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/dtos/cloudflare/CloudflareEmbeddingDto.kt` | DTOs for embedding request and response payloads |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/supabase/SupabaseRestClient.kt` | PostgREST client for Supabase tables and RPC endpoints |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/network/dtos/supabase/SupabaseMemoryDto.kt` | DTOs for pgvector memory entries and match_memories search queries |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/data/SupabaseVectorStore.kt` | Vector store interface managing save, search, and forget workflows |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/data/MemoryStore.kt` | Local memory store with write-through reactive lifecycle hooks |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/data/ChatSystemPromptBuilder.kt` | Renders retrieved relevant memories with relevance score into system prompt |
| `composeApp/src/commonMain/kotlin/com/dorai/ai/data/RemoteDataRepository.kt` | Coordinates live query embedding, bounded retrieval timeout, and prompt injection |

## Tests

- Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.network.CloudflareEmbeddingTest"`
- Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.network.supabase.SupabaseTest"`
- Run: `./gradlew :composeApp:desktopTest --tests "com.dorai.ai.data.ChatSystemPromptBuilderTest"`
