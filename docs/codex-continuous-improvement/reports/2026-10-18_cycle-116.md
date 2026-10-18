# Cycle 116 — PDF renderer construction cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E116 / F118 / US-R116-P2-01A
**Activation baseline:** `8945dab4b8521e71b5e284641757ce9f8b9678c0`
**Previous cycle:** Cycle 115 COMPLETE; exact code/test SHA `6be844634382b60203f8c6813b39d95794517ad2` passed draft-PR Actions #882 / API `39131997093`, job `117688922042`.

## Source evidence
`PdfCompareTool.compare` constructs left and right `PdfRenderer` objects without checking cancellation directly before either constructor. Cancellation after input copies or between the two renderer constructions can still open an unnecessary renderer. This is a source-level allocation/resource-work risk, not an observed crash or measured latency regression.

## Acceptance and scope
- Check cancellation before constructing either PDF renderer; a cancellation between left and right opens must skip the right constructor.
- Preserve existing renderer and descriptor cleanup, normal comparison results and original exception propagation.
- Add focused pure JVM tests for cancelled no-open, success ordering, left/right sequencing, open errors and cleanup behavior.
- Validate exact production/test SHA through draft-PR CI (six JVM groups, debug APK, unsigned release/R8 and Android lint).
- No device/emulator/OEM/benchmark or measured resource-savings claims.

## Next executable step
Add `openCompareRendererWithPreflight` and use it before both `PdfRenderer` constructors. Add deterministic JVM tests, commit only on the authorized branch, inspect authoritative CI and reconcile after terminal result. Keep PR #1 draft/unmerged and `main` unchanged.
