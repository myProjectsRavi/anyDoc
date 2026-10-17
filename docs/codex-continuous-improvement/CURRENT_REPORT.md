# Cycle 090 — Portable staged-file identity

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E090 / F092 / US-R090-P1-01A
**Previous cycle:** Cycle 089 COMPLETE, exact code/test SHA `9c6ac1a5d87b136449d596e5ce9ae8b31e287191`, authoritative draft-PR Actions API `38988957597`, job `117267167769` SUCCESS (nine gates).

## Evidence
`requireOriginalStagedIdentity` returns without comparing identity when the original NIO `fileKey` is null. Some providers may not expose that key. Cycle 089's known-key guard then cannot distinguish a replacement regular staging file from the original.

## Acceptance criteria
- Verify Android API 26+ `Os.lstat` `st_dev/st_ino` feasibility without assuming all providers expose stable identity.
- Add deterministic core PDF JVM tests for available/unavailable fallback identities, matching and mismatching entries; preserve normal publication and foreign-file safety.
- Implement the smallest safe fallback if evidence supports it, retain non-atomic race/unknown-provider caveats, validate exact code/test SHA through draft-PR CI.
- After terminal CI synchronize STATE, CURRENT_REPORT, BACKLOG, VALIDATION and Cycle 090 report. PR #1 draft/unmerged; main untouched.

## Exact next action
Inspect existing Android `Os.lstat` fallback, stage creation/identity checks and tests. Build a bounded injectable identity-source seam, then run core PDF tests and authoritative CI.
