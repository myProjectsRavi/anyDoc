# Cycle 126 — Shared cache-copy cancellation preflight

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E126 / F128 / US-R126-P2-01A
**Activation baseline:** `df3f9423ee514064d34fb9fb06f96dc2f6bf8470`
**Last validated production/test SHA:** `3854f24b68fd5a78c1eff236cdcf21723aeeac65` (Cycle 125, draft-PR Actions #919 / API `39181219306`, job `117834218043`, nine gates SUCCESS).

## Source evidence
Cycle 126 E126/F128/US-R126-P2-01A ACTIVE / INCOMPLETE before any production/test mutation. Source at `df3f9423ee514064d34fb9fb06f96dc2f6bf8470`: `Context.copyUriToCacheFile` calls `preflightCacheCopy(uri)` before any coroutine cancellation check; preflight may query ContentResolver metadata and StatFs, and subsequent temp-file creation and URI stream acquisition occur before the first `copyChunkWithCancellation` check. PdfCompareTool's caller-specific preflight does not protect other users of this shared helper. Add a minimal shared entry cancellation preflight before metadata and temp-file side effects, add deterministic JVM tests through a testable preflight seam, preserve temp registry ownership and exceptions, then validate the exact source/test SHA through draft-PR all-nine-gate CI. Source-level P2 responsiveness issue, not a reproduced Android crash.

## Acceptance
- A cancelled coroutine must not query URI metadata, inspect cache storage, create a temp file, or open the source URI in shared cache-copy entry.
- Active cache copies retain storage preflight, temp-file registry, copy-chunk cancellation, cleanup and exception identity.
- Add focused deterministic core PDF JVM regressions for cancelled entry, normal invocation and original preflight failure.
- Obtain authoritative draft-PR exact-code/test-SHA all-nine-gate CI; do not claim emulator, device or performance proof.

## Next executable step
Add a testable entry preflight to `PdfIoUtils.kt` before `preflightCacheCopy(uri)`, add focused tests, review the diff, commit source/tests and validate exact SHA. After terminal CI reconcile all canonical documents. PR #1 draft/unmerged; main untouched.
