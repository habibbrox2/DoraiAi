---
name: anr-audit-and-fix
description: Audits Android codebases for ANR risks and diagnoses ANR stack dumps.
  Use this skill when scanning Android code for ANR risks or triaging ANR stack dumps.
  Don't use for non-ANR crashes, dropped frames, general CPU profiling, or Perfetto
  system traces.
license: Apache-2.0 (upstream Google LLC). Vendored terms: ../../android-skills-LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-16'
  keywords:
  - Android
  - ANR
  - Application Not Responding
  - responsiveness
  - main thread
  - ANR stack dump
  - Sync Barrier Leak
---

Select the execution path matching the user's request:

## 1. Path A: Static codebase ANR risk audit

When auditing source files or scanning for thread-blocking operations:

- Follow [references/risk_analysis_guide.md](references/risk_analysis_guide.md) step by step: list candidates, **prove the executing thread** (trace callers when the thread depends on them), **prove the call blocks**, then attribute and grade. Report only candidates that pass these checks; list the dismissed ones separately with a one-line reason.
- Evaluate off-main-thread mutations of attached Views not merely as crash risks (`CalledFromWrongThreadException`), but as **Sync Barrier Leak ANR** risks with their own proof (off-main execution plus an attached View).

## 2. Path B: Trace-driven diagnosis and remediation

When analyzing ANR traces or stack dumps:

- Diagnose systematically using [references/trace_analysis_guide.md](references/trace_analysis_guide.md) and format the report with [references/anr_trace_report_template.md](references/anr_trace_report_template.md).
- Base findings strictly on log evidence. Propose application fixes **only** for `APP_DEFECT` or `APP_RESOURCE_OVERLOAD` (never for `EXTERNAL_BOTTLENECK`).

---

## DORAI AI scope note (local addendum)

> Added by DORAI AI. The upstream Google content above is unmodified except for the `license:`
> frontmatter line, which now points at the vendored Apache-2.0 copy in
> `.agents/skills/android-skills-LICENSE.txt`. Upstream path: `performance/anr-audit-and-fix`
> in https://github.com/android/skills.

### Where this applies in DORAI AI

- The app runs two foreground services with `android:foregroundServiceType="dataSync"`:
  `com.dorai.ai.DaemonService` and `com.dorai.ai.inference.ModelDownloadService`
  (`androidApp/src/main/AndroidManifest.xml`).
- The same kind of work also runs on desktop and iOS, where "ANR" does not exist.

### KMP boundary

- ANR analysis is Android-only: `ApplicationExitInfo`, `traces.txt`, `adb` traces.
- Work that belongs on every target stays in shared code; only the platform entry points differ.

### Rules for this repository

- Never do blocking I/O, network calls or heavy model work on the main thread — the daemon and the
  model download service are the usual suspects.
- A fix that moves work off the main thread must keep the user-visible behavior identical.

### Verify

- `./gradlew :androidApp:assembleFossDebug`
- Manually exercise the daemon and the model download flows on a device before handoff.
