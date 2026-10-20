# Cycle 152 — Compression progress-callback cancellation preflight

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E152/F154/US-R152-P2-01A
**Activation baseline:** `7a1beab654c06e34c4acdf7dfe900805a0079ffa`
**Last validated production/test SHA:** `7a1beab654c06e34c4acdf7dfe900805a0079ffa` (draft-PR Actions #1014 / API 39448544076, job 118716885293, nine gates SUCCESS).

## Source-backed risk
After PDF page rendering, JPEG encoding and output-page content drawing, `PdfCompressor.compressWithJpegRasterization` invokes `onProgress?.invoke(i + 1, renderer.pageCount)` without rechecking coroutine cancellation. A cancellation during drawing/encoding may still trigger stale progress notification before the next loop or save check. This is a bounded P2 callback-responsiveness issue, not a reproduced device crash.

## Acceptance
- Check cancellation immediately before invoking the optional progress callback, preserving successful callback count and order.
- Preserve bitmap recycling, page/document/descriptor cleanup, original exceptions, output dimensions, JPEG quality, staged publication, and successful output bytes.
- Add deterministic core PDF JVM regressions for cancelled callback skip, normal ordering, multiple pages, exception propagation, and actual coroutine cancellation.
- Validate exact code/test SHA through all nine configured draft-PR CI gates; no device/emulator/benchmark/race-freedom claim.
- Keep PR #1 draft/unmerged and main untouched.

## Next executable step
Inspect callback and cancellation helper placement, implement the smallest safe preflight, add focused tests, publish candidate on this branch and inspect exact-SHA authoritative CI. After terminal validation reconcile canonical documentation.
