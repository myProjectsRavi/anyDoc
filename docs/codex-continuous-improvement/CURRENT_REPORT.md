# Cycle 112 — PDF compare pre-allocation cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E112 / F114 / US-R112-P2-01A
**Activation baseline:** `2a44f5eb09990bfcb43d1bf5902b90fc5a9285a5`
**Latest documentation checkpoint before reconciliation:** `1bb5ce89e52a25617a918b9f4cdd339dfe472c7c`

## Evidence and scope
In `PdfCompareTool.diffBitmaps`, the normal two-page path creates a new ARGB bitmap before the first row cancellation check. The both-null fallback also allocates before checking cancellation. The missing-page copy branch already calls `checkCancelled()` before copy. A small preflight helper can check cancellation immediately before the two allocations, preserving cancellation exception identity and avoiding allocation when already cancelled.

## Acceptance
- Preflight must run before normal and both-null output bitmap allocations, and allocation must be skipped when preflight throws.
- Preserve bitmap ownership, output bytes/pixels, missing-page overlay, exception propagation and staged publication.
- Add focused deterministic JVM regressions for successful allocation, cancelled preflight, exception identity, allocation failure and cleanup.
- Validate the exact production/test SHA using authoritative draft-PR GitHub Actions: six JVM groups, debug APK, unsigned release/R8 and Android lint.
- No device/emulator/OEM or performance benchmark claims.

## Next executable step
Apply the smallest production/test correction on the authorized branch, inspect authoritative CI, then reconcile STATE.md, CURRENT_REPORT.md, BACKLOG.md, VALIDATION.md and this report only after terminal CI. Keep PR #1 draft and `main` untouched.
