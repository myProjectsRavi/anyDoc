# Cycle 127 — Cache-copy transfer-loop cancellation precedence

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E127 / F129 / US-R127-P2-01A
**Activation baseline:** `9ecc3c2bbafa02b433d789579523e06eac5c313f`
**Last validated production/test SHA:** `1a57f59f7ca760e585e76875d3a7c8a46c19929d` (Cycle 126, Actions #924 / API `39188092230`, job `117854690723`, nine gates SUCCESS).

## Source evidence
Cycle 127 E127/F129/US-R127-P2-01A ACTIVE / INCOMPLETE before production mutation. At `9ecc3c2bbafa02b433d789579523e06eac5c313f`, `Context.copyUriToCacheFile` loops over source chunks and reads `StatFs(cacheDir.absolutePath).availableBytes` before `copyChunkWithCancellation` checks cancellation. It also checks free space after transfer without a fresh cancellation check. If cancellation occurs after stream acquisition or during transfer, avoidable StatFs I/O or an insufficient-space exception can precede cancellation. Add a small suspend JVM-testable preflight around per-chunk storage checks, preserving reserve thresholds, chunk ordering, transfer semantics, temp cleanup, and original non-cancellation failures. Add deterministic core PDF JVM regressions and obtain exact-source/test-SHA draft-PR all-nine-gate CI. This is source-level P2 responsiveness/cancellation precedence, not a reproduced Android crash.

## Acceptance
- Already-cancelled transfer-loop iterations must skip free-space checks and transfers.
- Cancellation during transfer must take precedence over subsequent free-space queries.
- Active copy retains both existing free-space safety thresholds and exception/temporary-file ownership.
- Focused deterministic core PDF JVM regressions pass and exact-source/test-SHA draft-PR CI passes all nine configured gates.

## Next executable step
Add minimal coroutine cancellation preflight before transfer-loop free-space queries, with focused tests and no behavior change for active copies. Commit source/tests only after this active-cycle checkpoint is published. Inspect exact-SHA draft-PR CI and reconcile canonical documents after terminal CI. PR #1 draft/unmerged; main untouched.
