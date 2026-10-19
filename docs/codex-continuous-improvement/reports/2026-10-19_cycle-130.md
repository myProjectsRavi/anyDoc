# Cycle 130 — Cache-copy provider acquisition cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E130/F132/US-R130-P2-01A
**Activation baseline:** `8d661bcd0fd7fd1bb9fd3023fb797891c059164a`
**Last validated production/test SHA:** `8d661bcd0fd7fd1bb9fd3023fb797891c059164a`, draft-PR Actions #935 / API `39205271842`, job `117906646366`, SUCCESS all nine gates (Cycle 129).

## Source-backed risk
`Context.copyUriToCacheFile` registers its temporary file then enters `withRegisteredCacheCopy` and calls `contentResolver.openInputStream(uri)` without a fresh cancellation preflight. Cancellation between metadata preflight/temp registration and provider acquisition can still cause avoidable provider work. A cancellation check must execute INSIDE the existing registered-copy cleanup scope, not before it.

## Acceptance
- Already-cancelled jobs skip provider input acquisition and release their registered temporary file without touching other owners.
- Active copying preserves input bytes, free-space checks, cleanup and original exception identity.
- Focused deterministic core PDF JVM regressions and authoritative exact-code/test-SHA draft-PR all-nine-gate CI.

## Next executable step
Audit `PdfIoUtils.kt`, `CacheCopyOwnership.kt` and current cache-copy tests; add the smallest cancellation check immediately before provider stream acquisition inside `withRegisteredCacheCopy`. No source mutation while exact-head CI active. Keep PR #1 draft/unmerged, main untouched. Host JVM proof is not Android device/OEM/benchmark/race-freedom proof.
