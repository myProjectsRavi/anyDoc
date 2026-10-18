# Cycle 124 — Shared staged-output setup cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E124 / F126 / US-R124-P2-01A
**Activation baseline:** `f853ab1672e0f9002ac2962c8d9648defecdb7a2`
**Last validated production/test SHA:** `f853ab1672e0f9002ac2962c8d9648defecdb7a2` (Cycle 123, authoritative draft-PR Actions #913 / API `39172488960`, job `117808338166`, all nine configured gates SUCCESS).

## Source evidence
At `f853ab1672e0f9002ac2962c8d9648defecdb7a2`, `PdfIoUtils.withStagedOutputFilesUsingIdentityReaders` calls `validateOutputName`, `requireNonSymlinkOutputDirectory`, and `directory.exists() || directory.mkdirs()` before the first `currentCoroutineContext().ensureActive()` inside the request loop. Other callers of the shared staging helper are not protected by the new Cycle 123 compare-specific preflight. A cancelled coroutine may still trigger avoidable directory validation or creation, including with empty requests. Source-level P2 responsiveness risk only.

## Acceptance
- Check coroutine cancellation at shared staging entry before output-directory filesystem validation/creation, including empty requests.
- Preserve successful output naming, symlink protection, staged ownership-aware cleanup, collision/no-overwrite and exception identity.
- Add focused deterministic core PDF JVM tests for cancelled no-directory-creation, normal staging, errors and repeated invocations, without weakening existing tests.
- Validate the exact production/test SHA with all nine draft-PR CI gates before canonical completion.
- Do not claim emulator, physical-device, OEM, benchmark, peak-heap, measured latency or race-freedom evidence.

## Next executable step
Audit existing staging-helper tests and implement the smallest safe shared-entry cancellation preflight on `codex/anydoc-continuous-improvement` only. Add targeted regressions, inspect exact-HEAD draft-PR CI and reconcile all canonical documents only after authoritative terminal success. Keep PR #1 draft/unmerged and main unchanged.
