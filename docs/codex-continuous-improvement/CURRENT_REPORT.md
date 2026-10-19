# Cycle 129 — Scoped cache-copy callback cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E129 / F131 / US-R129-P2-01A
**Activation baseline:** `5ed14615d01643f7144aa661d12a457cfa8d8000`
**Last validated production/test SHA:** `5ed14615d01643f7144aa661d12a457cfa8d8000`, draft-PR Actions #932 / API `39200352030`, job `117891856932`, SUCCESS all nine gates.

## Evidence
`Context.withUriCopiedToCacheFile` finishes a registered cache copy then enters `withScopedCacheCopy(tempFile, block)` without checking cancellation before invoking the callback. The scoped cleanup releases registry ownership even on exceptions; the cancellation preflight must execute INSIDE this scope, not before it.

## Acceptance
- Cancelled jobs do not invoke the callback after copy completion.
- Registered temporary-file ownership is released on cancellation, normal completion and errors, preserving other owners and exception identity.
- Existing normal output, URI copying, staging, collision/no-overwrite and symlink safety remain unchanged.
- Focused deterministic core PDF JVM tests and exact-code/test-SHA authoritative draft-PR nine-gate CI.

## Next executable step
Audit `CacheCopyOwnership.kt`, `PdfIoUtils.kt` and `CacheCopyOwnershipTest.kt`; add smallest safe scoped callback preflight and focused tests. Do not mutate source while docs-head CI is queued/in progress. Keep PR #1 draft/unmerged and main unchanged. Source-level risk only; no Android device or performance proof.
