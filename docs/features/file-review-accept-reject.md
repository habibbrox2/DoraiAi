# File Review: Accept/Reject and Safe Filesystem Transactions

**Status:** Architecture baseline for implementation  
**Scope:** First-party, locally managed file changes in the agent workspace. This is not a promise that arbitrary shell or MCP side effects can be reversed.

## 1. Current implementation findings

The current Agent Activity timeline classifies tools by name and renders a diff when a tool payload happens to contain before/after snapshots. Its `Mark reviewed` and `Needs changes` controls are session-local annotations only.

The current file browser exposes `writeTextFile(path, content): Boolean`. Android writes directly with `File.writeBytes`, without an expected-content hash, staging area, atomic replacement, or durable rollback journal. `GuestFileMap` blocks path traversal and canonical-path escapes, but resolving a path and later opening it leaves a symlink/time-of-check-to-time-of-use race window.

The current tool set does not provide a first-party staged file-edit contract. MCP tools and shell commands may have their own side effects and cannot safely be treated as reversible app-managed edits.

## 2. Required behavior

A file edit proposed by the agent must not modify the target file before the user chooses **Accept**. **Reject** discards the staged proposal and leaves the target untouched. After an accepted change, a separate **Rollback** action may restore the original only if the target still matches the exact hash written by that transaction.

The UI must never show actionable Accept/Reject controls for an operation that the app cannot prove it staged and owns.

### Transaction states

```text
PROPOSED -> APPLYING -> ACCEPTED -> ROLLED_BACK
    |           |
    v           v
 REJECTED     CONFLICT
                |
                v
             FAILED
```

- `PROPOSED`: immutable original snapshot and proposed content are stored privately; target is unchanged.
- `APPLYING`: the journal records intent before touching the target.
- `ACCEPTED`: atomic replacement succeeded and the resulting SHA-256 was verified.
- `REJECTED`: staged data was discarded; target was never modified.
- `CONFLICT`: target no longer matches the original snapshot; no write is attempted.
- `FAILED`: apply failed. The transaction attempts to restore its backup and records whether recovery succeeded.
- `ROLLED_BACK`: an accepted change was restored after verifying that the current target still matches the accepted hash.

## 3. Component boundaries

### 3.1 Proposal API

Introduce a first-party `FileChangeController` with typed operations:

- `proposeTextChange(path, proposedContent, summary)`
- `getProposal(id)` / `observeProposals()`
- `accept(id)`
- `reject(id)`
- `rollback(id)`

A proposal contains an opaque ID, normalized workspace-relative path, operation kind, original SHA-256 (or an explicit “file absent” marker), proposed SHA-256, byte counts, timestamps, and transaction state. The original and proposed bytes live in app-private staging storage, not in chat history or tool arguments.

The model-facing tool may propose a change, but it must not accept, reject, or roll back on the user's behalf. Accept/Reject are UI actions requiring a direct user gesture.

### 3.2 Filesystem adapter

The platform adapter owns path resolution and the atomic compare-and-swap operation. The common contract must return typed outcomes rather than a Boolean:

- applied
- conflict (expected hash mismatch)
- missing
- invalid/out-of-root path
- symlink or unsupported file type
- too large / invalid UTF-8
- unsupported atomic move
- I/O failure with recovery status

The adapter must support the Android chat sandbox and DORAI Build workspace separately. Desktop must use an explicitly configured project root; it must not infer an arbitrary writable root from a model-supplied absolute path. iOS/Web targets without a supported local workspace must report the capability as unavailable.

### 3.3 Review UI

Each file proposal is rendered from the controller's durable proposal record, not reconstructed from an arbitrary tool result. The card shows path, operation, before/after diff, hashes (short form), conflict/error state, and transaction state.

- **Accept** invokes the controller's `accept(id)`.
- **Reject** invokes `reject(id)`.
- **Rollback** is available only for an accepted transaction with a retained backup and matching current hash.
- While applying, actions are disabled and progress/status is visible.
- A conflict offers refresh/review; it must not silently overwrite the user's newer content.
- The current “Mark reviewed / Needs changes” annotation may remain for unmanaged external operations, but must be visually distinct from transaction actions.

## 4. Safe apply algorithm

For one file, the apply operation must run on an I/O dispatcher and under a per-canonical-path in-process mutex:

1. Validate the proposal state and workspace root.
2. Normalize the relative path; reject absolute paths, empty segments where ambiguous, `.`, `..`, NUL, and platform separator tricks.
3. Resolve every path component without following symbolic links. Reject a symlink in any parent or at the target. Revalidate immediately before commit.
4. Require a regular UTF-8 text file, or an explicitly absent target for a create operation. Enforce a bounded size (initial target: 512 KiB).
5. Read current bytes and compute SHA-256. Compare with the proposal's original hash. On mismatch, return `CONFLICT` without writing.
6. Persist a transaction journal and a private backup of the original bytes and relevant file metadata before replacement.
7. Write proposed bytes to a uniquely named temporary sibling file; flush the file descriptor, preserve supported permissions, and verify the temporary file's SHA-256.
8. Replace the target using an atomic move on the same filesystem. If atomic replacement is unavailable, fail closed; do not fall back to truncating the target in place.
9. Re-read the target and verify the proposed SHA-256.
10. Mark the journal `ACCEPTED` only after verification. If any step after replacement fails, restore from the backup with the same atomic-replacement rules and record recovery outcome.

For rollback, compare the current target against the accepted SHA-256 before restoring the original backup. If the target has changed since acceptance, return `CONFLICT`; never overwrite later user or agent edits.

## 5. Crash recovery and retention

- Journal writes must themselves use temp-file + atomic replacement.
- On startup, reconcile incomplete `APPLYING` transactions. If the target hash equals the proposed hash, finalize as accepted; if it equals the original hash, mark the apply as not committed; otherwise preserve both the backup and target and mark conflict/recovery-required.
- Keep pending proposals across process restart.
- Expire rejected proposals immediately after cleanup; expire stale pending proposals after a bounded TTL (initial target: 24 hours).
- Retain accepted backups for a bounded rollback window (initial target: 7 days), subject to a storage quota.
- Never put staged source contents or backups in cloud conversation history, analytics, logs, or crash reports.

## 6. Security boundaries

- Use app-private storage for staging, backups, and journals.
- Restrict every proposal to a configured workspace root; no arbitrary absolute paths.
- Reject symlink traversal and re-check canonical containment at commit time.
- Use SHA-256 compare-and-swap to detect concurrent edits; a UI-level “file is unchanged” check is not sufficient.
- Keep a per-path mutex and revalidate immediately before atomic replacement. Document that arbitrary external processes do not honor the app mutex; hash conflict detection and atomic replacement are mandatory.
- Redact secrets in rendered previews, but never use redacted display strings as bytes for apply or rollback.
- Enforce strict UTF-8 and size limits. Binary, lossy-decoded, truncated, or incomplete snapshots are not eligible.
- Do not execute hooks, scripts, or model-provided shell commands as part of Accept/Reject.
- Do not promise rollback for MCP calls, network requests, terminal commands, package installs, or other external side effects.

## 7. Scope and rollout

### Phase A — transaction foundation
- Add common proposal/state/result models and controller contract.
- Add platform filesystem adapters and private durable staging/journal storage.
- Add SHA-256, atomic replace, conflict checks, rollback, startup recovery, and bounded retention.
- Add tests for path traversal, symlinks, stale hashes, missing/create targets, UTF-8/binary/size limits, atomic-move failure, interrupted apply, rollback conflict, and idempotency.

### Phase B — agent integration
- Add a first-party proposal tool that stages content without writing the target.
- Add proposal IDs to Agent Activity mapping and load proposal records from the controller.
- Wire Accept/Reject/Rollback callbacks through the chat timeline to the controller.
- Keep arbitrary MCP/shell file operations explicitly unmanaged and non-reversible.

### Phase C — validation
- Verify Android chat sandbox and DORAI Build use the correct independent roots.
- Verify desktop uses the configured project root and that unsupported platforms hide/disable transaction actions.
- Run common tests, desktop tests, Android FOSS compile/unit tests, and focused filesystem integration tests.
- Verify that no Accept/Reject action can bypass the user gesture or the hash/path checks.

## 8. Settings and remaining-screen migration gate

Settings and remaining screens are deliberately sequenced after Phases A–C. The Settings implementation already has an adaptive navigation rail/detail pane and section components; migration should audit and refine the existing implementation rather than replace it wholesale. After File Review is validated, migrate remaining Settings sections, provider/model selection, MCP/Skills, tool approval, sandbox, and other navigation/detail screens in small independently testable groups.
