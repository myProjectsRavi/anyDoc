# Cycle 146 — Compression document allocation cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E146/F148/US-R146-P2-01A
**Activation baseline:** `278427532d09d29bcadb5d9c4bc44ba8d7fb545d`
**Last validated code/test SHA:** `278427532d09d29bcadb5d9c4bc44ba8d7fb545d` (draft-PR CI #990, job 118405368286, all nine gates SUCCESS).

## Source-backed risk
At exact source/test SHA `278427532d09d29bcadb5d9c4bc44ba8d7fb545d`, `PdfCompressor.compressWithJpegRasterization` reads `renderer.pageCount` and validates a nonempty document, then constructs `PDDocument()` before the next cancellation check at the beginning of the page loop (`PdfCompressor.kt` lines 177-182). Cancellation during renderer page-count acquisition can therefore still allocate an output PDF document unnecessarily. Add a minimal JVM-testable cancellation preflight immediately before `PDDocument()` construction; preserve descriptor/renderer close ordering, empty-page error, original exception identity, page-count behavior, bitmap recycling, staging/no-overwrite, and output bytes. This is a source-backed P2 responsiveness concern, not a reproduced device failure.

## Acceptance
- Check cancellation immediately before constructing the output PDDocument after page-count validation.
- Preserve empty-page failure, nested descriptor/renderer ownership and cleanup, original exception identity, output bytes, and page-count behavior.
- Add deterministic core PDF JVM regressions for cancellation, allocation ordering, constructor failures, and successful cleanup.
- Require exact-code/test-SHA authoritative draft-PR CI across all nine gates.
- Keep PR #1 draft/unmerged, main unchanged; do not claim device, emulator, benchmark, or race-freedom evidence.

## Next executable step
Audit the latest compressor source and tests; implement the smallest JVM-testable pre-document allocation check on the authorized branch, validate exact SHA, and only then reconcile canonical docs.
