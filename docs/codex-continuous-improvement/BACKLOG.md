# AnyDoc Continuous Improvement Backlog

Canonical branch: `codex/anydoc-continuous-improvement`

This backlog is ordered by user-data safety, correctness, reliability, performance, UX, then optional capability. Items may move only when evidence changes priority.

## Active Cycle 003

### Epic E003 — Large-input and resource safety

#### Feature F005 — Media temporary-space preflight
- [CURRENT / IN_PROGRESS] `US-R003-P2-01A` — Estimate decoded 16-bit PCM cache demand before audio transcoding; reject known-insufficient cache space before decode; add converter-module unit tests and CI coverage.

## Closed Cycle 002

### Epic E002 — Background execution reliability

#### Feature F004 — Fail-safe batch foreground-service lifecycle
- [COMPLETE] `US-R002-P1-01A` — Prevent unsafe sticky foreground-service restart while executable batch queue state is process-local. Validated by successor CI.
- [COMPLETE / CI #163] `US-R002-P1-01B` — Durable persisted batch-task recovery, conservative recovered-task admission, and service-owned RUNNING/terminal persistence. Exact code HEAD `57704c181dcf374bbb61aea580fe92022858c619`; run #163 / API `36360973174` passed all required gates.

## Closed Cycle 001

### Epic E001 — User-data safety and reliability

#### Feature F001 — Failure-safe, non-destructive output publishing
- [COMPLETE] `US-R001-P1-03A` — Stage single-output PDF compression, merge, and searchable-OCR publishing.
- [COMPLETE] `US-R001-P1-03B` — Stage audio conversion and video-audio extraction publishing. CI run #85 passed on code HEAD `37c2fee82af4406bf969be9ae6f5eab74a0c9e7c`.
- [COMPLETE] `US-R001-P1-03C` — Make multi-output PDF split/batch publication atomic as a set so a later failure/cancellation does not leave a partial visible result set. CI run #92 passed on code HEAD `dc4c92f165dd22ee556abbbaa686c1ccc38594ad`.

#### Feature F002 — Lifecycle-safe incoming share handling
- [COMPLETE / CI #107] `US-R001-P1-04A` — Recreation/new-intent regression tests passed in run #107 on `da74754c3b3c8178e719f71ab8915948bc3adca0`, covering saved-state restoration and stale-consume protection.

#### Feature F003 — Large-input safety
- [NEXT] `US-R001-P2-01A` — Add representative input-size/free-space preflight now that mandatory P1 stories are complete.


### P0 — data loss / destructive output
- [COMPLETE] Eliminate overwrite-on-name-collision across confirmed audio, PDF, OCR, scanner bundle, extraction, split, stamp, ID-card, text-to-PDF, and business-card vCard output paths.
- [COMPLETE] Complete repository-wide output-path audit for any remaining direct final-file construction that can overwrite existing user data. Recursive current-branch source-tree sweep completed; missed business-card `.vcf` collision fixed.
- [COMPLETE] Add explicit regression coverage for representative multi-output publication semantics, including no early publish and later-writer failure cleanup.

### P1 — crash / lifecycle / corruption
- [COMPLETE] Make PDFBox initialization race-safe and retryable.
- [COMPLETE] Prevent memory-pressure cleanup from deleting active temporary files.
- [COMPLETE] Audit cancellation/failure behavior for scoped long-operation final outputs; staged publishing covers compression, merge, searchable OCR, audio conversion/extraction, split/extract/bookmarks, and batch stamping.
- [COMPLETE] Preserve incoming share-launch state across Activity/configuration recreation and process restoration where feasible; regression gate passed in run #107.
- [NOT_STARTED] Audit foreground-service restart semantics and queue recovery after process death.
- [COMPLETE] Resolve release signing safety: production `release` does not silently use the debug signing key; unsigned release/R8 CI gate passes.

### P2 — memory / ANR / large input
- [NOT_STARTED] Add defensible input-size and free-space guards to large PDF/audio/video operations.
- [NOT_STARTED] Audit every bitmap/PDF/native allocation path for 4 GB RAM behavior.
- [NOT_STARTED] Bound or eliminate avoidable full-file copies for very large inputs where Android URI constraints allow.
- [NOT_STARTED] Add generated large-input regression fixtures and cancellation tests.
- [NOT_STARTED] Benchmark PDF raster compression peak bitmap memory and latency.

### P3 — UX / accessibility / internationalization / fidelity
- [NOT_STARTED] Move synchronous settings reads out of Compose navigation construction.
- [NOT_STARTED] Preserve shared-intent UX across recreation and explain invalid/unsupported shared files.
- [NOT_STARTED] Audit hard-coded UI strings and touch-target/content-description coverage.
- [NOT_STARTED] Replace Latin-only OCR/searchable-PDF text-layer assumptions for non-Latin scripts, with a size-conscious offline font strategy.
- [NOT_STARTED] Verify RTL, long-string, CJK, Indic, Arabic, emoji, and unusual filename behavior.
- [NOT_STARTED] Review rasterizing PDF compression UX because it intentionally removes selectable/vector text.

### P4 — architecture / maintainability
- [NOT_STARTED] Resolve hybrid manual-DI + Hilt object graphs after behavior is covered by tests.
- [NOT_STARTED] Split oversized navigation wiring by feature without changing behavior.
- [NOT_STARTED] Add static enforcement for safe output allocation and temp-file handling.
- [NOT_STARTED] Remove tracked/generated build artifacts from future commits and confirm repository hygiene.

### P5 — optional product capability
- [DEFERRED] Wire future PDF repair/flatten/metadata/grayscale routes only after current reliability gates pass.
- [DEFERRED] Wire future image resize/crop/rotate/effects routes only after current reliability gates pass.
- [DEFERRED] Large dependency additions such as office-suite conversion require APK/RAM/license evaluation first.

## Rules
- P0/P1 discovered during implementation preempt lower priorities.
- No item becomes COMPLETE solely because code exists.
- Required Cycle 001 work must pass relevant CI before the report can close.
- Do not create Cycle 002 while Cycle 001 remains incomplete.


## Cycle 001 closure — 2026-09-27

- `R001-P2-01`: COMPLETE; large-input/free-space preflight validated by CI run #114 on `d85d80ffece62b49c3870763335938f5cc0ee0ca`.
- `R001-P3-01`: COMPLETE; final CI/regression/diff/documentation gate reconciled.
- Mandatory Cycle 001 backlog: 8/8 COMPLETE. Do not reopen these stories without new contradictory evidence.


### Cycle 002 run 011
- [IN_PROGRESS] `US-R002-P1-01B`: malformed retained-access callback syntax repaired; CI run #135 pending. Durable Room snapshot/recovery work remains gated on green CI.


### Cycle 002 run 012
- [IN_PROGRESS] `US-R002-P1-01B`: atomic Room snapshot replacement plus durable recovery mapper/tests committed at `cd2b45d683ce04efb0ecf84b58a11a5a38029568`; CI run #140 pending. Next slice wires restore/persist state transitions and task-ID advancement.


### Cycle 002 run 013
- [IN_PROGRESS] `US-R002-P1-01B`: runtime/ViewModel restore + continuous snapshot wiring and recovery hardening committed at `cf2a249ffc77eb91efa0d5a0c0857fac1b38bb9e`; PR CI #153 in progress. Remaining mandatory slice: service-side RUNNING/terminal persistence and final regression/CI closure.

### Cycle 002 validation checkpoint — run 014
- [IN_PROGRESS / CI_RECOVERY] `US-R002-P1-01B` — runtime restore/persist wiring is implemented, but validation exposed two test-fixture defects. Fixes are at `6d7eb138de94355e54537babd08cd65cb5e7fc7c` and `367c3785a8bae18fd2324ae673464706f316e7da`.
- Full CI for `367c3785a8bae18fd2324ae673464706f316e7da` is pending. Do not mark complete until app tests, debug, release/R8, and lint pass.
- Next bounded slice after green: service-side durable RUNNING/terminal transition persistence plus terminal-state snapshot regression coverage.


### Cycle 002 closure — 2026-09-28
- `US-R002-P1-01A`: COMPLETE.
- `US-R002-P1-01B`: COMPLETE; exact code HEAD `57704c181dcf374bbb61aea580fe92022858c619` passed GitHub Actions run #163 / API `36360973174` (core PDF unit tests, app unit tests, debug APK, unsigned release/R8, Android lint).
- Cycle 002 scoped mandatory backlog: 2/2 COMPLETE. Draft PR #1 remains open/draft/unmerged; `main` remains untouched.
