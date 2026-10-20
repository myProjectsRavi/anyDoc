# Cycle 145 — Compression resource acquisition cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E145/F147/US-R145-P2-01A
**Activation baseline:** `1055f5def96a9ccb05d6408817f70defa550dd79`
**Last validated code/test SHA:** `1055f5def96a9ccb05d6408817f70defa550dd79` (draft-PR CI #987, job 118395389633, nine gates SUCCESS).

## Source-backed risk
Cycle 145 E145/F147/US-R145-P2-01A ACTIVE / INCOMPLETE before production mutation. Source at 1055f5def96a9ccb05d6408817f70defa550dd79: PdfCompressor.compressWithJpegRasterization opens ParcelFileDescriptor and constructs PdfRenderer before its first cancellation check in the page loop. Cancellation during cache-copy completion can cause avoidable descriptor/renderer acquisition. Add JVM-testable cancellation preflight before both opens; preserve nested resource ownership, error identity, page-count handling and output bytes. Add focused JVM regressions, require exact-SHA nine-gate draft-PR CI. P2 source-backed responsiveness concern, not a reproduced device crash.

## Acceptance
- Check cancellation before opening ParcelFileDescriptor and before constructing PdfRenderer.
- Preserve nested use cleanup, original exception identity, output and page-count behavior.
- Add deterministic core PDF JVM tests for cancellation, resource ordering and cleanup.
- Require exact-code/test-SHA draft-PR CI across all nine gates.
- Keep PR #1 draft/unmerged, main unchanged. Do not claim device or benchmark evidence.

## Next executable step
Implement minimal resource-open preflight and focused tests on authorized branch; inspect exact-SHA CI, then reconcile canonical docs only after terminal validation.
