# Cycle 133 — Preserve writer failures during staged cleanup

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E133/F135/US-R133-P2-01A
**Activation baseline:** `826cc7c813ead8fd0674392a1aac65e9f5a22821`
**Last validated production/test SHA:** `826cc7c813ead8fd0674392a1aac65e9f5a22821`, draft-PR Actions #949 / API `39233506367`, job `117997004291`, all nine configured gates SUCCESS (Cycle 132).

## Source-backed risk
Cycle 133 E133/F135/US-R133-P2-01A ACTIVE / INCOMPLETE before production mutation. Source-backed P2: `PdfIoUtils.withStagedOutputFilesUsingIdentityReaders` catches writer/identity errors around lines 424-454, and its per-stage cleanup still uses `.exceptionOrNull()?.let(t::addSuppressed)` without an identity guard. If a request writer throws a Throwable and the injected `readAndroidIdentity` cleanup callback throws that same instance, Java self-suppression raises `IllegalArgumentException` and masks the writer failure. Reuse `addSuppressedUnlessSame` at this remaining cleanup site, add deterministic core PDF JVM regressions for same Throwable identity, distinct suppressed cleanup error, successful owned-temp cleanup and normal publication. Preserve fail-closed cleanup and output ownership. Require exact code/test SHA through draft-PR all-nine-gate CI; no device or performance claims.

## Acceptance
- Preserve the exact original writer exception when the staged cleanup identity reader throws that same Throwable.
- Suppress distinct cleanup exceptions without replacing the writer exception.
- Preserve owned-stage deletion, fail-closed identity behavior, successful publication, and temporary file ownership.
- Obtain authoritative exact-source/test-SHA draft-PR nine-gate CI; no device or performance claims.

## Next executable step
Update the remaining `t::addSuppressed` in the per-stage catch block to `addSuppressedUnlessSame(t, it)`; add focused JVM tests using injected identity readers; commit production/tests and validate. Keep PR #1 draft/unmerged and main untouched.
