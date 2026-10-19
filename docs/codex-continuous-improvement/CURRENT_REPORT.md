# Cycle 135 — Cancellation after provider input acquisition

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E135/F137/US-R135-P2-01A
**Activation baseline:** `84b7ed0b141e2eeb3923b9060980b1cbef89f3a3`
**Last validated production/test SHA:** `84b7ed0b141e2eeb3923b9060980b1cbef89f3a3`, authoritative draft-PR Actions #956 / API `39252968886`, job `118061098741`, all nine gates SUCCESS (Cycle 134).

## Source-backed risk
`Context.copyUriToCacheFile` checks cancellation before `contentResolver.openInputStream(uri)`, but after synchronous provider acquisition returns it enters `input.use`, constructs `Channels.newChannel(source)` and opens `FileOutputStream(tempFile)` without a fresh cancellation check. Cancellation during provider acquisition may therefore cause unnecessary channel/output setup before the next copy-loop check. Add a minimal coroutine preflight INSIDE `input.use` (to close input on cancellation) and INSIDE registered temp ownership (to release temp). Add deterministic host JVM tests for cancelled post-open callback skip/cleanup, success, original exceptions and ownership; exact-code/test-SHA draft-PR all-nine-gate CI required. Source-level P2 responsiveness risk, not an Android device-measured defect.

## Acceptance
- Check cancellation immediately after provider input acquisition and before channel/output setup, inside `input.use` and registered temp ownership.
- Preserve input close, registered-temp cleanup, original exception identity, free-space thresholds, copied bytes and staged output collision/no-overwrite behavior.
- Add focused deterministic core PDF JVM regressions; require authoritative exact-code/test-SHA draft-PR all-nine-gate CI before completion.
- Do not claim emulator, device, OEM, visual, benchmark, peak-heap or complete race-freedom evidence.

## Next executable step
Inspect the latest `PdfIoUtils.kt` cache-copy path and existing cancellation/ownership JVM tests; introduce the smallest pure-JVM-testable post-open cancellation preflight inside `input.use`, add regressions, then validate on draft PR #1. Do not touch `main` or merge/undraft PR #1.
