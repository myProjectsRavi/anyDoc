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

### Tests added
`core/pdf/src/test/java/com/docforge/core/pdf/PdfCoreSafetyTest.kt`
- retry after failed one-time initialization;
- one initialization across concurrent callers;
- active temp-file registry register/unregister lifecycle;
- non-conflicting naming preserves existing output.

### GitHub Actions
Workflow: `.github/workflows/anydoc-continuous-ci.yml`

Required jobs/steps:
- `:core:pdf:testDebugUnitTest`
- `:app:assembleDebug`
- `:app:assembleRelease` (unsigned release/R8 compile gate)
- `:app:lintDebug`

Validated baseline state: workflow run #60 (API run ID `36176743391`) completed **successfully** on code HEAD `903c257b1370bb6666cfa94207ad2017cd0337f8`. Documentation head `0633bfc1c7a873bb8380641752fa189b182e5274` subsequently passed run #66.

Audio transactional-output story validation: initial runs #75/#76 failed at `:core:pdf:compileDebugKotlin` because a public inline staging helper referenced a private helper. Follow-up CI exposed required suspend propagation through existing suspend writers and tests. After those root causes were fixed, push run #85 (API run ID `36223470957`) completed **successfully** on code HEAD `37c2fee82af4406bf969be9ae6f5eab74a0c9e7c`: core PDF unit tests, debug APK assembly, unsigned release/R8 assembly, and Android lint all passed. This closes `US-R001-P1-03B`.

Multi-output transactional publishing validation: push run #92 (API run ID `36228935470`) completed **successfully** on exact code HEAD `dc4c92f165dd22ee556abbbaa686c1ccc38594ad`. Core PDF unit tests (including the new set-level staging tests), debug APK assembly, unsigned release/R8 assembly, and Android lint all passed. This closes `US-R001-P1-03C` and mandatory item R001-P1-03.

### Sandbox
Attempted repository clone into the ChatGPT/Codex container.
Result: **BLOCKED BY ENVIRONMENT NETWORK** — rechecked in run 005 and `git ls-remote https://github.com/myProjectsRavi/anyDoc.git HEAD` still returns `Could not resolve host: github.com`.

No Gradle command from the feature branch has therefore been executed locally in this run. This limitation is explicit and must not be converted into a pass.

### Physical device
No physical Android device was used or claimed.

## Completion gate
Cycle 001 remains open. Output-safety item R001-P1-03 is validated complete; lifecycle item R001-P1-04 is the next mandatory P1 gate, followed by large-input preflight and the final CI/regression/documentation gate.
