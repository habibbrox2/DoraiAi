# Agent Activity and Reasoning Data Architecture

**Status:** Product and implementation contract  
**Applies to:** Chat timeline, provider adapters, tool/MCP execution, and streaming UI

## 1. Terminology

Kai may receive several kinds of information during an assistant turn. They must not be conflated:

| Data type | Meaning | User-facing policy |
|---|---|---|
| Agent activity event | Observable operation such as MCP call, shell invocation, approval request, or tool result | May be shown as a concise execution trace |
| Provider reasoning summary | Explicit summary field returned by a provider under its API contract | Optional; display only when supported and approved |
| Private chain-of-thought | Hidden internal model deliberation or raw private reasoning tokens | Do not expose as a step-by-step thought log |
| Final answer | User-facing response content | Render normally |
| Tool arguments/output | Data sent to or returned by a tool | Show only sanitized, bounded, permission-appropriate details |

The UI should describe observable work, not claim to reveal the model's private internal thoughts. The preferred feature name is **Activity**, **Agent Activity**, or **Execution trace**, rather than “Show AI's thoughts”.

## 2. Product goal

Give users confidence about what the agent is doing before and while it responds:

- Which tool or MCP server was invoked?
- Is the operation waiting, running, completed, or failed?
- Did the operation require approval?
- What safe, useful result did it return?
- How long did it take, when reliable timing is available?

A trace is an audit-friendly summary of runtime events. It is not a transcript of hidden reasoning.

## 3. Activity event model

The runtime should expose a typed event stream. A conceptual event shape:

```kotlin
data class AgentActivityEvent(
    val id: String,
    val turnId: String,
    val sequence: Long,
    val kind: ActivityKind,
    val label: String,
    val status: ActivityStatus,
    val startedAt: Long?,
    val durationMs: Long?,
    val toolName: String?,
    val serverName: String?,
    val safeSummary: String?,
    val safeOutputPreview: String?
)
```

This is a design contract, not a claim that the exact class currently exists. Adapt it to the existing runtime/event model before implementation.

Suggested kinds:

- `MODEL_STATUS`: thinking/streaming/waiting state without private content.
- `TOOL_CALL`: native tool, MCP, or sandbox action.
- `APPROVAL_REQUIRED`: user authorization needed.
- `TOOL_RESULT`: success or failure.
- `FALLBACK`: provider/model fallback.
- `NOTICE`: bounded informational event.

Suggested statuses:

- `QUEUED`
- `RUNNING`
- `WAITING_FOR_APPROVAL`
- `SUCCEEDED`
- `FAILED`
- `CANCELLED`

Use stable IDs and monotonically increasing sequence numbers per turn. Streaming updates should update an existing event rather than append duplicate rows.

## 4. Data flow

```text
Provider / Agent runtime
        │
        ├── assistant content ───────────────► Chat message
        │
        ├── tool-call request ───────────────► Tool policy + approval gate
        │                                          │
        │                                          ▼
        │                                    MCP / native tool / sandbox
        │                                          │
        │                                          ▼
        ├── activity lifecycle events ◄──── sanitized result/status
        │
        ▼
ChatUiState / presentation state
        │
        ▼
AgentActivityDisclosure in the assistant turn
```

Provider adapters remain responsible for provider-specific request/response formats. Runtime services remain responsible for tool execution and permissions. Composables only render immutable activity state and invoke typed user actions.

## 5. UI behavior

### Collapsed state

Show a compact disclosure above or adjacent to the assistant response:

- “Used Context7 · 1 tool”
- “Ran sandbox command · Completed”
- “Waiting for approval”
- “2 tools · 1 completed · 1 failed”

The summary must be derived from actual events. Never infer that a tool ran from the model's prose.

### Expanded state

Show an ordered timeline with:

- Tool/server name.
- Short sanitized action summary.
- Status and optional duration.
- Approval state.
- Bounded output preview or a link/action to inspect supported details.
- Clear error state and retry guidance where retry is safe.

Do not dump complete environment variables, credentials, file contents, raw request headers, or unrestricted shell output into the chat timeline.

### Streaming behavior

- Create an activity item when the runtime begins the operation.
- Update it on progress/status changes.
- Complete it with a sanitized result.
- Keep the disclosure associated with its assistant turn.
- Preserve stable order during concurrent tool calls.
- Allow users to collapse the panel without cancelling the underlying operation.
- Cancellation must call the runtime's explicit cancellation action, not merely hide the UI.

## 6. Privacy and security

- Treat tool names, arguments, outputs, and provider metadata as untrusted.
- Redact API keys, access tokens, passwords, cookies, authorization headers, and known secret patterns before presentation or persistence.
- Apply allowlisted summaries per tool type; do not assume arbitrary tool output is safe to display.
- Bound output previews by bytes/characters and provide explicit expansion only where policy permits.
- Do not expose hidden chain-of-thought, raw internal deliberation, or private reasoning token streams.
- A provider reasoning summary is not automatically safe: validate its source, format, size, and product eligibility before display.
- Keep user approval separate from the trace; a trace item cannot silently grant permission.
- Ensure logs do not leak data across conversations or users.
- Apply retention policy consistently to persisted activity events.

## 7. Provider reasoning compatibility

Provider-specific reasoning fields are a transport concern, not a UI contract. Some OpenAI-compatible providers use fields such as `reasoning_content`, `reasoning`, or `reasoning_details`; other providers use distinct APIs or do not support reasoning metadata.

Current code has historically used a per-service request mode to decide whether `reasoning_content` is echoed on tool round-trips. The exact provider matrix is volatile and must be revalidated against current vendor documentation and integration tests before changing it.

Rules:

1. Default to omitting unknown reasoning fields from outgoing requests.
2. Echo provider-specific fields only where the provider/model contract requires or documents it.
3. Keep provider DTO handling inside provider/network layers.
4. Do not make UI rendering depend on the outgoing request mode.
5. Preserve backward compatibility when reading older conversation records.
6. Do not assume a provider's raw reasoning payload is a user-facing summary.
7. Do not replay opaque provider signatures or reasoning metadata unless the API explicitly requires it.

### OpenAI Responses API

The Responses API uses typed output items, including a distinct reasoning item/summary where available. Treat these summaries as optional provider metadata. Do not reconstruct or expose hidden reasoning from non-summary payloads. Follow the API's documented item-continuity rules when sending subsequent requests; do not blindly replay incomplete item sequences after context trimming.

## 8. Existing implementation map

The repository has existing reasoning-related handling. Re-check these files on the implementation branch before changing behavior:

| File | Known responsibility |
|---|---|
| `composeApp/src/commonMain/.../data/Service.kt` | Per-service reasoning request mode |
| `composeApp/src/commonMain/.../ui/chat/ChatUiState.kt` | Message conversion and outgoing reasoning-field gate |
| `composeApp/src/commonMain/.../data/providers/OpenAIMessages.kt` | OpenAI-compatible message construction |
| `composeApp/src/commonMain/.../data/RemoteDataRepository.kt` | Response handling and conversation persistence |
| `composeApp/src/commonMain/.../network/dtos/openaicompatible/OpenAICompatibleChatRequestDto.kt` | Outgoing message DTO |
| `composeApp/src/commonMain/.../network/dtos/openaicompatible/OpenAICompatibleChatResponseDto.kt` | Incoming reasoning metadata normalization |
| `composeApp/src/commonMain/.../network/dtos/openairesponses/OpenAIResponsesResponseDto.kt` | Responses API reasoning output parsing |
| `composeApp/src/commonMain/.../data/Conversation.kt` | Persisted message data |
| `composeApp/src/commonMain/.../ui/chat/composables/BotMessage.kt` | Assistant response presentation |
| `composeApp/src/commonMain/.../ui/chat/ChatScreen.kt` | Turn grouping and timeline coordination |

The map describes previously observed responsibilities; verify exact symbols and current behavior before implementation.

## 9. Migration plan

### Step 1 — Audit

- Inventory current tool-call lifecycle callbacks and streaming state.
- Identify where MCP/native/sandbox events are emitted and where they are lost.
- Audit persisted message schema and migration defaults.
- Verify current reasoning display behavior and provider-specific DTO gates.
- Record secret-redaction coverage.

### Step 2 — Typed activity state

- Reuse an existing runtime event model if available.
- Otherwise introduce a typed event model with stable IDs, turn IDs, sequence numbers, and lifecycle status.
- Keep event collection outside composables.
- Add bounded, sanitized presentation summaries.

### Step 3 — Timeline disclosure

- Add collapsed activity summary and expanded ordered event list.
- Support running, waiting-for-approval, success, failure, and cancellation.
- Keep the response readable when activity is collapsed.
- Add semantics and keyboard/touch support.

### Step 4 — Persistence and compatibility

- Decide whether activity events are persisted or reconstructed from existing tool-call messages.
- If persisted, use optional/defaulted fields and explicit serialization tests.
- Avoid storing sensitive raw tool output.
- Keep old conversations readable.

### Step 5 — Validation

- Test concurrent MCP calls and out-of-order completions.
- Test cancellation, approval, retry, provider fallback, and streaming interruption.
- Test redaction with representative secrets.
- Verify no private reasoning text appears in the activity disclosure.
- Test compact and expanded layouts and screen-reader semantics.

## 10. Acceptance criteria

- The user can identify real tool/MCP/sandbox activity without reading private model deliberation.
- Events are associated with the correct assistant turn and remain correctly ordered.
- Running/completed/failed/approval states are accurate.
- Secrets and unrestricted tool output are not exposed.
- Provider-specific reasoning transport behavior remains isolated and regression-tested.
- Existing conversation persistence and historical messages remain compatible.
- Hiding the disclosure does not cancel runtime work.
- Cancellation and approval actions are explicit runtime operations.


## 11. Premium conversation implementation notes

The conversation UI renders a turn-scoped Agent Activity disclosure from existing persisted history. Tool-call IDs are the primary correlation key; legacy name-based matching is used only when older history lacks an ID. Tool approvals remain separate and continue to use the existing permission actions.

The disclosure shows bounded, redacted previews for tool arguments and results. It does not expose provider reasoning payloads or private chain-of-thought. File-operation tool names are surfaced as file activity when their identity clearly indicates a filesystem operation; a reviewed patch or full source diff is not claimed unless the runtime supplies before/after content.
