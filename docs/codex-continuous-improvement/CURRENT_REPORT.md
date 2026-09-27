# Current Report

**ID:** `2026-09-27_cycle-002`  
**Report:** `reports/2026-09-27_cycle-002.md`  
**Status:** ACTIVE / INCOMPLETE  
**Branch:** `codex/anydoc-continuous-improvement`  
**Baseline main:** `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`  
**Epic:** `E002` — Background execution reliability  
**Feature:** `F004` — Fail-safe batch foreground-service lifecycle  
**Current User Story:** `US-R002-P1-01B` — Add durable persisted queue recovery after process death

## Cycle 001 prerequisite

Cycle 001 is closed at 8/8 mandatory items. Exact feature candidate `d85d80ffece62b49c3870763335938f5cc0ee0ca` passed run #114; documentation head `0e0051367e696f578f4e6afed962b8e135c339c2` passed run #115.

## Cycle 002 mandatory stories

| User Story | Priority | Scope | State |
|---|---|---|---|
| US-R002-P1-01A | P1 | Prevent Android from sticky-restarting an in-memory-only batch execution | COMPLETE |
| US-R002-P1-01B | P1 | Add durable persisted queue recovery after process death | IN_PROGRESS |

## Current implementation

- `BatchQueueForegroundService` previously returned `START_STICKY` while `BatchQueueRuntimeStore` is process-local.
- Active/running queue paths now use a single non-sticky restart policy until durable recovery is implemented.
- Added `BatchQueueForegroundServicePolicyTest` to prevent accidental regression back to sticky restart behavior.
- Exact code/test HEAD before documentation writes: `59cce5de17c6665734dbad00b02062afc187a53d`.
- GitHub Actions run #120 (API `36308903917`) is queued on that exact HEAD; no pass is claimed yet.
- Draft PR #1 remains open, draft, and unmerged.
- `main` remains untouched.

## Completion gate for US-R002-P1-01A

The story becomes COMPLETE only after the exact code/test candidate passes required app unit tests, core PDF unit tests, debug build, unsigned release/R8 build, and lint in the branch CI workflow.

## Next exact action

Inspect run #120. Fix failures within the same story. If green, close `US-R002-P1-01A`, synchronize durable docs, then begin `US-R002-P1-01B`.


## Run 009 checkpoint

Run #122 validates 01A. SAF picker commit `52132492aab22ec3328b9fb701084e28bf7ff2e0` passed run #124. Durable read-grant retention is now at code HEAD `60a493934ecfa50a48a4f824ad906ca991774aed`; CI is pending. Next: transactional Room snapshot/recovery after CI.


## Run 012 checkpoint

- `US-R002-P1-01B` remains IN_PROGRESS.
- Transactional Room queue snapshot replacement added.
- Durable recovery mapper/store added with conservative `RUNNING -> QUEUED` semantics and malformed-row rejection.
- Regression tests added for recoverable filtering, ordering, running-task recovery, and malformed persisted rows.
- Exact code/test HEAD: `cd2b45d683ce04efb0ecf84b58a11a5a38029568`.
- Run #140 / API `36358926601` is queued; no pass is claimed yet.
- Next: CI repair if needed, then wire restoration/persistence into runtime/ViewModel/service without automatic execution.


## Run 013 checkpoint

- `US-R002-P1-01B` remains IN_PROGRESS.
- Runtime restoration now occurs only into an empty/non-processing queue and advances task IDs above restored work.
- ViewModel startup restores durable recoverable tasks before continuously snapshotting runtime queue state.
- Both SAF picker paths surface retained-access failures consistently.
- Recovery validation now rejects unsafe names/pathological IDs and deduplicates URIs before input-count validation.
- Exact code/test HEAD: `cf2a249ffc77eb91efa0d5a0c0857fac1b38bb9e`.
- PR CI #153 / API `36359225826` is in progress; no pass is claimed yet.
- Next bounded slice after green CI: persist foreground-service RUNNING and terminal transitions, then validate no automatic rerun after process recovery.
