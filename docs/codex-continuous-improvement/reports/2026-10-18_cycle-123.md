# Cycle 123 — PDF compare staging-setup cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E123 / F125 / US-R123-P2-01A
**Activation baseline:** `8d1bdf2c7be2f646d89d7e19421b5e3169a529be`
**Last validated production/test SHA:** `d4921e0b47f414c009a433e7d9adf599096cebb2` (Cycle 122, Actions #909 / API `39166370082`, job `117790052942`, nine configured gates SUCCESS).

## Source evidence
After `resolveCompareOutputDirectoryWithPreflight`, `PdfCompareTool.compare` sanitizes the name and invokes `withStagedOutputFile` without another cancellation preflight. `withStagedOutputFilesUsingIdentityReaders` validates names, checks directory symlinks and can create a directory before its first `ensureActive()`. Cancellation between directory resolution and staging entry may cause avoidable filesystem checks or effects. Source-level P2 responsiveness issue, not a reproduced crash.

## Acceptance
- Check cancellation immediately before entering staged output setup.
- Preserve output naming, directory validation, ownership-aware cleanup, staged publication and exception identity.
- Add deterministic JVM regression tests for cancellation, successful staging ordering and exception preservation.
- Validate exact production/test SHA through draft-PR CI: six JVM test groups, debug APK, unsigned release/R8, Android lint.
- Do not claim emulator/device/OEM/benchmark/measured latency/heap/race-freedom evidence.

## Next executable step
Implement the minimal preflight in `PdfCompareTool.kt` and focused core PDF JVM tests, commit only to `codex/anydoc-continuous-improvement`, inspect authoritative CI, then reconcile STATE.md, CURRENT_REPORT.md, BACKLOG.md, VALIDATION.md and this report after terminal CI. Keep PR #1 draft/unmerged and main unchanged.
