# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-03_cycle-027`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 027
**Current Epic:** `E027` — Heap-safe image format scaling
**Current Feature:** `F029` — Bounded scaled bitmap allocation
**Current User Story:** `US-R027-P2-01A` — Bound ImageFormatConverter scaled output allocation (ACTIVE)
**Last completed cycle:** Cycle 026
**State checkpoint timestamp:** 2026-10-03 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 026 validated code/test SHA: `05f815ad10c928d483ad2cd38c7b9325bc35a04f`
- Cycle 026 CI: run #366 / API `37138433125` — SUCCESS

## Cycle 026 completion evidence
Scanner perspective region decode now uses the shared heap-aware power-of-two region sampler, never requests beyond the 2000 px perspective-output quality ceiling, and preserves normalized corner mapping. The final visibility repair exposed the shared budget helpers cross-module. Exact candidate `05f815ad10c928d483ad2cd38c7b9325bc35a04f` passed all configured workflow gates.

## Cycle 027 source evidence
`ImageFormatConverter` decodes inputs through the shared constrained decoder but permits `scaleFactor` up to 3.0 and then calls `Bitmap.createScaledBitmap` with raw multiplied dimensions. A 3× linear scale is 9× pixel area, so the scaled bitmap can exceed the heap-aware decode budget by an order of magnitude while the source bitmap is still resident.

## Next executable step
Add a pure heap-aware scaled-size helper that preserves the requested scale when safe but proportionally clamps the output to a conservative ARGB_8888 byte budget, wire `scaleBitmap()` to it, add converter-module JVM tests for normal/downscale/upscale/constrained-heap/pathological dimensions, then validate the exact candidate.
