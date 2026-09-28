# Current Report

**ID:** `2026-09-28_cycle-008`  
**Report:** `reports/2026-09-28_cycle-008.md`  
**Status:** COMPLETE  
**Branch:** `codex/anydoc-continuous-improvement`  
**Baseline main:** `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`  
**Epic:** `E008` — Failure-safe PDF comparison output  
**Feature:** `F010` — Transactional PDF compare publication  
**Current User Story:** `US-R008-P1-01A` — Stage PdfCompareTool final PDF publication

## Implementation

- Initial production refactor: `5bd0edea9d17ad835a1554d64c0cb5e11006885d`.
- Regression tests: `a4746aac8a7523b9afd77803631e6677dc5460f2`.
- Final compile/resource fix and exact candidate: `08cf9090c32e773b595b74102508fd4b8800ab01`.
- Compare output now uses `withStagedOutputFile` and is published only after the complete PDF has been written.
- PdfDocument closes in `finally`; PdfRenderer pages use structured close; per-page comparison bitmaps recycle in `finally`.
- Added failure/cancellation publication regression tests.

## Validation

Initial run #227 / API `36427852795`: FAILED during core PDF compilation because Android PdfDocument is not Closeable and cannot use Kotlin `use`.

GitHub Actions PR run #229 / API `36428222433`: **SUCCESS** on exact repaired candidate `08cf9090c32e773b595b74102508fd4b8800ab01`.

Passed:
- core PDF unit tests
- converter unit tests
- app lifecycle/unit tests
- debug APK assembly
- unsigned release/R8 assembly
- Android lint

No emulator, benchmark, or physical-device result is claimed.

## Completion

`US-R008-P1-01A`: COMPLETE.  
Cycle 008: **1/1 COMPLETE**.

Draft PR #1 remains open/draft/unmerged. `main` remains untouched.

## Next exact action

Create Cycle 009 durably before code. Highest-priority confirmed P2 follow-up: bound PdfCompareTool's combined left/right/diff bitmap working set with a heap-aware ceiling.
