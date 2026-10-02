# AnyDoc Continuous Improvement State

**Canonical state:** this file  
**Branch:** `codex/anydoc-continuous-improvement`  
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.  
**Current report:** `2026-10-02_cycle-015`  
**Report status:** ACTIVE / INCOMPLETE  
**Cycle:** 015  
**Current Epic:** `E015` — Heap-safe ID-card sheet creation  
**Current Feature:** `F017` — Bounded dual image decode memory  
**Current User Story:** `US-R015-P2-01A` — Bound combined front/back bitmap memory (ACTIVE)  
**Hourly run counter:** 20  
**Six-hour checkpoint counter:** 0  
**Report creation timestamp:** 2026-10-01 UTC  
**Last completed cycle:** Cycle 014  
**State checkpoint timestamp:** 2026-10-01 UTC

## Git checkpoint

- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 014 validated code/test SHA: `973871f62be541e4d9c239272e832a0b2c2eafc1`
- Cycle 014 CI: run #275 / API `36962785571` — SUCCESS

## Next executable step

Bound `PdfIdCardTool` combined retained front/back bitmap memory with a conservative heap-aware decode ceiling, add focused regressions, and validate the exact candidate with GitHub Actions.
