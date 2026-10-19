# Cycle 139 — Interrupted provider metadata I/O

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E139/F141/US-R139-P2-01A
**Activation baseline:** `2069608de118b7bb8a47b185d4e798e9c9c1bfc7`
**Last validated production/test SHA:** `2069608de118b7bb8a47b185d4e798e9c9c1bfc7`, authoritative draft-PR Actions #968 / API `39302617044`, job `118231464639`, nine gates SUCCESS (Cycle 138).

## Source-backed risk
`PdfIoUtils.metadataProbeOrNull` preserves Java `CancellationException`, `InterruptedException` and Android `OperationCanceledException`, but still treats `java.io.InterruptedIOException` as an ordinary metadata failure and falls back. An interrupted provider metadata operation can therefore be obscured, allowing subsequent descriptor probing or cache-copy setup. This is a source-level P2 correctness concern, not a reproduced device defect.

## Acceptance
- Verify interrupted-I/O semantics and preserve original `InterruptedIOException` identity when appropriate; do not broaden cancellation handling without source evidence.
- Keep ordinary `IOException`/security metadata fallback and known-size behavior unchanged.
- Add focused deterministic core PDF JVM tests for interrupted query/descriptor, descriptor-skip, ordinary fallback, and exception identity.
- Require exact-code/test-SHA authoritative draft-PR all-nine-gate CI. Keep PR #1 draft/unmerged, `main` unchanged; do not claim Android runtime/OEM/benchmark proof.

## Next executable step
Audit current source and tests; implement only a justified bounded metadata-probe interruption fix, add JVM regressions, validate exact commit via draft-PR CI, then reconcile canonical documents after terminal success.
