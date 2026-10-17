# Cycle 101 — PDF compare cleanup error propagation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E101 / F103 / US-R101-P2-01A
**Previous cycle:** Cycle 100 COMPLETE at code/test SHA `42a9e068a6f6ad62679c4e85bcb91c6dc46767dc`, authoritative draft-PR Actions #825 / API `39066975014`, job `117496569062`, all nine configured gates SUCCESS. PR #1 remains draft/unmerged; main untouched.

## Source evidence
In `core/pdf/src/main/java/com/docforge/core/pdf/PdfCompareTool.kt`, a PdfDocument page is started and `drawBitmap`/`finishPage` execute before an unconditional `pdfDoc.close()` in `finally`. A drawing/finish exception followed by a cleanup exception could replace the original failure in Kotlin finally semantics. Verify whether Android PdfDocument.close can throw with an unfinished page. No device failure or test proof is claimed at activation.

## Scope and acceptance
Audit Android PdfDocument lifecycle behavior and existing tests, preserve original exceptions with suppressed cleanup errors where compatible, and retain cancellation, staged output, bitmap recycling, collision and memory-budget behavior. Add focused host JVM lifecycle/error tests before production mutation; validate exact code/test SHA in draft-PR Actions all nine gates. Do not claim Android visual/device/OEM/benchmark or full race-freedom proof.

## Next executable step
Read PdfCompareTool page lifecycle and Android PdfDocument API contract; design minimal close-on-failure regression seam and review compatibility. Do not change source before validating the issue and checking branch/CI concurrency.
