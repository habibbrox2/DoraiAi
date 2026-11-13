# Bangla / English Language — Device Verification Checklist

**Last verified:** not yet verified on a device (2026-11-13)

This checklist covers the parts of the language pipeline that **cannot** be verified on a
desktop JVM. Everything in it has to be run on real hardware (or an emulator with a real
Bengali IME) before the language system can be called device-verified.

Status legend: `[ ]` pending · `[x]` verified · `[-]` not applicable on this platform.

## 0. Prerequisites

- [ ] Android device or emulator, API 26+, with a **Bengali-capable IME** installed
      (Gboard / AOSP Indic keyboard / Mochi Indic). The AOSP Latin keyboard cannot produce
      the composition cases below.
- [ ] A Bengali language pack for the speech/voice mode (only for the voice items).
- [ ] Build under test: `git rev-parse HEAD` recorded here for the report.

## 1. Android — app language switch (Settings → General → Language)

- [ ] Set language to **বাংলা**. The whole UI re-labels immediately: settings, chat, sandbox,
      dashboard, workspace, dialogs, empty states.
- [ ] No English leftovers in primary surfaces (nav labels, buttons, section titles).
- [ ] Set language to **English** and confirm the same surfaces switch back.
- [ ] Kill and relaunch the app: the chosen language persists.
- [ ] Change the **device** language (not the in-app setting) — the app must keep following
      the in-app preference, not the system locale.
- [ ] Rotate the device / switch apps: no crash, no blank screen (activity recreate path).

## 2. Android — Bengali text input in chat

- [ ] Type `বাংলা` with the Bengali IME: composition (অসম্পূর্ণ → সম্পূর্ণ) must not leak into the
      message bubble before the composition is committed.
- [ ] Type a conjunct (`ক্ষ`, `স্ত্র`, `জ্ঞ`) and confirm the glyphs compose correctly on screen
      and are not shown as separate letters.
- [ ] Type a vowel sign (`কী`, `শ্রেণি`) and confirm the matra attaches to the right consonant.
- [ ] Paste Bengali text copied from another app: no `?`, no `�`, no reordered matra.
- [ ] Send a message with Bengali text and confirm the **assistant answer** comes back in
      Bengali (language policy) and renders conjuncts/emoji correctly.
- [ ] Long Bengali answer: confirm no mid-cluster truncation in the chat bubble and that the
      "…" ellipsis is appended after a complete cluster.

## 3. Android — terminal / workspace IME (grapheme-aware delete)

- [ ] Open the Workspace terminal, type `ক্ষ` with the Bengali IME.
- [ ] Press backspace once: the **whole conjunct** must disappear (not a half-cluster like
      `ক` or `্`).
- [ ] Press backspace again: the next cluster disappears.
- [ ] Type `কী`, backspace once: both `ক` and `ী` disappear together.
- [ ] Type an emoji with ZWJ (`👩‍💻`), backspace once: the whole emoji disappears.
- [ ] Type `abc`, then backspace three times: exactly `abc` is removed, no residue.
- [ ] Type Bengali, then use the IME's "select all + delete": the line is fully cleared.

## 4. Android — dynamic UI (DORAI UI blocks)

- [ ] An assistant answer containing a `dorai-ui` block renders its controls.
- [ ] Long text inside a dynamic-UI widget truncates with an ellipsis and never splits a
      Bengali cluster.
- [ ] Buttons/labels in a dynamic-UI block wrap instead of clipping on a small screen.
- [ ] Selecting assistant text around a dynamic-UI block does not splice the widget's text
      into the copied selection.

## 5. iOS — language switch

- [ ] Settings → General → Language → বাংলা: UI re-labels.
- [ ] **Relaunch the app** (iOS may need a restart to re-resolve native surfaces) and confirm
      the language stayed Bengali.
- [ ] Same for English.

## 6. Voice mode (Android only)

- [ ] Bengali dictation produces conjuncts correctly in the composer.
- [ ] The spoken answer reads Bengali prose numbers as Bengali words and keeps identifiers
      (`v2.1`, `1.5GB`) in written form.

## How to report

Record for each item: **pass / fail / blocked**, the device model and Android/iOS version,
the IME used, and the app build (`git rev-parse HEAD`). A blocked item (for example "no
Bengali IME available on this device") must be reported as blocked — **not** as passed.

Until this checklist is filled in, the language system must be described as
**"verified on desktop/CI, pending on-device verification"**.
