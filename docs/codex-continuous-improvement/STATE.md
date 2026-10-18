# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never modify or merge `main`.
**Current report:** `2026-10-18_cycle-109`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 109
**Current Epic:** `E109` — PDF compare raster diff performance
**Current Feature:** `F111` — Bounded row-buffer pixel comparison
**Current User Story:** `US-R109-P2-01A` — Reduce per-pixel Bitmap access overhead while preserving exact diff output (ACTIVE)
**Last completed cycle:** Cycle 108
**State checkpoint timestamp:** 2026-10-18 UTC
**Starting HEAD:** `d7712402afd910c76ee3b692053ad4269f8735a3`

## Canonical checkpoint
- Cycle 108 `E108/F110/US-R108-P2-01A` COMPLETE: exact production/test SHA `d7712402afd910c76ee3b692053ad4269f8735a3` passed authoritative draft-PR Actions #855 / API `39102640344`, job `117602763202` on 2026-10-18 UTC: all nine configured gates SUCCESS (core domain, core PDF, converter, scanner, PDF tools and app JVM tests, debug APK, unsigned release/R8, Android lint). Paired push `39102637960` was cancelled by concurrency, not acceptance evidence. Bounded ASCII filename sanitization preserves the 200-character cap, lazy blank fallback, Java regex codepoint semantics for valid surrogate pairs, unpaired surrogate replacement and numbered collision publication. Five added focused JVM test methods include 10,000 deterministic UTF-16 equivalence cases and 250,000 supplementary-codepoint input. No Android device/emulator/OEM, allocation benchmark or peak-memory proof.
- Cycle 109 `E109/F111/US-R109-P2-01A` ACTIVE **before production/test mutation**. Source-level evidence: `PdfCompareTool.diffBitmaps` invokes `getPixel` for left/right and `setPixel` for output in its inner pixel loop; these Bitmap APIs are called once or more per pixel. Audit row-buffer tradeoffs under the existing three-bitmap memory budget, preserve missing-page behavior, per-pixel RGB threshold/grey calculations, cancellation checks every 4096 columns and output percentages. Do not claim performance improvements without measurements.
- **Next exact action:** inspect existing PDF comparison tests and the diff loop; build a pure JVM-testable equivalence seam for bounded row chunks, then implement the smallest safe row-buffer optimization with focused regressions and exact-SHA draft-PR CI. Reconcile all canonical documents after terminal CI.
- **PR #1:** open/draft/unmerged; main untouched.

## History preservation
- Cycle 108 prior STATE.md at `d7712402afd910c76ee3b692053ad4269f8735a3`; previous Cycle 107 completion at `82ceba75aecf724f85d4605f7a304cfa2d71b6ec`. Earlier history remains in BACKLOG.md, VALIDATION.md and cycle reports. Original main SHA `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`.
