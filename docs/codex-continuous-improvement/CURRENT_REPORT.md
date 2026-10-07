# Current Report

**ID:** `2026-10-07_cycle-041`
**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic:** `E041` — Large-input HTML conversion safety
**Feature:** `F043` — Bounded HTML ingestion
**User Story:** `US-R041-P2-01A` — Bound HTML URI and raw-string ingestion before WebView rendering

## Starting evidence
Cycle 040 exact candidate `74f24148f5268744debc7158388af4bf38081283` passed authoritative GitHub Actions PR run #480 / API `37639874324` across core PDF tests, converter tests, scanner tests, app lifecycle tests, debug APK, unsigned release/R8, and Android lint. Sol 5.6 review is LGTM.

## Source finding
`HtmlPdfConverter.convertToPdf()` reads the complete HTML URI with `BufferedReader.readText()` without a ceiling. `convertHtmlStringToPdf()` also accepts arbitrary raw HTML and creates a WebView before any size guard, leaving a bypass if only URI ingestion is hardened.

## Acceptance criteria
- Bound HTML URI ingestion with the same conservative text-character ceiling used by Cycle 040.
- Reject oversized raw HTML before WebView creation/rendering.
- Preserve ordinary HTML conversion, staged PDF publication, WebView cleanup, and output naming.
- Add deterministic pure-JVM boundary coverage for the raw-content size policy.
- Pass authoritative GitHub Actions on the exact code/test candidate.
- Do not modify or merge `main`.

## Next exact action
Add a pure size-policy helper, wire bounded URI and raw-string checks into `HtmlPdfConverter`, add exact-limit/over-limit tests, then validate the exact feature-branch candidate.
