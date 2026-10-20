# AnyDoc Continuous Improvement Backlog

## Closed Cycle 008

### Epic E008 — Failure-safe PDF comparison output

#### Feature F010 — Transactional PDF compare publication
- [COMPLETE / CI #229] `US-R008-P1-01A` — Stage `PdfCompareTool` final PDF publication and guarantee document/page/bitmap cleanup on failure/cancellation. Exact candidate `08cf9090c32e773b595b74102508fd4b8800ab01` passed run #229 / API `36428222433`.
- [NEXT CANDIDATE / P2] Bound PdfCompareTool combined raster memory: left + right + diff ARGB_8888 bitmaps can coexist at renderScale up to 4× without a heap-aware ceiling.

## Closed Cycle 007

### Epic E007 — Failure-safe scan-image publication

#### Feature F009 — Transactional scan-image and ZIP outputs
- [COMPLETE / CI #218] `US-R007-P1-01A` — Stage non-ZIP scan page outputs as an all-or-nothing set and stage ZIP output without publishing intermediate page images. Exact candidate `e71273f59968bb218a8571a5065598db184c2c72` passed run #218 / API `36409673936`.
- [NEXT] Return to the P2 memory/large-input audit; choose the next bounded defect from current source evidence.

## Closed Cycle 006

### Epic E006 — Failure-safe PDF page-image publication

#### Feature F008 — Transactional page-image and ZIP outputs
- [COMPLETE / CI #209] `US-R006-P1-01A` — Stage non-ZIP page-image outputs as an all-or-nothing set and stage ZIP output without publishing intermediate page images. Exact candidate `eb355e7c4b7ce2039c02ee96153eef62a2391511` passed run #209 / API `36393317583`.
- [NEXT CANDIDATE / P1] Apply equivalent transactional publication safety to `ScanImageExporter`, which still writes scan pages and ZIP bundles directly to final paths.

## Closed Cycle 005

### Epic E005 — Heap-safe PDF page-image export

#### Feature F007 — Page-image raster memory budget
- [COMPLETE / CI #199] `US-R005-P2-01A` — Bound `PdfPageImageExporter` ARGB_8888 rasters by a heap-aware byte budget, guarantee bitmap recycling, and harden extreme-aspect clamp arithmetic. Exact candidate `aca6223a10cb73862f8dbed226e1292e1ca863e8` passed run #199 / API `36385089020`.
- [NEXT CANDIDATE / P1] Make page-image and ZIP final publication failure-safe; direct final-file writes can leave partial user-visible output after encoding/cancellation/ZIP failure.

## Closed Cycle 004

### Epic E004 — Heap-safe PDF rasterization

#### Feature F006 — PDF compressor raster memory budget
- [COMPLETE / CI #182] `US-R004-P2-01A` — Bound PDF-compressor ARGB_8888 page rasters with a heap-aware allocation budget and proportional downscale. Exact candidate `f38abd2cead70d106411ed474e9e375605d96b65` passed run #182 / API `36378666554`.
- [NEXT CANDIDATE] Continue P2 allocation audit with `PdfPageImageExporter`: bound page-size × scale-factor raster allocation and guarantee bitmap cleanup on encoding/render failure.


Canonical branch: `codex/anydoc-continuous-improvement`

This backlog is ordered by user-data safety, correctness, reliability, performance, UX, then optional capability. Items may move only when evidence changes priority.

## Closed Cycle 003

### Epic E003 — Large-input and resource safety

#### Feature F005 — Media temporary-space preflight
- [COMPLETE / CI #175] `US-R003-P2-01A` — Estimate decoded 16-bit PCM cache demand before audio transcoding; reject known-insufficient cache space before decode; converter-module tests and CI coverage added. Exact candidate `9b48f14915f14ba99f68282910fdabfaa95c15a1` passed run #175 / API `36371211681`.

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


## Cycle 015 closure — 2026-10-02
- [COMPLETE / CI #282] `US-R015-P2-01A` — Bound combined front/back ID-card bitmap memory with a heap-aware decode ceiling. Exact code/test SHA `8fbe11331df90911cce055e93a9535cc8459a22e` passed run #282 / API `37025519540`.
- [NEXT / P1] `US-R016-P1-01A` — Make PdfSigner final PDF publication transactional; current `outDoc.save(outputFile)` writes directly to the user-visible destination.

## Cycle 016 active
### Epic E016 — Failure-safe PDF signing
#### Feature F018 — Transactional signed PDF publication
- [ACTIVE] `US-R016-P1-01A` — Stage PdfSigner final PDF publication and add focused failure/cancellation regression coverage.


## Cycle 016 closure — 2026-10-02
- [COMPLETE / CI #289] `US-R016-P1-01A` — Stage PdfSigner final PDF publication. Exact code/test SHA `57a0664f8174065de9052963a5ff48188de2ba3a` passed run #289 / API `37027249729`.
- [NEXT / P1] `US-R017-P1-01A` — Stage PdfAnnotator final PDF publication; current `outDoc.save(outputFile)` writes directly to the final destination.

## Cycle 017 active
### Epic E017 — Failure-safe PDF annotation
#### Feature F019 — Transactional annotated PDF publication
- [ACTIVE] `US-R017-P1-01A` — Stage PdfAnnotator final PDF publication and add focused writer-failure/cancellation regressions.


## Cycle 016 closure — 2026-10-02
- [COMPLETE / CI #289] `US-R016-P1-01A` — PdfSigner final PDF publication is transactional via same-directory staging. Exact candidate `57a0664f8174065de9052963a5ff48188de2ba3a` passed run #289 / API `37027249729`.
- [NEXT / P1] `US-R017-P1-01A` — Make PdfAnnotator final PDF publication transactional.

## Cycle 017 active
### Epic E017 — Failure-safe PDF annotation
#### Feature F019 — Transactional annotated PDF publication
- [ACTIVE] `US-R017-P1-01A` — Stage PdfAnnotator final PDF publication and add focused failure/cancellation regression coverage.


## Cycle 017 closure — 2026-10-02
- [COMPLETE / CI #300] `US-R017-P1-01A` — PdfAnnotator final publication is transactional. Validated HEAD `1ff75b03667a798c6f3be02fd253932c6ffb7e53` passed run #300 / API `37029105532`.
- [NEXT / P1] `US-R018-P1-01A` — Make PdfPasswordTool protect/unlock final PDF publication transactional.

## Cycle 018 active
### Epic E018 — Failure-safe password PDF output
#### Feature F020 — Transactional protected/unlocked publication
- [ACTIVE] `US-R018-P1-01A` — Stage PdfPasswordTool protect/removePassword outputs and add focused failure/cancellation coverage.


## Cycle 018 closure — 2026-10-02
- [COMPLETE / CI #308] `US-R018-P1-01A` — PdfPasswordTool protect/unlock publication is transactional. Exact code/test SHA `853804e2863dbb630409ca6fcbdc7a854bca45a5` passed run #308 / API `37031208024`.
- [NEXT / P1] `US-R019-P1-01A` — Make PdfPageCropTool output publication transactional.

## Cycle 019 active
### Epic E019 — Failure-safe PDF page cropping
#### Feature F021 — Transactional cropped PDF publication
- [ACTIVE] `US-R019-P1-01A` — Stage both cropAllPages and cropPages outputs and add focused publication-safety regression coverage.


## Cycle 019 closure — 2026-10-02
- [COMPLETE / CI #315] `US-R019-P1-01A` — PdfPageCropTool cropAllPages/cropPages publication is transactional. Exact code/test SHA `0bed6b770f9ad9810e92cbf3c697347276b7dddf` passed push run #315 / API `37033912072`.
- [NEXT / P1] `US-R020-P1-01A` — Make PdfHeaderFooterTool final PDF publication transactional.

## Cycle 020 active
### Epic E020 — Failure-safe PDF header/footer export
#### Feature F022 — Transactional header/footer PDF publication
- [ACTIVE] `US-R020-P1-01A` — Stage PdfHeaderFooterTool output and add focused publication-safety regression coverage.


## Cycle 023 closure — 2026-10-03
- [COMPLETE / CI #339] `US-R023-P2-01A` — Guarantee PdfMerger image bitmap recycling on all append/render exit paths. Exact candidate `79d0d11a87de414ffd03937fd4590045ee7b6a8e` passed run #339 / API `37119773705`.
- [NEXT / P2] `US-R024-P2-01A` — Bound PdfOcrTool PDF-page ARGB_8888 raster allocation with a heap-aware byte ceiling while preserving the 1800 px quality cap.

## Cycle 024 active
### Epic E024 — Heap-safe PDF OCR rasterization
#### Feature F026 — Heap-aware OCR bitmap budget
- [COMPLETE / CI #345] `US-R024-P2-01A` — Add heap-aware OCR raster sizing and focused regression coverage. Exact candidate `31e2ff40cc58f4a80ad1d278daed2d7314be1661` passed run #345 / API `37135882621`.


## Cycle 024 closure — 2026-10-03
- [COMPLETE / CI #345] `US-R024-P2-01A` — Heap-aware PdfOcrTool page raster allocation. Exact candidate `31e2ff40cc58f4a80ad1d278daed2d7314be1661` passed run #345 / API `37135882621`.
- [NEXT / P2] `US-R025-P2-01A` — Bound shared constrained image decoding by runtime heap.

## Cycle 025 active
### Epic E025 — Heap-safe shared image decoding
#### Feature F027 — Runtime-heap-aware constrained bitmap decode
- [COMPLETE / CI #352] `US-R025-P2-01A` — Bound `decodeBitmapConstrained()` allocations and add focused budget tests. Exact candidate `9760f56e7067cf169a7336be06c01f7b3495ab83` passed run #352 / API `37137314847`.


## Cycle 025 closure — 2026-10-03
- [COMPLETE / CI #352] `US-R025-P2-01A` — Heap-aware shared constrained image decoding. Exact candidate `9760f56e7067cf169a7336be06c01f7b3495ab83` passed run #352 / API `37137314847`.
- [NEXT / P2] `US-R026-P2-01A` — Bound scanner perspective region decode before OpenCV native allocations.

## Cycle 026 active
### Epic E026 — Heap-safe scanner perspective correction
#### Feature F028 — Bounded document-region decode before OpenCV warp
- [ACTIVE] `US-R026-P2-01A` — Add heap-aware region sampling and focused sizing tests.


## Cycle 026 closure — 2026-10-03
- [COMPLETE / CI #366] `US-R026-P2-01A` — Heap-aware scanner perspective region decode with a 2000 px quality ceiling and power-of-two sampling. Exact candidate `05f815ad10c928d483ad2cd38c7b9325bc35a04f` passed run #366 / API `37138433125`.
- [NEXT / P2] `US-R027-P2-01A` — Bound ImageFormatConverter scaled output allocation by runtime heap.

## Cycle 027 active
### Epic E027 — Heap-safe image format scaling
#### Feature F029 — Bounded scaled bitmap allocation
- [ACTIVE] `US-R027-P2-01A` — Clamp scaled image dimensions to a conservative ARGB_8888 budget while preserving requested scale when safe.


## Cycle 027 closure — 2026-10-03
- [COMPLETE / CI #373] `US-R027-P2-01A` — Heap-safe ImageFormatConverter scaled output allocation. Exact candidate `bdaf63302abe83978a99a68f2d0c6cdb4d21eb8e` passed run #373 / API `37139896501`.
- [NEXT / P1] `US-R028-P1-01A` — Stage PdfTextExtractor TXT publication.

## Cycle 028 active
### Epic E028 — Failure-safe PDF text extraction
#### Feature F030 — Transactional extracted-text publication
- [ACTIVE] `US-R028-P1-01A` — Stage extracted TXT output and add failure/cancellation coverage.


## Cycle 028 closure — 2026-10-03
- [COMPLETE / CI #379] `US-R028-P1-01A` — Transactional PdfTextExtractor TXT publication. Exact candidate `9a623321759316f02205342bf4d3b67e05f07b82` passed run #379 / API `37140903238`.
- [NEXT / P1] `US-R029-P1-01A` — Stage DocumentPdfConverter output and guarantee PdfDocument cleanup.

## Cycle 029 active
### Epic E029 — Failure-safe document-to-PDF conversion
#### Feature F031 — Transactional document PDF publication
- [ACTIVE] `US-R029-P1-01A` — Stage final PDF and close Android PdfDocument on all exit paths.


## Cycle 029 closure — 2026-10-03
- [COMPLETE / CI #386] `US-R029-P1-01A` — Transactional DocumentPdfConverter publication plus unconditional PdfDocument cleanup. Exact candidate `b59881de6a87eb90fc1726746e8ba18a7dae7eae` passed run #386 / API `37141704316`.
- [NEXT / P1] `US-R030-P1-01A` — Stage HtmlPdfConverter final PDF publication.

## Cycle 030 active
### Epic E030 — Failure-safe HTML-to-PDF conversion
#### Feature F032 — Transactional HTML PDF publication
- [ACTIVE] `US-R030-P1-01A` — Stage HTML-rendered PDF and add failure/cancellation coverage.


## Cycle 030 closure — 2026-10-03
- [COMPLETE / CI #392] `US-R030-P1-01A` — Stage HtmlPdfConverter final PDF publication. Exact candidate `80d58adeccadc418691b0bdd1ffd654fb4ea4d66` passed run #392 / API `37142421834`.
- [NEXT / P1] `US-R031-P1-01A` — Stage TextPdfConverter final PDF publication.

## Cycle 031 active
### Epic E031 — Failure-safe text-to-PDF conversion
#### Feature F033 — Transactional text PDF publication
- [ACTIVE] `US-R031-P1-01A` — Stage TextPdfConverter final serialization and add focused failure/cancellation coverage.


## Cycle 031 closure — 2026-10-04
- [COMPLETE / CI #399] `US-R031-P1-01A` — Transactional TextPdfConverter publication plus failure-safe PdfDocument cleanup. Exact candidate `7870da321a9f6d865e8728ba636bee737272c9a7` passed run #399 / API `37166290751`.
- [NEXT / P1] `US-R032-P1-01A` — Stage BusinessCardParser final vCard publication.

## Cycle 032 active
### Epic E032 — Failure-safe business-card vCard export
#### Feature F034 — Transactional vCard publication
- [ACTIVE] `US-R032-P1-01A` — Stage final vCard bytes and add focused failure/cancellation coverage.


## Cycle 040 closure — 2026-10-07
- [COMPLETE / CI #480] `US-R040-P2-01A` — Bound TXT/CSV/RTF ingestion before unrestricted in-memory materialization. Exact code/test SHA `74f24148f5268744debc7158388af4bf38081283` passed PR run #480 / API `37639874324`.
- Focused tests lock boundary, overflow, and CSV line-semantics behavior.
- [NEXT / P2] `US-R041-P2-01A` — Bound HTML URI and raw-string ingestion before WebView rendering.

## Cycle 041 active
### Epic E041 — Large-input HTML conversion safety
#### Feature F043 — Bounded HTML ingestion
- [ACTIVE] `US-R041-P2-01A` — Reuse bounded URI ingestion and enforce the same limit on raw HTML before WebView rendering.


## Cycle 041 closure — 2026-10-07
- [COMPLETE / CI #483] `US-R041-P2-01A` — Bound HTML URI and raw-string ingestion before WebView rendering. Exact code/test SHA `45e9f3bffcc50d7458629ec332c550c2eaab1011` passed run #483 / API `37644316676` on attempt 2.
- Attempt 1 was an external Kotlin/Maven dependency-resolution failure before Cycle 041 code compiled.
- [NEXT / P2] `US-R042-P2-01A` — Eliminate whole-document redaction text materialization.

## Cycle 042 active
### Epic E042 — Large-PDF redaction memory safety
#### Feature F044 — Page-bounded redaction text scanning
- [ACTIVE] `US-R042-P2-01A` — Scan redaction auto-detect and irreversible verification text page-by-page instead of materializing all document text.


## Cycle 043 closure — 2026-10-08
- [COMPLETE / CI #499] `US-R043-P2-01A` — Combined PdfCompareTool raster budget, invalid-dimension hardening, deterministic budget tests, and cancellation/failure-safe diff bitmap ownership. Exact code/test SHA `ed6d500546c883108e15c722c0a2ce98b1b2f256` passed run #499 / API `37713745007`.
- Completion documentation HEAD `867585e4684f6a353f1fd5a400ccc39a6fb8ab9d` passed run #500 / API `37718067659`.

## Cycle 044 active
### Epic E044 — Memory-safe PDF text extraction
#### Feature F046 — Streamed PDF-to-TXT publication
- [COMPLETE / CI #503] `US-R044-P2-01A` — Replace whole-document PDF text/UTF-8 byte-array materialization with streaming UTF-8 output while preserving sorted text, empty-text fallback, accurate character count, and failure-safe staged publication. Add focused JVM regressions and validate exact code/test SHA via GitHub Actions.


## Cycle 044 closure — 2026-10-08
- [COMPLETE / CI #503] `US-R044-P2-01A` — Stream PDF text to staged UTF-8 TXT with character counting and whitespace fallback. Exact SHA `2f6561ad5014b8aa96a9384c3fbe0ca588079c6f` passed PR run #503 / API `37728082557` (all seven gates).
- [NEXT / P1] `US-R045-P1-01A` — Correct unescaped XMP metadata text in PdfAComplianceTool.

## Cycle 045 active
### Epic E045 — PDF/A metadata XML correctness
#### Feature F047 — Well-formed XMP metadata serialization
- [ACTIVE / P1] `US-R045-P1-01A` — XML-safe title/author/producer serialization, invalid-code-point handling, JVM XML parsing/round-trip regressions, exact-SHA CI.


## Cycle 045 closure — 2026-10-08
- [COMPLETE / CI #506] `US-R045-P1-01A` — XML-safe PDF/A XMP metadata, six JVM regressions. Exact SHA `c57c522549f79216623981d0d85a60fdcf48e823` passed PR run #506 / API `37733161386` (all seven gates).
- [NEXT / P1] `US-R046-P1-01A` — Prevent silent Bates numbering rollover in batch stamping.

## Cycle 046 active
### Epic E046 — Bates numbering integrity
#### Feature F048 — Overflow-safe Bates sequence
- [ACTIVE / P1] `US-R046-P1-01A` — Checked Long counter, deterministic boundary tests, exact-SHA CI, staged output preserved.


## Cycle 046 closure — 2026-10-08
- [COMPLETE / CI #516] `US-R046-P1-01A` — Checked Long Bates counter across pages/files with six focused JVM tests. Exact code/test SHA `d87426a1d042d571eb72f8b43313bedad63205e7` passed PR run #516 / API `37750707619`, all seven gates.
- [NEXT / P1] `US-R047-P1-01A` — Preserve saved signatures on failed replacement; source: `SavedSignatureStore.save()` truncates existing PNG before encode and ignores `bitmap.compress()` failure.

## Cycle 047 active
### Epic E047 — Saved signature integrity
#### Feature F049 — Atomic saved signature replacement
- [ACTIVE / P1] `US-R047-P1-01A` — Atomic replacement preserving old slot on encoding/write failure, deterministic regression tests, feature test CI gate, exact-SHA validation.


## Cycle 047 closure — 2026-10-08
- [COMPLETE / CI #519] `US-R047-P1-01A` — Atomic saved-signature PNG replacement with seven focused feature JVM tests. Exact code/test SHA `a38e779267f7300e8ebc86197e8ce25137a99326` passed PR run #519 / API `37793452165`, all eight gates.
- [NEXT / P1] `US-R048-P1-01A` — Prevent silent saved placement-template corruption/loss from fixed staging name, unchecked `renameTo`, and malformed-state fallback.

## Cycle 048 active
### Epic E048 — Signature placement template integrity
#### Feature F050 — Failure-safe placement template persistence
- [ACTIVE / P1] `US-R048-P1-01A` — Unique same-directory atomic JSON replacement, reject unreadable prior state, deterministic JVM failure tests, exact-SHA CI.


## Cycle 048 closure — 2026-10-08
- [COMPLETE / CI #524] `US-R048-P1-01A` — Atomic placement-template JSON persistence and corruption-safe ViewModel. Exact SHA `0f576dc1029af2f79f3a73ce726d162d716442a1`, PR run #524 / API `37809062234` SUCCESS, eight gates.
- [NEXT / P1] `US-R049-P1-01A` — Surface unreadable saved batch presets and DAO failures.

## Cycle 049 active
### Epic E049 — Saved batch preset integrity
#### Feature F051 — Visible preset read/corruption errors
- [ACTIVE / P1] `US-R049-P1-01A` — Explicit stored preset decode/DAO errors, ViewModel error handling, app JVM regressions, exact-SHA CI.


## Cycle 049 closure — 2026-10-08
- [COMPLETE / CI #527] `US-R049-P1-01A` — Explicit errors for corrupt saved batch presets and DAO failures; preserves prior UI data. Exact code/test SHA `7db88fa477614d34e6a54c50b0c0c031d76a724d` passed PR run #527 / API `37824852307`, all eight gates; 13 focused app JVM regressions.
- [NEXT / P1] `US-R050-P1-01A` — Prevent destructive batch queue snapshot replacement after recovery read failure.

## Cycle 050 active
### Epic E050 — Durable batch queue recovery safety
#### Feature F052 — No destructive writes after failed recovery
- [ACTIVE / P1] `US-R050-P1-01A` — Gate runtime snapshot persistence on successful recovery, preserve cancellation, add focused JVM regressions and exact-SHA CI.

## Cycle 050 closure — 2026-10-08
- [COMPLETE / CI #530] `US-R050-P1-01A` — Recovery-gated batch queue persistence. Exact SHA `b318a37cc68bab3c34ddaee4c6deac52017e6a15` passed PR run #530 / API `37853941252`, all eight gates. Eight focused app JVM regressions.
- [NEXT / P1] `US-R051-P1-01A` — Catch batch cancellation service-start failures and report them to the UI instead of crashing.

## Cycle 051 active
### Epic E051 — Batch cancellation crash safety
#### Feature F053 — Failure-safe cancellation service launch
- [ACTIVE / P1] `US-R051-P1-01A` — Testable cancellation-launch error handling with app JVM regression coverage and exact-SHA CI.


## Cycle 051 closure — 2026-10-09 UTC
- [COMPLETE / CI #533] `US-R051-P1-01A` — Service-start failures during cancellation are surfaced to the UI; four focused app JVM tests. Exact SHA `3d65db557207eb900bca99a9a8ff52b250fb5071` passed PR run #533 / API `37860102445`, eight gates.
- [NEXT / P1] `US-R052-P1-01A` — Reject corrupt recoverable queue rows to avoid silent partial restoration and subsequent destructive writes.

## Cycle 052 active
### Epic E052 — Saved batch queue integrity
#### Feature F054 — Strict recoverable task validation
- [ACTIVE / P1] `US-R052-P1-01A` — Strict active-row validation, intentional terminal filtering, app JVM regressions and exact-SHA CI.

## Cycle 052 closure — 2026-10-09 UTC
- [COMPLETE / CI 37889034940] US-R052-P1-01A: strict corrupt recoverable-row rejection. Code/test c22d28787adc5215161c580a4e2de7f5af29fa68, validated HEAD 71b2ace6400afcb2bac8c1326fc1ba67278f06a8, all eight gates passed.
- [NEXT / P1] US-R053-P1-01A: preserve coroutine cancellation in batch queue checkpoint writes.

## Cycle 053 active
### Epic E053 — Cancellation-safe batch queue persistence
#### Feature F055 — Cancellation-safe persistence checkpoints
- [ACTIVE / P1] US-R053-P1-01A: cancellation-safe suspend helper, service integration, focused JVM regressions and exact-SHA CI.


## Cycle 053 closure — 2026-10-09 UTC
- [COMPLETE / CI #547] `US-R053-P1-01A` — Cancellation-aware batch checkpoint writes and service cleanup. Exact code/test SHA `eeadc85e46b5387c087dc73d6231f8afc86edaf3`; authoritative PR run `37912518611` SUCCESS across eight gates.
- [NEXT / P1] `US-R054-P1-01A` — Prevent stale concurrent queue snapshot persistence.

## Cycle 054 active
### Epic E054 — Concurrent batch queue persistence integrity
#### Feature F056 — Current-state snapshot writes under shared mutex
- [ACTIVE / P1] `US-R054-P1-01A` — Capture latest tasks inside shared persistence lock, migrate service/ViewModel writes, contention regression, exact-SHA PR CI.


## Cycle 054 closure — 2026-10-09 UTC
- [COMPLETE / PR CI #550] `US-R054-P1-01A`: lock-then-read current task snapshot; service and ViewModel migrated; six focused app JVM regressions. Exact code/test SHA `c81fc5e1a575eb695166c5d29f9e344d76fc25f3`, run API `37924104373` SUCCESS, all eight gates.
- [NEXT / P1] `US-R055-P1-01A`: prevent queue recovery failure from unrecoverable URIs admitted by direct enqueue/preset replay.

## Cycle 055 active
### Epic E055 — Recoverable batch queue URI integrity
#### Feature F057 — URI admission and preset replay validation
- [ACTIVE / P1] `US-R055-P1-01A` — Validate every URI and preserve existing queue on invalid preset, with app JVM regression tests and exact-SHA CI.

## Cycle 055 closure — 2026-10-09 UTC
- [COMPLETE / PR CI #556] `US-R055-P1-01A` — Validate content URIs before queue admission; atomic preset validation and effective URI-count checks after deduplication. Exact code/test SHA `f6fe6b9256db881a898728c32ee0cbd261007e7e`, PR run API `37973956107` SUCCESS, all eight gates.
- [NEXT / P1] `US-R056-P1-01A` — Make batch terminal transitions atomic/idempotent to prevent conflicting terminal states and duplicate counters.

## Cycle 056 active
### Epic E056 — Batch task terminal-state integrity
#### Feature F058 — Atomic idempotent terminal transitions
- [ACTIVE / P1] `US-R056-P1-01A` — Guard RUNNING-to-terminal changes, update task and counters atomically, test repeated/conflicting/absent transitions, validate exact SHA through PR CI.


## Cycle 056 closure — 2026-10-09 UTC
- [COMPLETE / PR CI #559] `US-R056-P1-01A` — Atomic idempotent RUNNING-to-terminal task transitions and counters; ten focused app JVM regressions. Exact code/test SHA `2dd59048b0844ed347eb28960922b52e39c0c7bf`, authoritative PR run API `37993664370` SUCCESS, all eight gates.
- [NEXT / P1] `US-R057-P1-01A` — Prevent stale queue reorder snapshots from clobbering concurrent task additions or terminal outcomes.

## Cycle 057 active
### Epic E057 — Concurrent queue reorder integrity
#### Feature F059 — Atomic queue reorder from current state
- [ACTIVE / P1] `US-R057-P1-01A` — Validate and reorder QUEUED tasks atomically from current StateFlow state; preserve concurrent additions and terminal outcomes; focused app JVM regressions and exact-SHA PR CI.


## Cycle 057 closure — 2026-10-10 UTC
- [COMPLETE / PR CI #562] `US-R057-P1-01A` — Atomic queue reorder from current StateFlow; eight app JVM regressions. Exact code/test SHA `d545ea396c99aba08a1e5d345969d29d6aa2c567`, authoritative PR run API `38009446988` SUCCESS, all eight gates.
- [NEXT / P1] `US-R058-P1-01A` — Prevent running-task removal and rename races under concurrent execution.

## Cycle 058 active
### Epic E058 — Batch queue task mutation integrity
#### Feature F060 — Atomic task removal and output-name edits
- [ACTIVE / P1] `US-R058-P1-01A` — Guard task status and mutation in a single CAS loop, preserve queued/terminal removal, add app JVM regressions and exact-SHA PR CI.


## Cycle 058 closure — 2026-10-10 UTC
- [COMPLETE / PR CI #567] `US-R058-P1-01A` — CAS-guarded task removal/rename and seven app JVM regressions; exact SHA `48d3faa924aa4b424f48fae10fc2f45a591cc9d5`, authoritative PR run API `38013754231` SUCCESS, eight gates.
- [NEXT / P1] `US-R059-P1-01A` — Prevent concurrent queue clear/start from losing task state or returning stale queued IDs.

## Cycle 059 active
### Epic E059 — Batch queue clear/start integrity
#### Feature F061 — Atomic queue clearing and processing admission
- [ACTIVE / P1] `US-R059-P1-01A` — CAS-guard clear and begin-processing against same state; app JVM race regressions and exact-SHA PR CI.


## Cycle 059 closure — 2026-10-10 UTC
- [COMPLETE / PR CI #570] `US-R059-P1-01A` — Atomic queue clear and begin processing with five focused JVM regressions. Exact code/test SHA `a0e446b8161cf2619452a2530864c5dc14cab5fe`, authoritative PR run API `38017407741` SUCCESS, all eight gates.
- [NEXT / P1] `US-R060-P1-01A` — Prevent preset replay from replacing active processing tasks under concurrency.

## Cycle 060 active
### Epic E060 — Preset replay concurrency integrity
#### Feature F062 — Atomic preset queue replacement
- [ACTIVE / P1] `US-R060-P1-01A` — CAS-guard validated preset replay against current nonprocessing state; focused app JVM race tests and exact-SHA authoritative PR CI.


## Cycle 060 closure — 2026-10-10 UTC
- [COMPLETE / PR CI #573] `US-R060-P1-01A` — Atomic preset replay with four app JVM regressions (150 concurrent replay/start scenarios). Exact code/test SHA `6eb42d73360d7795bbb267d2d6ee217f8ded474a`, authoritative PR run API `38024299499` SUCCESS, eight gates.
- [NEXT / P1] `US-R061-P1-01A` — Prevent recovery overwriting concurrent queue mutations and reserve collision-free task IDs.

## Cycle 061 active
### Epic E061 — Recoverable queue admission integrity
#### Feature F063 — Atomic recovery and task-ID uniqueness
- [ACTIVE / P1] `US-R061-P1-01A` — CAS-guard recovery against current empty/nonprocessing queue, coordinate task-ID reservation across recovery/enqueue/preset replay, add focused app JVM race tests, validate exact-SHA PR CI.

## Cycle 061 closure — 2026-10-10 UTC
- [COMPLETE / PR CI #576] `US-R061-P1-01A` — Atomic recovery admission and coordinated task IDs. Seven focused app JVM regressions, including 450 concurrency scenarios. Exact SHA `e4ec1efc4556b4f3cea6124d53f28ed4fd1bf3a2`, PR run API `38034856244` SUCCESS, all eight gates.
- [NEXT / P1] `US-R062-P1-01A` — Prevent inactive dispatch and uncommitted task returns.

## Cycle 062 active
### Epic E062 — Batch queue dispatch claim integrity
#### Feature F064 — Atomic processing-aware task claims
- [ACTIVE / P1] `US-R062-P1-01A` — CAS-claim QUEUED tasks only while processing, return committed RUNNING tasks, add app JVM regressions and exact-SHA PR CI.


## Cycle 062 closure — 2026-10-10 UTC
- [COMPLETE / PR CI #583] `US-R062-P1-01A` — Atomic processing-aware task claims. Exact code/test SHA `d88c011fc4d04d3847b0be81416042f40f89eb37`; authoritative PR run API `38095021819` SUCCESS, eight gates. Rename/removal race setup repaired. No emulator, device or benchmark evidence.
- [NEXT / P1] `US-R063-P1-01A` — Scope cancellation to tasks admitted to active batch.

## Cycle 063 active
### Epic E063 — Batch queue cancellation scope integrity
#### Feature F065 — Run-scoped queued task cancellation
- [ACTIVE / P1] `US-R063-P1-01A` — Cancel only queued tasks in active run ID set; preserve later enqueue and terminal/running states; deterministic and concurrent app JVM regressions; exact-SHA PR CI.

## Cycle 063 closure — 2026-10-11 UTC
- [COMPLETE / PR CI #586] `US-R063-P1-01A` — Run-scoped cancellation and five JVM regressions. Exact SHA `b2cf41cb695c56370df583d59b4e3e81a364c249`; authoritative run API `38118363838` SUCCESS, eight gates.
- [NEXT / P1] `US-R064-P1-01A` — Prevent cancel-before-worker-attachment from stranding the batch in processing state.

## Cycle 064 active
### Epic E064 — Batch queue cancel/start handoff integrity
#### Feature F066 — Cancel-before-worker-start recovery
- [ACTIVE / P1] `US-R064-P1-01A` — Correct pending-start/pre-dispatch cancellation and idempotent terminal cleanup; focused JVM/Robolectric tests and exact-SHA PR CI.


## Cycle 064 closure — 2026-10-11 UTC
- [COMPLETE / PR CI #593] `US-R064-P1-01A` — Run-identified cancellation handoff, pre-dispatch cleanup, stale command/worker isolation, 13 focused app JVM tests. Exact SHA `0227722822dab7d4c6d9ed06c0d722b430a660cd`, API `38142774494` SUCCESS all eight gates. Paired push #592 cancelled by concurrency, not test evidence.
- [NEXT / P1] `US-R065-P1-01A` — Recover admitted batch when foreground service promotion throws before worker attachment.

## Cycle 065 active
### Epic E065 — Foreground service promotion integrity
#### Feature F067 — Run-scoped foreground promotion failure recovery
- [ACTIVE / P1] `US-R065-P1-01A` — Handle `startForeground()` failures without stranding current run; preserve newer runs; deterministic JVM/Robolectric regressions and exact-head CI.


## Cycle 065 closure — 2026-10-11 UTC
- [COMPLETE / PR CI #598] `US-R065-P1-01A` — Recover rejected foreground promotion on matching run, app-scope terminal persistence and prompt service release. Five focused JVM tests; SHA `33d58689a282ea0c1c8832b95752c29a85df4221`; API `38148001273` SUCCESS all eight gates.
- [NEXT / P1] `US-R066-P1-01A` — Terminalize interrupted RUNNING tasks on run failure without disturbing unrelated tasks.

## Cycle 066 active
### Epic E066 — Interrupted batch run terminal integrity
#### Feature F068 — Terminalize interrupted RUNNING tasks on run failure
- [ACTIVE / P1] `US-R066-P1-01A` — CAS-guard failed run, mark interrupted RUNNING tasks FAILED exactly once and update counters. Preserve queued/terminal tasks, stale-run isolation, and verify with app JVM regressions and PR CI.

## Cycle 066 completion / Cycle 067 activation — 2026-10-11 UTC
- Cycle 066 E066/F068/US-R066-P1-01A COMPLETE. Exact code/test SHA `ce5d8b0192ee68c8922b256c5baf9d56cf3949f4`; authoritative PR GitHub Actions run #601 / API `38165332836` SUCCESS (2026-10-11 UTC), all eight configured gates: core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, Android lint. `failRunIfCurrent` atomically terminalizes only admitted RUNNING tasks as FAILED, increments processed/failure counters exactly once, and preserves QUEUED, terminal, later-enqueued and newer-run work. Five focused Robolectric/JVM regressions were committed. Paired push run `38165328986` was cancelled by concurrency, not completion evidence. No emulator, device, benchmark or crash-free evidence.
- Cycle 067 E067/F069/US-R067-P1-01A ACTIVE / INCOMPLETE. Evidence: `BatchQueueForegroundService.persistTerminalCheckpoint` suspends while saving, then calls `BatchQueueRuntimeStore.reportPersistenceFailureIfIdle` on error; the latter checks only `!isProcessing`. An older delayed save failure can overwrite a newer completed batch's status or reintroduce a user-dismissed error after the app returns to idle. Next: bind persistence failure reporting to the exact terminal-state snapshot captured before the write, reject superseded snapshots, add deterministic JVM regressions for unchanged state, newer completion, dismissed error, same-message retry, and active processing, then validate exact code/test SHA with authoritative PR CI. PR #1 stays draft/unmerged; main untouched.

## Cycle 067 active
### Epic E067 — Run-scoped persistence failure reporting
#### Feature F069 — Suppress superseded terminal-save failures
- [ACTIVE / P1] `US-R067-P1-01A` — Bind delayed terminal persistence error reporting to the original idle terminal snapshot; preserve newer/dismissed UI state. Add focused app JVM regressions and validate exact-SHA PR CI.


## Cycle 067 completion / Cycle 068 activation — 2026-10-11 UTC
- [COMPLETE / PR CI #606] `US-R067-P1-01A`: exact code/test SHA `3aa276af37760db53365ee720c3ac525ac469eec`; PR run `38172791603` SUCCESS (eight configured gates). Snapshot-scoped persistence errors preserve newer/dismissed state; no device or benchmark evidence.
- [ACTIVE / P1] `US-R068-P1-01A` (`E068/F070`): fail-close the matching batch when per-task terminal checkpoint persistence fails; preserve terminal results, counters, queued work, newer runs and cancellation. Add JVM tests and exact-SHA PR CI.


## Cycle 068 completion / Cycle 069 activation — 2026-10-11 UTC
- Cycle 068 `E068/F070/US-R068-P1-01A` COMPLETE. Exact code/test SHA `7bcff3d9cc89d9ded303b3053b48b4cbc4b188b6` passed authoritative PR CI #610 / API `38176646082` (2026-10-11 UTC, job `114580380447`), all eight configured gates SUCCESS. A failed per-task terminal checkpoint now fail-closes the matching run and retries persistence without changing already completed results, counters or newer runs. Six focused Robolectric/JVM regressions cover success, failure, stale retries, duplicate callbacks and cancellation. No emulator, physical device, benchmark or crash-free claim.
- Cycle 069 `E069/F071/US-R069-P1-01A` ACTIVE / INCOMPLETE. Source: `BatchQueueForegroundService.processQueue` executes `executeTask(task)` and then calls `dependencies.historyRepository.insert(...)` before `markTaskSuccess`; an ordinary history-DAO failure enters the outer catch and marks a successfully published output as FAILED, risking misleading retries. Next: isolate non-cancellation history-recording failures from conversion success, preserve cancellation propagation and correct terminal state/output path/counters, add focused JVM tests, and validate exact implementation SHA via authoritative PR CI. PR #1 stays draft/unmerged; main untouched.

## Cycle 069 active
### Epic E069 — Batch conversion result integrity
#### Feature F071 — Separate history persistence from conversion outcome
- [ACTIVE / P1] `US-R069-P1-01A` — A failed history insert must not downgrade an already produced output to FAILED; retain cancellation and output/counter semantics, add app JVM regressions and exact-SHA PR CI.


## Cycle 069 closure — 2026-10-12 UTC
- [COMPLETE / PR CI #618] `US-R069-P1-01A` — History DAO insertion errors cannot downgrade a published conversion output. Exact SHA `5948a4b825fc23adcbb10ca54353af60fcbc8349`, API `38188235518` SUCCESS, eight gates. Six focused app JVM tests across helper/terminal behavior; no device evidence.
- [NEXT / P1] `US-R070-P1-01A` — Preserve published output when optional history insertion is canceled.

## Cycle 070 active
### Epic E070 — Published batch output cancellation integrity
#### Feature F072 — Commit conversion success before optional history
- [ACTIVE / P1] `US-R070-P1-01A` — Mark successful conversion output before suspendable history persistence; propagate cancellation to remaining queued tasks without downgrading published result; add deterministic JVM tests and exact-SHA authoritative PR CI.


## Cycle 070 closure / Cycle 071 activation — 2026-10-12 UTC
- [COMPLETE / PR CI #629] `US-R070-P1-01A`: exact code/test SHA `149918edaafee92f2e916c26dbfb9b5b104093a5`, API `38263750211` SUCCESS, all eight configured gates; published conversion success precedes optional history suspension. No device evidence.

## Cycle 071 active
### Epic E071 — Temporary-file cleanup integrity
#### Feature F073 — Protect fresh temporary files during memory pressure
- [ACTIVE / P1] `US-R071-P1-01A` — `DocForgeApp.onTrimMemory` currently requests zero-age cleanup, risking removal of freshly created `docforge_` files not yet registered as active. Introduce a conservative age floor and deterministic tests for fresh, stale, registered files; preserve stale cleanup. Exact-SHA PR CI required.


## Cycle 071 closure / Cycle 072 activation — 2026-10-12 UTC
- [COMPLETE / PR CI #632] `US-R071-P1-01A`: exact SHA `1454cde0de00b170fad27cb82a9fdf7b401b3bf6`, API `38270554697` SUCCESS, all eight configured gates. Thirty-minute cleanup floor and six focused JVM regressions. No device evidence.

## Cycle 072 active
### Epic E072 — Temporary-file registry synchronization
#### Feature F074 — Atomic active-file guard during cleanup
- [ACTIVE / P1] `US-R072-P1-01A` — Eliminate the check-then-delete race between `ActiveTempFileRegistry.isActive` and stale-file deletion. Synchronize registration with deletion, add deterministic concurrent tests, preserve existing age floor, validate exact-SHA authoritative PR CI. PR #1 draft; main untouched.

## Cycle 072 closure / Cycle 073 activation — 2026-10-12 UTC
- [COMPLETE / PR CI #637] `US-R072-P1-01A`: exact code/test SHA `4d4a9173ef9c996632d954c700b4f46f08bf66b2`, API `38297024163` SUCCESS, all eight gates. Atomic registration/deletion and deterministic JVM regression coverage. No device evidence.

## Cycle 073 active
### Epic E073 — Shared temporary-file ownership safety
#### Feature F075 — Reference-count overlapping temp-file owners
- [ACTIVE / P1] `US-R073-P1-01A` — Prevent the first unregister from unprotecting a path still registered by another owner. Preserve atomic deletion guard; add deterministic JVM tests for nested/concurrent registrations, final release, and cleanup. Exact-SHA PR CI required; PR #1 draft, main untouched.

## Cycle 074 closure / Cycle 075 activation — 2026-10-14 UTC
- [COMPLETE / PR CI #648] `US-R074-P1-01A` (`E074/F076`): code/test SHA `4c552177c69acd8d2e776e3c3c79b3bdf16a352c`; authoritative PR Actions API `38390851641` SUCCESS, eight gates.
- [ACTIVE / P1] `US-R075-P1-01A` (`E075/F077`): protect overlapping owners during `PdfIoUtils` cache-copy cleanup, avoid double unregister on copy failure, preserve cancellation and original errors; add focused JVM regressions and validate exact code/test SHA through draft-PR CI. No production mutation before activation. Main untouched.


## Cycle 076 closure — 2026-10-14 UTC
- [COMPLETE / PR CI #672] `US-R076-P1-01A` — Output name validation and same-process staged-publication serialization. Exact code/test SHA `a5c6300b352726fdc2796baad9e0c67c8ed8057b`; authoritative PR Actions `38641360760` SUCCESS, all eight gates. Five focused JVM regressions committed. No device or cross-process guarantee.

## Cycle 077 active
### Epic E077 — Symlink-safe output allocation
#### Feature F079 — Treat symlink destinations as occupied
- [ACTIVE / P1] `US-R077-P1-01A` — Evidence: `resolveNonConflictingFile` uses `File.exists()` to select a destination; a dangling symbolic link reports false, so a caller opening that returned path for writing can follow the link outside the intended directory. A standalone Java 21 filesystem reproduction created an outside file through such a dangling link; this is local evidence, not Android CI. Next exact mutation: treat symbolic-link path entries (including dangling links) as occupied during direct and staged output name allocation; preserve ordinary numbered collision behavior and prevent outside writes; add deterministic core PDF JVM symlink regressions, then obtain authoritative exact-SHA draft-PR CI. Keep PR #1 draft/unmerged; never modify main.


## Cycle 077 closure / Cycle 078 active — 2026-10-15 UTC
- [COMPLETE / PR CI #675] `US-R077-P1-01A` (`E077/F079`): exact code/test SHA `218e818d53ab3aaad45906fedf3e1912f79f7fd7` passed authoritative PR Actions API `38648563229`, all eight configured gates. Three core PDF JVM regressions cover dangling symlink output-name allocation. No device/emulator or post-allocation race guarantee.

### Epic E078 — Output-directory symlink boundary
#### Feature F080 — Reject symlinked leaf output directories before staging
- [ACTIVE / P1] `US-R078-P1-01A`: reject a symbolic-link leaf output directory before creating directories or invoking any staged writer; apply the same boundary to direct output-name resolution. Add focused core PDF JVM tests for absolute, relative and dangling links, no outside mutation, and normal directory output. Validate exact code/test SHA via draft-PR CI. This guard does not claim ancestor-symlink or post-check swap protection. Keep PR #1 draft and main unchanged.

## Cycle 078 closure / Cycle 079 active — 2026-10-15 UTC
- [COMPLETE / PR CI #680] `US-R078-P1-01A` (`E078/F080`): code/test SHA `2667e64ceaf2953b5410948ce3d95b54764634fa` passed authoritative draft-PR Actions API `38671818333`, all eight gates. Leaf output directory symlink rejection and five focused core PDF tests; no device, emulator or post-check swap guarantee.
### Epic E079 — Staging publication integrity
#### Feature F081 — Validate staged entries before publication
- [ACTIVE / P1] `US-R079-P1-01A`: reject staging-file symbolic-link and nonregular-file substitutions using NOFOLLOW_LINKS after each writer and before publication; preserve external targets and normal staged-output behavior. Add focused core PDF JVM regressions, validate exact code/test SHA through authoritative draft-PR CI. Residual hard-link aliasing, post-validation substitution and nonempty-directory cleanup need explicit follow-up. PR #1 draft/unmerged; main untouched.


## Cycle 079 closure / Cycle 080 activation — 2026-10-15 UTC
- Cycle 079 `E079/F081/US-R079-P1-01A` COMPLETE. Exact production/test SHA `c4b2420499add93a3ea41a7027cdacf9414b901a` passed authoritative draft-PR CI #691 / API `38803553526`, job `116659166895`, all eight gates: core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, Android lint. Seven committed core PDF JVM regressions. Paired push #690 / API `38803546956` was cancelled by concurrency and is not completion evidence. No emulator, device, benchmark, hard-link protection, or post-check race guarantee.
- Cycle 080 `E080/F082/US-R080-P1-01A` ACTIVE / INCOMPLETE. Evidence: `PdfIoUtils.kt` calls `stagedFile.delete()` for cleanup, which cannot remove a nonempty substituted directory, leaving orphaned staging content. Next: add no-follow recursive cleanup restricted to generated staging paths and focused core PDF JVM tests covering nested directories, nested symlinks, target preservation, batch failure and normal output, then exact-SHA draft-PR CI. Distinct remaining work: post-check substitution race and hard-link aliasing. PR #1 stays draft/unmerged; main untouched.


## Cycle 081 completion / Cycle 082 activation — 2026-10-16 UTC
- [COMPLETE / PR CI #705] `US-R081-P1-01A` — Publication-time NOFOLLOW recheck and available file-key comparison after rename, fail-closed cleanup for detected substitutions; six core PDF JVM regressions. Exact code/test SHA `cf43b166f91f2f02838461121288b84b48b38ba2`, authoritative PR API `38883604993` SUCCESS all eight gates. Push #704 cancelled by concurrency.
- [ACTIVE / P1] `US-R082-P1-01A` — Audit and mitigate staging hard-link aliasing without relying on unsupported Android filesystem facilities.

### Epic E082 — Staging hard-link alias integrity
#### Feature F084 — Detect hard-linked staged outputs
- [ACTIVE / P1] `US-R082-P1-01A` — Prove hard-link swap case, verify API 26+ support for link count / secure file handles, commit compatible regression-backed mitigation, run exact-head PR CI. Post-check races and ancestor swaps remain separately tracked.

## Cycle 082 closure / Cycle 083 active — 2026-10-16 UTC
- [COMPLETE / PR CI #708] `US-R082-P1-01A` (`E082/F084`): exact code/test SHA `5adbc2022c21dff292e39fef469f848e27888876`, API `38887203020` SUCCESS, all eight configured gates. Four core PDF JVM tests. Known hard-link aliases rejected where `unix:nlink` is supported; no device evidence or full race-safety claim.

### Epic E083 — Output-directory ancestry integrity
#### Feature F085 — Bound ancestor symlink redirection
- [ACTIVE / P1] `US-R083-P1-01A` — Reproduce ancestor-symlink redirection; assess Android storage alias compatibility; implement narrowly bounded guard and focused JVM tests; exact-SHA draft-PR CI. Main untouched; PR #1 draft.


## Cycle 083 closure / Cycle 084 activation — 2026-10-16 UTC
- [COMPLETE / PR CI #726] `US-R083-P1-01A` (`E083/F085`): lexical ancestor symlink guard, Android-supplied root canonicalization and four core PDF JVM regressions. Exact code/test SHA `90de62ed6d9d95d06851b639f3cba36589f8539c` passed draft-PR Actions API `38898836103`, job `116966393599`, all eight gates. CI #724 failed due duplicate Kotlin suppression annotation and was repaired before #726. No emulator/device or cross-process race-freedom claim.
### Epic E084 — Settings output-folder symlink integrity
#### Feature F086 — Reject linked output-folder leaves at resolution
- [COMPLETE / PR CI #734] `US-R084-P1-01A` — `DocForgeSettingsStore.resolveOutputDirectory` currently accepts symlinked configured child folders via `File.isDirectory` (public and fallback), which follows links. Add bounded NOFOLLOW guard to prevent resolver returning a symlinked leaf; preserve canonicalized platform roots and normal output paths. Add focused core-domain JVM regressions, validate exact code/test SHA in draft PR CI. No code mutation before canonical activation. PR #1 stays draft; main untouched.


## Cycle 084 authoritative completion — 2026-10-16 UTC
- `E084/F086/US-R084-P1-01A` COMPLETE for bounded configured-folder symlink-leaf checks. Exact code/test SHA `0fe410393b950317fcaa390ada107fb4624f080e` passed draft-PR Actions #734 / API `38904369991`, job `116984725783`. All nine gates SUCCESS: domain JVM, PDF JVM, converter JVM, scanner JVM, PDF tools JVM, app JVM, debug APK, unsigned release/R8 and Android lint.
- Code `8fba5e6413d08fdccac81c9bd8eaf2fb3f76effb` rejects linked real/dangling child folders with NOFOLLOW checks; `58b06d0f983d82272a795ad6ec963985c10df110` adds four domain JVM regressions; `e78bea5b565658c3d173de4668e8f2b984950e52` adds the domain CI step. Initial #732 failed compiling the tests due missing JUnit; `0fe410393b950317fcaa390ada107fb4624f080e` adds `testImplementation(libs.junit4)` and #734 passed.
- Residual limitations: path checks are not atomic against concurrent directory swaps; Android OEM/device compatibility and performance are not certified. No physical-device or emulator evidence claimed. PR #1 remains draft/unmerged; `main` unchanged.

## Cycle 085 activation — 2026-10-16 UTC
### Epic E085 — Portable staging hard-link checks
#### Feature F087 — Android link-count fallback when NIO attributes are unavailable
- [ACTIVE / P1] `US-R085-P1-01A` — `PdfIoUtils.stagedHardLinkCount` returns unknown when NIO `unix:nlink` is unsupported, allowing a known class of staged hard-link aliases to escape detection. Add Android `Os.lstat(...).st_nlink` fallback and deterministic JVM fallback-selection regressions; preserve unknown-provider limitations and validate exact code/test SHA in draft-PR CI. Cycle 084 complete at `0fe410393b950317fcaa390ada107fb4624f080e` / CI #734. PR #1 stays draft; main untouched.

## Cycle 085 authoritative completion — 2026-10-16 UTC
- `E085/F087/US-R085-P1-01A` COMPLETE. Exact production/test SHA `af624de8ee4f7e1a50dd1be12e2a460aec2b267a` passed authoritative draft-PR Actions #740 / API `38909368135`, job `117001519196`, all nine gates SUCCESS: domain JVM, core PDF JVM, converter JVM, scanner JVM, PDF tools JVM, app JVM, debug APK, unsigned release/R8 and Android lint. Push #739 was cancelled by concurrency and is not validation evidence.
- Production `PdfIoUtils.stagedHardLinkCount` now falls back to Android `Os.lstat(...).st_nlink` when NIO `unix:nlink` is unavailable. Four committed JVM fallback-selection tests passed in core PDF CI. Host-only Kotlin probe 5/5 passed with synthetic Android stubs, not device validation.
- Residual: if both providers cannot report a link count, unknown remains unknown; neither lookup eliminates filesystem TOCTOU races. No emulator, physical-device, OEM compatibility or benchmark evidence claimed. PR #1 draft/unmerged; main unchanged.

### Epic E086 — Published-output rollback identity safety
#### Feature F088 — Avoid deleting replaced outputs during batch rollback
- [COMPLETE / PR CI #743] `US-R086-P1-01A` — Unconditional `Files.deleteIfExists(outputFile.toPath())` during batch rollback can remove a different file substituted at that path after publication. Host-only reproduction 4/4 confirms the hazard; add bounded NOFOLLOW identity-aware rollback and deterministic JVM tests, exact-SHA PR CI. No cross-process atomicity claim. PR #1 draft, main untouched.


## Cycle 086 closure / Cycle 087 activation — 2026-10-16 UTC
- [COMPLETE / PR CI #743] `US-R086-P1-01A` (`E086/F088`): Exact code/test SHA `58a62f6c1e277cab3e921c8e1c78c07cfd069e39` passed authoritative draft-PR GitHub Actions #743 / API `38915971783`, job `117025709017`, on 2026-10-16 UTC. All nine gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, and Android lint. Paired push #742 was cancelled and is not validation evidence. Six committed core PDF JVM regressions cover unchanged output, replacement regular file, symlink, directory, unknown identity, and missing output. No emulator, physical device, benchmark or atomic race-freedom evidence.

### Epic E087 — Non-destructive failed-publication cleanup
#### Feature F089 — Preserve foreign entries after rejected post-move publication
- [ACTIVE / P0] `US-R087-P0-01A` — `publishCheckedStagedOutput` currently calls recursive `deleteStagedEntry(candidate)` after post-move rejection. A substituted nonempty directory can be moved to the final path then recursively erased, losing unrelated content. Add ownership-aware, non-recursive fail-closed cleanup and deterministic core PDF JVM tests; preserve ordinary publish/collision behavior. Validate exact code/test SHA via draft-PR CI. Remaining TOCTOU and foreign-path exposure risks must be documented; PR #1 draft, main untouched.


## Cycle 087 validation and closure — 2026-10-16 UTC
- `E087/F089/US-R087-P0-01A` COMPLETE for bounded identity-aware, non-recursive failed-publication cleanup. Exact code/test head `b455c28156d5566d788e09876ed127df10e44f13` passed draft-PR Actions #748 / API `38943994491`, job `117118776491`. All nine configured gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8 and Android lint.
- Source `publishCheckedStagedOutput` now attempts `rollbackPublishedOutputIfUnchanged(candidate, sourceAttributes.fileKey())` after failed post-move validation rather than recursively deleting a substituted directory; file-key unknown or mismatched entries remain in place. JVM swap regressions confirm preservation of substituted nonempty directory children, links and regular files, alongside normal publication and occupied-name behavior. Final hard-link alias expectation adjusted in `b455c281` after #746 failure; #748 passed.
- Remaining limitations: foreign entries may remain visible at the candidate path after rejection; no atomic filesystem identity-and-unlink, full TOCTOU safety, Android emulator/device evidence or race-free guarantee. No reviewer approval implied. PR #1 draft/unmerged; `main` unchanged.

### Epic E089 — Staged-file identity continuity
#### Feature F091 — Validate original staging identity before publication
- [COMPLETE / PR CI API 38988957597] `US-R089-P1-01A`: exact code/test SHA `9c6ac1a5d87b136449d596e5ce9ae8b31e287191`, all nine gates SUCCESS; three core PDF JVM regressions. Known file-key substitutions rejected at writer, batch and publication boundaries; unknown keys and non-atomic filesystem races remain limitations.

### Epic E090 — Portable staged-file identity
#### Feature F092 — Fallback identity when NIO fileKey is unavailable
- [ACTIVE / P1] `US-R090-P1-01A`: source `requireOriginalStagedIdentity` returns early when original fileKey is null, so replacement cannot be detected by that guard. Inspect Android `Os.lstat` inode/device identity support on minSdk 26; add focused JVM fallback-selection and mismatch regressions; implement only a compatible bounded guard, validate exact code/test SHA in draft-PR CI. Never claim atomic race safety or device testing. PR #1 draft; main untouched.

## Cycle 090 completion / Cycle 091 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #766] `US-R090-P1-01A` (`E090/F092`). Exact code/test SHA `8d6d1fb2e7c3567d174675f8324226002aef2bcc`; authoritative draft-PR Actions #766 / API `38991137058`, job `117273629538`, SUCCESS all nine gates (core domain/PDF/converter/scanner/PDF-tools/app JVM, debug APK, unsigned release/R8, Android lint). Paired push #765 was cancelled, not acceptance evidence. Four injected JVM regressions for NIO/Android identity fallback. JVM tests inject/simulate Android identity; no emulator, physical-device, OEM/provider, benchmark, atomic check/delete or complete TOCTOU/race-freedom evidence. Unknown identities remain unproven.
### Epic E091 — Identity-aware cleanup fallback
#### Feature F093 — Cleanup and rollback on null-key providers
- [ACTIVE / P1] `US-R091-P1-01A`: NIO-null keys prevent owned-stage deletion and batch rollback even when Android device/inode identity exists. Add narrowly scoped NOFOLLOW ownership fallback and deterministic injected core PDF JVM regressions; preserve foreign replacements, unknown-key fail-closed, writer errors/cancellation and normal collision behavior. Require exact-SHA authoritative PR CI; PR #1 draft, `main` untouched.

## Cycle 091 validated candidate — 2026-10-17 UTC
- [ACTIVE / PR CI #770 SUCCESS] `US-R091-P1-01A` (`E091/F093`): Exact production/test SHA `2552fc0137e584de5e134eecc9821f8d76544a67` passed authoritative draft-PR Actions #770 / API `39002954473`, job `117309527721`, on 2026-10-17 UTC. All nine configured gates SUCCESS: core domain, PDF, converter, scanner, PDF-tools and app JVM tests, debug APK, unsigned release/R8, and Android lint. Paired push #769 / API `39002951021` was cancelled by concurrency and is not validation evidence.
- Remaining acceptance gap: direct injected JVM tests cover fallback ownership, substitution, links/directories, missing/unknown identity and NIO precedence, but no new end-to-end batch-rollback or staging-cancellation regression was added for the null-key scenario. Keep Cycle 091 ACTIVE until these paths are covered or explicitly scoped/deferred with evidence. Android OEM/provider compatibility, emulator/device behavior and atomic check/unlink remain unverified.
- Next: add batch rollback/cancellation regression coverage, validate exact new SHA, reconcile canonical docs and cycle report before closure. PR #1 draft; main untouched.


## Cycle 091 completion / Cycle 092 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #773] Cycle 091 E091/F093/US-R091-P1-01A COMPLETE: exact production/test SHA `a72ec713043f8dc2490e8fcbbfda48c144ebc270` passed authoritative draft-PR Actions #773 / API `39008317260`, job `117325244525`, on 2026-10-17 UTC. All nine configured gates SUCCESS (domain, PDF, converter, scanner, PDF-tools, app JVM tests; debug APK; unsigned release/R8; Android lint). Paired push #772 / API `39008314535` was cancelled by concurrency, not validation evidence. Earlier code/test SHA `2552fc0137e584de5e134eecc9821f8d76544a67` passed #770 / API `39002954473`. Latest commit adds an injectable NIO/Android identity-reader seam and four integrated null-NIO-key JVM regressions covering multi-output rollback, cancellation, substituted foreign staging preservation and numbered collision. Tests are host simulations, not Android `Os.lstat` device verification. Unknown identities, non-atomic identity/check/delete races and OEM/provider behavior remain unproven.
### Epic E092 — Staging identity-capture failure safety
#### Feature F094 — Handle staging identity reader failures safely
- [ACTIVE / P1] `US-R092-P1-01A`: Cycle 092 E092/F094/US-R092-P1-01A ACTIVE / INCOMPLETE before production/test mutation. Evidence in `core/pdf/src/main/java/com/docforge/core/pdf/PdfIoUtils.kt` at `a72ec713043f8dc2490e8fcbbfda48c144ebc270`: `withStagedOutputFilesUsingIdentityReaders` creates a `.anydoc_stage_` file, then invokes `readNioFileKey` and optional `readAndroidIdentity` BEFORE entering the writer `try/catch` and enclosing `finally`. An exception from an identity reader escapes without the established ownership-aware cleanup path; the writer is not invoked, but the stage may be orphaned. First: reproduce with injected reader failures and verify no unrelated file is removed; implement the smallest safe capture/fallback and guarded cleanup without deleting entries whose ownership cannot be established; add core PDF JVM regressions, exact-SHA draft-PR CI, then reconcile documents. Do not claim cleanup is possible when both identity sources are unknown. PR #1 draft/unmerged; main untouched.

- [ACTIVE / Cycle 092] Code SHA `072eb963` passed CI #778 attempt 2; two injected-reader regression tests remain pending publication and exact-SHA CI.

## Cycle 092 completion / Cycle 093 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #787] `US-R092-P1-01A` (`E092/F094`): exact production/test SHA `7036384a7cbd37c7c3ebfbee0f6979a3968235cf` passed draft-PR Actions API `39028698857`, job `117385580946`, all nine gates SUCCESS. Production guards staging identity-reader failure; three committed JVM test files cover later-request cleanup, Android-reader failure and preservation of foreign staging entries. Unknown-identity stages may remain by design; no device/emulator or race-freedom claim.

### Epic E093 — Unknown staging identity publication safety
#### Feature F095 — Reject unverified staged-file substitution
- [ACTIVE / P1] `US-R093-P1-01A`: with both NIO fileKey and Android identity absent, original staged-file identity continuity is unknown and `requireOriginalStagedIdentity` returns early. Reproduce a regular-file substitution under injected null identity readers, determine compatible fail-closed behavior, add regression coverage and implement the smallest safe mitigation if supported. Validate exact SHA in authoritative draft-PR CI. Do not delete unknown/foreign entries. PR #1 draft; main untouched.

## Cycle 093 completion / Cycle 094 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #790] `US-R093-P1-01A` (`E093/F095`): exact code/test SHA `e4960f63dbcc055215e4f60b6716f8e543aa6d4f` passed draft-PR Actions API `39033315120`, job `117398111312`, all nine configured gates SUCCESS. Production batch staging fails closed when both identity providers return null, before writer execution. Two committed core PDF JVM regressions cover no-writer/no-publication and preservation of substituted foreign regular file. Unknown staging entries may remain; no Android device/OEM or atomic race evidence.

### Epic E094 — Direct publication helper identity contract
#### Feature F096 — Verify direct publication identity requirements
- [ACTIVE / P2] `US-R094-P2-01A`: internal `publishCheckedStagedOutput` accepts omitted original identity and permits unknown continuity (`null != false`). Audit direct helper call sites and test contracts; add focused regression for unknown-identity direct invocation, decide compatible guard/explicit identity requirement, and obtain exact-SHA authoritative draft-PR CI. Distinguish internal helper risk from production batch path. PR #1 draft, main untouched.

## Cycle 094 completion / Cycle 095 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #793] `US-R094-P2-01A` (`E094/F096`): direct publication now captures NIO/Android identity when omitted, fails closed if both unknown, verifies continuity after rename; three new core PDF JVM regressions. Exact SHA `f9254345d91125c9f6304dc95f22bb370bdb23a0` passed draft-PR Actions API `39036699340`, job `117407938037`, all nine gates SUCCESS. Residual non-atomic path race and foreign final-path entry after rejection; no device proof.

### Epic E095 — Small-heap PDF compare raster safety
#### Feature F097 — Audit heap-budget minimum against runtime ceiling
- [ACTIVE / P2] `US-R095-P2-01A`: `CompareRasterBudget.heapAwareBudgetBytes` returns at least 12 MiB even for 32 MiB max heaps, and current JVM test codifies this floor. Assess allocation pressure/quality on small heaps, add boundary tests, choose a compatible safe cap if warranted, and validate exact-SHA draft-PR CI. No emulator/device benchmark evidence; main untouched, PR #1 draft.


## Cycle 095 authoritative completion — 2026-10-17 UTC
- `E095/F097/US-R095-P2-01A` COMPLETE for bounded small-heap compare raster budgeting. Exact code/test SHA `34c7539e50630f53c365f877583506e40a5cf257` passed draft-PR Actions #796 / API `39041350384`. Documentation-head `43fe075dfe7b90ac10e9b479ff4aa8f15ef5b02e` passed Actions #797 / API `39043002749`, job `117426099487`, all nine configured gates SUCCESS.
- `CompareRasterBudget.heapAwareBudgetBytes` now limits budget to 1/8 of heap capped at 48 MiB, with a 12-byte floor for three 1-pixel ARGB_8888 bitmaps rather than a disproportionate 12 MiB minimum. JVM regressions assert 16/32/64 MiB boundaries, fitting three bitmaps and impossible minimum checks. No emulator, physical-device, performance/OOM measurements or universal memory safety claim.
- Next: audit the next distinct evidence-backed risk, activate a new cycle before production mutation. PR #1 draft/unmerged; main untouched.

## Cycle 095 completion / Cycle 096 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #796] `US-R095-P2-01A` (`E095/F097`): code/test SHA `34c7539e50630f53c365f877583506e40a5cf257` passed API `39041350384`, job `117422336197`, nine gates SUCCESS. Docs-head #797 SUCCESS. 12-byte minimum and small-heap JVM regressions; no measured device OOM or emulator evidence.
### Epic E096 — PDF compare remaining-heap raster safety
#### Feature F098 — Account for already-occupied Java heap
- [ACTIVE / P2] `US-R096-P2-01A`: compare currently budgets from max Java heap without subtracting occupied heap. Add deterministic headroom boundary tests and conservative remaining-heap cap, validate exact code/test SHA; PR #1 draft/unmerged, main untouched.


## Cycle 096 authoritative completion / Cycle 097 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #806] `US-R096-P2-01A` (`E096/F098`). Exact code/test SHA `386112813f01ea8daea2ee1cf7fc7bac28b2f5a1` passed authoritative draft-PR GitHub Actions #806 / API `39046842382`, job `117437154272`, on 2026-10-17 UTC. All nine configured gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools and app JVM tests; debug APK; unsigned release/R8; Android lint. Production `e78388d700b199049349155cb4c101688747c149` budgets raster bytes from remaining Java heap estimated by `maxMemory - (totalMemory - freeMemory)`; test `38611281` adds three focused JVM regressions for occupied, inconsistent and extreme occupancy plus combined bitmap fit. No emulator, physical-device, native-memory benchmark, measured OOM prevention or complete memory safety claimed.
### Epic E097 — PDF compare render-scale input safety
#### Feature F099 — Normalize non-finite render-scale input
- [ACTIVE / P2] `US-R097-P2-01A`: Cycle 097 `E097/F099/US-R097-P2-01A` ACTIVE / INCOMPLETE before any production/test mutation. Source at validated `38611281`: `PdfCompareTool.compare` uses `renderScale.coerceIn(0.5f, 4f)`; Kotlin `Float.NaN.coerceIn` remains NaN, so `(page.width * scale).toInt().coerceAtLeast(1)` yields 1 pixel, silently degrading comparison for a non-finite caller input. Host Kotlin reproduction observed NaN -> 1 px; not Android validation. First add a narrow finite/default render-scale normalization seam and deterministic JVM tests for NaN, +/-Infinity and ordinary values, without altering normal quality or cancellation. Validate exact code/test SHA via draft-PR CI. PR #1 draft/unmerged; `main` untouched.


## Cycle 098 authoritative completion / Cycle 099 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #817] `US-R098-P2-01A` (`E098/F100`): exact code/test SHA `dcc142664e7d09061003c212102efca4090734b1` passed draft-PR Actions API `39060565666`, job `117477503838`, all nine configured gates SUCCESS. Missing-page red overlay and five focused JVM regressions. #815 failed Kotlin compilation and was superseded by `dcc142664e7d09061003c212102efca4090734b1`. No Android bitmap visual/emulator/device evidence.
### Epic E099 — PDF compare descriptor lifecycle safety
#### Feature F101 — Close the first input descriptor on second open failure
- [ACTIVE / P1] `US-R099-P1-01A`: `PdfCompareTool.compare` opens left and right `ParcelFileDescriptor` before either enters `use`; a failure opening the second leaves the first unclosed. Implement nested open/use ownership with deterministic JVM close-on-failure tests; obtain exact-SHA draft-PR CI. PR #1 draft/unmerged; main untouched.


## Cycle 099 authoritative completion / Cycle 100 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #822] `US-R099-P1-01A` (`E099/F101`): Exact Cycle 099 production/test SHA `f621c4ecaf779c7791d320d287068c71e124e5ef` passed authoritative draft-PR Actions #822 / API `39063440811`, job `117486041589`, on 2026-10-17 UTC. All nine configured gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools and app JVM tests, debug APK, unsigned release/R8, Android lint. Production nested Closeable acquisition helper was introduced at `234a35d5f6340e85de621186c153dd52dfd74e94`; four host JVM lifecycle regressions at `f621c4ecaf779c7791d320d287068c71e124e5ef` cover first-open error, second-open error, block error and success, with close-on-failure and original exception propagation. No Android device/emulator/OEM/benchmark or complete resource-safety proof; PR #1 stays draft/unmerged, main untouched.
### Epic E100 — PDF compare output page geometry
#### Feature F102 — Preserve source page dimensions independent of raster scale
- [ACTIVE / P2] `US-R100-P2-01A`: Cycle 100 `E100/F102/US-R100-P2-01A` ACTIVE / INCOMPLETE before any production/test mutation. Source evidence in `core/pdf/src/main/java/com/docforge/core/pdf/PdfCompareTool.kt`: `requestedSize` multiplies source PDF page width/height by renderScale (default 1.5f), but `PdfDocument.PageInfo.Builder(diffBmp.width, diffBmp.height,...)` uses those raster pixel dimensions as PDF page dimensions in points. Thus a 595x842-point page at 1.5x is emitted near 892x1263 points, changing physical page geometry. This is a source-level output-fidelity concern, not device-verified behavior. First: verify intended output dimensions for equal, unequal and missing input pages; introduce a pure JVM-testable output-geometry calculation; preserve high-resolution raster diff while writing to original-size PDF page bounds using scaled canvas drawing if compatible; add deterministic JVM regressions and exact-SHA draft-PR CI. Do not claim Android visual/OEM proof.

## Cycle 100 completion / Cycle 101 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #825] `US-R100-P2-01A` (`E100/F102`): exact code/test SHA `42a9e068a6f6ad62679c4e85bcb91c6dc46767dc` passed draft-PR Actions API `39066975014`, job `117496569062`, all nine gates SUCCESS. Original source page points are preserved independently of raster scale; six JVM geometry regressions. No Android device/visual evidence.
### Epic E101 — PDF compare failure propagation
#### Feature F103 — Preserve primary errors during PDF document cleanup
- [ACTIVE / P2] `US-R101-P2-01A`: inspect PdfDocument startPage/drawBitmap/finishPage and unconditional finally close for original-error masking when cleanup also throws. Verify platform contract and existing tests, add deterministic JVM regression seam, implement smallest compatible fix only if justified, exact-SHA draft-PR CI. PR #1 draft; main untouched.


## Cycle 101 completion / Cycle 102 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #828] `US-R101-P2-01A` (`E101/F103`): Exact production/test SHA `d05bde850d46172ddb90463f7a2b0a65e92240f4` passed authoritative draft-PR Actions #828 / API `39071028675`, job `117508502508`, on 2026-10-17 UTC. All nine configured gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools, app JVM tests, debug APK, unsigned release/R8, Android lint. Six host JVM lifecycle regressions in `PdfCompareDocumentLifecycleTest.kt` verify normal close, draw/finish/cancellation failure preservation with suppressed close errors, close failure after successful write, and identical primary/cleanup throwable avoiding self-suppression. Production `withClosePreservingPrimaryFailure` wraps `PdfDocument` use. No emulator, physical device, OEM, visual/benchmark/peak heap or complete race-freedom proof.
### Epic E102 — PDF compare bitmap cleanup exception safety
#### Feature F104 — Preserve primary failure under self-suppression
- [ACTIVE / P2] `US-R102-P2-01A`: Source-level evidence in `PdfCompareTool.kt`: `withCleanupOnFailure` calls `failure.addSuppressed(cleanupFailure)` without checking identity. If the injected cleanup callback throws the exact same Throwable instance as the original block, Java rejects self-suppression with `IllegalArgumentException`, masking the primary error. This is a deterministic helper edge case, not an observed Android Bitmap.recycle failure. Add focused JVM tests and smallest compatible guard; preserve bitmap recycling and original error; validate exact code/test SHA in draft-PR CI.


## Cycle 102 completion / Cycle 103 activation — 2026-10-17 UTC
- [COMPLETE / PR CI #831] `US-R102-P2-01A` (`E102/F104`): Exact production/test SHA `6f130bdbe4b4b1cd5277a4061bc5704f8d21a21f` passed authoritative draft-PR Actions #831 / API `39075310686`, job `117521147882`, with all nine configured gates SUCCESS (core domain, PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, Android lint). Docs-head #832 / API `39077710177` and #833 / API `39079471345` also succeeded. Five host JVM regressions cover normal cleanup, original-error preservation, distinct cleanup suppression, identical Throwable self-suppression, and cancellation. No Android device/emulator/OEM/benchmark/peak heap or complete race-freedom evidence.
### Epic E103 — PDF compare page-render cleanup safety
#### Feature F105 — Preserve primary rendering failure during bitmap recycling
- [ACTIVE / P2] `US-R103-P2-01A`: Cycle 103 `E103/F105/US-R103-P2-01A` ACTIVE / INCOMPLETE before any production mutation. Source evidence: `PdfCompareTool.renderPage` catch block calls `bitmap.recycle()` directly before rethrowing `page.render` failure; a secondary recycle failure could mask the primary rendering exception. Source-level risk only, not an observed Android error. Inspect callers and test seam; reuse `withCleanupOnFailure` if compatible, add focused host JVM regressions, validate exact code/test SHA via draft-PR CI. Preserve cancellation and bitmap ownership. PR #1 remains draft/unmerged; main untouched.


## Cycle 103 completion / Cycle 104 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #835] `US-R103-P2-01A`: Cycle 103 E103/F105/US-R103-P2-01A COMPLETE: exact production/test SHA `372629e02698968168f521850e674113821dd1ed` passed draft-PR Actions #835/API `39084723314`, job `117548786248`, nine configured gates SUCCESS (six JVM regression tests). Push run `39084721401` cancelled. No Android device/emulator/benchmark evidence. PR #1 draft, main untouched.
### Epic E104 — PDF compare per-page bitmap cleanup safety
#### Feature F106 — Attempt all bitmap cleanup and preserve primary failures
- [ACTIVE / P2] `US-R104-P2-01A`: Cycle 104 E104/F106/US-R104-P2-01A ACTIVE before code mutation. `PdfCompareTool.compare` per-page finally (lines 275-279) recycles left, right and diff bitmaps sequentially. A recycle exception can skip remaining cleanup and mask the primary page-processing failure. This is a source-level risk, not an observed Android recycle exception. Add a small JVM-testable all-bitmap cleanup helper preserving primary errors/cancellation, suppressing cleanup failures without self-suppression, attempting all non-null bitmaps, and retaining success behavior. Obtain exact-SHA draft-PR CI.


## Cycle 104 authoritative completion / Cycle 105 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #839] `US-R104-P2-01A` (`E104/F106`): exact production/test SHA `e645be3fcdd48b06d9862cddd1c6f87f0ab98b68` passed draft-PR Actions #839 / API `39086774123`, job `117554919844`, all nine configured gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools and app JVM tests, debug APK, unsigned release/R8, Android lint. Six focused core PDF JVM tests cover all-resource recycle attempts, success, multiple cleanup failures, primary error, identical throwable, and cancellation. Source-level cleanup risk only; no Android device/emulator/OEM/benchmark evidence. PR #1 remains draft, main untouched.
### Epic E105 — PDF compare cancellation responsiveness for very wide pages
#### Feature F107 — Bound per-row cancellation latency during pixel diff
- [ACTIVE / P2] `US-R105-P2-01A`: `PdfCompareTool.diffBitmaps` checks cancellation once per row via `forEachDiffRow`, but each row can contain millions of pixels under the raster memory budget; a single extremely wide page can delay cancellation for a full pixel scan. Source-level risk, not device-measured latency. Introduce a small inline, JVM-testable column-iteration helper that checks cancellation periodically (at x=0 and every 4096 pixels) without changing pixel comparisons or output. Add deterministic JVM tests for normal iteration, checkpoint boundaries, early cancellation, zero-width handling, and wide-row bounded checks. Validate exact production/test SHA with draft-PR CI; no device/benchmark claims.

## Cycle 105 authoritative completion / Cycle 106 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #842] `US-R105-P2-01A` (`E105/F107`): exact production/test SHA `b836e2e0ec17f50db35f8b1e6143715c713269d1` passed authoritative draft-PR Actions #842/API `39090705908`, job `117566684347`, all nine configured gates SUCCESS. Six host JVM regressions cover ordinary/empty rows, 4096-column boundaries and early cancellation; `forEachDiffColumn` now checks cancellation at x=0 and each 4096 columns. No Android emulator/device/OEM/benchmark/latency measurement. PR #1 draft/unmerged, main untouched.
### Epic E106 — PDF compare output filename safety
#### Feature F108 — Bound sanitized filename length
- [ACTIVE / P2] `US-R106-P2-01A`: source evidence: `PdfCompareTool.compare` replaces unsafe characters in `outputName` but does not cap its length; the staging publisher constructs `baseName.pdf` and may append numeric collision suffixes. Long names may exceed filesystem per-component limits. Audit shared filename policies/tests; implement a bounded ASCII basename sanitizer that preserves normal names and collision behavior, add focused JVM tests, validate exact-SHA PR CI. No device-observed failure claimed.


## Cycle 106 completion / Cycle 107 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #845] `US-R106-P2-01A` (`E106/F108`): code/test SHA `3e5f464aff7800dba908c3a746586b89fca32704` passed draft-PR Actions API `39093774353`, job `117576728460`, all nine gates. No device/emulator evidence.
### Epic E107 — PDF compare serialization cancellation
#### Feature F109 — Cooperative cancellation during staged byte writes
- [ACTIVE / P2] `US-R107-P2-01A`: `PdfCompareTool.compare` calls `pdfDoc.writeTo(FileOutputStream)` without cancellation checks during byte writes. Add cancellation-aware <=64 KiB chunked OutputStream and focused JVM tests; validate exact-SHA draft-PR CI. PR #1 draft/unmerged, main untouched.

## Cycle 107 authoritative completion / Cycle 108 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #852] `US-R107-P2-01A` (`E107/F109`): Cycle 107 `E107/F109/US-R107-P2-01A` COMPLETE: exact production/test SHA `82ceba75aecf724f85d4605f7a304cfa2d71b6ec` passed authoritative draft-PR Actions #852 / API `39097399571`, job `117586609368`, all nine configured gates SUCCESS: core domain/PDF/converter/scanner/PDF-tools/app JVM tests, debug APK, unsigned release/R8, Android lint. Eight focused JVM regressions cover byte fidelity, 64 KiB chunk boundaries, cancellation, invalid ranges, downstream IO exceptions and stream lifecycle. Isolated Kotlin host run also passed 8/8. No Android emulator/physical device/OEM/benchmark or measured cancellation-latency evidence.
### Epic E108 — PDF compare filename sanitization memory safety
#### Feature F110 — Bound allocation for very long user output names
- [ACTIVE / P2] `US-R108-P2-01A`: `sanitizedCompareOutputBaseName` replaces unsafe characters across the full input before taking 200 characters, potentially allocating large intermediate strings. Audit Java regex codepoint and UTF-16 surrogate behavior; implement equivalent bounded prefix sanitization and focused JVM tests, then exact-SHA draft-PR CI. PR #1 draft/unmerged, main untouched.


## Cycle 108 authoritative completion / Cycle 109 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #855] `US-R108-P2-01A` (`E108/F110`): exact production/test SHA `d7712402afd910c76ee3b692053ad4269f8735a3` passed authoritative draft-PR Actions #855 / API `39102640344`, job `117602763202` on 2026-10-18 UTC: all nine configured gates SUCCESS (core domain, core PDF, converter, scanner, PDF tools and app JVM tests, debug APK, unsigned release/R8, Android lint). Paired push `39102637960` was cancelled by concurrency, not acceptance evidence. Bounded ASCII filename sanitization preserves the 200-character cap, lazy blank fallback, Java regex codepoint semantics for valid surrogate pairs, unpaired surrogate replacement and numbered collision publication. Five added focused JVM test methods include 10,000 deterministic UTF-16 equivalence cases and 250,000 supplementary-codepoint input. No Android device/emulator/OEM, allocation benchmark or peak-memory proof.
### Epic E109 — PDF compare raster diff performance
#### Feature F111 — Bounded row-buffer pixel comparison
- [ACTIVE / P2] `US-R109-P2-01A`: E109/F111/US-R109-P2-01A — PDF compare per-pixel raster diff performance: `PdfCompareTool.diffBitmaps` calls `Bitmap.getPixel` twice and `Bitmap.setPixel` once for every pixel. Audit a bounded row-buffer alternative that preserves output pixels, diff percentages, cancellation boundaries and heap-aware memory budget; add deterministic JVM regression seams and exact-SHA draft-PR CI before completion. No measured device latency claim.


## Cycle 109 authoritative completion / Cycle 110 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #859] `US-R109-P2-01A` (`E109/F111`): Exact Cycle 109 code/test SHA `e0caa3073c9b3c269793f8a3f05d1d8e0035ee39` passed draft-PR Actions #859 / API `39105462244`, job `117610256645` on 2026-10-18 UTC: six JVM test groups, debug APK, unsigned release/R8 and Android lint all SUCCESS (nine configured gates). Bounded 1024-pixel ARGB chunks use 12 KiB transient buffers, preserve RGB threshold/grey and missing-page padding, cancellation every 4096 columns and difference percentages. Six focused JVM regressions cover cancellation boundaries, thresholds, randomized ARGB equivalence and white padding. No Android emulator/device/OEM/benchmark or measured speedup evidence.
### Epic E110 — PDF compare raster buffer efficiency
#### Feature F112 — Avoid redundant full-chunk white initialization
- [ACTIVE / P2] `US-R110-P2-01A`: Cycle 110 `E110/F112/US-R110-P2-01A` ACTIVE before production/test mutation. Source-level evidence at `e0caa3073c9b3c269793f8a3f05d1d8e0035ee39`: `PdfCompareTool.diffBitmaps.readChunk` fills all `count` buffer entries white even when `Bitmap.getPixels` overwrites the entire chunk. Audit partial-width and missing-row white padding; move white initialization to only the uncopied tail with a pure JVM-testable seam, preserve exact ARGB diff/percentages/cancellation/ownership, add focused regressions and exact-SHA draft-PR CI. No measured speedup claim.


## Cycle 110 authoritative completion / Cycle 111 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #862] `US-R110-P2-01A` (`E110/F112`): Cycle 110 `E110/F112/US-R110-P2-01A` COMPLETE: exact code/test SHA `26d932cc81810e59ab6b7352930db21105b780b4` passed authoritative draft-PR Actions #862 / API `39108188107`, job `117618514524`, on 2026-10-18 UTC. All nine configured gates SUCCESS: core domain/PDF/converter/scanner/PDF-tools/app JVM tests, debug APK, unsigned release/R8, Android lint. `readChunk` now initializes only missing white tail pixels after getPixels; seven deterministic JVM regressions cover full/partial/missing chunks, reused buffers, boundaries and diff equivalence. No device/emulator/OEM/benchmark/peak-heap or measured speedup evidence.
### Epic E111 — PDF compare raster initialization efficiency
#### Feature F113 — Verify complete output coverage before eliminating redundant erase
- [ACTIVE / P2] `US-R111-P2-01A`: Cycle 111 `E111/F113/US-R111-P2-01A` ACTIVE / INCOMPLETE before production/test mutation. Source evidence at `26d932cc81810e59ab6b7352930db21105b780b4`: `PdfCompareTool.diffBitmaps` calls `output.eraseColor(Color.WHITE)` before nested `forEachDiffRow(h)`/`forEachDiffChunk(w)` loops write all output pixels via `output.setPixels`. If coverage is complete, the full-raster erase is redundant. First prove full positive-width/height coverage with a pure JVM deterministic traversal seam and check cancellation/exception cleanup, then remove only redundant initialization if safe; add focused JVM regressions, authoritative exact-SHA draft-PR CI. Do not claim Android measured speedup or uninitialized pixels safe without coverage proof.


## Cycle 111 authoritative completion / Cycle 112 active — 2026-10-18 UTC
- [COMPLETE / PR CI #865] `US-R111-P2-01A` (`E111/F113`): Cycle 111 `E111/F113/US-R111-P2-01A` COMPLETE: exact production/test SHA `2a44f5eb09990bfcb43d1bf5902b90fc5a9285a5` passed authoritative draft-PR Actions #865 / API `39113962627` (job `117635806487`) on 2026-10-18 UTC; all nine configured gates SUCCESS. Output raster erase removed after complete chunk-coverage proof; eight focused JVM tests. No Android device/emulator/benchmark or measured speedup.
### Epic E112 — PDF comparison cancellation before raster allocation
#### Feature F114 — Preflight cancellation before diff bitmap allocation
- [ACTIVE / P2] `US-R112-P2-01A`: Cycle 112 `E112/F114/US-R112-P2-01A` ACTIVE / INCOMPLETE: avoid creating PDF comparison output bitmap when coroutine cancellation is already signalled. Add a preflight check immediately before both blank and normal diff bitmap allocations; missing-page copy already checks cancellation. Preserve output pixel fidelity, cancellation identity, bitmap cleanup and existing publication behavior. Add deterministic JVM tests and validate exact code/test SHA.
- Latest documentation-head CI #868 / API `39117502705` passed on `1bb5ce89e52a25617a918b9f4cdd339dfe472c7c`; not production validation for Cycle 112. PR #1 draft/unmerged; `main` untouched.


## Cycle 112 authoritative completion / Cycle 113 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #871] `US-R112-P2-01A` (`E112/F114`): Cycle 112 `E112/F114/US-R112-P2-01A` COMPLETE: exact production/test SHA `418c37ccaa3e91ce4c717c65538685a7f2dfdbd8` passed authoritative draft-PR Actions #871 / API `39119917326`, job `117653275621`, on 2026-10-18 UTC. All nine configured gates SUCCESS: six JVM test groups, debug APK, unsigned release/R8 and Android lint. Preflight cancellation now precedes normal and both-null PDF diff bitmap allocations; six focused JVM regressions cover ordering, cancelled no-allocation, exception identity, allocation failure and cleanup. Independent host helper checks 6/6 passed. No emulator, physical device, benchmark, measured allocation savings or latency proof.
### Epic E113 — PDF render-page allocation cancellation
#### Feature F115 — Preflight cancellation before rendered-page bitmap allocation
- [ACTIVE / P2] `US-R113-P2-01A`: Cycle 113 `E113/F115/US-R113-P2-01A` ACTIVE / INCOMPLETE: audit cancellation immediately before `renderPage` bitmap allocation, especially cancellation between left and right page rendering. At Cycle 112 code HEAD, `PdfCompareTool.compare` checks cancellation at the start of the page loop, then reads page dimensions and fits raster budget; `renderPage` opens a page and calls `Bitmap.createBitmap` without a fresh cancellation preflight. Add a minimal cancellation-aware seam, preserve page renderer/bitmap cleanup and original exception identity, add deterministic JVM tests, and require exact-SHA authoritative CI. No performance claims.
- PR #1 remains draft/unmerged; `main` untouched.


## Cycle 113 authoritative completion / Cycle 114 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #876] `US-R113-P2-01A`: Cycle 113 `E113/F115/US-R113-P2-01A` COMPLETE: exact code/test SHA `c95a0c28094b7e4b958726b6a47584e0cbd7d13c` passed draft-PR Actions #876 / API `39125693956`, job `117670224339`, on 2026-10-18 UTC; all nine configured gates SUCCESS (six JVM groups, debug APK, unsigned release/R8 and Android lint). Initial candidate `4191a42bd19742ec655e8c7053bd86de91093732` failed core PDF Kotlin compilation in #874; the follow-up explicit lambda parameter type fixed it. Eight focused JVM regressions cover preflight order, cancellation before allocation, render cleanup and cancellation between left/right renders. No emulator/device/OEM, benchmark or measured latency evidence. PR #1 remains open/draft/unmerged; `main` untouched.
### Epic E114 — PDF renderer page-open cancellation
#### Feature F116 — Preflight cancellation before opening renderer page
- [ACTIVE / P2] `US-R114-P2-01A`: Cycle 114 `E114/F116/US-R114-P2-01A` ACTIVE / INCOMPLETE before production mutation: `PdfCompareTool.renderPage` currently calls `renderer.openPage(pageIndex)` before `renderComparePageWithPreflight` checks cancellation, so an already-cancelled operation may still open a page. Add a cancellation preflight before page opening while preserving the existing pre-bitmap check, missing-page semantics, page closing and original exception identity. Add focused pure JVM regressions for open suppression, successful open and thrown errors, then require exact-SHA draft-PR CI. No measured performance claim.

## Cycle 114 authoritative completion / Cycle 115 activation — 2026-10-18 UTC
- Cycle 114 E114/F116/US-R114-P2-01A COMPLETE: exact code/test SHA `1ad7ed5e777219a13db58a3772dd6c3246fa693c` passed authoritative draft-PR Actions #879 / API `39128006515`, job `117677147217`, on 2026-10-18 UTC. All nine configured gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, Android lint). Seven focused JVM tests in PdfComparePageOpenCancellationTest cover cancellation before opening, preflight/open errors, successful render and cleanup. The initial production commit `1609f7f0d172ad3f33954b94b8b7bab2e0db524b` inadvertently omitted the base tree; forward recovery `1ad7ed5e777219a13db58a3772dd6c3246fa693c` restored the full repository tree and exact candidate passed CI. No emulator, physical device, benchmark, or measured latency evidence. PR #1 stays draft/unmerged; main untouched.
- Cycle 115 E115/F117/US-R115-P2-01A ACTIVE / INCOMPLETE before production/test mutation. Source evidence: PdfCompareTool.pageSize opens PdfRenderer.Page without a cancellation preflight; the compare loop checks once before reading left/right sizes, so cancellation during left-page dimension access may still open the right page. Add a preflight before each non-null pageSize open using a pure JVM-testable dimension-reader seam. Preserve missing-page behavior, page closing, exception identity and geometry. Add deterministic JVM tests and require exact-SHA draft-PR CI; no Android device or measured speedup claim.


## Cycle 115 authoritative completion / Cycle 116 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #882] `US-R115-P2-01A` (`E115/F117`): exact production/test SHA `6be844634382b60203f8c6813b39d95794517ad2` passed authoritative draft-PR Actions #882 / API `39131997093`, job `117688922042`, with all nine configured gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint). Eight focused page-size cancellation JVM regressions. Documentation-only #883 at `8945dab4b8521e71b5e284641757ce9f8b9678c0` succeeded but does not replace exact-code validation. No Android device/emulator/OEM/benchmark or measured latency evidence.
### Epic E116 — PDF renderer construction cancellation
#### Feature F118 — Preflight before opening either renderer
- [ACTIVE / P2] `US-R116-P2-01A`: `PdfCompareTool.compare` constructs `PdfRenderer(lPfd)` and `PdfRenderer(rPfd)` without cancellation preflights after cache copies or between renderer opens. Introduce minimal JVM-testable preflight, preserve renderer/descriptor cleanup and error behavior, add deterministic JVM tests, validate exact production/test SHA via draft-PR CI. PR #1 draft/unmerged; `main` untouched.


## Cycle 116 authoritative completion / Cycle 117 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #886] `US-R116-P2-01A` (`E116/F118`): exact production/test SHA `a6ef1d8c7d989155fd3a4217e65301066c8a2369` passed authoritative draft-PR Actions #886 / API `39136348187`, job `117701568583`, all nine configured gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, Android lint). Eight new renderer-open preflight JVM regressions; independent isolated host 4/4. Paired push #885 cancelled by concurrency. No emulator/physical-device/OEM/benchmark or measured speedup evidence.
### Epic E117 — PDF output-page start cancellation
#### Feature F119 — Preflight immediately before starting an output page
- [ACTIVE / P2] `US-R117-P2-01A`: after raster diff completes, `PdfCompareTool.compare` computes geometry and invokes `pdfDoc.startPage(pageInfo)` without fresh cancellation preflight. Add minimal JVM-testable preflight, preserve page lifecycle/error handling, bitmap cleanup, staged output and collision behavior, deterministic tests and exact-SHA draft-PR CI. PR #1 draft/unmerged, `main` untouched.


## Cycle 117 authoritative validation — 2026-10-18 UTC
- `E117/F119/US-R117-P2-01A` COMPLETE for the bounded cancellation preflight immediately before starting a diff PDF output page. Exact source/test SHA `8599bc47cd87872ed3e35bacffcec214eaef397d` passed draft-PR Actions #889 / API `39137841432`, job `117705942735`, all nine configured gates SUCCESS (domain/PDF/converter/scanner/PDF tools/app JVM, debug APK, unsigned release/R8, Android lint).
- `PdfCompareTool.kt` calls `startComparePdfPageWithPreflight(checkCancelled)` before `pdfDoc.startPage`; six `PdfCompareOutputPageStartCancellationTest` JVM regressions cover cancellation before start, success ordering, preflight/start errors, cancellation after raster work and between output pages. Scope: source and JVM CI only; no Android emulator, device, OEM, performance or cancellation-latency proof.
- Keep PR #1 draft/unmerged and main untouched. Next: inspect remaining highest-priority concrete risks and activate the next cycle before any code mutation.

## Cycle 118 — active
### E118 — PDF compare serialization cancellation
#### F120 — Preflight before staged stream open
- [ACTIVE / P2] `US-R118-P2-01A`: `PdfCompareTool.compare` opens FileOutputStream and enters PdfDocument.writeTo without cancellation preflight after final page. Add check before stream open, focused JVM tests, exact-SHA CI; preserve staged output and exception semantics. Baseline `e488018383b565182215c31a0f784aaca90634eb`; Cycle 117 COMPLETE on Actions #889.


## Cycle 118 authoritative completion / Cycle 119 activation — 2026-10-18 UTC
- [COMPLETE / PR CI API 39143193841] `US-R118-P2-01A` (E118/F120): Cycle 118 E118/F120/US-R118-P2-01A COMPLETE: exact production/test SHA `6b8810da42bd65e1bad5c426e574fc6aa3053cb6` passed authoritative draft-PR Actions API `39143193841`, job `117721649795` (2026-10-18 UTC). All nine configured gates SUCCESS: six JVM groups, debug APK, unsigned release/R8 and Android lint. `writeComparePdfWithPreflight` checks cancellation before opening staged FileOutputStream and before PdfDocument.writeTo; nine focused JVM regressions cover cancellation, opening, writing, close and exception ownership. The paired push run `39143190052` was cancelled by concurrency; this is not a failing validation gate. No Android emulator, physical device, benchmark, peak-memory or cancellation-latency evidence. PR #1 remains draft/unmerged; main unchanged.
### Epic E119 — PDF compare document-construction cancellation
#### Feature F121 — Preflight before output PdfDocument allocation
- [ACTIVE / P2] `US-R119-P2-01A`: Cycle 119 E119/F121/US-R119-P2-01A ACTIVE / INCOMPLETE before any production/test mutation. Source evidence at `6b8810da42bd65e1bad5c426e574fc6aa3053cb6`: `PdfCompareTool.compare` constructs `android.graphics.pdf.PdfDocument()` inside the staged output callback without a fresh cancellation preflight. A cancellation after staging and before document construction may still allocate an output document before the next page-loop check. Introduce a minimal JVM-testable preflight helper immediately before constructing PdfDocument, preserve close-on-failure, page lifecycle, staged output, exception identity, and normal output bytes. Add focused JVM regression tests; validate exact code/test SHA via draft-PR CI. Source-level responsiveness concern, not a reproduced crash.


## Cycle 119 authoritative completion / Cycle 120 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #898] `US-R119-P2-01A` (E119/F121): exact code/test SHA `64967addfc3ebbd56506220899ad5ed0b70dcca9` passed authoritative draft-PR Actions #898 / API `39147699102`, job `117734802444`, all nine gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, lint). Output PdfDocument constructor now checks cancellation before allocation; eight focused host JVM tests. Push #897 cancelled, not acceptance. No device/emulator/benchmark/race-freedom claim.
### Epic E120 — PDF compare descriptor-open cancellation
#### Feature F122 — Preflight before cached PDF descriptor acquisition
- [ACTIVE / P2] `US-R120-P2-01A`: after cache-copy callbacks, `PdfCompareTool.compare` opens left/right ParcelFileDescriptor via `withOpenedCloseables` without a fresh cancellation check before each. Add pure JVM-testable preflight before first and second opens, preserve first-close-on-second-failure and exception identity, add focused JVM regressions, validate exact code/test SHA via authoritative draft-PR all-nine-gate CI. Activated before source mutation at `64967addfc3ebbd56506220899ad5ed0b70dcca9`; PR #1 draft/unmerged, main untouched.


## Cycle 120 authoritative completion / Cycle 121 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #39157338513] `US-R120-P2-01A` (E120/F122): Cycle 120 E120/F122/US-R120-P2-01A COMPLETE: exact production/test SHA `8977edb8e6a6212eebd3299dc2ab945b8e0c4f74` passed authoritative draft-PR Actions run `39157338513`, job `117763175051`, on 2026-10-18 UTC. All nine configured gates SUCCESS: six JVM groups (domain, PDF, converter, scanner, PDF tools, app), debug APK, unsigned release/R8, Android lint. `openCompareDescriptorWithPreflight` checks cancellation before both cached ParcelFileDescriptor acquisitions; eight focused JVM regressions cover cancellation, normal ordering, first/second open failures, cleanup ownership and suppression. Initial candidate `2993bb98a8ad77ffcd3c44faf064f4ee04577ac9` failed CI due incorrect test expectation for two sibling suppressed cleanup failures; `8977edb8e6a6212eebd3299dc2ab945b8e0c4f74` corrected the assertion and passed. No emulator, physical-device, OEM, benchmark, measured cancellation latency, peak heap or race-freedom evidence. PR #1 remains draft/unmerged, main untouched.
### Epic E121 — PDF compare input cache-copy cancellation
#### Feature F123 — Preflight before each input PDF cache copy
- [ACTIVE / P2] `US-R121-P2-01A`: Cycle 121 E121/F123/US-R121-P2-01A ACTIVE / INCOMPLETE before source/test mutation. Source at `8977edb8e6a6212eebd3299dc2ab945b8e0c4f74`: `PdfCompareTool.compare` enters `withUriCopiedToCacheFile` for left and then right input without cancellation preflight immediately before either cache copy. `copyUriToCacheFile` calls `preflightCacheCopy`, creates a temp file and opens the URI before the first chunk-level cancellation check, so cancellation between the left and right copies can still trigger unnecessary second-copy setup. Add a bounded JVM-testable cancellation preflight immediately before each copy, preserve scoped temp-file cleanup, URI behavior and exception identity. Add deterministic JVM regressions, validate exact code/test SHA through draft-PR CI. No Android device or measured latency claim.


## Cycle 121 authoritative completion / Cycle 122 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #906] `US-R121-P2-01A` (E121/F123): Cycle 121 E121/F123/US-R121-P2-01A COMPLETE: exact production/test SHA `cc3610234154d8149f914781cb1ec13866d9b235` passed authoritative draft-PR Actions #906 / API `39161742830`, job `117776294771`, on 2026-10-18 UTC. All nine configured gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, Android lint). `checkCompareCacheCopyPreflight` checks cancellation before both URI-to-cache copies; eight focused JVM regressions cover cancellation before left/right copy, normal sequencing, exception identity and cleanup. No Android emulator, physical-device, benchmark, measured latency or race-freedom evidence. PR #1 remains draft/unmerged, main untouched.
### Epic E122 — PDF compare output-directory cancellation
#### Feature F124 — Preflight before output-directory resolution
- [ACTIVE / P2] `US-R122-P2-01A`: Cycle 122 E122/F124/US-R122-P2-01A ACTIVE / INCOMPLETE before source/test mutation. Source at `cc3610234154d8149f914781cb1ec13866d9b235`: after opening both PdfRenderers and checking page count, `PdfCompareTool.compare` calls `DocForgeSettingsStore.resolveOutputDirectory` without a fresh cancellation preflight. The resolver reads SharedPreferences, resolves filesystem paths and can call `mkdirs()`; cancellation during the preceding renderer setup can therefore still trigger avoidable output-directory side effects. Add a bounded JVM-testable cancellation preflight immediately before output-directory resolution, preserving normal folder selection, symlink guards, exception identity and cleanup. Add deterministic regressions and require exact-SHA draft-PR CI. This is a source-level P2 responsiveness concern, not a reproduced crash.


## Cycle 122 authoritative completion / Cycle 123 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #909] `US-R122-P2-01A` (E122/F124): Cycle 122 E122/F124/US-R122-P2-01A COMPLETE: exact production/test SHA `d4921e0b47f414c009a433e7d9adf599096cebb2` passed draft-PR Actions #909 / API `39166370082`, job `117790052942`, on 2026-10-18 UTC. Six JVM groups, debug APK, unsigned release/R8, Android lint all SUCCESS. `resolveCompareOutputDirectoryWithPreflight` checks cancellation before preference/directory resolution; seven focused JVM regressions committed. Push #908 was cancelled by concurrency, not authoritative validation. Docs-head Actions #910 / API `39170730224` also passed at `8d1bdf2c7be2f646d89d7e19421b5e3169a529be`; it does not supersede the code SHA. No emulator, physical device, benchmark, measured latency, heap or race-freedom evidence.
### Epic E123 — PDF compare staging-setup cancellation
#### Feature F125 — Preflight before staged output setup
- [ACTIVE / P2] `US-R123-P2-01A`: Cycle 123 E123/F125/US-R123-P2-01A ACTIVE / INCOMPLETE before any production/test mutation. Source evidence at `8d1bdf2c7be2f646d89d7e19421b5e3169a529be`: `PdfCompareTool.compare` calls `withStagedOutputFile` after output-directory resolution and filename sanitation without a fresh cancellation preflight; `withStagedOutputFilesUsingIdentityReaders` validates names, checks directory symlinks and may call `mkdirs()` before its first `ensureActive()`. Cancellation in this interval can cause avoidable staging setup. Add a minimal JVM-testable preflight immediately before staging, preserve normal output and failure semantics, add focused tests, validate exact code/test SHA through draft-PR CI. P2 responsiveness concern, not reproduced crash. PR #1 draft/unmerged; main untouched.

## Cycle 123 authoritative completion / Cycle 124 activation — 2026-10-18 UTC
- [COMPLETE / PR CI #913] `US-R123-P2-01A` (E123/F125): exact code/test SHA `f853ab1672e0f9002ac2962c8d9648defecdb7a2` passed authoritative draft-PR Actions #913 / API `39172488960`, job `117808338166`, all nine gates SUCCESS. `checkCompareStagingPreflight` prevents cancelled PDF comparisons from entering staged-output setup; eight focused JVM tests. Paired push #912 cancelled by concurrency, not acceptance. No Android device/emulator/benchmark/race-freedom proof.
### Epic E124 — Shared staged-output setup cancellation
#### Feature F126 — Cancellation before staging directory side effects
- [COMPLETE / PR CI #916] `US-R124-P2-01A`: at `f853ab1672e0f9002ac2962c8d9648defecdb7a2`, `PdfIoUtils.withStagedOutputFilesUsingIdentityReaders` checks names/symlink path and can create directory before first coroutine `ensureActive`. Other callers are not covered by Cycle 123 compare-specific preflight. Add a bounded shared-entry coroutine cancellation check before filesystem side effects; preserve ownership/collision/no-overwrite and existing validation, add focused JVM regressions and exact-code/test-SHA nine-gate draft-PR CI. Source-level concern, not Android-measured defect. Activated before production mutation.


## Cycle 124 authoritative completion / Cycle 125 activation — 2026-10-18 UTC
- Cycle 124 E124/F126/US-R124-P2-01A COMPLETE: exact production/test SHA `7d3a735e8547c3abb9d458bebcb78c37d74afbb0` passed authoritative draft-PR Actions #916 / API `39177197162`, job `117822197047`, on 2026-10-18 UTC. All nine configured gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint). `PdfIoUtils.withStagedOutputFilesUsingIdentityReaders` checks cancellation before validating output names or touching directories; six focused JVM regressions in `PdfStagedEntryCancellationTest.kt` cover cancelled empty/nonempty requests, unchanged existing files, active empty batch, successful publication and invalid names. Paired push #915 was cancelled by concurrency and is not acceptance evidence. No emulator, physical device, OEM, benchmark, measured latency, peak heap or race-freedom proof. PR #1 remains draft/unmerged; main untouched.
### Epic E125 — Staged-writer cancellation precedence
#### Feature F127 — Cancellation before post-writer staged-file validation
- [COMPLETE / PR CI #919] `US-R125-P2-01A`: Cycle 125 E125/F127/US-R125-P2-01A ACTIVE / INCOMPLETE before production/test mutation. Source at `7d3a735e8547c3abb9d458bebcb78c37d74afbb0`: `PdfIoUtils.withStagedOutputFilesUsingIdentityReaders` calls `request.block(stagedFile)` then immediately performs `requireRegularStagedOutput` and `requireOriginalStagedIdentity` without a cancellation preflight. If a writer cancels its coroutine but returns after removing/replacing the staged file, filesystem validation can throw a different exception before the later `ensureActive()` check, obscuring cancellation and performing avoidable I/O. Add `currentCoroutineContext().ensureActive()` immediately after the writer returns, preserving cleanup and exception identity. Add deterministic core PDF JVM regressions for cancelled writer with missing staged file, ordinary cancellation, normal output and original writer exceptions; require exact-SHA draft-PR CI. This is a source-level P2 correctness/responsiveness concern, not a reproduced device crash.


## Cycle 125 authoritative completion / Cycle 126 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #919] `US-R125-P2-01A` (E125/F127): Cycle 125 E125/F127/US-R125-P2-01A COMPLETE: exact production/test SHA `3854f24b68fd5a78c1eff236cdcf21723aeeac65` passed authoritative draft-PR Actions #919 / API `39181219306`, job `117834218043`, on 2026-10-19 UTC. Six JVM test groups, debug APK, unsigned release/R8, and Android lint all SUCCESS (nine configured gates). `PdfIoUtils.withStagedOutputFilesUsingIdentityReaders` now checks cancellation immediately after each writer returns, before staged-file validation. Six focused `PdfStagedWriterCancellationTest` JVM regressions cover missing/valid stage cancellation, batch short-circuit, successful publication and writer exception identity. Documentation-head Actions #920 / API `39183379085` passed at `df3f9423ee514064d34fb9fb06f96dc2f6bf8470` but is not code validation. No Android emulator, physical device, benchmark, measured latency or race-freedom proof; PR #1 draft/unmerged, main unchanged.
### Epic E126 — Shared cache-copy cancellation preflight
#### Feature F128 — Cancellation before metadata and temporary-file setup
- [COMPLETE / PR CI #924] `US-R126-P2-01A`: Cycle 126 E126/F128/US-R126-P2-01A ACTIVE / INCOMPLETE before any production/test mutation. Source at `df3f9423ee514064d34fb9fb06f96dc2f6bf8470`: `Context.copyUriToCacheFile` calls `preflightCacheCopy(uri)` before any coroutine cancellation check; preflight may query ContentResolver metadata and StatFs, and subsequent temp-file creation and URI stream acquisition occur before the first `copyChunkWithCancellation` check. PdfCompareTool's caller-specific preflight does not protect other users of this shared helper. Add a minimal shared entry cancellation preflight before metadata and temp-file side effects, add deterministic JVM tests through a testable preflight seam, preserve temp registry ownership and exceptions, then validate the exact source/test SHA through draft-PR all-nine-gate CI. Source-level P2 responsiveness issue, not a reproduced Android crash.


## Cycle 126 authoritative completion / Cycle 127 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #924] `US-R126-P2-01A`: Cycle 126 E126/F128/US-R126-P2-01A COMPLETE: exact source/test SHA `1a57f59f7ca760e585e76875d3a7c8a46c19929d` passed draft-PR Actions #924 / API `39188092230`, job `117854690723`, on 2026-10-19 UTC. All nine configured gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint). Shared `withCacheCopyEntryPreflight` checks cancellation before and after URI metadata/storage preflight; five focused `PdfCacheCopyEntryPreflightTest` JVM regressions. Documentation-only Actions #925 / API `39192627590` succeeded at `9ecc3c2bbafa02b433d789579523e06eac5c313f`, not the production validation. No emulator, physical device, benchmark, measured latency, or race-freedom proof. PR #1 draft/unmerged; main unchanged.
### Epic E127 — Cache-copy transfer-loop cancellation precedence
#### Feature F129 — Cancellation before per-chunk free-space checks
- [ACTIVE / P2] `US-R127-P2-01A`: Cycle 127 E127/F129/US-R127-P2-01A ACTIVE / INCOMPLETE before production mutation. At `9ecc3c2bbafa02b433d789579523e06eac5c313f`, `Context.copyUriToCacheFile` loops over source chunks and reads `StatFs(cacheDir.absolutePath).availableBytes` before `copyChunkWithCancellation` checks cancellation. It also checks free space after transfer without a fresh cancellation check. If cancellation occurs after stream acquisition or during transfer, avoidable StatFs I/O or an insufficient-space exception can precede cancellation. Add a small suspend JVM-testable preflight around per-chunk storage checks, preserving reserve thresholds, chunk ordering, transfer semantics, temp cleanup, and original non-cancellation failures. Add deterministic core PDF JVM regressions and obtain exact-source/test-SHA draft-PR all-nine-gate CI. This is source-level P2 responsiveness/cancellation precedence, not a reproduced Android crash.


## Cycle 127 completion / Cycle 128 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #928] `US-R127-P2-01A`: Cycle 127 E127/F129/US-R127-P2-01A COMPLETE: exact source/test SHA `347bf97709aecd9033b9b4383a6e830918fafdd3` passed authoritative draft-PR Actions #928 / API `39195074528`, job `117876038162`, on 2026-10-19 UTC. All nine gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, Android lint). Nine `PdfCacheCopyLoopSpaceCancellationTest` JVM regressions; cancellation checks before/after StatFs and transfer. Docs-only Actions #929 / API `39197476003` succeeded at `8a3b4d5e76a408fca9106c22878f91ced68e3658`, not code validation. No emulator/device/benchmark/latency/race-freedom evidence; PR #1 draft/unmerged, main unchanged.
### Epic E128 — Batch staged-output validation cancellation
#### Feature F130 — Per-item final staged-file validation preflight
- [ACTIVE / P2] `US-R128-P2-01A`: Cycle 128 E128/F130/US-R128-P2-01A ACTIVE / INCOMPLETE before production mutation. Source at `8a3b4d5e76a408fca9106c22878f91ced68e3658`: final `prepared.forEach` staged-file validation checks cancellation only before the entire batch; cancellation during one item's validation may allow later unnecessary filesystem reads. Add per-item coroutine cancellation checks before/after validation, focused deterministic core PDF JVM regressions, and exact source/test SHA draft-PR nine-gate CI. Preserve active-job errors, staged identity, no-overwrite, cleanup, rollback. This is a source-level P2 concern, not a reproduced Android crash.

## Cycle 128 authoritative completion / Cycle 129 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #932] `US-R128-P2-01A` (E128/F130): Cycle 128 E128/F130/US-R128-P2-01A COMPLETE: exact production/test SHA `5ed14615d01643f7144aa661d12a457cfa8d8000` passed authoritative draft-PR Actions #932 / API `39200352030`, job `117891856932`, on 2026-10-19 UTC. All nine configured gates SUCCESS (domain, PDF, converter, scanner, PDF tools and app JVM tests, debug APK, unsigned release/R8, Android lint). `withStagedValidationPreflight` checks cancellation before and after each final staged-file validation, with seven focused JVM tests in `PdfStagedBatchValidationCancellationTest.kt`. Source-level P2 responsiveness mitigation; no Android emulator/device/OEM/benchmark/peak-heap/race-freedom evidence. PR #1 remains draft/unmerged, main unchanged.
### Epic E129 — Scoped cache-copy callback cancellation
#### Feature F131 — Cancellation-safe callback handoff under temp ownership
- [ACTIVE / P2] `US-R129-P2-01A`: Cycle 129 E129/F131/US-R129-P2-01A ACTIVE / INCOMPLETE before production/test mutation. Source at `5ed14615d01643f7144aa661d12a457cfa8d8000`: `Context.withUriCopiedToCacheFile` calls `copyUriToCacheFile(..., keepRegistered = true)` then `withScopedCacheCopy(tempFile, block)` without cancellation preflight before callback. Cancellation between copy completion and callback can trigger avoidable callback work. A preflight outside the scoped cleanup would risk leaking the registered temp; implement cancellation preflight INSIDE `withScopedCacheCopy`'s ownership scope, preserving registry release, original errors and suppression, normal bytes and callback return. Add deterministic core PDF JVM regressions for cancelled callback skip, cleanup, other-owner preservation, successful return and original failure; require exact-source/test-SHA draft-PR all-nine-gate CI. Source-level P2 risk, not Android-measured defect.

## Cycle 129 authoritative completion / Cycle 130 activation — 2026-10-19 UTC
- [COMPLETE / CI #935] `US-R129-P2-01A` — Scoped cache-copy callback cancellation inside cleanup. Exact code/test SHA `8d661bcd0fd7fd1bb9fd3023fb797891c059164a`; draft-PR Actions API `39205271842`, job `117906646366`, all nine gates SUCCESS. Four new host JVM tests; no Android device/OEM/benchmark/race-freedom evidence.
- [ACTIVE / INCOMPLETE / P2] `US-R130-P2-01A` — Cache-copy input provider open cancellation under registered temp-file cleanup. Source-backed gap at `8d661bcd0fd7fd1bb9fd3023fb797891c059164a`: `withRegisteredCacheCopy` invokes `contentResolver.openInputStream` without fresh cancellation preflight; preserve ownership, bytes, exceptions, storage checks; focused JVM regressions and exact-code/test-SHA draft-PR CI. Do not modify main or mark PR #1 ready.

## Cycle 130 authoritative completion / Cycle 131 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #942] `US-R130-P2-01A`: Cycle 130 E130/F132/US-R130-P2-01A COMPLETE: exact production/test SHA `1b8c660f264249a9f0db505103b15c36be9fae5a` passed authoritative draft-PR Actions #942 / API `39218339422`, job `117947505058`, on 2026-10-19 UTC. All nine configured gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, Android lint). Commits `a3cc350`, `98a04ea`, `1b8c660` add cancellation preflight inside registered temp ownership, use it before provider open, and add three core PDF JVM regressions. No emulator/device/OEM/benchmark/latency/race-freedom proof. PR #1 remains draft/unmerged; main unchanged.
### Epic E131 — Preserve provider metadata cancellation
#### Feature F133 — Cancellation-safe URI metadata fallback
- [ACTIVE / INCOMPLETE / P2] `US-R131-P2-01A`: Cycle 131 E131/F133/US-R131-P2-01A ACTIVE / INCOMPLETE. Source at `1b8c660f264249a9f0db505103b15c36be9fae5a`: `queryKnownInputSize` uses `runCatching { contentResolver.query(...) }.getOrNull()` and the same pattern for `openAssetFileDescriptor`. Both swallow `CancellationException` from provider metadata access and may continue fallback work while cancellation is signaled by a provider even if the coroutine job is still active. Preserve cancellation identity while retaining ordinary metadata-probe fallback; add focused core PDF JVM tests and exact-code/test-SHA draft-PR CI. Do not modify main or merge PR #1.


## Cycle 131 authoritative completion / Cycle 132 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #946] `US-R131-P2-01A`: Cycle 131 E131/F133/US-R131-P2-01A COMPLETE: exact production/test SHA `ea5e30e3fe4b99daf6ba043431e3db7dfb38cc0e` passed authoritative draft-PR Actions #946 / API `39231141696`, job `117989118935`, on 2026-10-19 UTC. All nine configured gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint). `PdfIoUtils.kt` now preserves provider-thrown `CancellationException` during URI query/descriptor metadata probes while retaining ordinary failure fallback; eight focused JVM regressions in `PdfCacheMetadataCancellationTest.kt`. An isolated Kotlin smoke harness also passed 8/8 but is not Android CI. Paired push #945 was cancelled by concurrency and is not authoritative validation. No emulator, physical device, OEM, benchmark, measured latency or race-freedom proof. PR #1 remains draft/unmerged; main untouched.
### Epic E132 — Preserve primary errors during staged rollback
#### Feature F134 — Self-suppression-safe staged publication cleanup
- [ACTIVE / INCOMPLETE / P2] `US-R132-P2-01A`: Cycle 132 E132/F134/US-R132-P2-01A ACTIVE / INCOMPLETE before any source/test mutation. Source at `ea5e30e3fe4b99daf6ba043431e3db7dfb38cc0e`: `publishCheckedStagedOutput` catches a post-move publication validation failure, then invokes `rollbackPublishedOutputIfUnchanged` through `runCatching` and calls `failure.addSuppressed(rollbackFailure)` without checking whether the rollback failure is the exact same Throwable instance as the original failure. A deterministic injected `readAndroidIdentity` callback could throw that same instance twice; Java self-suppression then raises `IllegalArgumentException`, obscuring the primary publication failure. First reproduce this narrowly in a JVM regression using injected identity readers, then guard self-suppression without changing rollback ownership, fail-closed identity checks, normal publication, or error identity. Require exact-source/test-SHA draft-PR all-nine-gate CI. This is a source-level P2 edge case, not a reproduced Android device defect.


## Cycle 132 authoritative completion / Cycle 133 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #949] `US-R132-P2-01A`: Cycle 132 E132/F134/US-R132-P2-01A COMPLETE: exact production/test SHA `826cc7c813ead8fd0674392a1aac65e9f5a22821` passed authoritative draft-PR GitHub Actions #949 / API `39233506367`, job `117997004291`, on 2026-10-19 UTC. All nine configured gates SUCCESS: six JVM unit-test groups, debug APK, unsigned release/R8, and Android lint. `PdfIoUtils.kt` guards both staged publication rollback suppression sites against attaching the primary Throwable to itself; five focused JVM regressions in `PdfStagedRollbackSelfSuppressionTest.kt` cover same/different exceptions, rollback, and successful publication. Paired push #948 was cancelled by concurrency and is not a failing gate. No emulator, physical device, benchmark, latency, or race-freedom evidence. PR #1 remains draft/unmerged; main unchanged.
### Epic E133 — Preserve writer failures during staged cleanup
#### Feature F135 — Self-suppression-safe per-stage cleanup
- [ACTIVE / INCOMPLETE / P2] `US-R133-P2-01A`: Cycle 133 E133/F135/US-R133-P2-01A ACTIVE / INCOMPLETE before production mutation. Source-backed P2: `PdfIoUtils.withStagedOutputFilesUsingIdentityReaders` catches writer/identity errors around lines 424-454, and its per-stage cleanup still uses `.exceptionOrNull()?.let(t::addSuppressed)` without an identity guard. If a request writer throws a Throwable and the injected `readAndroidIdentity` cleanup callback throws that same instance, Java self-suppression raises `IllegalArgumentException` and masks the writer failure. Reuse `addSuppressedUnlessSame` at this remaining cleanup site, add deterministic core PDF JVM regressions for same Throwable identity, distinct suppressed cleanup error, successful owned-temp cleanup and normal publication. Preserve fail-closed cleanup and output ownership. Require exact code/test SHA through draft-PR all-nine-gate CI; no device or performance claims.


## Cycle 133 exact-SHA validation / Cycle 134 activation — 2026-10-19 UTC
- Cycle 133 E133/F135/US-R133-P2-01A COMPLETE: exact production/test SHA `d5a15b024809faf4abd09e6687de6ba7cb5b6fd9` passed authoritative draft-PR Actions #952 / API `39237737247`, job `118010917722`, completed SUCCESS on 2026-10-19 UTC across all nine configured gates (six JVM groups, debug APK, unsigned release/R8, Android lint). `PdfIoUtils.kt` now uses `addSuppressedUnlessSame` in the per-stage writer cleanup handler; five focused host JVM tests in `PdfStagedWriterCleanupSelfSuppressionTest.kt` cover same-Throwable preservation, distinct suppression, owned-stage cleanup, successful publication and writer sequencing. Scope is host JVM/build/lint only; no Android device/emulator, visual, benchmark, peak-heap or race-freedom proof. PR #1 remains draft/unmerged; main unchanged.
- [COMPLETE / CI #956] `US-R134-P2-01A` — Preserve earlier prepared-stage cleanup failure evidence. Exact code/test SHA `84b7ed0b141e2eeb3923b9060980b1cbef89f3a3`, Actions #956 / API `39252968886`, job `118061098741`: all nine gates SUCCESS; five focused JVM tests. PR #1 remains draft; main unchanged.


## Cycle 134 exact-SHA completion / Cycle 135 activation — 2026-10-19 UTC
- Cycle 134 E134/F136/US-R134-P2-01A COMPLETE: exact production/test SHA `84b7ed0b141e2eeb3923b9060980b1cbef89f3a3` passed authoritative draft-PR Actions #956 / API `39252968886`, job `118061098741`, completed SUCCESS on 2026-10-19 UTC, all nine gates (domain, PDF, converter, scanner, PDF-tools, app JVM; debug APK; unsigned release/R8; Android lint). `PdfIoUtils.kt` retains the original staged writer/publication failure, suppresses distinct prepared-stage cleanup errors without self-suppression, and propagates cleanup failures without a primary error; five focused host JVM regressions in `PdfStagedPreparedCleanupFailureTest.kt` cover same/distinct/multiple cleanup errors, identity mismatch fail-closed, and normal two-file publication. Paired push #955 CANCELLED, not acceptance. No Android device/emulator/OEM, benchmark, measured latency, peak-heap or race-freedom proof. PR #1 stays draft/unmerged; main unchanged.
- [ACTIVE / P2] `US-R135-P2-01A` — `Context.copyUriToCacheFile` checks cancellation before `contentResolver.openInputStream(uri)`, but after synchronous provider acquisition returns it enters `input.use`, constructs `Channels.newChannel(source)` and opens `FileOutputStream(tempFile)` without a fresh cancellation check. Cancellation during provider acquisition may therefore cause unnecessary channel/output setup before the next copy-loop check. Add a minimal coroutine preflight INSIDE `input.use` (to close input on cancellation) and INSIDE registered temp ownership (to release temp). Add deterministic host JVM tests for cancelled post-open callback skip/cleanup, success, original exceptions and ownership; exact-code/test-SHA draft-PR all-nine-gate CI required. Source-level P2 responsiveness risk, not an Android device-measured defect.


## Cycle 135 authoritative completion / Cycle 136 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #959] `US-R135-P2-01A`: Cycle 135 E135/F137/US-R135-P2-01A COMPLETE: exact production/test SHA `ddbb652c9dc09c2c87fdea4bc2f5632b63ec59ac` passed authoritative draft-PR Actions #959 / API `39262756567`, job `118094439688`, on 2026-10-19 UTC. All nine configured gates SUCCESS: six JVM groups, debug APK, unsigned release/R8 and Android lint. `PdfIoUtils.kt` now checks cancellation immediately after provider stream acquisition inside input.use and registered-temp ownership; seven focused host JVM tests in `PdfCacheCopyPostOpenCancellationTest.kt` cover cancellation/cleanup, other-owner preservation, successful bytes/result, original error, close failure, and retained ownership. Paired push #958 CANCELLED, not acceptance. No Android emulator/device/OEM/benchmark/peak-heap/race-freedom proof. PR #1 remains draft/unmerged; main unchanged.
### Epic E136 — Cache input channel cancellation
#### Feature F138 — Cancellation before output stream acquisition
- [ACTIVE / INCOMPLETE / P2] `US-R136-P2-01A`: Cycle 136 E136/F138/US-R136-P2-01A ACTIVE / INCOMPLETE before production mutation. Source at `ddbb652c9dc09c2c87fdea4bc2f5632b63ec59ac`: `Context.copyUriToCacheFile` creates `Channels.newChannel(source)` and then opens `FileOutputStream(tempFile)` without a fresh cancellation preflight inside the channel's `.use` scope. Cancellation during channel acquisition can still cause avoidable output-file/channel setup before the transfer-loop preflight. Audit the source and tests; add a minimal coroutine ensureActive preflight INSIDE source-channel ownership before output opening, preserving provider stream close, registered temp cleanup, bytes, exception identity and free-space thresholds. Add deterministic core PDF host JVM regressions; require authoritative exact-code/test-SHA draft-PR all-nine-gate CI. This is a source-level P2 responsiveness risk, not a reproduced Android device defect.


## Cycle 136 exact-SHA completion / Cycle 137 activation — 2026-10-19 UTC
- [COMPLETE / CI #962] `US-R136-P2-01A`: Cycle 136 E136/F138/US-R136-P2-01A COMPLETE: exact production/test SHA `acb2c57c0a7756fe3292a51915a9c8707dc0cefe` passed authoritative draft-PR Actions #962 / API `39273664657`, job `118133050266`, on 2026-10-19 UTC. All nine configured gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint). `PdfIoUtils.kt` now checks cancellation after cache input-channel acquisition inside channel ownership; seven core PDF JVM regressions in `PdfCacheCopyPostChannelCancellationTest.kt` cover cancellation/close, other-owner preservation, unchanged bytes, output failure, channel-close failure and suppressed cleanup. Paired push #961 cancelled by concurrency, not acceptance evidence. No emulator, physical device, OEM, benchmark, measured latency, peak heap or race-freedom proof. PR #1 remains draft/unmerged; main untouched.
### Epic E137 — Final cache-copy cancellation
#### Feature F139 — Completion preflight under registered temporary-file ownership
- [ACTIVE / INCOMPLETE / P2] `US-R137-P2-01A`: Source at `acb2c57c0a7756fe3292a51915a9c8707dc0cefe`: `Context.copyUriToCacheFile` completes the channel copy and closes its input/output resources, then returns `tempFile` from the registered-copy ownership callback without a final coroutine cancellation preflight. If cancellation arrives during final channel or provider-stream closure, the callback can report success and `withRegisteredCacheCopy(keepRegistered=false)` unregisters without deleting the partial/cancelled temp file. Add a small testable completion preflight INSIDE registered ownership immediately before returning `tempFile`; preserve original exceptions, file bytes, keepRegistered behavior and cleanup of cancelled copies. Add focused deterministic core PDF JVM tests, then require exact-code/test-SHA nine-gate draft-PR CI. This is a source-level P2 cancellation/temporary-file-lifecycle risk, not a reproduced device defect.

## Cycle 137 authoritative completion / Cycle 138 activation — 2026-10-19 UTC
- [COMPLETE / CI #965] `US-R137-P2-01A`: Cycle 137 E137/F139/US-R137-P2-01A COMPLETE: exact production/test SHA `9a6a936c5f61194bf240ffce4eaac9e47ae7cb16` passed authoritative draft-PR Actions #965 / API `39291026016`, job `118191420770`, on 2026-10-19 UTC. All nine configured gates SUCCESS: six JVM groups, debug APK, unsigned release/R8, Android lint. `PdfIoUtils.kt` now checks cancellation after cache-copy stream/channel closure while the temp file remains registered; nine focused JVM regressions in `PdfCacheCopyCompletionCancellationTest.kt` cover provider/channel-close cancellation, temp ownership, successful bytes, retained leases and original exception preservation. Push #964 / API `39291016815` was cancelled by concurrency, not acceptance. No emulator, physical device, benchmark, measured latency, peak heap or race-freedom evidence. PR #1 stays draft/unmerged, main untouched.
### Epic E138 — Metadata probe cancellation integrity
#### Feature F140 — Propagate provider interruption/cancellation
- [ACTIVE / INCOMPLETE / P2] `US-R138-P2-01A`: Cycle 138 E138/F140/US-R138-P2-01A ACTIVE / INCOMPLETE before production/test mutation. Source at `9a6a936c5f61194bf240ffce4eaac9e47ae7cb16`: `metadataProbeOrNull` in `PdfIoUtils.kt` rethrows `java.util.concurrent.CancellationException`, but its broad `catch (Exception)` silently converts `InterruptedException` and Android `android.os.OperationCanceledException` into optional-metadata fallback. This can ignore a provider's interruption/cancellation, proceed to descriptor lookup or cache-copy setup, and lose the original failure. Preserve these cancellation/interruption signals while retaining ordinary metadata fallback; add deterministic JVM tests for interruption on query/descriptor and normal fallback, and require exact-SHA draft-PR nine-gate CI. This is a source-level P2 correctness concern, not a reproduced device crash.

## Cycle 138 authoritative completion / Cycle 139 activation — 2026-10-19 UTC
- [COMPLETE / CI #968] `US-R138-P2-01A`: Cycle 138 E138/F140/US-R138-P2-01A COMPLETE, exact code/test SHA `2069608de118b7bb8a47b185d4e798e9c9c1bfc7` passed authoritative draft-PR Actions #968 / API `39302617044`, job `118231464639`, all nine configured gates SUCCESS. Three new JVM tests cover query/descriptor `InterruptedException` identity and no descriptor fallback; production additionally propagates Android `OperationCanceledException`. No device/OEM/runtime Android cancellation proof; PR #1 remains draft and main unchanged.
### Epic E139 — Interrupted metadata I/O integrity
#### Feature F141 — Preserve interruption semantics during provider metadata probes
- [ACTIVE / INCOMPLETE / P2] `US-R139-P2-01A`: `metadataProbeOrNull` at `2069608de118b7bb8a47b185d4e798e9c9c1bfc7` still catches `java.io.InterruptedIOException` as ordinary `Exception`, potentially masking interrupted provider metadata I/O and continuing fallback. First verify appropriate fail-fast semantics, then implement the smallest safe exception propagation and focused JVM tests while preserving ordinary IOException fallback, original exception identity, cursor/descriptor cleanup, and free-space behavior. Require exact-SHA nine-gate draft-PR CI; do not infer Android device results.

## Cycle 139 authoritative completion / Cycle 140 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #971] `US-R139-P2-01A`: Cycle 139 E139/F141/US-R139-P2-01A COMPLETE: exact production/test SHA `e0dfc902e0cd988022616b3ada28fa55877129f1` passed authoritative draft-PR Actions #971 / API `39316414595`, job `118278969825`, on 2026-10-19 UTC. All nine configured gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint). `PdfIoUtils.metadataProbeOrNull` propagates `InterruptedIOException` except `SocketTimeoutException`, which remains optional-metadata fallback; five focused JVM tests cover interrupted query/descriptor, exception identity, socket timeout and ordinary IOException fallback. Paired push run `39316403273` cancelled by concurrency and is not acceptance. No Android emulator, physical device, OEM, benchmark, measured latency or race-freedom proof. PR #1 remains draft/unmerged; main untouched.
### Epic E140 — Interrupted NIO metadata I/O integrity
#### Feature F142 — Preserve ClosedByInterruptException in metadata probes
- [ACTIVE / INCOMPLETE / P2] `US-R140-P2-01A`: At exact validated source SHA `e0dfc902e0cd988022616b3ada28fa55877129f1`, `PdfIoUtils.metadataProbeOrNull` catches `java.nio.channels.ClosedByInterruptException` in its generic `catch (Exception)` and returns null, because this interruption signal extends `IOException` but not `InterruptedIOException`. A provider metadata operation that surfaces this exception could silently trigger descriptor fallback or cache-copy work. This is a source-level P2 risk, not a reproduced Android device defect. Keep ordinary IOException, SocketTimeoutException, and AsynchronousCloseException fallback unchanged. Add deterministic core PDF JVM regressions; require exact-code/test-SHA draft-PR nine-gate CI; keep PR #1 draft/unmerged and main untouched.


## Cycle 140 authoritative completion / Cycle 141 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #974] `US-R140-P2-01A`: Cycle 140 E140/F142/US-R140-P2-01A COMPLETE: exact production/test SHA `0f4b23fd33724a3ca23fb0faa1c5f38190462e92` passed authoritative draft-PR Actions #974 / API `39320180315`, job `118292004034`, on 2026-10-19 UTC. All nine configured gates SUCCESS: six JVM test groups (domain, PDF, converter, scanner, PDF tools, app), debug APK, unsigned release/R8 and Android lint. `PdfIoUtils.metadataProbeOrNull` now propagates `ClosedByInterruptException` without descriptor fallback; focused core PDF JVM regressions cover query/descriptor interruption identity and ordinary/timeout/asynchronous-close fallback. Documentation-only #975 / API `39324808346` passed at `a91b6e4960c67eae68419eed1a3242c165fb75a7`; it is not production validation. No emulator, physical-device, OEM, benchmark, latency, peak-memory or race-freedom evidence. PR #1 remains draft/unmerged; main untouched.
### Epic E141 — PDF page-count cancellation integrity
#### Feature F143 — Preserve cancellation in page-count fallback
- [ACTIVE / INCOMPLETE / P1] `US-R141-P1-01A`: At `a91b6e4960c67eae68419eed1a3242c165fb75a7`, `PdfCompressor.getPageCount` (core/pdf/src/main/java/com/docforge/core/pdf/PdfCompressor.kt:87-93) wraps the suspending cache-copy and PDF page-count operation in `runCatching { ... }.getOrDefault(0)`. Kotlin `runCatching` catches `Throwable`, including `CancellationException`, and can turn a cancelled page-count request into a misleading successful zero-page result. It can also mask fatal `Error` subclasses. This is a source-backed cancellation/correctness risk, not a reproduced Android device failure. Replace the broad Throwable-swallowing page-count fallback with a narrow JVM-testable helper that rethrows cancellation/interruption signals and fatal errors while returning zero for ordinary recoverable page-count exceptions. Add focused core PDF JVM regressions for original cancellation identity, cancelled coroutine, recoverable IOException fallback, valid count and fatal Error propagation. Require authoritative exact-code/test-SHA draft-PR nine-gate CI; do not change main or PR draft state.

## Cycle 141 authoritative completion / Cycle 142 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #978] `US-R141-P1-01A`: Cycle 141 E141/F143/US-R141-P1-01A COMPLETE: exact production/test SHA `28149d1426f94a1ba4b4cc492f85d1742dd674b6` passed authoritative draft-PR Actions #978 / API `39328986272`, job `118322155998`, all nine configured gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint). `PdfCompressor.pageCountOrZero` now propagates cancellation, interruption and fatal errors; 12 focused JVM regressions cover normal count, recoverable errors, original cancellation identity, cancelled coroutine before/after probe, interruption, timeout and fatal Error. No emulator, physical device, OEM, benchmark or cancellation-latency evidence. PR #1 remains draft/unmerged, main untouched.
### Epic E142 — Preserve cancellation during page-count fallback
#### Feature F144 — Recheck cancellation before recoverable page-count defaults
- [COMPLETE / PR CI #981] `US-R142-P1-01A`: Cycle 142 E142/F144/US-R142-P1-01A ACTIVE / INCOMPLETE before production/test mutation. At exact validated source SHA `28149d1426f94a1ba4b4cc492f85d1742dd674b6`, `PdfCompressor.pageCountOrZero` checks coroutine cancellation before and after successful probes but returns `0` directly from its `catch (_: Exception)` and `SocketTimeoutException` recovery branches. If a provider cancels the coroutine and then throws a recoverable `IOException`/timeout during cleanup, the cancellation can be misreported as a successful zero-page hint. Add an `ensureActive` preflight before either recoverable zero fallback; preserve ordinary recoverable zero behavior, interruption/cancellation identity, fatal Error propagation, and all existing tests. Add deterministic JVM regressions for cancellation + recoverable IOException/timeout, then require authoritative exact-code/test-SHA nine-gate draft-PR CI. Source-backed race/correctness risk, not a reproduced device failure.

## Cycle 142 authoritative completion / Cycle 143 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #981] `US-R142-P1-01A`: Exact code/test SHA `dfcfa4e6400a0b01b8efb83c7685413bbcd1f32a`, draft-PR Actions #981 / API `39331493841`, job `118330932885` SUCCESS, nine configured gates. No emulator/device/benchmark evidence.
### Epic E143 — Preserve cancellation before compression serialization
#### Feature F145 — Check cancellation before saving compressed output
- [ACTIVE / INCOMPLETE / P2] `US-R143-P2-01A`: At `dfcfa4e6400a0b01b8efb83c7685413bbcd1f32a`, `PdfCompressor.compressWithJpegRasterization` calls `onProgress` after rendering the final page and then invokes `outDoc.save(outputFile)` without a cancellation check. If a progress callback cancels the coroutine, the expensive serialization may still execute. Introduce a JVM-testable cancellation preflight before save, preserving existing cleanup and exception behavior. Add focused core PDF JVM tests; require exact code/test SHA draft-PR nine-gate CI. Source-level P2 responsiveness risk; no reproduced crash. Keep PR #1 draft/unmerged, main unchanged.

## Cycle 143 authoritative completion / Cycle 144 activation — 2026-10-19 UTC
- [COMPLETE / PR CI #984] `US-R143-P2-01A`: Cycle 143 E143/F145/US-R143-P2-01A COMPLETE: exact production/test SHA `10fef67dc697a0046dce0fe5d628a8dc9c55e39f` passed authoritative draft-PR Actions #984 / API `39335370125`, job `118343523855`, all nine configured gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint); complete 181428-character job logs inspected with BUILD SUCCESSFUL. `PdfCompressor.kt` adds `saveCompressedPdfWithPreflight` immediately after final progress callback and before `PDDocument.save`; five focused host JVM regressions cover cancelled coroutine, exception identity, preflight/save ordering and success. Paired push #983 was cancelled by concurrency and is not acceptance. No emulator, physical-device, OEM, benchmark, peak-heap or complete race-freedom proof. PR #1 remains draft/unmerged; main unchanged.
### Epic E144 — Preserve cancellation before compression directory resolution
#### Feature F146 — Recheck cancellation after PDFBox initialization
- [ACTIVE / INCOMPLETE / P2] `US-R144-P2-01A`: At exact validated SHA `10fef67dc697a0046dce0fe5d628a8dc9c55e39f`, `PdfCompressor.compress` invokes `PdfBoxInit.ensure(context)` and then `DocForgeSettingsStore.resolveOutputDirectory` without a fresh cancellation preflight between them. If initialization cancels the coroutine (or cancellation occurs during it), output-directory resolution may still access preferences or create directories before the later staging/copy checks. This is a source-backed P2 responsiveness/side-effect concern, not a reproduced device failure. Implement minimal JVM-testable post-init pre-directory cancellation check, focused regression tests, and exact-code/test-SHA nine-gate draft-PR CI. PR #1 draft/unmerged; main unchanged.

## Cycle 144 completion / Cycle 145 activation — 2026-10-20 UTC
- [COMPLETE / CI #987] `US-R144-P2-01A`: Cycle 144 E144/F146/US-R144-P2-01A COMPLETE. Exact code/test SHA 1055f5def96a9ccb05d6408817f70defa550dd79 passed draft-PR Actions #987 (39351014549), job 118395389633, on 2026-10-20 UTC. All nine configured gates SUCCESS: six JVM test groups, debug APK, unsigned release/R8, Android lint. Six JVM regressions in PdfCompressorDirectoryPreflightTest.kt; PDFBox post-initialization cancellation check precedes output-directory resolution. Paired push #986 cancelled by concurrency, not validation failure. No device, emulator, benchmark, latency or race-freedom evidence. PR #1 draft/unmerged, main untouched.
### Epic E145 — Preserve cancellation before compression resource acquisition
#### Feature F147 — Preflight descriptor and renderer acquisition
- [ACTIVE / INCOMPLETE / P2] `US-R145-P2-01A`: Cycle 145 E145/F147/US-R145-P2-01A ACTIVE / INCOMPLETE before production mutation. Source at 1055f5def96a9ccb05d6408817f70defa550dd79: PdfCompressor.compressWithJpegRasterization opens ParcelFileDescriptor and constructs PdfRenderer before its first cancellation check in the page loop. Cancellation during cache-copy completion can cause avoidable descriptor/renderer acquisition. Add JVM-testable cancellation preflight before both opens; preserve nested resource ownership, error identity, page-count handling and output bytes. Add focused JVM regressions, require exact-SHA nine-gate draft-PR CI. P2 source-backed responsiveness concern, not a reproduced device crash.

## Cycle 145 completion / Cycle 146 activation — 2026-10-20 UTC
- [COMPLETE / CI #990] `US-R145-P2-01A`: Cycle 145 E145/F147/US-R145-P2-01A COMPLETE: exact production/test SHA `278427532d09d29bcadb5d9c4bc44ba8d7fb545d` passed authoritative draft-PR Actions #990 / API `39354257316`, job `118405368286`, on 2026-10-20 UTC. All NINE configured gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, Android lint); full 180130-character job logs inspected with BUILD SUCCESSFUL. `PdfCompressor.kt` adds cancellation preflights immediately before ParcelFileDescriptor and PdfRenderer opens; seven deterministic JVM regressions in `PdfCompressorResourceOpenPreflightTest.kt` cover cancelled opens, order, descriptor ownership/close, original exception identity, and successful nested cleanup. Paired push #989 cancelled by concurrency, not acceptance. No emulator, physical device, OEM, benchmark, peak-heap or complete race-freedom proof; PR #1 draft/unmerged and main untouched.
### Epic E146 — Preserve cancellation before compression document allocation
#### Feature F148 — Preflight output PDF document construction
- [ACTIVE / INCOMPLETE / P2] `US-R146-P2-01A`: At exact source/test SHA `278427532d09d29bcadb5d9c4bc44ba8d7fb545d`, `PdfCompressor.compressWithJpegRasterization` reads `renderer.pageCount` and validates a nonempty document, then constructs `PDDocument()` before the next cancellation check at the beginning of the page loop (`PdfCompressor.kt` lines 177-182). Cancellation during renderer page-count acquisition can therefore still allocate an output PDF document unnecessarily. Add a minimal JVM-testable cancellation preflight immediately before `PDDocument()` construction; preserve descriptor/renderer close ordering, empty-page error, original exception identity, page-count behavior, bitmap recycling, staging/no-overwrite, and output bytes. This is a source-backed P2 responsiveness concern, not a reproduced device failure. Add focused JVM tests and require exact-code/test-SHA nine-gate draft-PR CI.

### Epic E147 — Preserve cancellation before compression bitmap allocation
#### Feature F149 — Preflight raster bitmap allocation
- [ACTIVE / INCOMPLETE / P2] `US-R147-P2-01A`: At exact code/test SHA `aa8addd54ce046e39ca6ea5787e8caf3fbb73500`, compression may allocate an ARGB_8888 bitmap after cancellation during page opening or raster sizing because there is no immediate preflight before `Bitmap.createBitmap`. Add smallest JVM-testable cancellation preflight, deterministic cleanup/order tests and exact-SHA nine-gate draft-PR CI. Keep PR #1 draft/unmerged, main untouched.

## Cycle 146 authoritative completion / Cycle 147 activation — 2026-10-20 UTC
- Cycle 146 E146/F148/US-R146-P2-01A COMPLETE: exact production/test SHA `aa8addd54ce046e39ca6ea5787e8caf3fbb73500` passed draft-PR Actions #994 / API `39367662646`, job `118448084104`, completed SUCCESS 2026-10-20 04:21 UTC. All nine configured gates SUCCESS (domain/PDF/converter/scanner/PDF-tools/app JVM tests, debug APK, unsigned release/R8, Android lint). `PdfCompressor.kt` preflights cancellation before `PDDocument()` allocation after nonempty page-count validation; seven committed core PDF JVM regressions in `PdfCompressorDocumentOpenPreflightTest.kt` cover cancellation, ordering, constructor/processing failures, exception identity and nested cleanup. Paired push run `39367659901` cancelled by concurrency and is not acceptance. No emulator, physical device, OEM, benchmark, measured latency or race-freedom proof. PR #1 remains draft/unmerged, main unchanged.
- Cycle 147 E147/F149/US-R147-P2-01A ACTIVE / INCOMPLETE before production/test mutation. At `aa8addd54ce046e39ca6ea5787e8caf3fbb73500`, `PdfCompressor.compressWithJpegRasterization` calculates `boundedPdfRasterSize` and immediately allocates `Bitmap.createBitmap` without a fresh cancellation preflight. Cancellation during `renderer.openPage` or size computation can therefore still allocate a large ARGB_8888 bitmap before the next cooperative check. Introduce a minimal preflight directly before bitmap allocation, preserve bitmap recycling, page close, output staging, render behavior and exception identity; add focused deterministic core PDF JVM tests; validate exact code/test SHA via draft-PR nine-gate CI. Source-backed P2 responsiveness concern, not a reproduced Android device defect.

## Cycle 147 completion / Cycle 148 activation — 2026-10-20 UTC
- [COMPLETE / CI #997] `US-R147-P2-01A`: Cycle 147 E147/F149/US-R147-P2-01A COMPLETE: exact production/test SHA `626af3903dc940fc52591d42993241ecb2b96da8` passed authoritative draft-PR Actions #997 / API `39372246599`, job `118462490112`, all nine gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint). Eight focused `PdfCompressorBitmapAllocationPreflightTest` JVM regressions cover cancellation, ordering, allocation/render failure, cleanup and real coroutine cancellation. Paired push #996 was cancelled by concurrency, not acceptance. No emulator, physical-device, OEM, benchmark, measured cancellation latency or race-freedom evidence; PR #1 stays draft/unmerged, main untouched.
### Epic E148 — Preserve cancellation before compression JPEG encoding
#### Feature F150 — Preflight JPEG embedding after raster rendering
- [COMPLETE / CI #1000] `US-R148-P2-01A`: Cycle 148 E148/F150/US-R148-P2-01A ACTIVE / INCOMPLETE before production/test mutation. Source at `626af3903dc940fc52591d42993241ecb2b96da8`: after `PdfRenderer.Page.render`, `PdfCompressor.compressWithJpegRasterization` adds an output page and invokes potentially expensive `JPEGFactory.createFromImage` without rechecking coroutine cancellation. Cancellation during raster rendering may therefore still trigger JPEG encoding. Add a minimal preflight immediately before JPEG encoding; preserve original PDF dimensions, JPEG quality, output bytes on success, bitmap recycling, page/document/descriptor ownership, staged output, and original exception identity. Add focused core PDF JVM regressions and require exact-code/test-SHA nine-gate draft-PR CI. P2 source-backed responsiveness concern, not a reproduced device failure.

## Cycle 148 completion / Cycle 149 activation — 2026-10-20 UTC
- [COMPLETE / CI #1000] `US-R148-P2-01A`: Cycle 148 E148/F150/US-R148-P2-01A COMPLETE: exact production/test SHA `55f90c61625357d72949bcaa94ba658b6856c12e` passed authoritative draft-PR Actions #1000 / API `39384376248`, job `118501057462`, on 2026-10-20 UTC. All nine configured gates SUCCESS: six JVM test groups (domain/PDF/converter/scanner/PDF tools/app), debug APK, unsigned release/R8 and Android lint. `PdfCompressor.kt` checks cancellation immediately before JPEGFactory encoding; eight core PDF JVM tests in `PdfCompressorJpegEncodingPreflightTest.kt` cover cancellation, encoding order, exception identity, resource cleanup, JPEG parameters and actual coroutine cancellation. Paired push run 39384370861 was cancelled by workflow concurrency, not authoritative failure. No emulator, physical device, OEM, benchmark, measured cancellation latency or race-freedom evidence. PR #1 remains draft/unmerged; main untouched.
### Epic E149 — Preserve cancellation before compression PDF content-stream setup
#### Feature F151 — Preflight content-stream allocation after JPEG encoding
- [COMPLETE / PR CI #1004] `US-R149-P2-01A`: Cycle 149 E149/F151/US-R149-P2-01A ACTIVE / INCOMPLETE before production/test mutation. Source at `55f90c61625357d72949bcaa94ba658b6856c12e`, `PdfCompressor.kt` lines 210-218: after potentially long JPEGFactory.createFromImage returns, compression constructs `PDPageContentStream` and calls `drawImage` without a fresh coroutine cancellation preflight. Cancellation during JPEG encoding can therefore lead to avoidable PDF content-stream allocation and drawing before the next page-loop check. Add the smallest preflight immediately before stream creation, reuse `openCompressionResourceWithPreflight`, preserve stream/bitmap/page/document/descriptor cleanup, exception identity, output dimensions, JPEG quality, staging/no-overwrite, and successful bytes. Add focused core PDF JVM regressions; require authoritative exact-code/test-SHA nine-gate draft-PR CI. This is a source-backed P2 responsiveness concern, not a reproduced device defect.

## Cycle 149 authoritative completion / Cycle 150 activation — 2026-10-20 UTC
- [COMPLETE / PR CI #1004] `US-R149-P2-01A`: E149/F151 completed at exact source/test SHA `3c9705a65ada389cb6336411e1c9a8a6fe67ac49`, draft-PR Actions #1004 / API `39386914554`, job `118508930835`, nine gates SUCCESS. Eight core PDF JVM regressions cover content-stream preflight and cleanup. No Android device/emulator/benchmark evidence; PR #1 draft, main unchanged.
### Epic E150 — Preserve cancellation before compression page rendering
#### Feature F152 — Preflight rasterization after bitmap/canvas setup
- [ACTIVE / INCOMPLETE / P2] `US-R150-P2-01A`: Source at `287ebeda470f8d793b00b98afbb88f56ccb1b6cb` shows `PdfCompressor.compressWithJpegRasterization` calls potentially expensive `PdfRenderer.Page.render` after bitmap allocation, Canvas creation and white fill without a fresh cancellation preflight. Cancellation during bitmap setup may initiate unnecessary rendering. Add minimal existing-helper preflight before render; preserve output, cleanup and exception identity, add deterministic core PDF JVM regressions, and require exact code/test SHA nine-gate draft-PR CI. Source-level P2 responsiveness concern, not a reproduced device defect.

## Cycle 150 authoritative completion / Cycle 151 activation — 2026-10-20 UTC
- [COMPLETE / CI #1009] `US-R150-P2-01A`: Cycle 150 E150/F152/US-R150-P2-01A COMPLETE: exact production/test SHA `e9f14e592cb242ddae842600e840155fde872f6f` passed authoritative draft-PR Actions #1009 / API `39412349453`, job `118592955874`, 2026-10-20 UTC. All nine configured gates SUCCESS: domain, PDF, converter, scanner, PDF tools and app JVM tests; debug APK; unsigned release/R8; Android lint. `PdfCompressor.kt` preflights cancellation immediately before `PdfRenderer.Page.render` after canvas white fill. Eight committed JVM regressions in `PdfCompressorPageRenderPreflightTest.kt` cover cancellation, ordering, original exceptions, bitmap/resource cleanup, multiple pages and actual coroutine cancellation. Job log confirms BUILD SUCCESSFUL. Paired push run `39412342440` was cancelled by concurrency and is not acceptance evidence. No emulator, physical device, OEM, benchmark, measured latency or race-freedom proof. PR #1 remains draft/unmerged, main unchanged.
### Epic E151 — Preserve cancellation before compression output-page allocation
#### Feature F153 — Preflight PDFBox page construction after rendering
- [ACTIVE / INCOMPLETE / P2] `US-R151-P2-01A`: Cycle 151 E151/F153/US-R151-P2-01A ACTIVE / INCOMPLETE before production/test mutation. Source at exact validated SHA `e9f14e592cb242ddae842600e840155fde872f6f`: after potentially expensive `PdfRenderer.Page.render`, `PdfCompressor.compressWithJpegRasterization` constructs `PDPage(PDRectangle(...))` and adds it to `outDoc` before the next cancellation preflight at `JPEGFactory.createFromImage`. Cancellation during rendering can therefore cause avoidable output-document page allocation/mutation. Introduce the smallest JVM-testable cancellation preflight before page construction/addition, preserving page dimensions, JPEG quality, bitmap recycling, page/document/descriptor ownership, exception identity, staging/no-overwrite and successful output bytes. Add deterministic core PDF JVM regressions; require authoritative exact code/test SHA nine-gate draft-PR CI. Source-level P2 responsiveness risk, not a reproduced Android device defect.

## Cycle 151 authoritative completion / Cycle 152 activation — 2026-10-20 UTC
- [COMPLETE / CI #1014] `US-R151-P2-01A`: Cycle 151 E151/F153/US-R151-P2-01A COMPLETE: exact production/test SHA `7a1beab654c06e34c4acdf7dfe900805a0079ffa` passed authoritative draft-PR Actions #1014 / API `39448544076`, job `118716885293`, on 2026-10-20 UTC. All nine configured gates SUCCESS (six JVM test groups, debug APK, unsigned release/R8, Android lint); full job logs inspected and `BUILD SUCCESSFUL` confirmed. `PdfCompressor.kt` checks cancellation before output `PDPage` allocation/addPage after render; eight committed `PdfCompressorOutputPagePreflightTest` JVM regressions cover cancellation, ordering, exception identity, cleanup, multiple pages and actual coroutine cancellation. Paired push #1013 / API `39448536354` was cancelled by workflow concurrency, not a validation failure. No Android emulator, physical device, OEM, benchmark, measured cancellation latency or race-freedom evidence. PR #1 draft/unmerged, main unchanged.
### Epic E152 — Preserve cancellation before compression progress callbacks
#### Feature F154 — Preflight progress notification after PDF content drawing
- [ACTIVE / INCOMPLETE / P2] `US-R152-P2-01A`: Cycle 152 E152/F154/US-R152-P2-01A ACTIVE / INCOMPLETE before production/test mutation. Source at `7a1beab654c06e34c4acdf7dfe900805a0079ffa`: after rendering, JPEG encoding and PDF content-stream drawing, `PdfCompressor.compressWithJpegRasterization` calls `onProgress?.invoke(i + 1, renderer.pageCount)` without a fresh cancellation preflight; cancellation during page encoding/drawing can still invoke a stale progress callback before the next page-loop check or save preflight. Add a minimal preflight immediately before the callback using existing `checkCancelled`, preserving successful progress count/order, cleanup, exception identity, output bytes and staged publication. Add deterministic core PDF JVM regressions and require exact-SHA nine-gate draft-PR CI. Source-backed P2 callback-responsiveness concern, not a reproduced device crash.


## Cycle 152 authoritative completion / review freeze — 2026-10-20 UTC
- Cycle 152 `E152/F154/US-R152-P2-01A` COMPLETE. Exact production/test SHA `1d7ed3b50cca63ab53e903b74352962575a4cdd1` passed authoritative draft-PR Actions #1019 / API `39461182825`, job `118760852338`. All nine configured gates SUCCESS: core domain, core PDF, converter, scanner, PDF-tools and app JVM tests; debug APK; unsigned release/R8; Android lint.
- Production adds a cancellation preflight immediately before non-null compression progress callbacks. Six committed `PdfCompressorProgressPreflightTest` JVM regressions cover cancellation skip, ordering/arguments, absent callback behavior, callback exception identity, multi-page ordering and actual coroutine cancellation. A missing `ensureActive` test import was corrected in the final validated SHA.
- Review-readiness audit: PR #1 is mergeable, branch was 0 commits behind `main` at the audit, and there were no unresolved inline review threads or submitted change-request reviews. The cumulative PR is intentionally large and requires human review before merge.
- REVIEW FREEZE: do not activate another autonomous improvement cycle or mutate production code until explicit reviewer/user feedback reopens engineering. This is review readiness, not production certification.
- Residual evidence limits: no Android emulator/physical-device/OEM compatibility run, benchmark, measured cancellation latency, exhaustive race proof, or claim that every latent defect is eliminated. `main` remains untouched.

- [COMPLETE / CI #1019 / REVIEW FREEZE] `US-R152-P2-01A` — Final active canonical story closed; no next cycle activated.
