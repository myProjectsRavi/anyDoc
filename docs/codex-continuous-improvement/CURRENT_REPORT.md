# Current Report

**ID:** `2026-10-09_cycle-055`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E055` — Recoverable batch queue URI integrity
**Feature:** `F057` — URI admission and preset replay validation
**User Story:** `US-R055-P1-01A` — Reject unrecoverable batch task URIs before enqueue

## Previous validated cycle
Cycle 054 E054/F056/US-R054-P1-01A COMPLETE. Exact code/test SHA `c81fc5e1a575eb695166c5d29f9e344d76fc25f3`, authoritative PR CI #550 / API `37924104373`: SUCCESS across core PDF, converter, scanner, PDF tools, app JVM tests, debug APK, unsigned release/R8, and Android lint. Shared write mutex now captures current task list only after acquiring the lock. Both ViewModel and foreground service use the new provider; six JVM tests cover contention, latest-state writes, cancellation, and exceptional cleanup. No device/emulator/benchmark validation is claimed.

## Confirmed source defect
`BatchQueueRuntimeStore.addTask()` accepts any `Uri` when the input count matches. Preset replay uses `mapNotNull` on raw URI strings and silently discards blanks while admitting file/http/relative URI strings. Recovery mapper rejects non-content URIs and blanks, so persisted queued work can become unrecoverable.

## Acceptance
- Admit only nonblank content URIs, consistent with `BatchQueuePersistenceMapper.fromEntity`.
- Do not silently discard malformed preset input entries or replace an existing queue on failure.
- Preserve ordinary valid content URIs, task ordering, existing filenames and batch behavior.
- Add deterministic JVM tests for malformed direct input, all-or-nothing preset validation, valid replay, and recovery compatibility.
- Exact-SHA authoritative PR CI; PR #1 draft/unmerged and no main mutation.

## Next exact action
Cycle 055 E055/F057/US-R055-P1-01A ACTIVE / INCOMPLETE. Source: `BatchQueueRuntimeStore.addTask()` only validates URI count; `replaceQueueWithPreset()` silently skips blank URI strings and accepts non-content schemes. Persisted recoverable queue mapper rejects non-content schemes and blanks, so an admitted queue may fail recovery after restart. Next: enforce content URI and nonblank per-item validation at admission for both direct tasks and preset loads, preserve previous queue and counters when preset validation fails, add deterministic app JVM tests for rejected/valid/mixed inputs, then validate exact code/test SHA via authoritative PR CI.
