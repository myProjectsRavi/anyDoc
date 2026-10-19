# Cycle 141 — PDF page-count cancellation integrity

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E141/F143/US-R141-P1-01A
**Activation baseline:** `a91b6e4960c67eae68419eed1a3242c165fb75a7`
**Last validated production/test SHA:** `0f4b23fd33724a3ca23fb0faa1c5f38190462e92`, authoritative draft-PR Actions #974 / API `39320180315`, job `118292004034`, all nine configured gates SUCCESS (Cycle 140).

## Source-backed risk
At `a91b6e4960c67eae68419eed1a3242c165fb75a7`, `PdfCompressor.getPageCount` (core/pdf/src/main/java/com/docforge/core/pdf/PdfCompressor.kt:87-93) wraps the suspending cache-copy and PDF page-count operation in `runCatching { ... }.getOrDefault(0)`. Kotlin `runCatching` catches `Throwable`, including `CancellationException`, and can turn a cancelled page-count request into a misleading successful zero-page result. It can also mask fatal `Error` subclasses. This is a source-backed cancellation/correctness risk, not a reproduced Android device failure.

## Acceptance
- Preserve original `CancellationException` (including cancelled coroutine) rather than returning a successful zero-page result.
- Do not mask fatal `Error` subclasses or interruption signals; keep ordinary invalid/unsupported PDF page-count failures as zero, preserving current UI fallback.
- Add deterministic core PDF JVM tests covering success, recoverable exception, cancellation identity and fatal errors.
- Require authoritative exact-code/test-SHA draft-PR nine-gate CI. No Android device, emulator or benchmark claims. Keep PR #1 draft/unmerged; never modify main.

## Next executable step
Replace the broad Throwable-swallowing page-count fallback with a narrow JVM-testable helper that rethrows cancellation/interruption signals and fatal errors while returning zero for ordinary recoverable page-count exceptions. Add focused core PDF JVM regressions for original cancellation identity, cancelled coroutine, recoverable IOException fallback, valid count and fatal Error propagation. Require authoritative exact-code/test-SHA draft-PR nine-gate CI; do not change main or PR draft state.
