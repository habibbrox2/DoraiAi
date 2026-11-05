# Kotlin + TypeScript Hybrid Architecture

## 1. Purpose

Establish a safe, versioned foundation for Kotlin and TypeScript to coexist without coupling their build systems or transferring privileged ownership to the TypeScript side.

## 2. Runtime topology

Kotlin is the application and capability authority. TypeScript is an independently built frontend/integration platform. A bridge is a later implementation decision and is not part of this foundation.

Potential transports must be selected per deployment target only after evaluating lifecycle, origin isolation, serialization, cancellation, and threat model. Do not assume that Android WebView, desktop browser, and hosted web require the same transport.

## 3. Contract model

The initial protocol uses JSON envelopes:

- Request: request ID, message type, protocol version, operation type, payload.
- Response: matching request ID, success state, result or bounded error.
- Event: event ID, session ID, monotonic sequence, event type, payload.

Schemas live in `hybrid/contracts/schemas/`. The protocol version is recorded in `hybrid/contracts/protocol-version.json`.

## 4. Capability security

- Kotlin validates every incoming message and independently checks authorization.
- TypeScript receives only explicitly granted capabilities.
- Never expose arbitrary shell commands, unrestricted filesystem paths, secrets, or raw platform objects through a generic bridge.
- User confirmation is required for destructive or sensitive actions.
- Apply request size, payload depth, string length, timeout, cancellation, and rate limits in runtime code.
- Use origin/session binding and replay protection where the selected transport requires them.
- Logs must redact secrets and bound payload output.

## 5. Reliability

- Correlate requests and responses with unique request IDs.
- Define timeout and cancellation semantics before transport implementation.
- Events use a per-session monotonic sequence; consumers detect gaps and duplicates.
- Errors use stable machine-readable codes and bounded user-safe messages.
- Major protocol incompatibility fails closed.
- Optional fields may be added compatibly; semantic changes require a versioning decision.

## 6. Existing Dynamic UI

The current `dorai-ui` JSON format remains a constrained Compose presentation protocol. It is not a JavaScript execution mechanism and should not be silently replaced by a TypeScript renderer. Any future browser renderer must preserve allowlisted node/action semantics and Kotlin-owned privileged operations.

## 7. Implementation sequence

1. Approve and stabilize shared schemas.
2. Add Kotlin and TypeScript contract validation with shared fixtures.
3. Select a transport per platform and document its threat model.
4. Implement a narrow capability registry and authorization tests.
5. Add lifecycle, cancellation, timeout, and observability tests.
6. Integrate one low-risk feature end-to-end before expanding capabilities.
