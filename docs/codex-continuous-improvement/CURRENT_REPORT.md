# Cycle 099 — PDF compare descriptor lifecycle safety

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E099 / F101 / US-R099-P1-01A
**Previous cycle:** Cycle 098 COMPLETE. Exact code/test SHA `dcc142664e7d09061003c212102efca4090734b1` passed draft-PR Actions #817 / API `39060565666`, job `117477503838`, all nine gates SUCCESS. Earlier #815 failed Kotlin compilation and is not acceptance evidence. No emulator/device/benchmark evidence.

## Source evidence
`PdfCompareTool.compare` currently opens the left `ParcelFileDescriptor` and then opens the right descriptor before entering either `use` scope. If the second open fails, the left descriptor escapes automatic cleanup. This is a source-level descriptor-lifetime issue, not a demonstrated crash.

## Scope and acceptance
Place each descriptor acquisition inside the preceding descriptor's `use` scope. Prefer a small reusable Closeable pair helper only if it enables deterministic JVM coverage without changing Android behavior. Verify first-open failure, second-open failure, block failure, and success close both resources exactly once, including exception propagation. Preserve PDF renderer/page/cancellation/output semantics. Obtain exact-SHA draft-PR CI before completion; do not claim emulator/device proof.

## Next action
Inspect current test utilities and implement bounded nested-open cleanup with deterministic core PDF JVM tests, run exact-SHA CI, then synchronize canonical docs. PR #1 draft/unmerged; main untouched.
