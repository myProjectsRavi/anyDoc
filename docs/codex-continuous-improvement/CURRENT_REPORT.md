# Current Report

**ID:** `2026-10-02_cycle-015`  
**Status:** ACTIVE / INCOMPLETE  
**Branch:** `codex/anydoc-continuous-improvement`  
**Epic:** `E015` — Heap-safe ID-card sheet creation  
**Feature:** `F017` — Bounded dual image decode memory  
**User Story:** `US-R015-P2-01A` — Bound combined front/back bitmap memory

## Starting evidence

Cycle 014 is complete. Exact code/test SHA `973871f62be541e4d9c239272e832a0b2c2eafc1` passed GitHub Actions run #275 / API `36962785571`.

Cycle 015 was activated canonically in `STATE.md` before production/test mutation. Activation HEAD `45f8330e60b58e87f96a782f64fdcf3121c997b8` passed GitHub Actions run #276 / API `36964642960`.

## Source finding

`PdfIdCardTool.createFrontBackSheet()` retains the decoded front bitmap while decoding and retaining the back bitmap. Both calls currently use a fixed `maxLongEdge = 1800`, so two near-square ARGB_8888 inputs can coexist without a heap-aware combined ceiling.

## Acceptance criteria

- Preserve current front/back sheet layout and output behavior.
- Derive a conservative per-image decode long-edge ceiling from a combined two-bitmap ARGB_8888 heap budget.
- Clamp the ceiling so normal/high-memory devices do not exceed the existing 1800 px quality cap.
- Add focused pure unit regressions for low, normal, and high heap sizes.
- Validate the exact code/test candidate through authoritative GitHub Actions.
- Do not claim emulator, benchmark, or physical-device evidence.

## Next exact action

Introduce the pure heap-aware ID-card decode budget helper, wire both front/back decodes through its ceiling, add focused regressions, and validate the exact candidate with GitHub Actions.
