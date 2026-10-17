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
Inspect `PdfIoUtils.kt` cleanup/rollback call paths and existing tests; thread original Android identity into ownership-safe deletion/rollback and add focused injected JVM regressions. PR #1 draft/unmerged; `main` untouched.
