# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-07_cycle-043`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 043
**Current Epic:** `E043` — PDF comparison raster memory safety
**Current Feature:** `F045` — Combined comparison raster budget
**Current User Story:** `US-R043-P2-01A` — Bound PdfCompareTool combined raster memory (ACTIVE)
**Last completed cycle:** Cycle 042
**State checkpoint timestamp:** 2026-10-07 UTC

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
