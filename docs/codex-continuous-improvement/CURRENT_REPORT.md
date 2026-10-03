# Current Report

**ID:** `2026-10-03_cycle-025`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E025` — Heap-safe shared image decoding
**Feature:** `F027` — Runtime-heap-aware constrained bitmap decode
**User Story:** `US-R025-P2-01A` — Bound decodeBitmapConstrained allocation by runtime heap

## Starting evidence
Cycle 024 exact candidate `31e2ff40cc58f4a80ad1d278daed2d7314be1661` passed GitHub Actions run #345 / API `37135882621`.

## Source finding
`decodeBitmapConstrained()` limits dimensions by requested long edge only. The common 2200 px square ARGB_8888 case is ~19.4 MiB, and pre-P EXIF rotation may temporarily hold both source and rotated bitmaps.

## Acceptance criteria
- Preserve caller-requested quality ceiling; never upscale.
- Add conservative runtime-heap-derived ARGB_8888 decode ceiling accounting for possible transform duplication.
- Apply equivalent effective bounds on ImageDecoder and BitmapFactory paths.
- Preserve EXIF behavior and existing caller API.
- Add pure JVM tests for heap clamp, normal dimensions, low-heap downscale, extreme aspect ratio, and pathological dimensions.
- Pass authoritative GitHub Actions on the exact code/test candidate.

## Next exact action
Implement and wire a pure decode-bound helper, add focused tests, then validate.
