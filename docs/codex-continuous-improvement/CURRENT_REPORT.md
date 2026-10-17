# Cycle 098 — PDF compare missing-page diff visibility

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E098 / F100 / US-R098-P2-01A
**Previous cycle:** Cycle 097 COMPLETE. Code/test SHA `c658610d54deeacbbffb55dbc542f3de0a8257e1` passed draft-PR CI #809 (API `39049802481`, job `117445934547`), nine configured gates SUCCESS. Documentation checkpoint `e75394355fefa29a29b8ee1a308a56d10350b2c4` passed #810. No device/emulator/benchmark evidence.

## Source evidence
`PdfCompareTool.diffBitmaps` returns a copy of the present page and 100f when its counterpart is missing, without the documented red highlight. This is source-level output correctness evidence, not an observed Android crash.

## Acceptance
Mark missing-page differences red while preserving dimensions and legibility where possible, 100% diff reporting, normal two-page behavior, cancellation and bitmap cleanup. Add deterministic core PDF JVM tests; validate exact combined SHA in draft-PR CI. Keep PR #1 draft, main untouched.

## Next action
Implement a bounded missing-page red overlay and testable helper, obtain exact-SHA CI, then reconcile canonical docs.
