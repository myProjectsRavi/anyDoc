# AnyDoc Continuous Improvement Backlog

Canonical branch: `codex/anydoc-continuous-improvement`

This backlog is ordered by user-data safety, correctness, reliability, performance, UX, then optional capability. Items may move only when evidence changes priority.

## Active Cycle 001

### Epic E001 — User-data safety and reliability

#### Feature F001 — Failure-safe, non-destructive output publishing
- [COMPLETE] `US-R001-P1-03A` — Stage single-output PDF compression, merge, and searchable-OCR publishing.
- [COMPLETE] `US-R001-P1-03B` — Stage audio conversion and video-audio extraction publishing. CI run #85 passed on code HEAD `37c2fee82af4406bf969be9ae6f5eab74a0c9e7c`.
- [COMPLETE] `US-R001-P1-03C` — Make multi-output PDF split/batch publication atomic as a set so a later failure/cancellation does not leave a partial visible result set. CI run #92 passed on code HEAD `dc4c92f165dd22ee556abbbaa686c1ccc38594ad`.

#### Feature F002 — Lifecycle-safe incoming share handling
- [CURRENT] `US-R001-P1-04A` — Add recreation/new-intent regression evidence for saved-state-backed share launch before marking R001-P1-04 complete.

#### Feature F003 — Large-input safety
- [QUEUED] `US-R001-P2-01A` — Add representative input-size/free-space preflight after mandatory P1 stories.


### P0 — data loss / destructive output
- [CI_PENDING] Eliminate overwrite-on-name-collision across confirmed audio, PDF, OCR, scanner bundle, extraction, split, stamp, ID-card, text-to-PDF, and business-card vCard output paths.
- [CI_PENDING] Complete repository-wide output-path audit for any remaining direct final-file construction that can overwrite existing user data. Recursive current-branch source-tree sweep completed; missed business-card `.vcf` collision fixed.
- [NOT_STARTED] Add explicit regression coverage for representative multi-output tools, not only the shared resolver.

### P1 — crash / lifecycle / corruption
- [CI_PENDING] Make PDFBox initialization race-safe and retryable.
- [CI_PENDING] Prevent memory-pressure cleanup from deleting active temporary files.
- [COMPLETE] Audit cancellation/failure behavior for scoped long-operation final outputs; staged publishing covers compression, merge, searchable OCR, audio conversion/extraction, split/extract/bookmarks, and batch stamping.
- [NOT_STARTED] Preserve incoming share-launch state across Activity/configuration recreation and process restoration where feasible.
- [NOT_STARTED] Audit foreground-service restart semantics and queue recovery after process death.
- [NOT_STARTED] Resolve release signing safety: production `release` must not silently use the debug signing key.

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
