# Cycle 095 — Small-heap PDF compare raster budget review

**Status:** COMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E095 / F097 / US-R095-P2-01A
**Previous cycle:** Cycle 094 COMPLETE; exact code/test SHA `f9254345d91125c9f6304dc95f22bb370bdb23a0` passed authoritative draft-PR Actions #793 / API `39036699340`, job `117407938037`, all nine configured gates SUCCESS.

## Source evidence
`CompareRasterBudget.heapAwareBudgetBytes` currently uses `(maxHeapBytes / 8L).coerceIn(12 MiB, 48 MiB)`. For a 32 MiB maximum heap, the budget is 12 MiB, 37.5% of the heap instead of the nominal 12.5%. The current `PdfCompareRasterBudgetTest` asserts this 12 MiB floor. This is a source-level allocation-pressure concern, not observed Android OOM evidence.

## Acceptance and boundaries
- Reconcile existing intentional 12 MiB quality floor with realistic Android heap limits and simultaneous left/right/diff ARGB_8888 allocations.
- Add focused deterministic JVM boundary tests for small heaps and `fit`; choose the smallest safe change only after evaluating quality and compatibility.
- Preserve normal compare behavior, missing-page handling, cancellation, staged publication and resource recycling.
- Validate exact combined code/test SHA through authoritative draft-PR CI before marking COMPLETE. No device/emulator/OEM/benchmark or race-free claims. PR #1 stays draft; main untouched.

## Next executable step
Inspect `PdfCompareRasterBudgetTest` and `CompareRasterBudget.fit` together, quantify minimum safe working set for 16/32/64 MiB heaps, then implement a bounded regression-backed correction if warranted.


## Cycle 095 authoritative completion — 2026-10-17 UTC
- `E095/F097/US-R095-P2-01A` COMPLETE for bounded small-heap compare raster budgeting. Exact code/test SHA `34c7539e50630f53c365f877583506e40a5cf257` passed draft-PR Actions #796 / API `39041350384`. Documentation-head `43fe075dfe7b90ac10e9b479ff4aa8f15ef5b02e` passed Actions #797 / API `39043002749`, job `117426099487`, all nine configured gates SUCCESS.
- `CompareRasterBudget.heapAwareBudgetBytes` now limits budget to 1/8 of heap capped at 48 MiB, with a 12-byte floor for three 1-pixel ARGB_8888 bitmaps rather than a disproportionate 12 MiB minimum. JVM regressions assert 16/32/64 MiB boundaries, fitting three bitmaps and impossible minimum checks. No emulator, physical-device, performance/OOM measurements or universal memory safety claim.
- Next: audit the next distinct evidence-backed risk, activate a new cycle before production mutation. PR #1 draft/unmerged; main untouched.
