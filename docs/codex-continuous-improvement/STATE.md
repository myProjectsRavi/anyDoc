# AnyDoc Continuous Improvement State

**Canonical state:** this file  
**Branch:** `codex/anydoc-continuous-improvement`  
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.  
**Current report:** `2026-09-25_1837_cycle-001`  
**Report status:** ACTIVE / INCOMPLETE  
**Cycle:** 001  
**Current Epic:** `E001` — User-data safety and reliability  
**Current Feature:** `F001` — Failure-safe, non-destructive output publishing  
**Current User Story:** `US-R001-P1-03C` — Atomic multi-output split/batch publishing  
**Hourly run counter:** 3  
**Six-hour checkpoint counter:** 0  
**Report creation timestamp:** 2026-09-25T18:37:33Z baseline checkpoint  
**Last completed full audit:** not yet complete; initial Cycle 001 audit is active  
**State checkpoint timestamp:** 2026-09-26 UTC, run 003

## Git checkpoint

- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Previously validated code HEAD: `903c257b1370bb6666cfa94207ad2017cd0337f8` via workflow run #60 (success).
- Previously validated documentation head: `0633bfc1c7a873bb8380641752fa189b182e5274` via workflow run #66 (success).
- Current audio-story code HEAD before this documentation checkpoint: `4a707f61c957cca3c6b364cdb723c0e6fa00013f`; workflow run #70 is in progress.
- This STATE write itself advances the branch, so the next invocation MUST read actual branch HEAD.
- Draft validation PR: #1, open, **do not merge while CURRENT_REPORT is incomplete**.
- Uncommitted work: none represented by the connected GitHub mutation flow; writes in this run were committed atomically per file.
- Feature branch comparison before durable-doc commits: ahead of main, behind by 0.
- Main was not modified.

## Completion

**4 / 8 mandatory Cycle 001 items COMPLETE**

Items are marked COMPLETE only when their required evidence is present. Compile-only success is not treated as lifecycle/device evidence.

### Mandatory states

1. **R001-P0-01 — Non-destructive output allocation:** COMPLETE / run #60 passed after recursive collision audit
2. **R001-P1-01 — Race-safe retryable PDFBox initialization:** COMPLETE / targeted unit tests passed in run #60
3. **R001-P1-02 — Active temp-file protection:** COMPLETE / registry unit coverage passed in run #60
4. **R001-P1-03 — Failure/cancellation-safe staged outputs:** IN_PROGRESS / `US-R001-P1-03B` COMPLETE via run #85; current story `US-R001-P1-03C`
5. **R001-P1-04 — Lifecycle-safe shared launch:** TEST_PENDING / build passes, recreation/new-intent regression evidence still required
6. **R001-P1-05 — Release signing safety:** COMPLETE / unsigned release+R8 gate passed in run #60
7. **R001-P2-01 — Representative large-input preflight:** NOT_STARTED
8. **R001-P3-01 — Final CI/regression/diff/docs gate:** CI_PENDING

## Current implementation task

**Primary:** finish Feature `F001` through short user stories. `US-R001-P1-03B` is complete; current story `US-R001-P1-03C` covers atomic multi-output split/batch publication.

**Current subtask:** inspect and implement `US-R001-P1-03C` so split/batch multi-output operations do not expose a partial result set if a later item fails or cancellation occurs.

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

**PARTIALLY VALIDATED / CURRENT STORY CI_PENDING.**

Workflow run #60 passed on code HEAD `903c257b1370bb6666cfa94207ad2017cd0337f8`, including core PDF unit tests, debug assembly, unsigned release/R8 assembly, and lint. Run #66 also passed on documentation head `0633bfc1c7a873bb8380641752fa189b182e5274`. The current audio-story head `4a707f61c957cca3c6b364cdb723c0e6fa00013f` is being validated by run #70 and must not be marked complete until terminal success is observed.

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

1. Fetch actual branch HEAD after durable-doc commits and confirm `main` remains untouched.
2. Work only on `US-R001-P1-03C`: inspect split/batch multi-output publication and ensure a later failure/cancellation cannot leave a partial user-visible result set.
3. Add focused regression coverage where feasible, then use GitHub Actions for authoritative validation.
4. Update STATE.md, CURRENT_REPORT.md, BACKLOG.md and VALIDATION.md before ending the next run.
5. Do **not** create Cycle 002 while Cycle 001 is incomplete.
