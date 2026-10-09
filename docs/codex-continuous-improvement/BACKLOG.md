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
