# Current Report

**ID:** `2026-10-08_cycle-049`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E049` — Saved batch preset integrity
**Feature:** `F051` — Visible preset read/corruption errors
**User Story:** `US-R049-P1-01A` — Surface unreadable saved batch presets

## Prior validated cycle
Cycle 048 exact code/test SHA `0f576dc1029af2f79f3a73ce726d162d716442a1` passed PR run #524 / API `37809062234` on 2026-10-08 UTC; all eight gates passed (core PDF, converter, scanner, PDF tools, app unit tests, debug APK, unsigned release/R8, lint). Prior candidate `76f3ad84001b580e6f19e9e9ff688a9dc27413f3` passed PR run #522. Push runs #521/#523 were cancelled and are not used as completion evidence.

## P1 source evidence
`BatchQueuePresetStore.readPresets()` catches DAO and JSON errors and returns `emptyList()`; `decodeTasks()` also silently converts malformed JSON to an empty list, causing saved presets to disappear from the UI without an error. `BatchQueueViewModel` reads and refreshes presets without error handling.

## Acceptance
- Valid saved presets continue to load, save and delete normally.
- DAO failures and malformed stored JSON are not misrepresented as an empty preset list.
- ViewModel surfaces read and refresh errors, preserving prior UI list where appropriate.
- Focused app JVM regression tests, exact code/test SHA CI, and terminal-CI documentation synchronization.

## Next exact action
Implement read/decode error handling and ViewModel propagation with focused tests, then validate authoritative CI. This checkpoint precedes Cycle 049 production/test mutations. PR #1 draft/unmerged; main untouched.
