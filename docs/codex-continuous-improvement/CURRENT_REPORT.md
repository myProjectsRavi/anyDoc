# Cycle 151 — Compression output-page allocation cancellation preflight

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E151/F153/US-R151-P2-01A
**Activation baseline:** `e9f14e592cb242ddae842600e840155fde872f6f`
**Last validated production/test SHA:** `e9f14e592cb242ddae842600e840155fde872f6f` (Actions #1009, API 39412349453, job 118592955874, nine gates SUCCESS).

## Source-backed risk
After `PdfRenderer.Page.render`, `PdfCompressor.compressWithJpegRasterization` constructs `PDPage(PDRectangle(...))` and adds it to `outDoc` without rechecking cancellation. A cancellation during rendering may still mutate the output PDF before the existing JPEG-encoding preflight. This is a bounded P2 responsiveness/side-effect concern, not a reproduced Android failure.

## Acceptance
- Check coroutine cancellation immediately before `PDPage` allocation and `outDoc.addPage` using existing `openCompressionResourceWithPreflight` helper.
- Preserve original page dimensions, page count, JPEG quality, output bytes on success, bitmap recycling, resource close order, original exceptions, staged publication and no-overwrite behavior.
- Add deterministic core PDF JVM regressions for cancellation after render, no page mutation, success ordering, allocation/addPage exceptions and cleanup, multiple pages and actual coroutine cancellation.
- Require exact code/test SHA nine-gate draft-PR CI. Do not claim Android device, emulator, benchmark, latency or race-freedom evidence.
- Keep PR #1 draft/unmerged; never modify main.

## Next executable step
Commit the smallest preflight around output page creation/addition plus focused JVM tests on this branch, inspect authoritative exact-SHA CI, repair failures and reconcile all canonical docs after terminal CI.
