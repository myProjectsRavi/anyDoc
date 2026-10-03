# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-03_cycle-023`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 023
**Current Epic:** `E023` — Failure-safe PDF merge image resources
**Current Feature:** `F025` — Deterministic merge bitmap cleanup
**Current User Story:** `US-R023-P2-01A` — Recycle PdfMerger image bitmap on all exit paths (ACTIVE)
**Last completed cycle:** Cycle 022
**State checkpoint timestamp:** 2026-10-03 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 022 validated code/test SHA: `82af38fc794a2d9c41d8b7e57ad0925d0d12f527`
- Cycle 022 CI: run #332 / API `37090120456` — SUCCESS

## Completion evidence
PdfFormTool fill/build publication is transactional with focused failure/cancellation coverage. Exact candidate `82af38fc794a2d9c41d8b7e57ad0925d0d12f527` passed run #332 / API `37090120456`.

## Cycle 023 source evidence
PdfMerger image handling calls `bitmap.recycle()` only after `appendImagePage()` succeeds. An exception or cancellation can skip deterministic recycling.

## Next executable step
Move bitmap recycling into a `finally` boundary, preserve merge behavior, add focused coverage where practical, and validate the exact candidate with GitHub Actions.
