# Cycle 114 — PDF renderer page-open cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E114 / F116 / US-R114-P2-01A
**Activation baseline:** `c95a0c28094b7e4b958726b6a47584e0cbd7d13c`
**Previous cycle:** Cycle 113 COMPLETE, draft-PR Actions #876/API `39125693956` SUCCESS.

## Source evidence
In `PdfCompareTool.renderPage`, the non-null page path currently executes `renderer.openPage(pageIndex).use { page -> renderComparePageWithPreflight(...) }`. The preflight inside the page scope avoids a cancelled bitmap allocation but still opens a `PdfRenderer.Page` after cancellation. This is a source-level resource-work concern, not an observed Android crash.

## Acceptance and scope
- Check cancellation before opening a non-null renderer page and retain the existing cancellation check immediately before bitmap allocation.
- Preserve missing-page behavior, page closure, successful raster content, cleanup, exception identity and cancellation propagation.
- Add focused JVM regressions covering cancelled no-open, successful open, thrown preflight/open exceptions, and no extra bitmap allocation on cancellation.
- Validate exact code/test SHA via draft-PR CI: six JVM groups, debug APK, unsigned release/R8, Android lint.
- No emulator/device/OEM, measured speedup or memory-savings claim.

## Next executable step
Introduce a tiny testable preflight-before-open helper and focused JVM tests. Commit only on `codex/anydoc-continuous-improvement`; inspect exact-SHA authoritative CI and reconcile all canonical documents after terminal validation. Keep PR #1 draft/unmerged.
