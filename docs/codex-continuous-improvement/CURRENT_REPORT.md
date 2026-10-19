# Cycle 134 — Preserve prepared-stage cleanup failure evidence

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E134/F136/US-R134-P2-01A
**Activation baseline:** `d5a15b024809faf4abd09e6687de6ba7cb5b6fd9`
**Last validated production/test SHA:** `d5a15b024809faf4abd09e6687de6ba7cb5b6fd9`, authoritative draft-PR Actions #952 / API `39237737247`, job `118010917722`, completed SUCCESS on 2026-10-19 UTC across all nine configured gates (six JVM groups, debug APK, unsigned release/R8, Android lint) (Cycle 133).

## Source-backed risk
In `PdfIoUtils.withStagedOutputFilesUsingIdentityReaders`, the outer `finally` loop over `prepared` uses `runCatching { deleteOwnedStagedEntry(...) }` and discards its exception. When an earlier stage is prepared and a later writer fails, a cleanup identity-reader exception for that earlier stage is silently lost rather than attached to the primary writer failure. This obscures evidence of an unremoved staging file; an identity mismatch must still fail closed without deleting unrelated entries. Audit the exact failure/cleanup ordering and add deterministic JVM regressions before changing behavior. Source-level P2 diagnostics and cleanup correctness risk, not a measured Android defect.

## Acceptance
- Preserve the exact original writer/publication exception when cleanup of an earlier prepared stage throws; retain distinct cleanup exception evidence without self-suppression.
- Preserve successful output publication, original exception identity, fail-closed file-identity checks, stage ownership, collision/no-overwrite and cancellation semantics.
- Add focused host JVM tests for an earlier prepared stage plus a later failing writer, same/different cleanup exceptions, successful publication and cleanup.
- Require authoritative exact-code/test-SHA draft-PR nine-gate CI; do not claim Android device, benchmark, OEM or race-freedom validation.

## Next executable step
Inspect `PdfIoUtils.kt` outer prepared-stage `finally` (near lines 498-502) and current staged-cleanup JVM tests. Design the smallest ownership-safe exception-handling change and focused regressions; avoid modifying code before this cycle is durably activated. Keep PR #1 draft/unmerged and main untouched.
