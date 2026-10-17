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
