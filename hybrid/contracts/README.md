# Shared Contracts

JSON Schema is the language-neutral source of truth for future Kotlin/TypeScript messages.

## Rules

1. Keep protocol envelopes versioned and backward-compatible where possible.
2. Enforce transport-level byte limits, timeouts, cancellation, and rate limits at runtime; JSON Schema cannot enforce these controls.
3. Reject unknown protocol versions and malformed envelopes.
4. Treat all payloads as untrusted data.
5. Schema validity is not authorization. Kotlin must independently authorize every capability and validate all arguments.
6. Never place API keys, access tokens, private file contents, or other secrets in protocol metadata or logs.
7. Add contract fixtures and compatibility tests before implementing a bridge.

The current schemas define request, response, and event envelopes only. They do not define product features or grant execution permissions.
