# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never implement/merge autonomous continuous-improvement work directly on `main`.
**Current report:** `2026-10-07_cycle-041`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 041
**Current Epic:** `E041` — Large-input HTML conversion safety
**Current Feature:** `F043` — Bounded HTML ingestion
**Current User Story:** `US-R041-P2-01A` — Bound HTML URI and raw-string ingestion before WebView rendering (ACTIVE)
**Last completed cycle:** Cycle 040
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
