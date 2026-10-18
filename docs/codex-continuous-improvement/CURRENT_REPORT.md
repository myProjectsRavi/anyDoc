# Cycle 109 — PDF compare raster diff performance

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E109 / F111 / US-R109-P2-01A
**Previous cycle:** Cycle 108 COMPLETE: exact production/test SHA `d7712402afd910c76ee3b692053ad4269f8735a3` passed authoritative draft-PR Actions #855 / API `39102640344`, job `117602763202` on 2026-10-18 UTC: all nine configured gates SUCCESS (core domain, core PDF, converter, scanner, PDF tools and app JVM tests, debug APK, unsigned release/R8, Android lint). Paired push `39102637960` was cancelled by concurrency, not acceptance evidence. Bounded ASCII filename sanitization preserves the 200-character cap, lazy blank fallback, Java regex codepoint semantics for valid surrogate pairs, unpaired surrogate replacement and numbered collision publication. Five added focused JVM test methods include 10,000 deterministic UTF-16 equivalence cases and 250,000 supplementary-codepoint input. No Android device/emulator/OEM, allocation benchmark or peak-memory proof.

## Source evidence and acceptance
`PdfCompareTool.diffBitmaps` currently performs per-pixel `left.getPixel`, `right.getPixel`, and `output.setPixel` operations for every pixel. This is a source-level CPU overhead risk on large PDFs, not a measured ANR or benchmark. Assess bounded row buffers without violating heap-aware bitmap budget; preserve exact output color and difference percentages, unequal page white padding, missing-page behavior, 4096-column cancellation boundaries, exception cleanup and bitmap ownership. Add deterministic JVM equivalence tests and authoritative exact-SHA draft-PR CI.

## Next action
Inspect existing PDF compare regression coverage and source, implement the smallest compatible row-buffer change only after evidence, then exact-SHA CI and canonical reconciliation. Keep PR #1 draft/unmerged and main untouched.
