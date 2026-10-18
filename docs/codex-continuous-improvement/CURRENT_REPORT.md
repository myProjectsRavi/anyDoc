# Cycle 117 — PDF output-page start cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E117 / F119 / US-R117-P2-01A
**Activation baseline:** `a6ef1d8c7d989155fd3a4217e65301066c8a2369`
**Previous cycle:** Cycle 116 COMPLETE; exact code/test SHA `a6ef1d8c7d989155fd3a4217e65301066c8a2369` passed draft-PR Actions #886 / API `39136348187`, job `117701568583`.

## Source evidence
`PdfCompareTool.compare` checks cancellation in raster diff loops, then computes output page geometry, builds `PdfDocument.PageInfo`, and invokes `pdfDoc.startPage(pageInfo)` without a fresh cancellation preflight. A cancellation in this gap may still start an unnecessary output page. This is a source-level work/cancellation responsiveness risk, not a reproduced Android crash.

## Acceptance and scope
- Check cancellation immediately before `pdfDoc.startPage(pageInfo)` for each output page.
- Preserve existing normal page rendering, `finishPage`, original exception identity, document cleanup, bitmap recycling, staging and collision safety.
- Add focused pure JVM regressions for cancelled no-start, successful start, original start exceptions, and cancellation between raster completion and output page start.
- Require exact production/test SHA draft-PR CI (six JVM groups, debug APK, unsigned release/R8 and Android lint).
- No Android emulator/physical-device/OEM/benchmark or measured cancellation-latency claims.

## Next executable step
Introduce a small `startComparePdfPageWithPreflight` helper in `PdfCompareTool.kt` and wrap the `pdfDoc.startPage(pageInfo)` call. Add deterministic JVM tests and obtain exact-SHA CI. Keep PR #1 draft/unmerged and `main` unchanged.
