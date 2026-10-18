# Cycle 104 — Preserve primary failures and clean up all PDF comparison bitmaps

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E104 / F106 / US-R104-P2-01A
**Previous cycle:** Cycle 103 COMPLETE; code/test `372629e02698968168f521850e674113821dd1ed` passed draft-PR Actions #835/API `39084723314`, job `117548786248`, all nine gates.

## Evidence and acceptance
Cycle 104 E104/F106/US-R104-P2-01A ACTIVE before code mutation. `PdfCompareTool.compare` per-page finally (lines 275-279) recycles left, right and diff bitmaps sequentially. A recycle exception can skip remaining cleanup and mask the primary page-processing failure. This is a source-level risk, not an observed Android recycle exception. Add a small JVM-testable all-bitmap cleanup helper preserving primary errors/cancellation, suppressing cleanup failures without self-suppression, attempting all non-null bitmaps, and retaining success behavior. Obtain exact-SHA draft-PR CI.

## Next action
Implement narrow helper and deterministic core PDF JVM tests; inspect exact-SHA CI; reconcile STATE/CURRENT_REPORT/BACKLOG/VALIDATION/report after terminal CI. PR #1 draft, main untouched; no device/emulator/OEM/benchmark claims.
