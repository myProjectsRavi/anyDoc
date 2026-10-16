# Current Report

**ID:** `2026-10-16_cycle-085`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E085 / F087 / US-R085-P1-01A
**Previous cycle:** 084 COMPLETE; exact code/test SHA `0fe410393b950317fcaa390ada107fb4624f080e`, draft-PR CI #734 SUCCESS (nine gates).

## Source evidence
`stagedHardLinkCount` in `PdfIoUtils.kt` queries `Files.getAttribute(path, "unix:nlink", NOFOLLOW_LINKS)` and returns null when unsupported. `requireNoKnownStagedHardLinks` accepts null, so the check can miss aliases on Android filesystems/providers without NIO unix attributes. Android API 21+ provides `Os.lstat` with `StructStat.st_nlink`, potentially providing an additional count source. This does not solve path substitution races.

## Acceptance criteria
- Add Android `Os.lstat(...).st_nlink` fallback only when NIO count is unavailable; do not treat unknown counts as proof of safety.
- Preserve known hard-link rejection and normal staged publication.
- Add focused core PDF JVM tests covering fallback selection, known primary counts, and unavailable counts; do not claim Android runtime validation from JVM tests.
- Obtain authoritative draft-PR CI on exact combined code/test SHA; inspect and fix failures.
- Synchronize STATE, CURRENT_REPORT, BACKLOG, VALIDATION and this cycle report after terminal CI. Keep PR #1 draft/unmerged; never modify main.

## Active checkpoint
Activated before production mutation from branch HEAD `49bf794533caa8d39631c1b53f709e3fa7dcc1d0`. Next: implement the smallest safe fallback and focused tests. No Cycle 085 production/test commit or CI evidence claimed yet.
