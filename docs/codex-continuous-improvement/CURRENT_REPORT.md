# Cycle 089 — Staged-file identity continuity

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E089 / F091 / US-R089-P1-01A
**Previous cycle:** Cycle 088 COMPLETE, exact code/test SHA `c37dfae34f3d29f0d2cbbfa71769d7dc121473af`, authoritative draft-PR Actions #758 / API `38958983854` attempt 2 SUCCESS (nine gates).

## Evidence
The staging API captures the initial `stagedFileKey`, but its post-writer `requireRegularStagedOutput` does not compare that identity to the initial key. The later publication identity comparison starts after the writer, so a replaced regular file can pass when it has one hard link.

## Acceptance criteria
- Deterministic core PDF JVM test reproduces regular-file substitution after staging and before publication.
- Reject known original-key mismatches without deleting foreign replacements; normal writer and collision behavior preserved.
- Unknown file keys and non-atomic path races explicitly documented.
- Exact combined production/test SHA passes all configured draft-PR CI gates.
- Synchronize STATE, CURRENT_REPORT, BACKLOG, VALIDATION and cycle report after terminal CI; keep PR #1 draft and main untouched.

## Exact next step
Inspect `withStagedOutputFiles` and `requireRegularStagedOutput`, add a focused regression, then implement the smallest compatible check and run authoritative CI.
