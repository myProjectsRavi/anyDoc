# Cycle 119 — PDF compare output-document construction cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E119 / F121 / US-R119-P2-01A
**Activation baseline:** `6b8810da42bd65e1bad5c426e574fc6aa3053cb6`
**Previous cycle:** Cycle 118 COMPLETE; exact code/test SHA `6b8810da42bd65e1bad5c426e574fc6aa3053cb6` passed draft-PR Actions API `39143193841`, job `117721649795`, all nine configured gates.

## Source evidence
`PdfCompareTool.compare` creates an Android `PdfDocument()` within `withStagedOutputFile` without a cancellation preflight immediately before construction. The page loop checks cancellation, but cancellation after staging may still create a document unnecessarily. This is a bounded source-level P2 responsiveness risk, not an observed Android crash.

## Acceptance
- Check cancellation immediately before creating the output `PdfDocument`.
- Preserve existing page lifecycle, bitmap cleanup, `withClosePreservingPrimaryFailure`, staged publication and exception identity.
- Add deterministic pure JVM tests for cancellation before construction, successful construction, thrown construction and cancellation between staging and document construction.
- Require exact production/test SHA draft-PR CI: six JVM groups, debug APK, unsigned release/R8 and Android lint.
- No Android device, emulator, OEM, performance or measured cancellation-latency claims.

## Next executable step
Introduce a small `createComparePdfDocumentWithPreflight` helper, wrap the existing `android.graphics.pdf.PdfDocument()` construction, add JVM regressions and commit only on the authorized branch. Do not publish production/test changes before this active checkpoint is committed. Keep PR #1 draft/unmerged; never modify main.
