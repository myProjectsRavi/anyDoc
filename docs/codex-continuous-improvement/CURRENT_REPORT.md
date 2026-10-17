# Cycle 102 — Bitmap cleanup self-suppression safety

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E102 / F104 / US-R102-P2-01A
**Previous cycle:** Cycle 101 COMPLETE. Exact production/test SHA `d05bde850d46172ddb90463f7a2b0a65e92240f4` passed authoritative draft-PR Actions #828 / API `39071028675`, job `117508502508`, on 2026-10-17 UTC. All nine configured gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools, app JVM tests, debug APK, unsigned release/R8, Android lint. Six host JVM lifecycle regressions in `PdfCompareDocumentLifecycleTest.kt` verify normal close, draw/finish/cancellation failure preservation with suppressed close errors, close failure after successful write, and identical primary/cleanup throwable avoiding self-suppression. Production `withClosePreservingPrimaryFailure` wraps `PdfDocument` use. No emulator, physical device, OEM, visual/benchmark/peak heap or complete race-freedom proof.

## Source evidence and acceptance
Source-level evidence in `PdfCompareTool.kt`: `withCleanupOnFailure` calls `failure.addSuppressed(cleanupFailure)` without checking identity. If the injected cleanup callback throws the exact same Throwable instance as the original block, Java rejects self-suppression with `IllegalArgumentException`, masking the primary error. This is a deterministic helper edge case, not an observed Android Bitmap.recycle failure. Add focused JVM tests and smallest compatible guard; preserve bitmap recycling and original error; validate exact code/test SHA in draft-PR CI.

## Next executable step
Review existing `withCleanupOnFailure` callers and tests; reproduce identical Throwable cleanup case in a deterministic core PDF JVM regression. Make smallest safe fix only after confirming reachability/compatibility. Recheck branch HEAD/PR and exact-head CI before mutation. PR #1 draft/unmerged; main untouched.


## Validated candidate awaiting canonical reconciliation
- On 2026-10-17 UTC, production/test SHA `6f130bdbe4b4b1cd5277a4061bc5704f8d21a21f` passed draft-PR Actions #831 (API `39075310686`), job `117521147882`, with all nine configured gates successful. Docs-head CI #832 (API `39077710177`) also succeeded.
- Cycle 102 remains ACTIVE until STATE.md and all canonical reports are synchronized. Attempted atomic six-document reconciliation was blocked before mutation. Next: reconcile Cycle 102 completion and activate the next evidence-backed story before production edits.
