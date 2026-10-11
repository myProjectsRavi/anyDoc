# Current Report

**ID:** `2026-10-11_cycle-067`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E067` — Run-scoped persistence failure reporting
**Feature:** `F069` — Suppress superseded terminal-save failures
**User Story:** `US-R067-P1-01A` — Keep stale terminal persistence errors from replacing newer UI state

## Validated previous cycle
Cycle 066 E066/F068/US-R066-P1-01A COMPLETE. Exact code/test SHA `ce5d8b0192ee68c8922b256c5baf9d56cf3949f4`; authoritative PR GitHub Actions run #601 / API `38165332836` SUCCESS (2026-10-11 UTC), all eight configured gates: core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, Android lint. `failRunIfCurrent` atomically terminalizes only admitted RUNNING tasks as FAILED, increments processed/failure counters exactly once, and preserves QUEUED, terminal, later-enqueued and newer-run work. Five focused Robolectric/JVM regressions were committed. Paired push run `38165328986` was cancelled by concurrency, not completion evidence. No emulator, device, benchmark or crash-free evidence.

## Confirmed source risk
Cycle 067 E067/F069/US-R067-P1-01A ACTIVE / INCOMPLETE. Evidence: `BatchQueueForegroundService.persistTerminalCheckpoint` suspends while saving, then calls `BatchQueueRuntimeStore.reportPersistenceFailureIfIdle` on error; the latter checks only `!isProcessing`. An older delayed save failure can overwrite a newer completed batch's status or reintroduce a user-dismissed error after the app returns to idle. Next: bind persistence failure reporting to the exact terminal-state snapshot captured before the write, reject superseded snapshots, add deterministic JVM regressions for unchanged state, newer completion, dismissed error, same-message retry, and active processing, then validate exact code/test SHA with authoritative PR CI. PR #1 stays draft/unmerged; main untouched.

## Acceptance
- Report failed terminal persistence only when the exact pre-write terminal state is still current and idle.
- Preserve a newer batch's completion status, user-dismissed errors and newer run identity even if old/new error strings are identical.
- Keep cancellation propagation and the existing persistence success path unchanged.
- Add focused deterministic app JVM tests and validate the exact code/test SHA through authoritative PR CI.
- Keep PR #1 draft/unmerged; never modify `main`.

## Next exact action
Introduce snapshot-scoped terminal-save failure reporting in the runtime and foreground service; add regressions, commit on the feature branch and validate CI.
