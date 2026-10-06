# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-06_cycle-036`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 036
**Current Epic:** `E036` — Heap-safe PDF comparison
**Current Feature:** `F038` — Combined PDF compare raster budget
**Current User Story:** `US-R036-P2-01A` — Bound combined left/right/diff ARGB_8888 bitmap memory during PDF comparison (ACTIVE)
**Last completed cycle:** Cycle 035
**State checkpoint timestamp:** 2026-10-06 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 035 validated code/test SHA: `6fa0cfcf6f86a48956ef746f36e678edde546b78`
- Cycle 035 CI: run #437 / API `37389234670` — SUCCESS

## Cycle 035 completion evidence
`PdfCompareTool.diffBitmaps()` now performs bounded row-level cancellation checks, and deterministic regression coverage proves cancellation prevents processing the next diff row. Exact candidate `6fa0cfcf6f86a48956ef746f36e678edde546b78` passed authoritative GitHub Actions run #437.

## Cycle 036 source evidence
The durable backlog already identifies a P2 PDF-compare memory risk: left + right + diff ARGB_8888 bitmaps can coexist at render scale up to 4x without a combined heap-aware ceiling. This is the highest-priority explicit actionable candidate after Cycle 035.

## Next executable step
Inspect current `PdfCompareTool` raster sizing/allocation paths, derive a conservative combined bitmap byte budget from runtime heap, implement the smallest semantics-preserving clamp, add focused sizing/allocation regression coverage, and validate the exact candidate through authoritative GitHub Actions.
