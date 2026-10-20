# Cycle 150 — Compression page-render cancellation preflight

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E150/F152/US-R150-P2-01A
**Activation baseline:** `287ebeda470f8d793b00b98afbb88f56ccb1b6cb`
**Last validated production/test SHA:** `3c9705a65ada389cb6336411e1c9a8a6fe67ac49` (draft-PR Actions #1004, API 39386914554, job 118508930835, nine gates SUCCESS).

## Source-backed risk
After allocating an ARGB_8888 bitmap, `PdfCompressor.compressWithJpegRasterization` creates a canvas and fills it white, then calls `PdfRenderer.Page.render` without rechecking cancellation. Cancellation during bitmap/canvas setup can still initiate expensive rendering. This is a bounded P2 responsiveness risk, not a reproduced Android device failure.

## Acceptance
- Check coroutine cancellation immediately before `page.render` using the existing JVM-testable `openCompressionResourceWithPreflight` helper.
- Preserve output dimensions, JPEG quality, page count and successful output bytes, bitmap recycling and page/document/descriptor ownership, staging/no-overwrite and original exceptions.
- Add deterministic core PDF JVM tests for cancelled rendering, order, renderer errors and cleanup, multiple pages, and real coroutine cancellation.
- Require exact production/test SHA nine-gate draft-PR Actions success. No device/emulator/benchmark/latency/race-freedom claims.
- Keep PR #1 draft/unmerged; never modify main.

## Next executable step
Implement the smallest render preflight, add focused tests, publish a code/test candidate on the authorized branch, then inspect exact-SHA authoritative CI and reconcile canonical docs only after terminal CI.
