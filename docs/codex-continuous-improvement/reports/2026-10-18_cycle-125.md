# Cycle 125 — Staged-writer cancellation precedence

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E125 / F127 / US-R125-P2-01A
**Activation baseline:** `7d3a735e8547c3abb9d458bebcb78c37d74afbb0`
**Last validated production/test SHA:** `7d3a735e8547c3abb9d458bebcb78c37d74afbb0` (Cycle 124, draft-PR Actions #916 / API `39177197162`, job `117822197047`, nine gates SUCCESS).

## Source evidence
Cycle 125 E125/F127/US-R125-P2-01A ACTIVE / INCOMPLETE before production/test mutation. Source at `7d3a735e8547c3abb9d458bebcb78c37d74afbb0`: `PdfIoUtils.withStagedOutputFilesUsingIdentityReaders` calls `request.block(stagedFile)` then immediately performs `requireRegularStagedOutput` and `requireOriginalStagedIdentity` without a cancellation preflight. If a writer cancels its coroutine but returns after removing/replacing the staged file, filesystem validation can throw a different exception before the later `ensureActive()` check, obscuring cancellation and performing avoidable I/O. Add `currentCoroutineContext().ensureActive()` immediately after the writer returns, preserving cleanup and exception identity. Add deterministic core PDF JVM regressions for cancelled writer with missing staged file, ordinary cancellation, normal output and original writer exceptions; require exact-SHA draft-PR CI. This is a source-level P2 correctness/responsiveness concern, not a reproduced device crash.

## Acceptance
- Check cancellation immediately after the staging writer returns, before regular-file and identity validation.
- A writer that cancels and removes its staging file must surface cancellation rather than a staged-file validation error.
- Preserve ordinary successful staging, output collisions, symlink checks, identity ownership and cleanup; preserve original writer errors.
- Add focused deterministic core PDF JVM tests and obtain authoritative exact-code/test-SHA draft-PR CI (all nine configured gates).
- Do not claim emulator, device, benchmark or race-freedom evidence.

## Next executable step
Implement one cancellation preflight after `request.block(stagedFile)` in PdfIoUtils, add focused tests, review the diff, run exact-SHA PR CI, and reconcile canonical documents after terminal validation. Keep PR #1 draft/unmerged and main untouched.
