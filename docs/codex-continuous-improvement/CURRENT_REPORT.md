# Cycle 115 — PDF page-size open cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E115 / F117 / US-R115-P2-01A
**Activation baseline:** `1ad7ed5e777219a13db58a3772dd6c3246fa693c`
**Previous cycle:** Cycle 114 COMPLETE; draft-PR Actions #879 / API `39128006515` SUCCESS.

## Source evidence
`PdfCompareTool.pageSize` checks only the page index, then opens a renderer page to read width/height without checking cancellation. The compare loop's cancellation check occurs before both left/right size reads. Cancellation between those reads may still open the right page. This is a source-level avoidable resource-work issue, not a device-observed crash.

## Acceptance and scope
- Check cancellation before opening any existing page for size/dimensions, including the right page after left-side processing.
- Preserve missing-page null behavior, returned width/height, successful page close, original exception identity, and suppression of cleanup exceptions.
- Add pure JVM deterministic regressions for cancellation no-open, successful dimensions and cleanup, thrown open/read/close cases and two-page cancellation sequencing.
- Validate the exact production/test SHA via draft-PR CI (six JVM groups, debug APK, unsigned release/R8, lint).
- No emulator/device/OEM, benchmark, measured latency or allocation-savings claims.

## Next executable step
Introduce a tiny testable page-size read helper and thread cancellation through `pageSize` call sites. Commit only on `codex/anydoc-continuous-improvement`. Inspect exact-SHA authoritative CI and reconcile canonical documents after terminal validation. Keep PR #1 draft/unmerged.
