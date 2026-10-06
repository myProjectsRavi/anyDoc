# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-06_cycle-037`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 037
**Current Epic:** `E037` — Reserve-safe unknown-size cache import
**Current Feature:** `F039` — Pre-transfer cache reserve guard
**Current User Story:** `US-R037-P2-01A` — Preserve the 32 MiB cache reserve before every unknown-size URI transfer chunk (ACTIVE)
**Last completed cycle:** Cycle 036
**State checkpoint timestamp:** 2026-10-06 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 035 validated code/test SHA: `6fa0cfcf6f86a48956ef746f36e678edde546b78`
- Cycle 035 CI: run #437 / API `37389234670` — SUCCESS

## Cycle 035 completion evidence
`PdfCompareTool.diffBitmaps()` now performs bounded row-level cancellation checks, and deterministic regression coverage proves cancellation prevents processing the next diff row. Exact candidate `6fa0cfcf6f86a48956ef746f36e678edde546b78` passed authoritative GitHub Actions run #437.

## Cycle 036 completion evidence
Current `PdfCompareTool` already applies `CompareRasterBudget` to the simultaneous left + right + diff ARGB_8888 working set, with focused arithmetic/aspect-ratio coverage. Exact branch checkpoint `ebdae3820081f3a118eebcd0a55cd1daf9dc7ffc` passed authoritative GitHub Actions run #438 / API `37394776462`.

## Cycle 037 source evidence
`copyUriToCacheFile()` checks unknown-size cache free space only after each transfer. A transfer may consume up to `FILE_CHANNEL_COPY_CHUNK_BYTES` (8 MiB), so allowing a chunk whenever only the 32 MiB reserve remains can consume the reserve before the post-transfer check rejects further copying.

## Next executable step
Require `CACHE_COPY_FREE_SPACE_RESERVE_BYTES + FILE_CHANNEL_COPY_CHUNK_BYTES` before each unknown-size transfer, keep the existing post-transfer/cancellation/cleanup behavior, add deterministic boundary tests for just-below/at-threshold space, and validate the exact candidate through authoritative GitHub Actions.
