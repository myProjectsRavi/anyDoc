# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-06_cycle-038`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 038
**Current Epic:** `E038` — Heap-safe video audio extraction
**Current Feature:** `F040` — Bounded extractor sample buffer allocation
**Current User Story:** `US-R038-P2-01A` — Reject unreasonable MediaExtractor sample-buffer demands before allocation (ACTIVE)
**Last completed cycle:** Cycle 037
**State checkpoint timestamp:** 2026-10-06 UTC

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 037 validated code/test SHA: `2e6755b7bc51c291232ec3d2b82ad2319a018dca`
- Cycle 037 CI: run #443 / API `37453064894` — SUCCESS
- Cycle 037 documentation checkpoint: `78a750b3bafd51a4ef5d7fdca5ae963bcda902b4`; run #444 / API `37457391483` — SUCCESS

## Cycle 037 completion evidence
Unknown-size URI cache copies preflight the 32 MiB reserve plus one 8 MiB maximum transfer chunk before each transfer, while retaining post-transfer reserve, cancellation, and cleanup behavior. Deterministic boundary coverage rejects 40 MiB - 1 and accepts exactly 40 MiB.

## Cycle 038 source evidence
`VideoAudioExtractor.selectBufferSize()` trusts `MediaFormat.KEY_MAX_INPUT_SIZE` without an upper bound and passes the result directly to `ByteBuffer.allocate()`. Malformed or extreme container metadata can therefore request an unreasonable heap allocation before extraction begins.

## Next executable step
Introduce a deterministic conservative sample-buffer ceiling, reject metadata above that ceiling before allocation, preserve the existing 256 KiB fallback/minimum behavior, add converter-module boundary tests, and validate the exact candidate through authoritative GitHub Actions.
