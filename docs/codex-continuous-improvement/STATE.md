# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-08_cycle-048`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 048
**Current Epic:** `E048` — Signature placement template integrity
**Current Feature:** `F050` — Failure-safe placement template persistence
**Current User Story:** `US-R048-P1-01A` — Preserve saved templates on failed update (ACTIVE)
**Last completed cycle:** Cycle 047
**State checkpoint timestamp:** 2026-10-07 UTC

## Current next executable step
Harden `SignaturePlacementTemplateStore.writeTemplates()`: it writes to a fixed `.tmp` path and calls `renameTo(templatesFile)` without checking success, so failed publication can silently report success and leave old or incomplete state. Implement unique same-directory staging and checked atomic replacement, with failure-preservation JVM regressions and feature-module CI. Also prevent unreadable/corrupt template data from silently being treated as an empty set before mutation. This checkpoint precedes Cycle 048 production/test mutation.

## Git checkpoint
- Baseline `main`: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`
- Cycle 037 validated code/test SHA: `2e6755b7bc51c291232ec3d2b82ad2319a018dca`
- Cycle 037 CI: run #443 / API `37453064894` — SUCCESS
- Cycle 037 documentation checkpoint: `78a750b3bafd51a4ef5d7fdca5ae963bcda902b4`; run #444 / API `37457391483` — SUCCESS

## Cycle 037 completion evidence
Unknown-size URI cache copies preflight the 32 MiB reserve plus one 8 MiB maximum transfer chunk before each transfer, while retaining post-transfer reserve, cancellation, and cleanup behavior. Deterministic boundary coverage rejects 40 MiB - 1 and accepts exactly 40 MiB.

## Cycle 038 source evidence
`VideoAudioExtractor.selectBufferSize()` trusts `MediaFormat.KEY_MAX_INPUT_SIZE` without an upper bound and passes the result directly to `ByteBuffer.allocate()`. Malformed or extreme container metadata can therefore request an unreasonable heap allocation before extraction begins.

## Next executable step
Introduce a deterministic conservative sample-buffer ceiling, reject metadata above that ceiling before allocation, preserve the existing 256 KiB fallback/minimum behavior, add converter-module boundary tests, and validate the exact candidate through authoritative GitHub Actions.

## Cycle 038 completion evidence
- Exact validated code/test SHA: `a1caa7451a21b2df7744b2f481d357630c1e886d`.
- GitHub Actions PR run #453 / API `37541238001`: **SUCCESS**.
- `VideoAudioExtractor` preserves the 256 KiB fallback/minimum and rejects sample-buffer metadata above the conservative 8 MiB ceiling before allocation.
- Pure-JVM boundary coverage includes absent metadata, 64 KiB, 1 MiB, exact 8 MiB, and 8 MiB + 1 rejection.
- No emulator, benchmark, or physical-device evidence is claimed.

## Next executable step
Select the highest-priority remaining evidence-backed P2 allocation/large-input story, activate Cycle 039 durably, then mutate production code only after that checkpoint exists.


## Cycle 039 completion evidence
- Exact validated code/test SHA: `810fc18675cf22f14620c78a1ac0745afe346e59`.
- GitHub Actions PR run #464 / API `37587816150`: **SUCCESS**.
- Typed-signature raster sizing is bounded before allocation and focused JVM boundary regressions cover safe dimensions, exact limits, overflow, invalid padding, and non-finite measurements.
- No emulator, benchmark, or physical-device evidence is claimed.

## Next executable step
Select the highest-priority remaining evidence-backed P2 large-input/full-file-copy story and activate Cycle 040 durably before production mutation.


## Cycle 040 completion evidence
- Exact validated code/test SHA: `74f24148f5268744debc7158388af4bf38081283`.
- GitHub Actions PR run #480 / API `37639874324`: **SUCCESS**.
- TXT, RTF, and CSV ingestion now use a shared 4 Mi-character bounded reader before full in-memory materialization.
- Focused converter JVM regressions cover below-limit and exact-limit reads, one-character-over rejection, invalid limits, Int.MAX_VALUE arithmetic safety, and CSV empty/trailing-newline semantics.
- Sol 5.6 review found and repaired CSV return-type semantics, trailing-line behavior, and bounded-reader integer-overflow risk before closure.
- No emulator, benchmark, or physical-device evidence is claimed.

## Cycle 041 starting evidence
`HtmlPdfConverter.convertToPdf()` still reads an HTML content URI with unbounded `readText()`. The public/raw `convertHtmlStringToPdf()` entry point also accepts arbitrarily large strings before WebView creation, so URI-only hardening would leave a bypass.

## Next executable step
Reuse the bounded text reader for HTML URI ingestion, enforce the same conservative character ceiling on raw HTML before WebView rendering, add focused converter-module boundary tests, and validate the exact candidate through authoritative GitHub Actions.


## Cycle 041 completion evidence
- Exact validated code/test SHA: `45e9f3bffcc50d7458629ec332c550c2eaab1011`.
- GitHub Actions PR run #483 / API `37644316676`, attempt 2: **SUCCESS**.
- Attempt 1 failed before Cycle 041 code compiled because Maven dependency resolution could not fetch `org.jetbrains.kotlin:kotlin-script-runtime:2.1.0`; no code change was made for that infrastructure failure.
- HTML content URI reads now use the shared 4 Mi-character bounded reader.
- Raw HTML is rejected above the same ceiling before dispatching to Main or creating a WebView.
- Focused JVM tests cover below-limit, exact-limit, one-over, and invalid-limit behavior.
- Sol 5.6 review: LGTM.
- No emulator, benchmark, or physical-device evidence is claimed.

## Cycle 042 starting evidence
`PdfRedactionTool` calls `PDFTextStripper().getText(document)` across the entire PDF for auto-detect PII, then materializes the entire output text again during irreversible verification. This duplicates avoidable whole-document text heap pressure even though redaction itself is page-oriented.

## Next executable step
Refactor auto-detect and irreversible verification to extract and inspect one PDF page at a time, preserve term detection and annotation verification semantics, add focused pure-JVM paging/early-exit regression coverage, and validate the exact candidate through authoritative GitHub Actions.


## Cycle 042 completion evidence
- Exact validated code/test SHA: `6e0fa61975c01fddea83f0b78b726e4f689d9808`.
- GitHub Actions PR run #492 / API `37665821129`: **SUCCESS**.
- Page-bounded redaction scans and focused paging regressions validated.

## Cycle 043 starting evidence
The durable backlog identifies `PdfCompareTool` combined left/right/diff ARGB_8888 raster memory as unresolved P2 work.

## Next executable step
Inspect comparison raster sizing and lifetimes, add an overflow-safe combined-memory ceiling with focused tests, then validate the exact candidate through authoritative GitHub Actions.


## Cycle 043 completion and Cycle 044 activation — 2026-10-08 UTC
- Cycle 043 validated code/test SHA: `ed6d500546c883108e15c722c0a2ce98b1b2f256`.
- Authoritative PR run #499 / API `37713745007`: SUCCESS across all seven configured gates.
- Cycle 043 completion report committed at `867585e4684f6a353f1fd5a400ccc39a6fb8ab9d` and its exact docs HEAD passed PR run #500 / API `37718067659` (all seven gates SUCCESS).
- Cycle 043 is COMPLETE; do not restart it absent contradictory evidence.
- **Cycle 044 is ACTIVE / INCOMPLETE**: Epic `E044`, Feature `F046`, Story `US-R044-P2-01A`.
- Source evidence: `PdfTextExtractor.extractToTxt` uses `PDFTextStripper.getText(document)` to materialize entire PDF text, then `toByteArray(UTF_8)` before writing staged TXT, duplicating large output memory.
- **Next exact mutation:** replace whole-document `getText()`/UTF-8 byte-array materialization with `PDFTextStripper.writeText(document, Writer)` directly into the staged file. Preserve sorted extraction, blank/whitespace fallback text, `extractedChars`, UTF-8 encoding, and staged publication; add pure JVM streaming/whitespace/count/failure regressions; run authoritative CI on exact code/test SHA. No device/benchmark evidence claimed.


## Cycle 044 completion and Cycle 045 activation — 2026-10-08 UTC
- Cycle 044 validated code/test SHA: `2f6561ad5014b8aa96a9384c3fbe0ca588079c6f`. Authoritative PR run #503 / API `37728082557`: SUCCESS, all seven gates (core PDF, converter, scanner, app tests, debug APK, unsigned release/R8, Android lint).
- PDF text streams directly to staged UTF-8 output with fallback and character counting. Seven focused JVM regressions included. No emulator, physical-device or benchmark evidence.
- Cycle 044 COMPLETE; Cycle 045 ACTIVE / INCOMPLETE: `E045` → `F047` → `US-R045-P1-01A`.
- Source evidence: `PdfAComplianceTool.buildPdfAXmpMetadata` inserts raw title, author and producer in XML text nodes, corrupting XMP for ampersands, angle brackets or XML-invalid code points.
- Next exact mutation: XML-safe text escaping/sanitization and focused JVM XML parsing/round-trip tests, followed by exact-SHA CI.
- Blockers: none at activation. PR #1 remains draft; main must remain untouched.


## Cycle 045 completion and Cycle 046 activation — 2026-10-08 UTC
- Cycle 045 exact code/test SHA `c57c522549f79216623981d0d85a60fdcf48e823`: authoritative PR run #506 / API `37733161386` SUCCESS (all seven gates).
- Cycle 045 COMPLETE. Cycle 046 ACTIVE: `E046` / `F048` / `US-R046-P1-01A`.
- Source evidence: `PdfBatchStampTool` increments an Int Bates counter; after Int.MAX_VALUE it wraps negative, and the formatter coerces the label to zero. This silently corrupts document numbering.
- Next: checked Long counter, deterministic boundary tests, exact-SHA CI, then synchronize CURRENT_REPORT/BACKLOG/VALIDATION and cycle reports.
- PR #1 draft/unmerged; main untouched. No device/benchmark evidence claimed.


## Cycle 046 completion / Cycle 047 activation — 2026-10-08 UTC
- Cycle 046 `E046 / F048 / US-R046-P1-01A`: COMPLETE. Exact production/test SHA `d87426a1d042d571eb72f8b43313bedad63205e7`; authoritative PR run #516 / API `37750707619` SUCCESS on this SHA, all seven gates (core PDF, converter, scanner, app tests, debug assembly, unsigned release/R8, lint).
- Checked Long Bates sequence rejects rollover beyond Int.MAX_VALUE, including across pages/files; six core PDF JVM regressions cover maximum, overflow, consecutive pages, multi-file continuity, nonpositive start, and repeated rollover rejection. No emulator, benchmark or physical-device evidence.
- Cycle 047 ACTIVE / INCOMPLETE: `E047 / F049 / US-R047-P1-01A` — prevent saved signature data loss on failed PNG replacement.
- Source evidence: `SavedSignatureStore.save()` opens the existing slot with `FileOutputStream(file)`, truncating the old PNG before compression, and ignores the Boolean result of `bitmap.compress`. A false return or exception can destroy the previously saved signature.
- Next: implement same-directory atomic replacement preserving the prior slot on failure, reject failed PNG compression, add focused deterministic tests, include feature-module test task in authoritative CI, and validate exact code/test SHA. No Cycle 047 production/test mutation is claimed at activation.
- PR #1 remains draft/unmerged; main untouched.


## Cycle 047 completion / Cycle 048 activation — 2026-10-08 UTC
- Cycle 047 `E047 / F049 / US-R047-P1-01A`: COMPLETE. Exact code/test SHA `a38e779267f7300e8ebc86197e8ce25137a99326`; authoritative PR run #519 / API `37793452165`: SUCCESS, all eight gates (core PDF, converter, scanner, PDF tools signature, app unit tests, debug APK, unsigned release/R8, Android lint). The paired push run #518 was cancelled by the shared concurrency group and is not completion evidence.
- `SavedSignatureStore.save()` now stages PNG bytes in the same directory, rejects `Bitmap.compress()` failure, checks interruption, syncs staged bytes and atomically replaces the final slot. Seven focused feature JVM regressions cover success, failure preservation, first-save cleanup and interruption. Independent sandbox Kotlin smoke covered eight scenarios; no Android device, emulator, benchmark or power-loss durability claim.
- Cycle 048 ACTIVE / INCOMPLETE: `E048 / F050 / US-R048-P1-01A` — failure-safe saved placement template persistence. Source: `SignaturePlacementTemplateStore.writeTemplates()` uses a shared fixed `.tmp` file and unchecked `renameTo`; `readTemplates()` silently returns an empty list on malformed stored JSON, risking data loss on the next mutation.
- Next exact action: implement unique same-directory staged JSON publication with checked atomic replacement; reject unreadable/corrupt prior state before save/delete; add deterministic feature JVM tests and validate exact code/test SHA. No Cycle 048 production/test mutation at activation. PR #1 draft/unmerged; main untouched.
