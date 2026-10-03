# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-03_cycle-024`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 024
**Current Epic:** `E024` — Heap-safe PDF OCR rasterization
**Current Feature:** `F026` — Heap-aware OCR bitmap budget
**Current User Story:** `US-R024-P2-01A` — Bound PdfOcrTool PDF-page raster allocation (ACTIVE)
**Last completed cycle:** Cycle 023
**State checkpoint timestamp:** 2026-10-03 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 023 validated code/test SHA: `79d0d11a87de414ffd03937fd4590045ee7b6a8e`
- Cycle 023 CI: run #339 / API `37119773705` — SUCCESS

## Completion evidence
`PdfMerger` image-page bitmap cleanup is protected by a `finally` boundary. Exact candidate `79d0d11a87de414ffd03937fd4590045ee7b6a8e` passed run #339 / API `37119773705`.

## Cycle 024 source evidence
`PdfOcrTool.processPdf()` caps the page long edge at 1800 px but does not derive an ARGB_8888 allocation ceiling from the runtime heap. OCR also requires ML Kit/native working memory, so a fixed raster ceiling is not sufficient on constrained devices.

## Next executable step
Preserve the 1800 px quality ceiling while adding a conservative heap-aware OCR bitmap byte budget, proportional overflow-safe raster sizing, focused JVM tests, and authoritative GitHub Actions validation of the exact candidate.
