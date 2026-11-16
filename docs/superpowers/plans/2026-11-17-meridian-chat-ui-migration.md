# Meridian Chat UI Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate the DORAI AI runtime chat UI to the Meridian design language from `G:\Web\chat-ui-screens` (Bengali-first, purple/indigo accent, dark/light/mobile) while retaining all DORAI features (voice, attachments, dynamic UI, history, tool approval, TTS).

**Architecture:** Phased migration. Phase 1 (done in this session): bug fixes + composer keyboard hints + accessibility touch targets + mojibake. Phase 2: color/palette alignment + empty-state Meridian layout. Phase 3: tool-card and message-row visual alignment. Phase 4: mobile responsive fixes (code horizontal scroll, composer overlap, shortcut wrap). Phase 5: docs/changelog + screenshot tests.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3, Compose Resources (values/values-bn).

**Spec:** `G:\Web\chat-ui-screens\README.txt` + prototype screens; `docs/features/chat.md`; Bengali policy `docs/development/bengali-language-and-rendering-policy.md`.

## Global Constraints
- Keep Gradle root project name `dorai`, packages, bundle IDs unchanged.
- All user-facing strings via Compose Resources (values + values-bn at minimum).
- No weakening of security configs.
- Bengali/Unicode integrity: no mojibake, grapheme-safe truncation.
- 48dp minimum touch targets; reduced motion respected.

## Phase 1 (completed this session)
- Fixed mojibake in `ChatEmptyState.kt:458` ("Â·" → "·").
- Fixed collapse-all convergence in `ChatMessageRow.kt:527-539`.
- Added keyboard hints below composer in `DoraiComposer.kt` + new string keys `composer_hint_send`/`composer_hint_newline` (values + values-bn).
- Localized LogoAnimation default contentDescription (null instead of hardcoded English).
- Model pill touch target ≥48dp in `ChatDetailPane.kt`.

## Phase 2 (in progress)
- [x] Empty-state hero aligned to Meridian text: `chat_welcome_ask` → "What would you like to work on today?" / "আজ কী নিয়ে কাজ করবেন?" (values + values-bn).
- [x] ~~Align color tokens~~ — DECISION: no separate preset. Chat UI keeps reading the app's active preset via `LocalDoraiColors`; Meridian applies to layout/typography, not a forced palette.
- [x] Rewrite empty-state layout to Meridian suggestion rows (icon + divider rows) instead of QuickPromptCard grid; DORAI shortcuts kept as wrapping FlowRow.
- [ ] Decide fate of "Your AI. Your World." tagline (use or remove) — currently unused; left as-is.
- [x] **Resource issue resolved:** new Compose string accessors must be imported explicitly (`import ...Res.string.composer_hint_send`) — extension properties are not in scope via the `Res` import alone. Keyboard hints now fully localized (values + values-bn).

## Phase 3
- Tool card visual alignment: collapse/expand, Input/Output key-value, role chips, sub-agent timeline, collapse-all footer.
- Message row: timestamps, action buttons, streaming caret.

## Phase 4
- Mobile: horizontal scroll for code blocks, composer overlap padding, shortcut row wrap, top-bar crowding.

## Phase 5
- Update docs/features/chat.md, CHANGELOG.md.
- Run screenshot tests, spotlessCheck.

## Review Focus
- Bengali glyph rendering and line-height in monospace badges.
- Unicode truncation at grapheme boundaries.
- Reduced motion on all animations.
- Touch target sizes on mobile.
- Composer overlap with scrollable content.
