# Current Report

**ID:** `2026-10-16_cycle-087`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E087 / F089 / US-R087-P0-01A
**Previous cycle:** 086 COMPLETE; exact code/test SHA `58a62f6c1e277cab3e921c8e1c78c07cfd069e39`; authoritative draft-PR CI #743 / API `38915971783` SUCCESS, nine gates.

## Source evidence
`publishCheckedStagedOutput` catches post-move validation failures and invokes `deleteStagedEntry(candidate)`, which recursively traverses directories without following symbolic links. A staged entry substituted with a nonempty directory immediately before the move can therefore be renamed to the candidate and its unrelated contents recursively deleted. The previous host-only probe passed 5/5 assertions; this is not Android CI.

## Acceptance criteria
- Preserve unrelated substituted directories and their children, symlinks, and replacement regular files when post-move checks fail; never traverse foreign contents for cleanup.
- Preserve original publication failure, and attach cleanup errors as suppressed exceptions where relevant.
- Keep ordinary successful staged publishing and occupied-name collision behavior unchanged.
- Add deterministic core PDF JVM regressions; validate exact production/test SHA through authoritative draft-PR GitHub Actions, diagnose failures, and review residual exposed-path risk.
- Synchronize STATE, CURRENT_REPORT, BACKLOG, VALIDATION and cycle report after terminal CI. PR #1 must stay draft/unmerged; never modify main.

## Active checkpoint
Activated durably before Cycle 087 production/test mutation from branch SHA `58a62f6c1e277cab3e921c8e1c78c07cfd069e39`. Next: inspect `publishCheckedStagedOutput` cleanup ownership and existing `PdfStagedPublicationSwapTest`, implement a narrow identity-aware, non-recursive fail-closed mitigation and regression coverage, then authoritative exact-SHA CI. Do not claim cross-process atomicity, emulator or physical-device validation.
