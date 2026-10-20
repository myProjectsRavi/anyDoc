# Cycle 147 — Compression bitmap allocation cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E147/F149/US-R147-P2-01A
**Activation baseline:** `aa8addd54ce046e39ca6ea5787e8caf3fbb73500`
**Last validated code/test SHA:** `aa8addd54ce046e39ca6ea5787e8caf3fbb73500` (draft-PR CI #994, API 39367662646, job 118448084104, nine gates SUCCESS).

## Source-backed risk
In `PdfCompressor.compressWithJpegRasterization`, `boundedPdfRasterSize` computes dimensions and the next statement allocates `Bitmap.createBitmap` without a fresh cancellation preflight. Cancellation during page opening or size computation can lead to avoidable ARGB_8888 allocation before the next cancellation check. This is a bounded P2 responsiveness/memory concern, not a reproduced device crash.

## Acceptance
- Check coroutine cancellation immediately before compression bitmap allocation, without changing dimensions, JPEG quality, page rendering or output bytes.
- Preserve `bitmap.recycle()` in `finally`, page/document/descriptor ownership, original exceptions, staging and no-overwrite behavior.
- Add deterministic core PDF JVM regressions for cancellation before allocation, ordering, successful processing, allocation failure, render failure and resource cleanup.
- Require exact code/test SHA nine-gate draft-PR Actions success. Do not infer emulator, device, OEM, benchmark or race-freedom evidence.
- Keep PR #1 draft/unmerged; never modify main.

## Next executable step
Implement a small cancellation preflight before `Bitmap.createBitmap` using the existing JVM-testable preflight helper, add focused regressions, commit on the authorized branch, then inspect authoritative exact-SHA CI.
