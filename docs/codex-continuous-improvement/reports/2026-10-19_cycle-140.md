# Cycle 140 — Interrupted NIO metadata I/O

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E140/F142/US-R140-P2-01A
**Activation baseline:** `e0dfc902e0cd988022616b3ada28fa55877129f1`
**Last validated production/test SHA:** `e0dfc902e0cd988022616b3ada28fa55877129f1`, authoritative draft-PR Actions #971 / API `39316414595`, job `118278969825`, nine gates SUCCESS (Cycle 139).

## Source-backed risk
At exact validated source SHA `e0dfc902e0cd988022616b3ada28fa55877129f1`, `PdfIoUtils.metadataProbeOrNull` catches `java.nio.channels.ClosedByInterruptException` in its generic `catch (Exception)` and returns null, because this interruption signal extends `IOException` but not `InterruptedIOException`. A provider metadata operation that surfaces this exception could silently trigger descriptor fallback or cache-copy work. This is a source-level P2 risk, not a reproduced Android device defect. Keep ordinary IOException, SocketTimeoutException, and AsynchronousCloseException fallback unchanged.

## Acceptance
- Preserve original `java.nio.channels.ClosedByInterruptException` identity from metadata query/descriptor probes; do not perform descriptor fallback after interrupted query.
- Preserve existing fallback for ordinary `IOException`, `SocketTimeoutException` and `AsynchronousCloseException`; preserve valid size and exception cleanup behavior.
- Add deterministic core PDF JVM tests for direct probe, interrupted query/descriptor, no descriptor fallback, ordinary/timeout/async-close fallback.
- Require authoritative exact-code/test-SHA draft-PR nine-gate CI. No Android device, emulator or benchmark claims. Keep PR #1 draft/unmerged; never modify main.

## Next executable step
Inspect current metadata helper/tests; implement the smallest safe exception propagation and focused JVM regressions; validate exact code/test SHA through draft-PR CI; reconcile canonical docs after terminal CI.
