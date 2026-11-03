# Dynamic UI Architecture — kai-ui

**Status:** Existing feature with proposed hardening and extension  
**Rendering:** Kotlin Compose Multiplatform  
**Transport format:** JSON component trees inside assistant-message code fences

## 1. Purpose

kai-ui lets an assistant response contain interactive, native UI rather than only prose. The model emits a declarative JSON description; Kai parses it into a typed component tree and renders supported components using Compose.

The model output is untrusted input. kai-ui is a constrained presentation protocol, not an executable-code format.

## 2. Existing implementation

The current feature supports:

- JSON component trees in `kai-ui` code fences.
- Single-object JSON and multi-line NDJSON.
- A unified markdown AST where kai-ui blocks are first-class blocks.
- Recursive Compose rendering.
- Interactive forms and callback actions.
- Frozen/read-only historical snapshots.
- Interactive mode in the same conversation.
- Local parsing repair and field-level fallback behavior.
- Text extraction for speech output.

The feature is enabled by default and can be disabled in Settings. Disabling generation instructions affects subsequent requests; parsing remains enabled so previously saved kai-ui messages continue to render.

## 3. Current component vocabulary

The current renderer supports the following families. The source model remains authoritative; update this list when the node registry changes.

| Family | Current node examples |
|---|---|
| Layout | column, row, card, box, divider |
| Content | text, image, icon, code |
| Interactive | button, text input, checkbox, switch, select, radio group, slider, chip group |
| Feedback | progress, countdown, alert |
| Navigation | tabs, accordion |
| Display | quote, badge, stat, avatar |
| Data | list, table |

### Actions

- `callback`: collect form values and submit a structured user event through the normal chat flow.
- `toggle`: show/hide a local target element.
- `open_url`: open a validated external URL through the platform handler.
- `copy_to_clipboard`: copy an explicit literal value using the platform clipboard integration.

Callbacks are conversational events. They must not be represented as privileged system operations. File export, printing, shell execution, payment, or other privileged operations require dedicated runtime APIs, permission checks, and explicit user confirmation.

## 4. Message lifecycle and interactivity

- Only the latest eligible assistant layout in the active conversation is interactive.
- Older layouts render as read-only snapshots.
- Form state belongs to its layout instance.
- Cross-step state is carried through the conversation/event history, not hidden global mutable state.
- A submitted layout remains visible as a frozen snapshot with the submitted values.
- Reopening/editing a snapshot must be an explicit user action and must follow the existing conversation truncation/resubmit semantics.
- Countdown callbacks must be cancelled or rendered inert when the originating message is no longer active.
- Interactive mode changes how the next answer is generated; it does not create a separate chat or replace the timeline/composer.

## 5. Parsing and rendering safety

The parser should be tolerant of common model formatting mistakes, but tolerance must not become permission escalation.

Required controls:

1. Decode into typed serializable models; reject unsupported root shapes.
2. Enforce maximum input bytes, nesting depth, node count, string length, and collection size.
3. Use an allowlist of node types and action types.
4. Drop unknown nodes safely and report bounded diagnostics in debug/test builds.
5. Validate URLs and reject unsafe schemes such as `javascript:`, `data:`, and arbitrary local-file paths.
6. Never evaluate JavaScript, Kotlin, HTML, shell, or dynamic class names from model output.
7. Never let model-provided identifiers escape their component tree or address arbitrary application state.
8. Bound image dimensions/URLs and avoid loading untrusted local paths.
9. Keep action execution behind typed callbacks and runtime permission boundaries.
10. Make malformed blocks fail locally; one invalid child must not crash the entire message renderer.

Parser repair may fix harmless syntax defects such as trailing commas or truncated braces when safe. It must not invent actions, silently upgrade unknown action types, or reinterpret malformed data as privileged instructions.

## 6. Proposed schema evolution

The existing kai-ui format must remain readable. New schema metadata should be optional so existing persisted conversations continue to work.

Proposed envelope for future generation:

```json
{
  "schema": "kai-ui",
  "version": 1,
  "layout": {
    "type": "column",
    "children": [
      {
        "type": "text",
        "style": "title",
        "text": "Example"
      }
    ]
  }
}
```

Compatibility rules:

- Continue accepting the current unwrapped root node and current NDJSON format.
- New writers may emit the versioned envelope.
- Readers must validate the declared version and provide a clear unsupported-version fallback.
- Unknown optional properties are ignored; unknown node/action types are not executed.
- Schema changes require fixture updates and round-trip tests.
- Do not make a model-generated schema version select arbitrary renderer code.

## 7. Component registry architecture

The renderer should use a closed registry mapping a known node discriminator to a Compose renderer.

Conceptual flow:

```text
Assistant message
    ↓
Markdown parser
    ↓
kai-ui block extraction
    ↓
JSON limits + schema validation
    ↓
Typed node tree
    ↓
Allowlisted component registry
    ↓
Compose UI
    ↓
Typed action callback
    ↓
ChatActions / approved platform capability
```

The registry should provide:

- Stable node discriminator and schema version.
- Typed decoder and validator.
- Compose renderer.
- Accessibility semantics defaults.
- Read-only behavior for historical messages.
- Test fixtures for valid, invalid, and boundary inputs.

Do not introduce a generic plugin loader or runtime code generation system for this feature.

## 8. Generative micro-canvas patterns

### Interactive game

- Present HP, level, currency, and defensive stats as compact semantic metrics.
- Use progress indicators for values with meaningful bounds.
- Keep artwork responsive and avoid stretching.
- Present the next available game action as a clear callback button.
- Preserve the game state in the assistant's structured response or the application's existing game integration; UI-local state is not authoritative.

### Recipe or multi-step workflow

- Use a scrollable checklist with clear completed/current states.
- Keep ingredient quantities and steps readable at large font scales.
- Use local checkbox state only for presentation unless the workflow explicitly submits progress.

### Charts and data panels

- Require typed, bounded data points and explicit labels/units.
- Provide a text/table alternative for accessibility.
- Never fabricate missing data or infer metrics from decorative values.

## 9. Performance and accessibility

- Avoid deeply nested unbounded layouts.
- Use lazy containers for long lists when supported by the component contract.
- Keep image loading bounded and cancellable.
- Minimize recomposition by keeping node models immutable.
- Use stable keys for repeated items.
- Provide semantics for controls, progress, selection, and error states.
- Ensure touch targets meet the app's minimum interaction size.
- Respect system font scaling, contrast themes, and reduced motion.
- Keep a static fallback when animation or advanced rendering is unavailable.

## 10. Testing strategy

- Parser unit tests: valid JSON, NDJSON, malformed input, unknown nodes, unknown actions, version mismatch, size/depth limits.
- Serialization tests: old saved messages and new versioned envelopes.
- Renderer tests: every registered node in interactive and read-only modes.
- Action tests: callback payload, toggle scope, URL validation, clipboard action, stale-message protection.
- Security tests: malicious URL schemes, oversized trees, deeply nested nodes, unexpected field types, and attempted privileged action injection.
- Screenshot tests: game widget, recipe checklist, data panel, dark/OLED/light themes, compact and expanded layouts.
- Regression tests: interactive mode persistence, callback round-trip, edit/resubmit behavior, and TTS extraction.

## 11. Key implementation locations

| File | Responsibility |
|---|---|
| `composeApp/src/commonMain/.../ui/dynamicui/KaiUiNode.kt` | Serializable node model |
| `composeApp/src/commonMain/.../ui/dynamicui/UiAction.kt` | Typed action model |
| `composeApp/src/commonMain/.../ui/dynamicui/KaiUiParser.kt` | Parse and sanitize kai-ui blocks |
| `composeApp/src/commonMain/.../ui/dynamicui/KaiUiNodeBuilders.kt` | Tolerant typed node construction |
| `composeApp/src/commonMain/.../ui/dynamicui/KaiUiRenderer.kt` | Compose renderer |
| `composeApp/src/commonMain/.../ui/dynamicui/KaiUiTts.kt` | Speakable text extraction |
| `composeApp/src/commonMain/.../ui/markdown/MarkdownParser.kt` | Markdown/kai-ui AST parsing |
| `composeApp/src/commonMain/.../ui/markdown/MarkdownRenderer.kt` | Block dispatch |
| `composeApp/src/commonMain/.../ui/chat/composables/BotMessage.kt` | Message integration |
| `composeApp/src/commonMain/.../ui/chat/ChatScreen.kt` | Chat/interactive mode integration |
| `composeApp/src/commonMain/.../ui/chat/ChatActions.kt` | UI callbacks and interactive-mode actions |
| `composeApp/src/commonMain/.../data/AppSettings.kt` | Dynamic UI preference |

## 12. TypeScript boundary

The existing kai-ui renderer remains Kotlin Compose Multiplatform. The new TypeScript workspace does not execute model-provided JavaScript and does not replace the current JSON component-tree protocol. If a future browser-side renderer is introduced, it must consume an explicitly versioned contract, preserve the closed component/action allowlist, and delegate privileged operations to Kotlin-owned capability APIs. No TypeScript renderer or bridge is implemented by the hybrid foundation.

## 13. Rollout plan

1. Audit the current node model, parser, renderer, and persisted message fixtures.
2. Add parser limits and security tests before expanding the node vocabulary.
3. Add optional schema envelope/version support without breaking old messages.
4. Introduce registry abstractions only where they simplify the existing renderer.
5. Add game/recipe/data micro-canvas layouts using existing safe node types first.
6. Add new node types only with typed model, renderer, semantics, and tests.
7. Validate Android, Desktop, and Web builds before enabling new generation instructions by default.
