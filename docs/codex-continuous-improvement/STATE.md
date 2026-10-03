# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-03_cycle-025`
**Report status:** COMPLETE
**Cycle:** 025
**Current Epic:** `E025` — Heap-safe shared image decoding
**Current Feature:** `F027` — Runtime-heap-aware constrained bitmap decode
**Current User Story:** `US-R025-P2-01A` — Bound decodeBitmapConstrained allocation by runtime heap (COMPLETE)
**Last completed cycle:** Cycle 025
**State checkpoint timestamp:** 2026-10-03 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 025 validated code/test SHA: `9760f56e7067cf169a7336be06c01f7b3495ab83`
- Cycle 025 CI: run #352 / API `37137314847` — SUCCESS

## Completion evidence
Shared constrained image decoding now honors caller quality ceilings while enforcing a conservative runtime-heap-derived ARGB_8888 budget with headroom for transformed-bitmap duplication. Exact candidate `9760f56e7067cf169a7336be06c01f7b3495ab83` passed all configured CI gates in run #352.

## Next executable step
Continue the evidence-backed P2 allocation/large-input audit and durably activate the next cycle before its first production mutation.
