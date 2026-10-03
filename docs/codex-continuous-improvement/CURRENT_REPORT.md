# Current Report

**ID:** `2026-10-03_cycle-027`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E027` — Heap-safe image format scaling
**Feature:** `F029` — Bounded scaled bitmap allocation
**User Story:** `US-R027-P2-01A` — Bound ImageFormatConverter scaled output allocation

## Starting evidence
Cycle 026 exact candidate `05f815ad10c928d483ad2cd38c7b9325bc35a04f` passed GitHub Actions #366 / API `37138433125` across core PDF tests, converter tests, app lifecycle tests, debug APK, unsigned release/R8, and lint.

## Source finding
`ImageFormatConverter` constrains source decode memory, but `scaleFactor` is allowed through 3.0 and `Bitmap.createScaledBitmap` allocates dimensions multiplied directly by that factor. A 3× linear upscale can require 9× source pixel memory while the source bitmap remains resident.

## Acceptance criteria
- Preserve requested scaling when the resulting bitmap fits the runtime budget.
- Never upscale/downscale to dimensions whose ARGB_8888 allocation exceeds the conservative heap-aware ceiling.
- Preserve aspect ratio and avoid integer/float overflow for pathological dimensions.
- Preserve existing quality/format/publication behavior.
- Add converter JVM tests for 1×, downscale, safe upscale, constrained budget, and pathological dimensions.
- Pass authoritative GitHub Actions on the exact code/test candidate.

## Next exact action
Implement/test a pure bounded scale-size helper and use it before `Bitmap.createScaledBitmap`.
