# Cycle 128 — Final staged-file validation cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E128 / F130 / US-R128-P2-01A
**Activation baseline:** `8a3b4d5e76a408fca9106c22878f91ced68e3658`
**Last validated production/test SHA:** `347bf97709aecd9033b9b4383a6e830918fafdd3`, draft PR CI #928 SUCCESS (run 39195074528).

## Evidence
`PdfIoUtils.withStagedOutputFilesUsingIdentityReaders` checks cancellation once before `prepared.forEach` final staged-file validation, but not between item checks. Cancellation during an early item's validation may still permit filesystem reads for subsequent items.

## Acceptance
- Stop validation I/O promptly after cancellation.
- Preserve active-job errors, staged identity, cleanup, collision and rollback semantics.
- Add focused core PDF JVM tests; require exact-source/test-SHA draft-PR CI.

## Next executable step
Introduce per-item coroutine cancellation checks before and after final staged-file validation; add deterministic tests. Keep PR #1 draft/unmerged and main unchanged.
