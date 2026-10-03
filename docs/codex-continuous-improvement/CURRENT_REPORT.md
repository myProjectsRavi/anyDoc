# Current Report

**ID:** `2026-10-03_cycle-024`
**Status:** COMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E024` — Heap-safe PDF OCR rasterization
**Feature:** `F026` — Heap-aware OCR bitmap budget
**User Story:** `US-R024-P2-01A` — Bound PdfOcrTool PDF-page raster allocation

## Starting evidence
Cycle 023 exact candidate `79d0d11a87de414ffd03937fd4590045ee7b6a8e` passed GitHub Actions run #339 / API `37119773705`.

## Source finding
`PdfOcrTool.processPdf()` currently preserves quality with a fixed 1800 px long-edge ceiling, but its ARGB_8888 bitmap allocation has no runtime-heap-derived byte ceiling. OCR processing also needs ML Kit/native working memory.

## Acceptance criteria
- Preserve 1800 px as the maximum OCR quality ceiling.
- Add a conservative heap-aware byte budget for the single ARGB_8888 page bitmap.
- Preserve aspect ratio and avoid arithmetic overflow/pathological-dimension failures.
- Keep deterministic bitmap recycling.
- Add focused JVM tests for normal, constrained-heap, extreme-aspect, and pathological dimensions.
- Pass authoritative GitHub Actions on the exact code/test candidate.

## Next exact action
Implement a bounded OCR raster-size helper, wire `processPdf()` to it, add focused unit tests, and validate the exact candidate.


## Completion evidence
Exact code/test candidate `31e2ff40cc58f4a80ad1d278daed2d7314be1661` passed GitHub Actions run #345 / API `37135882621`: core PDF unit tests, converter tests, app lifecycle tests, debug APK, unsigned release/R8, and Android lint all succeeded. No emulator, benchmark, or physical-device evidence is claimed.
