# Cycle 103 — Preserve PDF page-rendering failure during bitmap cleanup

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E103 / F105 / US-R103-P2-01A
**Previous cycle:** Cycle 102 COMPLETE. Exact production/test SHA `6f130bdbe4b4b1cd5277a4061bc5704f8d21a21f` passed authoritative draft-PR Actions #831 / API `39075310686`, job `117521147882`, with all nine configured gates SUCCESS (core domain, PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, Android lint). Docs-head #832 / API `39077710177` and #833 / API `39079471345` also succeeded. Five host JVM regressions cover normal cleanup, original-error preservation, distinct cleanup suppression, identical Throwable self-suppression, and cancellation. No Android device/emulator/OEM/benchmark/peak heap or complete race-freedom evidence.

## Source evidence and acceptance
`core/pdf/src/main/java/com/docforge/core/pdf/PdfCompareTool.kt` `renderPage`: after a `page.render` failure, the catch directly invokes `bitmap.recycle()` before `throw t`. If recycle throws, the original failure may be replaced. Investigate existing `withCleanupOnFailure` helper and callers; implement a minimal compatible preservation of primary Throwable and suppressed cleanup error, with focused deterministic host JVM tests. Do not claim Android Bitmap.recycle has been observed throwing.

## Next executable step
Re-read exact branch HEAD/PR/CI and primary activity. Inspect `renderPage` and cleanup tests, introduce only a bounded regression-backed change, obtain all-nine-gate exact-code/test-SHA draft-PR CI. Keep PR #1 draft/unmerged; main untouched. No device/emulator/OEM/benchmark/race-freedom claims.
