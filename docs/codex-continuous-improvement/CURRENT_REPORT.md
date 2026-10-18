# Cycle 113 — PDF render-page allocation cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E113 / F115 / US-R113-P2-01A
**Activation baseline:** `418c37ccaa3e91ce4c717c65538685a7f2dfdbd8`
**Previous cycle:** Cycle 112 COMPLETE, Actions #871/API `39119917326` SUCCESS.

## Source evidence
In `PdfCompareTool.compare`, cancellation is checked at the start of each page loop, but `pageSize`, raster fitting and left/right rendering follow. `renderPage` calls `Bitmap.createBitmap` after opening the renderer page without a fresh cancellation preflight. Cancellation after the loop check or between left/right renders can therefore still allocate a large bitmap.

## Acceptance and scope
- Check cancellation before each non-null rendered-page bitmap allocation; do not alter missing-page behavior.
- Preserve `PdfRenderer.Page` close semantics and bitmap recycling when rendering fails.
- Preserve successful raster content, output publication, exception identity and cancellation propagation.
- Add deterministic JVM regressions for cancelled preflight, successful allocation, cleanup on render failure and right-page cancellation timing.
- Validate the exact code/test SHA through authoritative draft-PR CI, including all six JVM groups, debug APK, unsigned release/R8 and Android lint.
- Do not claim device/emulator/OEM validation or measured speedup.

## Next executable step
Review `renderPage` and `PdfCompareRenderPageCleanupTest.kt`; implement one bounded cancellation-preflight seam, focused JVM tests and exact-SHA CI. Keep PR #1 draft/unmerged and `main` untouched.
