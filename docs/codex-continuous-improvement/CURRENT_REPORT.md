# Current Report

**ID:** `2026-10-01_cycle-014`  
**Status:** ACTIVE / INCOMPLETE  
**Branch:** `codex/anydoc-continuous-improvement`  
**Epic:** `E014` — Heap-safe PDF comparison  
**Feature:** `F016` — Bounded PdfCompareTool raster allocation  
**User Story:** `US-R014-P2-01A` — Bound combined comparison raster memory

## Starting evidence

Cycle 013 is complete. Its OCR transactional-publication candidate `490d0b611bae83b5b2142779010e9ddea5def892` passed GitHub Actions run #264 / API `36884683987`.

Cycle 014 was activated canonically in `STATE.md` before production/test mutation. Current branch activation HEAD is `a2ff5373be9e7c58fc25442de9c6cc19efe4c75a`; GitHub Actions run #266 / API `36899851444` completed successfully on that exact HEAD.

## Source finding

`PdfCompareTool` already contains `CompareRasterBudget` and accounts for simultaneous left, right, and diff ARGB_8888 rasters. The remaining bounded defect is arithmetic robustness: `workingSetBytes()` uses exact Long addition/multiplication, so extreme but representable Int dimensions can throw `ArithmeticException` before `fit()` can downscale them.

## Acceptance criteria

- Preserve combined left + right + diff raster accounting.
- Make working-set accounting saturate rather than overflow for pathological dimensions.
- Preserve proportional fitting within the supplied byte budget.
- Add focused unit regressions for accounting, fitting, and extreme-dimension saturation.
- Validate the exact code/test candidate through the authoritative GitHub Actions workflow.
- Do not claim emulator, benchmark, or physical-device evidence.

## Next exact action

Add focused `CompareRasterBudget` regressions and replace overflow-prone working-set arithmetic with saturating arithmetic, then validate the exact candidate with GitHub Actions.
