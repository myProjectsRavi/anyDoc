# Current Report

**ID:** `2026-10-11_cycle-066`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E066` — Interrupted batch run terminal integrity
**Feature:** `F068` — Terminalize interrupted RUNNING tasks on run failure
**User Story:** `US-R066-P1-01A` — Prevent stranded RUNNING tasks after run failure

## Validated previous cycle
Cycle 065 E065/F067/US-R065-P1-01A COMPLETE. Exact validated code/test SHA `33d58689a282ea0c1c8832b95752c29a85df4221`; authoritative PR workflow #598 / API `38148001273` SUCCESS across core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8 and lint. Production catches foreground promotion exceptions, fail-closes only the matching admitted run, releases failed service start promptly, and persists terminal state in application scope after service destruction. Five focused JVM/Robolectric tests cover success, SecurityException, platform restrictions, stale retries, cancellation propagation. No emulator, physical device, performance benchmark or crash-free evidence claimed.

## Confirmed source risk
Cycle 066 E066/F068/US-R066-P1-01A ACTIVE / INCOMPLETE. Source: `BatchQueueRuntimeStore.failRunIfCurrent()` clears processing and run ID without terminalizing a matching `RUNNING` task; the orphan can no longer be claimed by `startNextQueuedTask()` or removed by `removeTask()`. Next: atomically transition only RUNNING tasks admitted to that exact run into FAILED with an interrupted-run diagnostic and consistent counters; leave QUEUED tasks available for explicit retry, preserve SUCCESS/FAILED/CANCELED tasks and later enqueues, prevent stale failed runs from changing newer runs. Add deterministic app JVM tests and validate exact SHA via authoritative PR CI.

## Acceptance
- Only `RUNNING` tasks admitted to the current failed run become `FAILED` exactly once.
- Counters and terminal error preserve coherent state; no automatic redo of possibly partially finished output.
- Queued and already-terminal tasks, later enqueues and newer run IDs remain intact.
- Add deterministic app JVM regression coverage; validate code/test SHA with PR CI.
- Keep PR #1 draft/unmerged and `main` untouched.

## Next exact action
Modify the CAS transition in `failRunIfCurrent` and add focused tests, then validate authoritative CI and synchronize all canonical docs after terminal result.
