# Current Report

**ID:** `2026-10-09_cycle-052`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E052` — Saved batch queue integrity
**Feature:** `F054` — Strict recoverable task validation
**User Story:** `US-R052-P1-01A` — Reject corrupt recoverable queue rows without destructive replacement

## Prior validated cycle
Cycle 051 `E051 / F053 / US-R051-P1-01A` COMPLETE: exact code/test SHA `3d65db557207eb900bca99a9a8ff52b250fb5071` passed authoritative PR CI #533 / API `37860102445` on 2026-10-08 UTC (SUCCESS; eight configured gates). The cancellation launch now reports service-start errors and preserves cancellation propagation; four focused app JVM regressions were committed. Push #532 was cancelled by concurrency, not completion evidence. No emulator, device, benchmark, or crash-free claim.

## Source evidence and scope
`BatchQueuePersistenceMapper.fromEntities()` maps persisted rows with `mapNotNull`; invalid active rows are silently dropped. Existing Cycle 050 recovery gating prevents snapshot writes only if the read fails. A strict mapper error is therefore required to prevent partial recovery and subsequent overwrite.

## Acceptance
- Corrupt QUEUED/RUNNING rows fail the entire recovery, never returning a partial queue.
- Valid QUEUED and RUNNING rows retain existing recovery semantics.
- Terminal SUCCESS/FAILED/CANCELED rows remain intentionally excluded even if their payloads are malformed.
- Focused app JVM tests and exact-SHA authoritative GitHub Actions pass.

## Next exact action
Cycle 052 `E052 / F054 / US-R052-P1-01A` ACTIVE / INCOMPLETE: `BatchQueuePersistenceMapper.fromEntities()` currently uses `mapNotNull(::fromEntity)` and silently discards malformed QUEUED/RUNNING persisted rows. That can yield a partial queue and subsequent destructive snapshot replacement. Next exact mutation: reject malformed recoverable rows with explicit exceptions, preserve intentional terminal-row filtering, add app JVM regression tests for invalid status/type/URI/count/ID/output and mixed valid-corrupt snapshots, then validate exact code/test SHA in authoritative GitHub Actions. PR #1 remains draft/unmerged; main untouched.
