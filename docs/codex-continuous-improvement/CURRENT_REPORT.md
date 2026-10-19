# Cycle 137 — Cancellation after cache-copy resource closure

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E137/F139/US-R137-P2-01A
**Activation baseline:** `acb2c57c0a7756fe3292a51915a9c8707dc0cefe`
**Last validated production/test SHA:** `acb2c57c0a7756fe3292a51915a9c8707dc0cefe`, authoritative draft-PR Actions #962 / API `39273664657`, job `118133050266`, all nine gates SUCCESS (Cycle 136).

## Source-backed risk
Source at `acb2c57c0a7756fe3292a51915a9c8707dc0cefe`: `Context.copyUriToCacheFile` completes the channel copy and closes its input/output resources, then returns `tempFile` from the registered-copy ownership callback without a final coroutine cancellation preflight. If cancellation arrives during final channel or provider-stream closure, the callback can report success and `withRegisteredCacheCopy(keepRegistered=false)` unregisters without deleting the partial/cancelled temp file. Add a small testable completion preflight INSIDE registered ownership immediately before returning `tempFile`; preserve original exceptions, file bytes, keepRegistered behavior and cleanup of cancelled copies. Add focused deterministic core PDF JVM tests, then require exact-code/test-SHA nine-gate draft-PR CI. This is a source-level P2 cancellation/temporary-file-lifecycle risk, not a reproduced device defect.

## Acceptance
- Cancellation after channel/provider-stream close is detected before reporting successful cache-copy completion.
- Cancellation stays inside registered temporary-file ownership so an incomplete copy is released/deleted without harming another owner.
- Normal copy bytes, original exception identity, retained leases and storage thresholds remain unchanged.
- Focused JVM regression tests and authoritative exact-code/test-SHA nine-gate draft-PR CI before closure. No emulator/device/performance claims.

## Next executable step
Inspect the current cache-copy completion path and ownership tests; implement a narrow cancellation completion helper and regression tests, then run exact-SHA draft-PR CI. Never modify main or undraft/merge PR #1.
