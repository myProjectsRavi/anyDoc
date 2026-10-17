# Cycle 091 — Identity-aware cleanup and rollback fallback

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E091 / F093 / US-R091-P1-01A
**Previous cycle:** Cycle 090 COMPLETE. Exact code/test SHA `8d6d1fb2e7c3567d174675f8324226002aef2bcc`; authoritative draft-PR Actions #766 / API `38991137058`, job `117273629538`, SUCCESS all nine gates (core domain/PDF/converter/scanner/PDF-tools/app JVM, debug APK, unsigned release/R8, Android lint). Paired push #765 was cancelled, not acceptance evidence.

## Source evidence
`PdfIoUtils.deleteOwnedStagedEntry` returns false if expected NIO fileKey is null; `rollbackPublishedOutputIfUnchanged` does the same. `PublishedStagedOutput` stores only the NIO fileKey. Cycle 090 added Android device/inode fallback for continuity but not for cleanup or batch rollback. This can leave owned stages or partial published batches on providers with Android identity but no NIO key.

## Acceptance
- Prefer known NIO identity; otherwise use a known Android st_dev/st_ino identity through a narrow injectable reader. Unknown identity fails closed.
- Before deletion, require NOFOLLOW regular-file and matching original ownership; never recurse into symlinks, directories or foreign replacements. Preserve original writer error and cancellation semantics.
- Add deterministic core PDF JVM tests for owned stage cleanup, batch rollback, replaced/missing/unknown entries and cancellation on null-key simulated providers. Preserve normal publication and existing tests.
- Obtain authoritative exact-code/test-SHA draft-PR CI; only then reconcile all five documents and close if criteria pass. JVM tests inject/simulate Android identity; no emulator, physical-device, OEM/provider, benchmark, atomic check/delete or complete TOCTOU/race-freedom evidence. Unknown identities remain unproven.

## Next executable step
Add deterministic batch rollback and staging cancellation regression coverage (including simulated null-NIO identity where feasible), run exact-SHA authoritative draft-PR CI, then close Cycle 091 only after all acceptance criteria are met. Keep PR #1 draft/unmerged; `main` untouched.

## Code/test candidate and authoritative validation — 2026-10-17 UTC
- Exact production/test SHA `2552fc0137e584de5e134eecc9821f8d76544a67` passed authoritative draft-PR Actions #770 / API `39002954473`, job `117309527721`, on 2026-10-17 UTC. All nine configured gates SUCCESS: core domain, PDF, converter, scanner, PDF-tools and app JVM tests, debug APK, unsigned release/R8, and Android lint. Paired push #769 / API `39002951021` was cancelled by concurrency and is not validation evidence.
- The correction is committed on the authorized branch; NOFOLLOW identity-aware cleanup uses captured Android device/inode when original NIO fileKey is null. Six focused JVM tests were added; existing core PDF tests also passed.
- Remaining acceptance gap: direct injected JVM tests cover fallback ownership, substitution, links/directories, missing/unknown identity and NIO precedence, but no new end-to-end batch-rollback or staging-cancellation regression was added for the null-key scenario. Keep Cycle 091 ACTIVE until these paths are covered or explicitly scoped/deferred with evidence. Android OEM/provider compatibility, emulator/device behavior and atomic check/unlink remain unverified.
