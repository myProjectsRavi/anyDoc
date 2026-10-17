# Cycle 094 — Direct publication helper identity contract

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E094 / F096 / US-R094-P2-01A
**Previous cycle:** Cycle 093 COMPLETE; exact code/test SHA `e4960f63dbcc055215e4f60b6716f8e543aa6d4f` passed authoritative draft-PR Actions #790 / API `39033315120`, job `117398111312`, all nine gates SUCCESS.

## Source evidence
Internal `publishCheckedStagedOutput` has default null original NIO/Android identities; a null `stagedIdentityContinuity` result passes the post-move `!= false` guard. The production batch wrapper now captures/requires identity before writer execution, but direct internal helper calls in JVM tests omit original identity. This is a contract gap for future direct callers, not a proven currently reachable Android exploit.

## Acceptance and boundaries
- Enumerate actual direct helper call sites and their semantics; preserve the existing swap/hard-link/normal publication test intent.
- Reproduce null-identity helper behavior in focused JVM tests, then choose a minimal compatible API contract if evidence warrants it. Do not weaken identity checks or delete unverified/foreign entries.
- Validate exact code/test SHA through authoritative draft-PR CI, then reconcile STATE/CURRENT_REPORT/BACKLOG/VALIDATION and cycle report.
- No emulator, physical device, OEM compatibility or atomic race-freedom claim. PR #1 draft and main untouched.

## Next executable step
Review `PdfStagedHardLinkTest` and `PdfStagedPublicationSwapTest` direct calls, decide whether original identity must be explicit or safely captured at entry, and add a deterministic regression before production mutation.
