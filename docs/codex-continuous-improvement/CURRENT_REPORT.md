# Cycle 098 — PDF compare missing-page diff visibility

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E098 / F100 / US-R098-P2-01A
**Previous cycle:** Cycle 097 COMPLETE; Exact code/test SHA `386112813f01ea8daea2ee1cf7fc7bac28b2f5a1` passed authoritative draft-PR GitHub Actions #806 / API `39046842382`, job `117437154272`, on 2026-10-17 UTC. All nine configured gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools and app JVM tests; debug APK; unsigned release/R8; Android lint. Production `e78388d700b199049349155cb4c101688747c149` budgets raster bytes from remaining Java heap estimated by `maxMemory - (totalMemory - freeMemory)`; test `38611281` adds three focused JVM regressions for occupied, inconsistent and extreme occupancy plus combined bitmap fit. No emulator, physical-device, native-memory benchmark, measured OOM prevention or complete memory safety claimed.

## Source evidence
`PdfCompareTool.compare` uses `renderScale.coerceIn(0.5f, 4f)`; NaN passes the clamp and `requestedSize` collapses pages to 1x1 through Float-to-Int conversion. Host Kotlin probe verified NaN -> 1 px. This is a caller-input quality/correctness issue, not an observed Android crash.

## Acceptance and boundaries
- Normalize NaN to the existing default 1.5f; retain clamp for finite values and infinities.
- Add deterministic core PDF JVM tests covering NaN, positive/negative infinity, under/over range and normal values.
- Preserve default behavior, existing bitmap budget, staging, cancellation and recycling.
- Validate exact code/test SHA through draft-PR CI. No emulator/device/benchmark or universal memory safety claims. Keep PR #1 draft and main untouched.

## Next executable step
Add a small internal render-scale normalization helper in `PdfCompareTool.kt`, invoke it in compare, add `PdfCompareScaleTest.kt`, validate exact SHA via PR Actions, then reconcile all canonical documents.


## Cycle 098 checkpoint
- Cycle 097 code/test `c658610d54deeacbbffb55dbc542f3de0a8257e1` passed draft PR CI #809 (all nine gates); documentation checkpoint `e75394355fefa29a29b8ee1a308a56d10350b2c4` passed #810.
- E098/F100/US-R098-P2-01A is ACTIVE. Missing-page comparison currently copies the only present bitmap and reports 100%, but does not apply its documented red highlight. Next: add safe red overlay and deterministic JVM tests, then exact-SHA CI. No emulator/device proof. PR #1 draft; main untouched.
