# Current Report

**ID:** `2026-10-11_cycle-065`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E065` — Foreground service promotion integrity
**Feature:** `F067` — Run-scoped foreground promotion failure recovery
**User Story:** `US-R065-P1-01A` — Recover batch after foreground promotion failure

## Last validated story
Cycle 064 E064/F066/US-R064-P1-01A COMPLETE. Exact code/test SHA `0227722822dab7d4c6d9ed06c0d722b430a660cd` passed authoritative PR CI #593 / API `38142774494` on 2026-10-11 UTC. All eight gates SUCCESS: core PDF unit tests, converter unit tests, scanner unit tests, PDF tools unit tests, app JVM tests, debug APK, unsigned release/R8, Android lint. 13 focused cancellation handoff regressions cover pre-worker/pre-dispatch cancellation, atomic worker launch, run-scoped pending cancellation, later enqueues, duplicate/late callbacks, retry with same task IDs but unique run IDs, stale worker dispatch and terminal transitions, old persistence failures and counters. Push run #592 canceled by concurrency, not validation. No emulator/physical-device/benchmark/crash-free evidence claimed.

## Confirmed source risk
Cycle 065 E065/F067/US-R065-P1-01A ACTIVE / INCOMPLETE. Source evidence: `BatchQueueForegroundService.onStartCommand(ACTION_RUN_QUEUE)` calls Android `startForeground()` after the run was admitted; if foreground promotion throws (for example a platform start restriction or SecurityException), onStartCommand has no failure handler and the runtime can remain `isProcessing=true` without a worker. The ViewModel only handles errors from `startForegroundService()`, not later promotion errors inside the service. Next: add a small, testable foreground-promotion failure boundary; fail only the matching active run using its run ID, preserve retryable queued work, stop the failed service start safely, add focused JVM/Robolectric tests for successful promotion, platform/security exception, and stale-run isolation, then validate exact code/test SHA in authoritative GitHub Actions. Keep PR #1 draft/unmerged; do not touch main.

## Acceptance
- No ACTIVE batch stranded when Android rejects foreground promotion after RUN admission.
- A rejected or stale start fails only its matching run; later retry remains intact.
- Preserve queued tasks, run IDs, error visibility, service lifecycle, and existing safe behavior.
- Add deterministic JVM/Robolectric tests for success, foreground/security exceptions, and stale-run behavior.
- Authoritative PR CI validates exact code/test SHA. No claim of physical device validation.

## Next executable action
Introduce a small pure/testable foreground-promotion safety seam, use the run-scoped failure transition, add focused tests, and validate the exact feature-branch candidate. Do not mutate main or merge PR #1.
