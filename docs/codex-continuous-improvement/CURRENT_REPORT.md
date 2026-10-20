# Cycle 149 — Compression content-stream cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E149/F151/US-R149-P2-01A
**Activation baseline:** `55f90c61625357d72949bcaa94ba658b6856c12e`
**Last validated code/test SHA:** `55f90c61625357d72949bcaa94ba658b6856c12e` (draft-PR Actions #1000, API 39384376248, job 118501057462, nine gates SUCCESS).

## Source-backed risk
After JPEG encoding, `PdfCompressor.compressWithJpegRasterization` creates a `PDPageContentStream` and draws the encoded image without a fresh cancellation check. Cancellation during encoding may still trigger avoidable PDF content-stream work. This is a bounded P2 responsiveness concern, not a reproduced device failure.

## Acceptance
- Check cancellation immediately before `PDPageContentStream(outDoc, pdPage)` using the existing JVM-testable resource-preflight helper.
- Preserve normal dimensions, JPEG quality, page count and successful bytes, stream/bitmap/page/document/descriptor ownership, staging/no-overwrite, and original exceptions.
- Add focused deterministic JVM regressions for cancellation before stream open, ordering, failure and cleanup, and real coroutine cancellation.
- Require exact production/test SHA nine-gate draft-PR Actions success. No device/emulator/benchmark/race-freedom claims.
- Keep PR #1 draft/unmerged and never modify main.

## Next executable step
Implement smallest preflight before PDPageContentStream allocation, add focused tests, commit on authorized branch, then inspect exact-SHA authoritative CI.
