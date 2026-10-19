# Cycle 142 — Cancellation integrity for recoverable page-count failures

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E142/F144/US-R142-P1-01A
**Activation baseline:** `28149d1426f94a1ba4b4cc492f85d1742dd674b6`
**Last validated code/test SHA:** `28149d1426f94a1ba4b4cc492f85d1742dd674b6`, authoritative draft-PR Actions #978 / API `39328986272`, job `118322155998`, all nine configured gates SUCCESS (Cycle 141).

## Source-backed risk
At `28149d1426f94a1ba4b4cc492f85d1742dd674b6`, `PdfCompressor.pageCountOrZero` calls `ensureActive` before and after a successful probe, but its recoverable `Exception` and `SocketTimeoutException` handlers return `0` without checking the coroutine Job. If a provider cancels the Job and then throws a recoverable exception while unwinding/closing, the helper may convert cancellation into a successful zero-page result. This is a source-level P1 correctness risk, not a reproduced device crash.

## Acceptance
- Check `currentCoroutineContext().ensureActive()` before returning zero for ordinary recoverable errors and socket timeouts.
- Preserve existing fallback to zero for non-cancelled recoverable errors, exception identity for cancellation/interruption, and fatal Error propagation.
- Add deterministic core PDF JVM regressions for cancellation followed by IOException and SocketTimeoutException, plus unchanged non-cancelled fallback.
- Validate exact source/test SHA with authoritative draft-PR nine-gate CI; no device/emulator/benchmark claims.
- Keep PR #1 draft/unmerged and main untouched.

## Next executable step
Implement the smallest safe change to both recoverable fallback branches in `core/pdf/src/main/java/com/docforge/core/pdf/PdfCompressor.kt`; add focused JVM regressions in `PdfCompressorPageCountCancellationTest.kt`; publish only on `codex/anydoc-continuous-improvement` and inspect authoritative exact-SHA CI.

## Validation
No Cycle 142 production/test candidate exists at activation. Cycle 141 exact SHA `28149d1426f94a1ba4b4cc492f85d1742dd674b6` passed CI #978.
