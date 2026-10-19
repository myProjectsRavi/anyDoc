# Cycle 138 — Provider metadata interruption and cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E138/F140/US-R138-P2-01A
**Activation baseline:** `9a6a936c5f61194bf240ffce4eaac9e47ae7cb16`
**Last validated production/test SHA:** `9a6a936c5f61194bf240ffce4eaac9e47ae7cb16`, authoritative draft-PR Actions #965 / API `39291026016`, job `118191420770`, nine gates SUCCESS (Cycle 137).

## Source-backed risk
`PdfIoUtils.metadataProbeOrNull` rethrows Java `CancellationException` but its generic exception fallback suppresses `InterruptedException` and Android `OperationCanceledException`. A cancelled/interrupted metadata query should not trigger a descriptor fallback or continue copying. This is source-level P2 cancellation correctness, not a reproduced device defect.

## Acceptance
- Preserve interruption and Android provider cancellation without masking original exception identity.
- Preserve fallback for ordinary query/descriptor failures and known-size behavior.
- Add focused JVM regressions for interruption and ordinary fallback, and require exact-code/test-SHA nine-gate draft-PR CI.
- Keep PR #1 draft/unmerged and `main` untouched; no device, emulator or benchmark claims.

## Next executable step
Implement minimal metadata-probe exception propagation in `PdfIoUtils.kt`, add deterministic JVM tests, commit only on authorized branch, inspect exact-SHA CI, then synchronize all canonical documents after terminal validation.
