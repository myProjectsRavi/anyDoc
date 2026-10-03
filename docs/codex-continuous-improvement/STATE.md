# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-03_cycle-025`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 025
**Current Epic:** `E025` — Heap-safe shared image decoding
**Current Feature:** `F027` — Runtime-heap-aware constrained bitmap decode
**Current User Story:** `US-R025-P2-01A` — Bound decodeBitmapConstrained allocation by runtime heap (ACTIVE)
**Last completed cycle:** Cycle 024
**State checkpoint timestamp:** 2026-10-03 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 024 validated code/test SHA: `31e2ff40cc58f4a80ad1d278daed2d7314be1661`
- Cycle 024 CI: run #345 / API `37135882621` — SUCCESS

## Completion evidence
`PdfOcrTool` heap-aware raster budgeting passed all configured gates on exact candidate `31e2ff40cc58f4a80ad1d278daed2d7314be1661`.

## Cycle 025 source evidence
Shared `decodeBitmapConstrained()` currently caps only by requested long edge. At the common 2200 px ceiling a square ARGB_8888 decode is about 19.4 MiB, and pre-P EXIF correction can briefly allocate a second transformed bitmap. Multiple PDF/image tools depend on this shared decoder.

## Next executable step
Add a conservative runtime-heap-aware decode budget, derive an aspect-preserving effective long edge before allocation on both ImageDecoder and BitmapFactory paths, add focused pure JVM budget tests, and validate the exact candidate with GitHub Actions.
