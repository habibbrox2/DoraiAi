# Kotlin / TypeScript Communication Contrac

**Initial protocol:** `brox-kai-hybrid 1.0`
**Status:** Schema foundation only; no transport implementation.

## Envelope types

| Envelope | Required identity                  | Purpose                                       |
| -------- | ---------------------------------- | --------------------------------------------- |
| Request  | `requestId`                        | Ask Kotlin-owned host for a defined operation |
| Response | `requestId`                        | Return result or structured error             |
| Event    | `eventId`, `sessionId`, `sequence` | Publish ordered runtime state/activity        |

Schemas are in `hybrid/contracts/schemas/`.

## Normative requirements for future implementations

- Validate JSON against the matching schema before dispatch.
- Reject unsupported major versions and unknown operation types.
- Match responses to outstanding request IDs; reject unsolicited or stale responses.
- Bound message bytes, nesting, collection sizes, and string lengths.
- Enforce timeouts, cancellation, concurrency limits, and backpressure.
- Authenticate the transport endpoint/session where applicable.
- Authorize every operation at the Kotlin capability layer; schema validation is not authorization.
- Redact secrets from logs and error messages.
- Do not pass arbitrary Kotlin reflection names, class names, shell commands, or filesystem paths as generic operations.

A transport must not be implemented until its lifecycle and threat model are documented for each target.
