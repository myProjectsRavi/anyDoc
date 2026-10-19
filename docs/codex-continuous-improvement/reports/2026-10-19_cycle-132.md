# Cycle 132 — Preserve primary errors during staged rollback

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E132/F134/US-R132-P2-01A
**Activation baseline:** `ea5e30e3fe4b99daf6ba043431e3db7dfb38cc0e`
**Last validated production/test SHA:** `ea5e30e3fe4b99daf6ba043431e3db7dfb38cc0e`, draft-PR Actions #946 / API `39231141696`, job `117989118935`, SUCCESS all nine gates (Cycle 131).

## Source-backed risk
`publishCheckedStagedOutput` invokes rollback in a `runCatching` catch handler, then attaches its exception using `failure.addSuppressed`. When an injected identity reader throws the exact same Throwable during publication validation and rollback, self-suppression throws `IllegalArgumentException` and masks the primary failure. Source-level P2 edge case, not an observed Android crash.

## Acceptance
- Reproduce same-Throwable failure via injected identity reader in a focused JVM test; original exception identity survives.
- Preserve normal rollback, staged-file ownership, existing identity guards, exception suppression for distinct failures, and successful publication.
- Obtain authoritative exact-source/test-SHA draft-PR nine-gate CI; no Android device or performance claims.

## Next executable step
Inspect `publishCheckedStagedOutput` and related JVM tests; add a narrow guard before `addSuppressed`, with focused regressions for same and distinct Throwable objects. Do not modify main or merge PR #1.
