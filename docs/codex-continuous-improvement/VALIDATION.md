# Validation

## Cycle 001 current evidence

### Repository / branch safety
- Repository: `myProjectsRavi/anyDoc`
- Default branch: `main`
- Continuous branch: `codex/anydoc-continuous-improvement`
- Branch created from `main` at `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`.
- After the first implementation sweep, GitHub comparison reported the feature branch ahead and not behind; `main` was not modified by this automation.

### Source-level validation performed
- Re-read every file before mutation.
- Used content-SHA guarded GitHub updates; stale-content writes would fail instead of overwriting unseen changes.
- Re-ran targeted output-allocation audits across the complete current-branch `core/pdf` source set, all converter engine files, and other file-writing source paths discovered from a recursive Git tree.
- Verified newer fetched tools `PdfCompareTool`, `PdfPageCropTool`, `PdfHeaderFooterTool`, and `PdfAComplianceTool` already use `resolveNonConflictingFile`.
- Verified several future-route tool class paths mentioned in prior architecture notes do not currently exist at the expected locations; no implementation was invented.
- Recursive tree audit found and fixed an additional business-card `.vcf` same-name overwrite path outside the PDF/converter modules.
- Verified encrypted-vault filenames are generated independently of user-selected output names; saved-signature slot replacement is intentional application state rather than an output-collision path.
- Added staged-output regression tests covering successful publish with an existing destination and failure cleanup without exposing a final file.
- Added set-level staged-output regression tests proving later-writer failure leaves no partial final set and finals remain unpublished until every writer has succeeded.

### Lifecycle regression tests added

`app/src/test/java/com/docforge/app/share/ShareLaunchViewModelTest.kt`
- pending shared request restores from saved state into a new ViewModel until the destination consumes it;
- consuming the restored request clears the durable state so a later recreation does not replay it;
- a newer incoming request supersedes the old pending request;
- a late/stale consume callback for the old request cannot clear the newer request.

The app module now uses JUnit + Robolectric only in `testImplementation`, and CI runs `:app:testDebugUnitTest`.

### Tests added
`core/pdf/src/test/java/com/docforge/core/pdf/PdfCoreSafetyTest.kt`
- retry after failed one-time initialization;
- one initialization across concurrent callers;
- active temp-file registry register/unregister lifecycle;
- non-conflicting naming preserves existing output;
- single-output staged publish preserves an existing destination;
- single-output writer failure removes the partial staging file;
- multi-output publish exposes no final until every writer succeeds;
- later multi-output writer failure leaves no partial final result set.

### GitHub Actions
Workflow: `.github/workflows/anydoc-continuous-ci.yml`

Required jobs/steps:
- `:core:pdf:testDebugUnitTest`
- `:app:testDebugUnitTest` (Robolectric shared-launch lifecycle regressions)
- `:app:assembleDebug`
- `:app:assembleRelease` (unsigned release/R8 compile gate)
- `:app:lintDebug`

Validated baseline state: workflow run #60 (API run ID `36176743391`) completed **successfully** on code HEAD `903c257b1370bb6666cfa94207ad2017cd0337f8`. Documentation head `0633bfc1c7a873bb8380641752fa189b182e5274` subsequently passed run #66.

Audio transactional-output story validation: initial runs #75/#76 failed at `:core:pdf:compileDebugKotlin` because a public inline staging helper referenced a private helper. Follow-up CI exposed required suspend propagation through existing suspend writers and tests. After those root causes were fixed, push run #85 (API run ID `36223470957`) completed **successfully** on code HEAD `37c2fee82af4406bf969be9ae6f5eab74a0c9e7c`: core PDF unit tests, debug APK assembly, unsigned release/R8 assembly, and Android lint all passed. This closes `US-R001-P1-03B`.

Multi-output transactional publishing validation: push run #92 (API run ID `36228935470`) completed **successfully** on exact code HEAD `dc4c92f165dd22ee556abbbaa686c1ccc38594ad`. Core PDF unit tests (including the new set-level staging tests), debug APK assembly, unsigned release/R8 assembly, and Android lint all passed. This closes `US-R001-P1-03C` and mandatory item R001-P1-03.

Lifecycle-story validation: GitHub Actions PR run #107 (API run ID `36261736461`) completed **successfully** on exact branch HEAD `da74754c3b3c8178e719f71ab8915948bc3adca0`. Every required step passed: core PDF unit tests, app lifecycle unit tests, debug APK assembly, unsigned release/R8 assembly, and Android lint. The lifecycle tests cover saved-state restoration, consume-without-replay, newer-intent supersession, and stale-consume protection. This closes `US-R001-P1-04A` and mandatory item R001-P1-04. Earlier run #105 was cancelled by subsequent branch activity and is not used as completion evidence.

CI efficiency correction: push and draft-PR events for the same feature branch now share one concurrency group, and docs-only changes under `docs/codex-continuous-improvement/**` are ignored by the Android validation workflow so durable checkpoints cannot repeatedly cancel useful builds.

### Sandbox
Attempted repository clone into the ChatGPT/Codex container.
Result: **BLOCKED BY ENVIRONMENT NETWORK** — rechecked in run 005 and `git ls-remote https://github.com/myProjectsRavi/anyDoc.git HEAD` still returns `Could not resolve host: github.com`.

No Gradle command from the feature branch has therefore been executed locally in this run. This limitation is explicit and must not be converted into a pass.

### Physical device
No physical Android device was used or claimed.

## Completion gate
Cycle 001 remains open. Output-safety item R001-P1-03 and lifecycle item R001-P1-04 are validated complete. The next mandatory story is `US-R001-P2-01A` for representative large-input/free-space preflight, followed by the final CI/regression/documentation gate.


## Cycle 002 current evidence

### US-R002-P1-01A — non-sticky batch restart policy
- Confirmed current execution state is held in process-local `BatchQueueRuntimeStore`.
- Updated `BatchQueueForegroundService` so active/running queue starts return `START_NOT_STICKY` through a single restart-policy helper until durable recovery exists.
- Added `BatchQueueForegroundServicePolicyTest` asserting the non-sticky contract.
- Exact code/test HEAD before durable documentation writes: `59cce5de17c6665734dbad00b02062afc187a53d`.
- GitHub Actions run #120 (API `36308903917`) is queued at latest observation. No pass is claimed yet.
- No emulator or physical-device evidence is claimed.


### US-R002-P1-01B persistence primitives

- Transactional `BatchQueueTaskDao.replaceAll()` added so a durable snapshot is replaced atomically.
- `BatchQueuePersistenceMapper` persists only recoverable `QUEUED`/`RUNNING` tasks.
- Persisted `RUNNING` entries restore as `QUEUED` with an explicit recovery warning and are never auto-executed by the mapper.
- Malformed task type/status, non-content URI, invalid input count, invalid ID, and blank output name are rejected.
- Robolectric/JUnit mapper tests were added.
- Exact code/test HEAD: `cd2b45d683ce04efb0ecf84b58a11a5a38029568`.
- GitHub Actions run #140 / API `36358926601` is queued; no pass is claimed yet.
- No emulator or physical-device result is claimed.


### US-R002-P1-01B runtime recovery wiring

- Runtime restores persisted work only when the current in-memory queue is empty and not processing.
- Only restored QUEUED work is admitted; persisted RUNNING entries are already mapped to QUEUED by the persistence mapper and are never auto-started.
- Runtime task IDs advance above restored IDs.
- ViewModel startup restores persisted tasks before collecting and snapshotting runtime state.
- Single/multiple SAF picker paths both surface persistable-grant retention failures.
- Recovery validation rejects malformed type/status/URI/input-count, nonpositive or `Long.MAX_VALUE` IDs, and unsafe output base names; duplicate URIs are collapsed before validation.
- Exact code/test HEAD: `cf2a249ffc77eb91efa0d5a0c0857fac1b38bb9e`.
- PR CI #153 / API `36359225826` is in progress; no pass is claimed yet.
- No emulator or physical-device evidence is claimed.

## Cycle 002 run 014 validation

- Run #155 / API `36359277310`: **FAILED** after core PDF tests passed; `:app:compileDebugUnitTestKotlin` failed because `BatchQueuePersistenceMapperTest.task()` referenced undefined `outputBaseName`.
- Repair commit: `6d7eb138de94355e54537babd08cd65cb5e7fc7c`.
- Run #157 / API `36359528419`: **FAILED** after compilation succeeded; `BatchQueuePersistenceMapperTest.restore_rejectsMalformedTypeStatusUriAndInputCount` failed because its entity fixture ignored the supplied unsafe output base name.
- Repair commit: `367c3785a8bae18fd2324ae673464706f316e7da`.
- Exact code HEAD before documentation writes: `367c3785a8bae18fd2324ae673464706f316e7da`.
- Runs #158 / API `36359747983` and #159 / API `36359750553` are the current successor validation attempts. No pass is claimed until a terminal successful run covers the exact code candidate (or a documentation-only descendant containing the same code tree).
- No emulator, benchmark, or physical-device evidence is claimed.


## Cycle 002 final validation

Exact code HEAD `57704c181dcf374bbb61aea580fe92022858c619` passed GitHub Actions run #163 / API `36360973174`. Core PDF unit tests, app unit tests, debug APK assembly, unsigned release/R8 assembly, and Android lint passed. This closes `US-R002-P1-01B` and Cycle 002 at 2/2 scoped stories complete. No emulator, benchmark, or physical-device result is claimed.

## Cycle 003 run 001 — pre-implementation evidence

- Cycle 002 closure independently rechecked: run #163 / API `36360973174` SUCCESS on exact code HEAD `57704c181dcf374bbb61aea580fe92022858c619`.
- Selected `US-R003-P2-01A`.
- Source inspection confirms `AudioFormatConverter` creates `docforge_audio_*.pcm` in `context.cacheDir`, registers it as active, writes complete decoder output to it, then re-reads it for encoding.
- Decoder explicitly requires `AudioFormat.ENCODING_PCM_16BIT`, so expected decoded bytes can be estimated from duration × sample rate × channels × 2 bytes when duration metadata is known.
- No decoded-size/free-space preflight exists before `createTempPcmFile()`.
- No converter-module unit-test step exists in current CI before this story.
- No emulator, benchmark, or physical-device evidence is claimed.

## Cycle 003 completion evidence

- Exact candidate HEAD: `9b48f14915f14ba99f68282910fdabfaa95c15a1`.
- GitHub Actions run #175 / API `36371211681`: **SUCCESS**.
- Core PDF unit tests: SUCCESS.
- Converter unit tests (new authoritative lane): SUCCESS.
- App lifecycle/unit tests: SUCCESS.
- Debug APK assembly: SUCCESS.
- Unsigned release/R8 assembly: SUCCESS.
- Android lint: SUCCESS.
- PCM guard coverage includes a representative 60-second 44.1 kHz stereo estimate, conservative defaults for missing rate/channels, unknown/invalid duration behavior, and saturation for pathological metadata.
- No emulator, benchmark, or physical-device evidence is claimed.


## Cycle 004 completion evidence

- Story: `US-R004-P2-01A` — bound PDF compressor page bitmap allocation.
- Production commit: `67bae0bec912a1ceeee7885f15c2976759a7383e`.
- Exact test-inclusive candidate: `f38abd2cead70d106411ed474e9e375605d96b65`.
- GitHub Actions run #182 / API `36378666554`: **SUCCESS**.
- Core PDF unit tests: SUCCESS, including new raster-budget tests.
- Converter unit tests: SUCCESS.
- App lifecycle/unit tests: SUCCESS.
- Debug APK assembly: SUCCESS.
- Unsigned release/R8 assembly: SUCCESS.
- Android lint: SUCCESS.
- Regression coverage verifies:
  - normal A4 at 150 DPI is not unnecessarily downscaled;
  - oversized pages are proportionally reduced to fit the ARGB_8888 byte budget;
  - the heap-aware budget is clamped between conservative bounds;
  - pathological page dimensions do not overflow allocation arithmetic.
- No measured peak-memory benchmark is claimed by this story.
- No emulator or physical-device evidence is claimed.


## Cycle 005 run 001 — pre-completion evidence

- Cycle 004 exact candidate `f38abd2cead70d106411ed474e9e375605d96b65` passed run #182 / API `36378666554`.
- Partial `PdfPageImageExporter` cleanup commit `0004c118cbdbcab88c71333a6cc697b5e8bac5a2` moved bitmap recycling into `finally`; run #186 / API `36382246055` passed all existing workflow gates.
- Source inspection confirms page-image raster width/height still come directly from PDF page dimensions × caller `scaleFactor` with no allocation ceiling.
- No completion claim for Cycle 005 yet.
- No emulator, benchmark, or physical-device evidence is claimed.
