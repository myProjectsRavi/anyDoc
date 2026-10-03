# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-03_cycle-028`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 028
**Current Epic:** `E028` — Failure-safe PDF text extraction
**Current Feature:** `F030` — Transactional extracted-text publication
**Current User Story:** `US-R028-P1-01A` — Stage PdfTextExtractor TXT publication (ACTIVE)
**Last completed cycle:** Cycle 027
**State checkpoint timestamp:** 2026-10-03 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 027 validated code/test SHA: `bdaf63302abe83978a99a68f2d0c6cdb4d21eb8e`
- Cycle 027 CI: run #373 / API `37139896501` — SUCCESS

## Cycle 027 completion evidence
`ImageFormatConverter` now derives scaled dimensions from the requested scale and heap-aware ARGB_8888 budget, preserving the requested scale when safe and proportionally reducing oversized upscales. Exact candidate `bdaf63302abe83978a99a68f2d0c6cdb4d21eb8e` passed all configured workflow gates.

## Cycle 028 source evidence
`PdfTextExtractor.extractToTxt()` allocates a collision-safe final `.txt` path and writes bytes directly with `FileOutputStream(output)`. A writer failure or cancellation can therefore leave a partial user-visible extracted-text file.

## Next executable step
Route TXT serialization through `withStagedOutputFile`, preserve extraction/page-count/naming semantics, add focused failure/cancellation publication tests, and validate the exact candidate.
