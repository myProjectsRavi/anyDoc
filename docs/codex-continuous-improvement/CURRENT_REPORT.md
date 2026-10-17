# Cycle 095 — Small-heap PDF compare raster budget review

**Status:** ACTIVE / INCOMPLETE
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
