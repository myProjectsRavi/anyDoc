# Current Report

**ID:** `2026-10-10_cycle-058`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E058` — Batch queue task mutation integrity
**Feature:** `F060` — Atomic task removal and output-name edits
**User Story:** `US-R058-P1-01A` — Prevent concurrent running-task removal or rename

## Previous validated cycle
Cycle 057 `E057 / F059 / US-R057-P1-01A` COMPLETE. Exact code/test SHA `d545ea396c99aba08a1e5d345969d29d6aa2c567` passed authoritative PR workflow #562 / API `38009446988` on 2026-10-10 UTC, job `114085804215` SUCCESS. All eight validation gates passed (core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, lint). Eight app JVM reorder regressions cover queued-order edges, terminal status, concurrent enqueue and completion. No emulator, physical device, benchmark, or crash-free evidence.

## Source evidence
`BatchQueueRuntimeStore.removeTask` checks task status and processing against an earlier snapshot, then filters from a later state in `_state.update`. A concurrent `startNextQueuedTask` can transition the task to RUNNING between those steps, so it can be removed while executing. `updateOutputBaseName` similarly checks QUEUED before an unconditional later `updateTask`, potentially editing a running task.

## Acceptance
- Revalidate existence/status on the same state that is atomically replaced, retrying if concurrent changes invalidate the snapshot.
- Preserve existing ability to remove queued and terminal tasks, but reject removal of RUNNING tasks.
- Allow output-name edits only for QUEUED tasks; do not rename RUNNING/terminal tasks even under contention.
- Preserve unrelated tasks and counters, return meaningful errors, add focused app JVM regressions, and validate exact implementation SHA through authoritative PR CI.
- Keep PR #1 draft/unmerged; never modify main.

## Next exact action
Implement CAS-guarded removal and output-name editing in `BatchQueueRuntimeStore.kt`; add app JVM regressions for invalid status, missing IDs, concurrency and no lost state. Publish only to `codex/anydoc-continuous-improvement`, inspect exact-SHA CI, and synchronize checkpoints after terminal validation.
