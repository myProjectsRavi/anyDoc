# AnyDoc Continuous Improvement State

**Canonical state:** this file  
**Branch:** `codex/anydoc-continuous-improvement`  
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.  
**Current report:** `2026-09-25_1837_cycle-001`  
**Report status:** ACTIVE / INCOMPLETE  
**Cycle:** 001  
**Hourly run counter:** 2  
**Six-hour checkpoint counter:** 0  
**Report creation timestamp:** 2026-09-25T18:37:33Z baseline checkpoint  
**Last completed full audit:** not yet complete; initial Cycle 001 audit is active  
**State checkpoint timestamp:** 2026-09-25 UTC, run 002

## Git checkpoint

- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Latest code HEAD validated/observed before documentation-only checkpoint commits: `903c257b1370bb6666cfa94207ad2017cd0337f8`
- This STATE write itself advances the branch by one commit, so the next invocation MUST read actual branch HEAD rather than assuming the pre-checkpoint SHA above.
- Draft validation PR: #1, open, **do not merge while CURRENT_REPORT is incomplete**.
- Uncommitted work: none represented by the connected GitHub mutation flow; writes in this run were committed atomically per file.
- Feature branch comparison before durable-doc commits: ahead of main, behind by 0.
- Main was not modified.

## Completion

**0 / 8 mandatory Cycle 001 items COMPLETE**

Implemented work is not counted as COMPLETE while required CI remains unobserved.

### Mandatory states

1. **R001-P0-01 — Non-destructive output allocation:** CI_PENDING / recursive source collision audit complete
2. **R001-P1-01 — Race-safe retryable PDFBox initialization:** CI_PENDING
3. **R001-P1-02 — Active temp-file protection:** CI_PENDING
4. **R001-P1-03 — Failure/cancellation-safe staged outputs:** IN_PROGRESS / CI_PENDING
5. **R001-P1-04 — Lifecycle-safe shared launch:** CI_PENDING
6. **R001-P1-05 — Release signing safety:** CI_PENDING
7. **R001-P2-01 — Representative large-input preflight:** NOT_STARTED
8. **R001-P3-01 — Final CI/regression/diff/docs gate:** CI_PENDING

## Current implementation task

**Primary:** obtain CI evidence for the current changes, fix any compile/test/lint/R8 problem immediately, then continue R001-P1-03 staged output publishing for remaining high-risk long operations.

**Current subtask:** observe GitHub Actions run #60 for code HEAD `903c257b1370bb6666cfa94207ad2017cd0337f8`; if it passes, apply evidence to pending items; if it fails, inspect logs and fix root cause.

## Work completed in run 002

### Staged output safety
- Added reusable same-directory staged-output publishing in `PdfIoUtils.kt`.
- Final user-visible files are created only after the writer block succeeds.
- Staging files are deleted on failure/cancellation.
- Final publish never intentionally replaces an existing destination and retries with a non-conflicting suffix if a competing writer wins the name race.
- Migrated `PdfCompressor`, `PdfMerger`, and searchable-PDF generation in both PDF OCR and image OCR to staged publishing.
- Added JVM regression tests for successful staged publish with an existing destination and failure cleanup.

### Expanded P0 collision audit
- Used the current branch recursive Git tree to enumerate the actual source tree rather than relying on default-branch code search.
- Audited the complete active `core/pdf` source set and converter engine files for direct final-output construction.
- Found and fixed a missed direct overwrite in `BusinessCardParser` vCard export by switching to non-conflicting allocation.
- Verified saved-signature slot replacement is intentional application state.
- Verified vault encryption uses generated internal filenames rather than user-selected final output names.

### Business-card resource safety
- Bitmap recycling now occurs in `finally` around ML Kit recognition.
- ML Kit recognizer is always closed.
- Cancellation callback is handled explicitly.

### Validation during run 002
- Repeated sandbox network check still failed with `Could not resolve host: github.com`.
- GitHub Actions run #60 was observed in progress on code HEAD `903c257b1370bb6666cfa94207ad2017cd0337f8`.
- At the last observation, checkout, Java 17 setup, Gradle setup, and wrapper setup had succeeded; core PDF unit tests were still running.
- No CI pass is claimed yet.
- Branch comparison: feature branch remains ahead of `main` and behind by 0; `main` was not modified.

## Work completed in run 001

### P0 output safety
Converted confirmed destructive/direct-collision final outputs to `resolveNonConflictingFile` in:
- `feature/converter/VideoAudioExtractor.kt`
- `feature/converter/AudioFormatConverter.kt`
- `feature/converter/TextPdfConverter.kt`
- `core/pdf/PdfSplitter.kt`
- `core/pdf/PdfRedactionTool.kt`
- `core/pdf/PdfOcrTool.kt`
- `core/pdf/PdfBatchStampTool.kt`
- `core/pdf/PdfTextExtractor.kt`
- `core/pdf/PdfPageImageExporter.kt` (ZIP)
- `core/pdf/PdfIdCardTool.kt`
- `core/pdf/ScanImageExporter.kt` (ZIP)

Verified fetched safe allocator usage in:
- `PdfCreator`
- `PdfMerger`
- `PdfCompressor`
- `PdfSigner`
- `PdfAnnotator`
- `PdfPasswordTool`
- `ImageFormatConverter`
- `DocumentPdfConverter`
- `PdfCompareTool`
- `PdfPageCropTool`
- `PdfHeaderFooterTool`
- `PdfAComplianceTool`

### P1 initialization safety
- Replaced premature AtomicBoolean PDFBox guard with retryable serialized initializer.
- Added failure/retry and concurrent one-time execution tests.

### P1 temporary-file safety
- Added `core/domain/.../ActiveTempFileRegistry.kt`.
- PDF URI copy registers active cache files continuously through the caller block.
- Audio PCM files register while in use.
- `DocForgeApp.cleanStaleTempFiles` skips active files.
- Added registry lifecycle unit test.

### Share lifecycle safety
- Added `ShareLaunchViewModel` with `SavedStateHandle` persistence.
- `MainActivity` now owns pending shared-file launches through the Activity ViewModel.
- `DocForgeNavHost` consumes the request only after the target screen accepts prefilled URIs.
- Recreation no longer depends on replaying the original SEND intent.

### Release safety
- Removed the release build's explicit debug signing fallback.
- Production signing remains external; no signing secret was added.
- CI now includes unsigned `:app:assembleRelease` to compile/R8 the release variant.

### Test / CI infrastructure
- Added JUnit 4.13.2 to core PDF tests.
- Added `PdfCoreSafetyTest.kt`.
- Added `.github/workflows/anydoc-continuous-ci.yml`.
- Opened draft PR #1 solely for observable CI/review; no merge authorized.

### Durable state
Created:
- `BACKLOG.md`
- `CHANGELOG.md`
- `BENCHMARKS.md`
- `VALIDATION.md`
- `FEATURE_MATRIX.md`
- `TEST_MATRIX.md`
- `UX_AUDIT.md`
- `PERFORMANCE_BASELINE.md`
- `CURRENT_REPORT.md`
- `reports/2026-09-25_1837_cycle-001.md`
- this `STATE.md`

## Completed report items

None are marked COMPLETE yet because required remote validation is pending.

## Incomplete report items

See mandatory checklist above. Also continue:
- repository-wide final-output path audit;
- partial-output cleanup/atomic publish audit;
- background-service lifecycle/recovery;
- memory/disk guards;
- accessibility/internationalization;
- release/R8 safety;
- tests and benchmarks.

## Blocked report items

No product decision is currently blocked.

### Environment limitation
The current ChatGPT/Codex sandbox cannot resolve `github.com`, so it cannot clone the repository and run Gradle locally. Do not repeatedly burn an hourly run on the same failed clone unless the environment indicates connectivity changed.

## Latest successful validation

Source-level:
- GitHub comparison confirmed continuous branch is separate from main.
- Content-SHA guarded updates succeeded.
- Targeted output-allocation source audit completed for fetched primary PDF/converter classes.
- Draft PR #1 created without modifying main.
- Workflow run #28 became observable and reached the core PDF unit-test step; subsequent pushes superseded that checkpoint under branch concurrency, so it is not a final pass.

No Gradle/CI success is claimed yet.

## Latest failed / unavailable validation

- Sandbox clone: failed with `Could not resolve host: github.com`.
- Combined status for the earlier feature-branch commit returned no statuses.
- Commit-workflow query returned no runs before the draft PR existed; that connector query is PR-oriented.

## GitHub Actions status

**CI_PENDING.**

Draft PR workflow run #28 was observed in progress on an earlier checkpoint. The latest code/doc pushes require a fresh/synchronized run. Next invocation must query workflow runs associated with current PR/head, inspect jobs/logs for failures, fix root causes, and rerun/advance the branch. Never mark the first three mandatory fixes COMPLETE until relevant checks pass.

## Sandbox validation status

**BLOCKED BY NETWORK/DNS for repository clone.**

No physical-device validation.

## Active benchmarks

None yet.

## Benchmark baselines

Not yet measured. See `BENCHMARKS.md` and `PERFORMANCE_BASELINE.md`.

## Measured improvements

None claimed. Correctness/safety changes were implemented, but no performance percentage is available.

## Important changed files

Code:
- app `DocForgeApp.kt`, `MainActivity.kt`, `ShareLaunchViewModel.kt`, `DocForgeNavHost.kt`, and release Gradle config
- core domain `ActiveTempFileRegistry.kt`
- core PDF initializer/IO/output classes listed above
- converter audio/video/text classes
- core PDF test/build config
- version catalog
- branch CI workflow

Docs:
- all files under `docs/codex-continuous-improvement/`

## Known limitations

- Local cloning remains unavailable because the sandbox cannot resolve `github.com`; however, GitHub's recursive tree endpoint now provides complete current-branch path enumeration for source auditing.
- Source enumeration is no longer the blocker; executable local Gradle validation remains blocked by sandbox DNS/network.
- No current emulator or physical-device result.
- No performance baseline yet.
- OCR searchable layer still needs global Unicode strategy.
- shared-launch lifecycle fix is implemented but still requires recreation/new-intent regression evidence.
- release signing safety is implemented but still requires current-HEAD CI evidence.
- final output writes are not yet uniformly transactional/atomic under failure/cancellation.

## Next exact action

1. Read this file and `CURRENT_REPORT.md`.
2. Fetch actual current branch HEAD and draft PR #1 state.
3. Inspect GitHub Actions run #60 (or the latest run for code HEAD `903c257b1370bb6666cfa94207ad2017cd0337f8`).
4. If CI failed, inspect logs and fix the root cause before lower-priority work.
5. If CI passed, mark only the evidence-supported pending items COMPLETE; do not infer emulator/physical-device coverage.
6. Continue R001-P1-03 with remaining high-risk direct-final writers, prioritizing PDF split/batch and audio conversion/extraction paths.
7. Then begin R001-P2-01 representative input-size/free-space preflight.
8. Update durable docs before ending.
9. Do **not** create Cycle 002 while Cycle 001 is incomplete.
