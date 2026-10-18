# Cycle 106 — PDF compare output filename length safety

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E106 / F108 / US-R106-P2-01A
**Previous cycle:** Cycle 105 COMPLETE; exact production/test SHA `b836e2e0ec17f50db35f8b1e6143715c713269d1` passed authoritative draft-PR Actions #842/API `39090705908`, job `117566684347`, all nine gates.

## Evidence and acceptance
`PdfCompareTool.compare` sanitizes `outputName` using an ASCII replacement regex but does not bound its length. `withStagedOutputFile` builds `baseName.pdf` and may append numeric collision suffixes. Excessively long user-supplied names can exceed filesystem component limits and fail publication. Source-level risk only, not reproduced on an Android device. Audit shared filename policies and current tests; implement a conservative deterministic basename limit preserving ordinary names, collision handling, staging and cancellation. Add focused host JVM regressions for long input, fallback, normal input, and collision-suffix allowance. Obtain authoritative exact-SHA draft-PR CI.

## Next action
Inspect existing output-name helpers/tests and implement only the smallest compatible fix; run exact-code/test-SHA CI, then reconcile canonical docs. Keep PR #1 draft/unmerged; main untouched. No device/OEM/benchmark claims.
