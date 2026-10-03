# Current Report

**ID:** `2026-10-03_cycle-026`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E026` — Heap-safe scanner perspective correction
**Feature:** `F028` — Bounded document-region decode before OpenCV warp
**User Story:** `US-R026-P2-01A` — Bound scanner perspective region decode by heap and output ceiling

## Starting evidence
Cycle 025 exact candidate `9760f56e7067cf169a7336be06c01f7b3495ab83` passed GitHub Actions #352 / API `37137314847`.

## Source finding
Scanner region decoding allows a fixed 3600 px ARGB_8888 long edge before perspective correction. OpenCV then creates additional native source/transformed allocations, while corrected output itself is capped to 2000 px.

## Acceptance criteria
- Never region-decode beyond the 2000 px perspective-output quality ceiling.
- Add a conservative runtime-heap-derived per-bitmap ceiling leaving headroom for native Mats/output bitmap.
- Preserve normalized-corner mapping and power-of-two BitmapRegionDecoder sampling.
- Add pure tests for budget clamp, normal size, constrained heap, extreme aspect ratio, and overflow safety.
- Pass authoritative GitHub Actions on the exact candidate.

## Next exact action
Extract/test scanner perspective decode sizing, wire it into region sampling, then validate.
