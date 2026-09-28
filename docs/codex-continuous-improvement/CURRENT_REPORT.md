# Current Report

**ID:** `2026-09-28_cycle-008`  
**Report:** `reports/2026-09-28_cycle-008.md`  
**Status:** ACTIVE / INCOMPLETE  
**Branch:** `codex/anydoc-continuous-improvement`  
**Baseline main:** `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`  
**Epic:** `E008` — Failure-safe PDF comparison output  
**Feature:** `F010` — Transactional PDF compare publication  
**Current User Story:** `US-R008-P1-01A` — Stage PdfCompareTool final PDF publication

## Priority change

Fresh source inspection for the planned P2 allocation audit found a P1 defect first. `PdfCompareTool` writes directly to a final output path and closes its `PdfDocument` only on the success path. P1 preempts P2 under the backlog rules.

## Scope

Stage compare output with `withStagedOutputFile`, guarantee `PdfDocument` cleanup across failure/cancellation, preserve collision-safe naming/result semantics, and add focused regression coverage.

## Completion gate

Require core PDF tests, converter tests, app tests, debug assembly, unsigned release/R8, and lint on the exact code/test candidate.

## Next exact action

Implement the smallest transactional compare-output refactor and validate it.
