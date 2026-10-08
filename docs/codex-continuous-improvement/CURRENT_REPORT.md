# Current Report

**ID:** `2026-10-08_cycle-044`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E044` — Memory-safe PDF text extraction
**Feature:** `F046` — Streamed PDF-to-TXT publication
**User Story:** `US-R044-P2-01A` — Stream PDF text directly into staged TXT

## Prior cycle validation
Cycle 043 exact code/test SHA `ed6d500546c883108e15c722c0a2ce98b1b2f256` passed PR run #499 / API `37713745007`. Completion report commit `867585e4684f6a353f1fd5a400ccc39a6fb8ab9d` passed PR run #500 / API `37718067659`. All seven configured gates succeeded in both runs.

## Source finding
`PdfTextExtractor.extractToTxt` calls `PDFTextStripper.getText(document)` across all pages, holds that String, and then allocates a second full `toByteArray(Charsets.UTF_8)`. Large extracted PDFs can transiently duplicate full-output memory.

## Acceptance criteria
- Stream `PDFTextStripper.writeText(document, Writer)` directly into the same-directory staged TXT, without full-document String or UTF-8 byte-array materialization.
- Preserve `sortByPosition`, full-page range, UTF-8 encoding, output filename policy, and transactional no-partial-final semantics.
- Preserve fallback `[No extractable text found in this PDF.]` for empty or whitespace-only extracted text.
- Preserve `extractedChars` semantics (UTF-16 character units of published text), safely handle overflow.
- Add focused pure JVM tests for chunked output, whitespace-only, nonblank, Unicode/surrogates, writer errors, and staged cleanup.
- Pass authoritative GitHub Actions on the exact code/test candidate; keep PR #1 draft/unmerged; never modify main.

## Next exact action
Implement a small testable streaming Writer/character-count helper, wire `PdfTextExtractor` to PDFTextStripper's streaming API inside `withStagedOutputFile`, add JVM regressions, and validate exact SHA. No device or benchmark evidence is claimed.
