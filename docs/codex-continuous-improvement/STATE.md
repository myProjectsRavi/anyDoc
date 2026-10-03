# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-03_cycle-026`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 026
**Current Epic:** `E026` — Heap-safe scanner perspective correction
**Current Feature:** `F028` — Bounded document-region decode before OpenCV warp
**Current User Story:** `US-R026-P2-01A` — Bound scanner perspective region decode by heap and output ceiling (ACTIVE)
**Last completed cycle:** Cycle 025
**State checkpoint timestamp:** 2026-10-03 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 025 validated code/test SHA: `9760f56e7067cf169a7336be06c01f7b3495ab83`
- Cycle 025 CI: run #352 / API `37137314847` — SUCCESS

## Cycle 026 source evidence
`ScannerScreen.decodeDocumentRegionForPerspective()` region-decodes with a fixed 3600 px long edge in ARGB_8888. A square decode can approach 49.4 MiB before OpenCV creates source/transformed native Mats and an output bitmap. `DocumentEdgeDetector.perspectiveCorrect()` already caps corrected output at 2000 px, so the 3600 px source ceiling is unnecessarily expensive.

## Next executable step
Derive a conservative heap-aware region-decode ceiling no larger than the 2000 px perspective output ceiling, keep aspect ratio/corner mapping intact, add pure unit tests for sizing/sample selection, and validate the exact candidate.
