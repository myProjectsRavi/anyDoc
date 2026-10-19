# Cycle 143 — Preserve cancellation before compression serialization

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E143/F145/US-R143-P2-01A
**Activation baseline:** `dfcfa4e6400a0b01b8efb83c7685413bbcd1f32a`
**Last validated code/test SHA:** `dfcfa4e6400a0b01b8efb83c7685413bbcd1f32a`, authoritative draft-PR Actions #981 / API `39331493841`, job `118330932885`, all nine configured gates SUCCESS (Cycle 142).

## Source-backed risk
At `dfcfa4e6400a0b01b8efb83c7685413bbcd1f32a`, `PdfCompressor.compressWithJpegRasterization` calls `onProgress` after each page and immediately calls `outDoc.save(outputFile)` after the final callback without another cancellation check. A callback cancelling the Job on the final page can therefore cause unnecessary PDF serialization before scoped staging cleanup. This is a P2 source-level responsiveness concern, not a reproduced crash.

## Acceptance
- Check cancellation immediately before `outDoc.save(outputFile)`, including after final-page progress callback.
- Preserve successful output, exception identity and staged-file cleanup semantics.
- Add deterministic core PDF JVM tests for cancellation, ordering, save failure identity and successful save.
- Validate exact source/test SHA via draft-PR nine-gate CI; do not claim device/emulator/benchmark evidence.
- Keep PR #1 draft/unmerged; never modify main.

## Next executable step
Introduce a minimal JVM-testable save preflight in `PdfCompressor.kt`, add focused tests, publish only on `codex/anydoc-continuous-improvement`, inspect exact candidate CI, and reconcile canonical documents after terminal validation.

## Validation
No Cycle 143 source/test candidate exists at activation. Cycle 142 code/test SHA `dfcfa4e6400a0b01b8efb83c7685413bbcd1f32a` passed CI #981.
