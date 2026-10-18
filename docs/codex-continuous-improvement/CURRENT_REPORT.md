# Cycle 111 — PDF compare raster initialization efficiency

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E111 / F113 / US-R111-P2-01A
**Activation baseline:** `26d932cc81810e59ab6b7352930db21105b780b4`
**Previous cycle:** Cycle 110 COMPLETE. Cycle 110 `E110/F112/US-R110-P2-01A` COMPLETE: exact code/test SHA `26d932cc81810e59ab6b7352930db21105b780b4` passed authoritative draft-PR Actions #862 / API `39108188107`, job `117618514524`, on 2026-10-18 UTC. All nine configured gates SUCCESS: core domain/PDF/converter/scanner/PDF-tools/app JVM tests, debug APK, unsigned release/R8, Android lint. `readChunk` now initializes only missing white tail pixels after getPixels; seven deterministic JVM regressions cover full/partial/missing chunks, reused buffers, boundaries and diff equivalence. No device/emulator/OEM/benchmark/peak-heap or measured speedup evidence.

## Source evidence and acceptance
At the activation baseline, `PdfCompareTool.diffBitmaps` calls `output.eraseColor(Color.WHITE)` immediately before a complete row/chunk traversal which appears to write each output pixel using `setPixels`. First prove coverage for all positive dimensions and partial final chunks. Preserve exactly identical diff colors and percentages, missing-page overlay, unequal dimensions, cancellation at row/4096-column boundaries, output staging/no-overwrite, and all bitmap ownership/cleanup. Remove the erase only if full coverage is demonstrated; add deterministic JVM tests and exact-code/test-SHA authoritative draft-PR CI. This is a source-level optimization candidate, not an Android performance result.

## Next executable step
Review current loop/tests and build a pure JVM full-coverage regression. Only then make a minimal production change, validate all nine configured CI gates, and reconcile canonical docs after terminal success. PR #1 stays draft; `main` untouched.
