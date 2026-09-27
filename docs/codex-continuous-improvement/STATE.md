# AnyDoc Continuous Improvement State

**Canonical state:** this file  
**Branch:** `codex/anydoc-continuous-improvement`  
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.  
**Current report:** `2026-09-27_cycle-002`  
**Report status:** ACTIVE / INCOMPLETE  
**Cycle:** 002  
**Current Epic:** `E002` — Background execution reliability  
**Current Feature:** `F004` — Fail-safe batch foreground-service lifecycle  
**Current User Story:** `US-R002-P1-01B` — Add durable persisted queue recovery after process death  
**Hourly run counter:** 7  
**Six-hour checkpoint counter:** 0  
**Report creation timestamp:** 2026-09-25T18:37:33Z baseline checkpoint  
**Last completed full audit:** not yet complete; initial Cycle 001 audit is active  
**State checkpoint timestamp:** 2026-09-26 UTC, run 006

## Git checkpoint

- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Last fully validated code HEAD: `dc4c92f165dd22ee556abbbaa686c1ccc38594ad` (run #92).
- Exact branch/code candidate HEAD before this documentation checkpoint: `657bbd6b62c21b22b222e48f91efae4b052a890f`.
- GitHub Actions PR run #105 (API run ID `36261620135`) is currently validating that exact candidate HEAD; no pass is claimed yet.
- Draft validation PR: #1, open, **do not merge while CURRENT_REPORT is incomplete**.
- Feature branch comparison before this checkpoint: ahead of main, behind by 0; merge-base remains the baseline main commit.
- Main was not modified.
- This STATE consistency write itself advances the branch by one commit. A file cannot embed its own resulting Git commit SHA without changing that SHA, so the next invocation MUST fetch the actual branch HEAD before writing.

## Completion

**Cycle 001: 8 / 8 mandatory items COMPLETE**

**Cycle 002: 1 / 2 scoped mandatory items COMPLETE**

Items are marked COMPLETE only when their required evidence is present. Compile-only success is not treated as lifecycle/device evidence.

### Mandatory states

1. **R001-P0-01 — Non-destructive output allocation:** COMPLETE / run #60 passed after recursive collision audit
2. **R001-P1-01 — Race-safe retryable PDFBox initialization:** COMPLETE / targeted unit tests passed in run #60
3. **R001-P1-02 — Active temp-file protection:** COMPLETE / registry unit coverage passed in run #60
4. **R001-P1-03 — Failure/cancellation-safe staged outputs:** COMPLETE / `US-R001-P1-03C` validated by run #92
5. **R001-P1-04 — Lifecycle-safe shared launch:** COMPLETE / run #107 passed on `da74754c3b3c8178e719f71ab8915948bc3adca0`
6. **R001-P1-05 — Release signing safety:** COMPLETE / unsigned release+R8 gate passed in run #60
7. **R001-P2-01 — Representative large-input preflight:** COMPLETE / run #114
8. **R001-P3-01 — Final CI/regression/diff/docs gate:** COMPLETE

## Current implementation task

**Primary:** make batch foreground execution fail-safe across process death before attempting durable recovery.

**Current subtask:** retain durable read access for SAF-selected batch inputs, then wire transactional Room queue snapshots and conservative process-death restoration.


## Cycle 002 run 001

- Cycle 001 closure was verified before starting new work.
- Selected exactly one bounded story: `US-R002-P1-01A`.
- Confirmed `BatchQueueForegroundService` returned `START_STICKY` while executable queue state lives only in process-local `BatchQueueRuntimeStore`.
- Changed active/running queue restart policy to `START_NOT_STICKY` until durable recovery exists.
- Added `BatchQueueForegroundServicePolicyTest` to lock the non-sticky restart contract.
- Exact code/test HEAD before durable documentation writes: `59cce5de17c6665734dbad00b02062afc187a53d`.
- GitHub Actions PR run #120 (API `36308903917`) was queued for that exact HEAD at latest observation; no CI pass is claimed yet.
- Draft PR #1 remains open/draft/unmerged. `main` remains untouched.
- Next action: inspect run #120, fix any failure, and only then mark `US-R002-P1-01A` complete. After that, begin `US-R002-P1-01B` for true durable queue recovery using persisted task state.



## Cycle 002 run 012

- Resumed `US-R002-P1-01B` after authoritative run #136 passed on branch head `14ccb5da0eb104d1c9ded61500d8fb8dbcf80c60`.
- Added transactional Room `BatchQueueTaskDao.replaceAll()` so durable queue snapshots cannot expose a delete/insert gap.
- Added `BatchQueuePersistenceStore` and deterministic mapping for recoverable queue state.
- Durable snapshots keep only `QUEUED` and `RUNNING` work; persisted `RUNNING` restores conservatively as `QUEUED` with a review-before-rerun message.
- Recovery mapping rejects malformed task type/status, non-content URIs, invalid input counts, nonpositive IDs, and blank output names instead of inventing runnable work.
- Added Robolectric/JUnit coverage for recoverable-only snapshots, queue ordering, `RUNNING -> QUEUED` recovery, and malformed-row rejection.
- Exact code/test HEAD before documentation writes: `cd2b45d683ce04efb0ecf84b58a11a5a38029568`.
- GitHub Actions run #140 (API `36358926601`) is queued on that exact HEAD; no pass is claimed yet.
- Next executable action: inspect #140 and fix any failure. If green, wire restore/snapshot persistence into `BatchQueueRuntimeStore`, `BatchQueueViewModel`, and the foreground service without auto-executing recovered tasks.
- Draft PR #1 remains open/draft/unmerged. `main` remains untouched.

## Cycle 002 run 009

- `US-R002-P1-01A` is technically COMPLETE: successor CI run #122 (API `36308968653`) passed required gates on documentation head containing code/test candidate `59cce5de17c6665734dbad00b02062afc187a53d`.
- Continued `US-R002-P1-01B`.
- Prior SAF contract change `52132492aab22ec3328b9fb701084e28bf7ff2e0` passed authoritative PR run #124 (API `36322209314`).
- Added explicit `takePersistableUriPermission(... FLAG_GRANT_READ_URI_PERMISSION)` before admitting OpenDocument/OpenMultipleDocuments selections to the batch queue.
- Exact code HEAD before this documentation write: `60a493934ecfa50a48a4f824ad906ca991774aed`.
- Run #127 (API `36325082770`) is pending and run #126 is still validating the immediately preceding commit; no pass is claimed for the new grant-retention code yet.
- Attempt to add dedicated ViewModel error surfacing was blocked by connector safety checks twice; the buildable fallback currently rejects the task on grant failure without adding it to the queue. A clearer user-visible failure message remains part of this same story.
- Next action: inspect #127/successor CI, fix any failure, then add transactional Room snapshot replacement and recovery mapping with persisted RUNNING -> QUEUED semantics.

## Work completed in run 006

- Selected exactly one story: `US-R001-P1-04A`.
- Added Robolectric/JUnit app test support without adding runtime/APK dependencies.
- Added `ShareLaunchViewModelTest` regression coverage for saved-state restoration until explicit consumption.
- Added regression coverage proving a new incoming share supersedes the old pending request and a stale destination callback cannot clear the newer request.
- Added `:app:testDebugUnitTest` as a mandatory GitHub Actions step.
- Reduced CI idle/churn: push and PR events for the same feature branch now share one concurrency lane, and documentation-only continuous-improvement checkpoint commits are ignored by the Android workflow.
- GitHub Actions run #105 (API run ID `36261620135`) is executing on exact candidate HEAD `657bbd6b62c21b22b222e48f91efae4b052a890f`. At the latest observation, runner setup succeeded and core PDF unit tests were executing; app lifecycle tests were still pending.
- No lifecycle CI pass, emulator result, or physical-device result is claimed yet.
- External automation repair: the prior hourly task had disabled itself after its last run; a fresh hourly AnyDoc runner was created with explicit no-idle watchdog semantics. Scheduler maximum frequency remains hourly.

## Work completed in run 005

- Selected exactly one story: `US-R001-P1-03C`.
- Added set-level transactional staging via `withStagedOutputFiles`: every output is written to same-directory staging files before any final file is published.
- Added rollback of finals created by the current transaction if publication fails after one or more moves.
- Preserved non-overwriting collision handling for every final filename.
- Migrated `PdfSplitter.extractPages`, `splitEveryNPages`, `splitByBookmarks`, and `PdfBatchStampTool.stampBatch` to set-level staged publication.
- Added JVM regression tests proving no first final is visible while later writers are still running and no partial final set remains after a later writer fails.
- GitHub Actions push run #92 (API run ID `36228935470`) passed on exact code HEAD `dc4c92f165dd22ee556abbbaa686c1ccc38594ad`: core PDF unit tests, debug APK assembly, unsigned release/R8 assembly, and Android lint all succeeded.
- Sandbox network check still fails with `Could not resolve host: github.com`; no local Gradle execution is claimed.
- No emulator or physical-device result is claimed.
- `US-R001-P1-03C` and mandatory item R001-P1-03 are COMPLETE.

## Work completed in run 004

- Resumed exactly at `US-R001-P1-03B` CI recovery checkpoint.
- Diagnosed runs #75/#76: Kotlin public-inline/private-helper visibility failure in `PdfIoUtils.kt`.
- Removed `inline`, then correctly made `withStagedOutputFile` suspend-capable to preserve suspend/cancellation-aware writers.
- Propagated suspend contracts through `PdfSplitter.saveStagedPdf` and audio conversion helpers; updated staged-output unit tests to run in coroutine context.
- GitHub Actions push run #85 (API run ID `36223470957`) passed core PDF unit tests, debug APK assembly, unsigned release/R8 assembly, and Android lint on code HEAD `37c2fee82af4406bf969be9ae6f5eab74a0c9e7c`.
- `US-R001-P1-03B` is COMPLETE. No emulator or physical-device result is claimed.
- Durable documentation commits advance HEAD beyond the validated code SHA; next run must fetch actual branch HEAD.

## Work completed in run 003

### Epic / Feature / User Story operating model
- Durable state now names the active Epic, Feature, and User Story so every hourly invocation resumes one bounded unit instead of reopening a large report.
- Current hierarchy: Epic `E001` -> Feature `F001` -> User Story `US-R001-P1-03B`.
- Next queued story is `US-R001-P1-03C`: atomic multi-output split/batch publishing so later failure does not leave a partial set of user-visible outputs.

### CI recovery
- Resolved recorded workflow run #60 to API run ID `36176743391`.
- Run #60 completed successfully: core PDF unit tests, debug APK assembly, unsigned release/R8 assembly, and Android lint all passed.
- Documentation head `0633bfc1c7a873bb8380641752fa189b182e5274` also passed run #66.
- Current audio-story head `4a707f61c957cca3c6b364cdb723c0e6fa00013f` started run #70; no pass is claimed yet.

### Transactional audio publishing
- Exposed the existing same-directory staged-output primitive to dependent feature modules.
- AudioFormatConverter now stages AAC/M4A, WAV, MP3/FLAC passthrough, and encoded MP3/FLAC outputs before publication.
- VideoAudioExtractor now stages M4A and MP3 extraction outputs before publication.
- Codec failure, cancellation, or writer failure deletes the staging file instead of leaving a partial final file.
- Existing collision-safe final naming remains in place; final publish still never intentionally replaces an existing output.

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

- R001-P0-01 — non-destructive output allocation
- R001-P1-01 — race-safe/retryable PDFBox initialization
- R001-P1-02 — active temp-file protection
- R001-P1-03 — failure/cancellation-safe staged outputs
- R001-P1-05 — release signing safety

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

- GitHub comparison confirms the continuous branch remains separate from main and behind by 0.
- Draft PR #1 remains open, draft, and unmerged.
- Push run #92 (API run ID `36228935470`) passed on exact code HEAD `dc4c92f165dd22ee556abbbaa686c1ccc38594ad`.
- Passed gates on run #92: core PDF unit tests, debug APK assembly, unsigned release/R8 assembly, and Android lint.
- No emulator or physical-device validation is claimed.

## Latest failed / unavailable validation

- Sandbox clone: failed with `Could not resolve host: github.com`.
- Combined status for the earlier feature-branch commit returned no statuses.
- Commit-workflow query returned no runs before the draft PR existed; that connector query is PR-oriented.

## GitHub Actions status

**LIFECYCLE STORY PASSED.**

Workflow run #105 (API run ID `36261620135`) is validating exact candidate HEAD `657bbd6b62c21b22b222e48f91efae4b052a890f`. The workflow now includes focused app lifecycle JVM tests in addition to core PDF tests, debug assembly, unsigned release/R8 assembly, and lint. Do not mark `US-R001-P1-04A` or R001-P1-04 complete until this run (or a justified successor on the same code) passes.

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
- release signing safety has CI compile/R8 evidence through run #92, but no signed production artifact is claimed.
- the scoped Cycle 001 long-operation output paths are transactionally staged; future audits may still discover additional non-mandatory output paths.

## Next exact action

1. Inspect GitHub Actions run #140 (API `36358926601`) for exact code/test HEAD `cd2b45d683ce04efb0ecf84b58a11a5a38029568`.
2. Diagnose and repair any failing compile/test/build/lint gate within `US-R002-P1-01B`.
3. When green, wire persisted recovery into runtime/ViewModel/service state transitions; restored work must remain user-triggered and must never auto-execute.
4. Add task-ID counter advancement and persistence-transition regression coverage.
5. Keep draft PR #1 unmerged and `main` untouched.
