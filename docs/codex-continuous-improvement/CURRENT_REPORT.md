# Cycle 136 — Cancellation after cache input channel acquisition

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E136/F138/US-R136-P2-01A
**Activation baseline:** `ddbb652c9dc09c2c87fdea4bc2f5632b63ec59ac`
**Last validated production/test SHA:** `ddbb652c9dc09c2c87fdea4bc2f5632b63ec59ac`, authoritative draft-PR Actions #959 / API `39262756567`, job `118094439688`, all nine gates SUCCESS (Cycle 135).

## Source-backed risk
Cycle 136 E136/F138/US-R136-P2-01A ACTIVE / INCOMPLETE before production mutation. Source at `ddbb652c9dc09c2c87fdea4bc2f5632b63ec59ac`: `Context.copyUriToCacheFile` creates `Channels.newChannel(source)` and then opens `FileOutputStream(tempFile)` without a fresh cancellation preflight inside the channel's `.use` scope. Cancellation during channel acquisition can still cause avoidable output-file/channel setup before the transfer-loop preflight. Audit the source and tests; add a minimal coroutine ensureActive preflight INSIDE source-channel ownership before output opening, preserving provider stream close, registered temp cleanup, bytes, exception identity and free-space thresholds. Add deterministic core PDF host JVM regressions; require authoritative exact-code/test-SHA draft-PR all-nine-gate CI. This is a source-level P2 responsiveness risk, not a reproduced Android device defect.

## Acceptance
- Check cancellation immediately after source-channel acquisition and before output FileOutputStream creation, inside the channel `use` and existing provider-stream/registered-temp ownership.
- Preserve input/channel close, registered temp cleanup, original exception identity, free-space thresholds, normal bytes and staged output collision/no-overwrite.
- Add focused deterministic core PDF JVM regressions; require authoritative exact-code/test-SHA draft-PR all-nine-gate CI before completion.
- Do not claim Android emulator, physical device, OEM, benchmark, peak heap or complete race-freedom evidence.

## Next executable step
Re-read `PdfIoUtils.kt` and core PDF cancellation/ownership tests; implement a small JVM-testable cancellation preflight inside `Channels.newChannel(source).use` before FileOutputStream creation; validate exact source/test SHA on draft PR #1. Never touch main or undraft/merge PR #1.
