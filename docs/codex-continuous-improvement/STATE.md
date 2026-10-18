# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never modify or merge `main`.
**Current report:** 2026-10-18_cycle-120
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 120
**Current Epic:** `E120` — PDF compare descriptor-open cancellation
**Current Feature:** `F122` — Preflight before opening either cached PDF descriptor
**Current User Story:** `US-R120-P2-01A` — Avoid cached PDF descriptor acquisition after cancellation (ACTIVE / INCOMPLETE)
**Last completed cycle:** Cycle 119
**State checkpoint timestamp:** 2026-10-18 UTC
**Last validated code/test HEAD:** `64967addfc3ebbd56506220899ad5ed0b70dcca9`
**Current branch HEAD at activation:** 64967addfc3ebbd56506220899ad5ed0b70dcca9

## Canonical checkpoint
- Cycle 114 E114/F116/US-R114-P2-01A COMPLETE: exact code/test SHA `1ad7ed5e777219a13db58a3772dd6c3246fa693c` passed authoritative draft-PR Actions #879 / API `39128006515`, job `117677147217`, on 2026-10-18 UTC. All nine configured gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, Android lint). Seven focused JVM tests in PdfComparePageOpenCancellationTest cover cancellation before opening, preflight/open errors, successful render and cleanup. The initial production commit `1609f7f0d172ad3f33954b94b8b7bab2e0db524b` inadvertently omitted the base tree; forward recovery `1ad7ed5e777219a13db58a3772dd6c3246fa693c` restored the full repository tree and exact candidate passed CI. No emulator, physical device, benchmark, or measured latency evidence. PR #1 stays draft/unmerged; main untouched.
- Cycle 115 E115/F117/US-R115-P2-01A COMPLETE: exact production/test SHA `6be844634382b60203f8c6813b39d95794517ad2` passed authoritative draft-PR Actions #882 / API `39131997093`, job `117688922042`, on 2026-10-18 UTC. All nine configured gates SUCCESS: six JVM groups, debug APK, unsigned release/R8 and Android lint. Eight focused JVM regressions in PdfComparePageSizeCancellationTest cover cancellation, page dimensions, closing, exceptions and left/right sequencing. Documentation-only #883 / API `39134307442` also succeeded at `8945dab4b8521e71b5e284641757ce9f8b9678c0`; it is not the production validation. No emulator, physical device, benchmark or measured latency evidence.
- Cycle 116 E116/F118/US-R116-P2-01A COMPLETE: exact production/test SHA `a6ef1d8c7d989155fd3a4217e65301066c8a2369` passed authoritative draft-PR Actions #886 / API `39136348187`, job `117701568583`, on 2026-10-18 UTC. All nine configured gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, Android lint). New `openCompareRendererWithPreflight` checks cancellation before left/right PdfRenderer construction; eight focused JVM regressions cover no-open cancellation, ordering, two-renderer ownership and original/suppressed exceptions. Supplementary isolated Kotlin host checks 4/4 PASS; these do not establish Android device performance. The paired push #885 was cancelled by concurrency. No emulator/physical-device/OEM/benchmark or measured latency evidence.
- Cycle 117 E117/F119/US-R117-P2-01A COMPLETE; exact production/test SHA `8599bc47cd87872ed3e35bacffcec214eaef397d` passed Actions #889.
- **Next exact action:** Cycle 120: add cancellation preflight before opening the first and second cached PDF ParcelFileDescriptor, preserving close-on-failure, then add deterministic JVM regressions and validate exact code/test SHA with draft-PR CI. No source mutation before this activation.
- **PR #1:** open/draft/unmerged; `main` untouched.

## History preservation
- Cycle 113 exact code/test SHA `c95a0c28094b7e4b958726b6a47584e0cbd7d13c` passed Actions #876 / API `39125693956`.
- Cycle 112 exact code/test SHA `418c37ccaa3e91ce4c717c65538685a7f2dfdbd8` passed Actions #871 / API `39119917326`.
- Earlier history remains in BACKLOG.md, VALIDATION.md and cycle reports. Original main SHA `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`.


## Cycle 117 authoritative validation — 2026-10-18 UTC
- `E117/F119/US-R117-P2-01A` COMPLETE for the bounded cancellation preflight immediately before starting a diff PDF output page. Exact source/test SHA `8599bc47cd87872ed3e35bacffcec214eaef397d` passed draft-PR Actions #889 / API `39137841432`, job `117705942735`, all nine configured gates SUCCESS (domain/PDF/converter/scanner/PDF tools/app JVM, debug APK, unsigned release/R8, Android lint).
- `PdfCompareTool.kt` calls `startComparePdfPageWithPreflight(checkCancelled)` before `pdfDoc.startPage`; six `PdfCompareOutputPageStartCancellationTest` JVM regressions cover cancellation before start, success ordering, preflight/start errors, cancellation after raster work and between output pages. Scope: source and JVM CI only; no Android emulator, device, OEM, performance or cancellation-latency proof.
- Keep PR #1 draft/unmerged and main untouched. Next: inspect remaining highest-priority concrete risks and activate the next cycle before any code mutation.

## Cycle 118 active checkpoint — 2026-10-18 UTC
- E118/F120/US-R118-P2-01A ACTIVE before production mutation. Branch baseline `e488018383b565182215c31a0f784aaca90634eb`. Source: after page rendering, PdfCompareTool.compare opens FileOutputStream(stagedFile) and calls PdfDocument.writeTo without a fresh cancellation check before setup; the CancellationCheckingOutputStream checks only at write time. Add preflight before stream creation, preserve output/cleanup, add deterministic JVM tests and exact-SHA CI. PR #1 stays draft/unmerged, main untouched.

## Cycle 118 completion / Cycle 119 activation — 2026-10-18 UTC
- Cycle 118 E118/F120/US-R118-P2-01A COMPLETE: exact production/test SHA `6b8810da42bd65e1bad5c426e574fc6aa3053cb6` passed authoritative draft-PR Actions API `39143193841`, job `117721649795` (2026-10-18 UTC). All nine configured gates SUCCESS: six JVM groups, debug APK, unsigned release/R8 and Android lint. `writeComparePdfWithPreflight` checks cancellation before opening staged FileOutputStream and before PdfDocument.writeTo; nine focused JVM regressions cover cancellation, opening, writing, close and exception ownership. The paired push run `39143190052` was cancelled by concurrency; this is not a failing validation gate. No Android emulator, physical device, benchmark, peak-memory or cancellation-latency evidence. PR #1 remains draft/unmerged; main unchanged.
- Cycle 119 E119/F121/US-R119-P2-01A ACTIVE / INCOMPLETE before any production/test mutation. Source evidence at `6b8810da42bd65e1bad5c426e574fc6aa3053cb6`: `PdfCompareTool.compare` constructs `android.graphics.pdf.PdfDocument()` inside the staged output callback without a fresh cancellation preflight. A cancellation after staging and before document construction may still allocate an output document before the next page-loop check. Introduce a minimal JVM-testable preflight helper immediately before constructing PdfDocument, preserve close-on-failure, page lifecycle, staged output, exception identity, and normal output bytes. Add focused JVM regression tests; validate exact code/test SHA via draft-PR CI. Source-level responsiveness concern, not a reproduced crash.

## Cycle 119 authoritative completion / Cycle 120 activation — 2026-10-18 UTC
- Cycle 119 E119/F121/US-R119-P2-01A COMPLETE: exact production/test SHA `64967addfc3ebbd56506220899ad5ed0b70dcca9` passed authoritative draft-PR Actions #898 / API `39147699102`, job `117734802444`, all nine gates SUCCESS (six JVM groups, debug APK, unsigned release/R8, Android lint). `createComparePdfDocumentWithPreflight` checks cancellation immediately before constructing output PdfDocument; eight focused JVM regressions cover cancellation, constructor failure, ownership and suppressed cleanup. Paired push #897 cancelled by concurrency and is not validation evidence. No Android device/emulator/OEM/benchmark/peak heap/race-freedom proof.
- Cycle 120 E120/F122/US-R120-P2-01A ACTIVE / INCOMPLETE before production mutation. Source at `64967addfc3ebbd56506220899ad5ed0b70dcca9`: after URI-to-cache copying, `withOpenedCloseables` calls `ParcelFileDescriptor.open(leftFile, MODE_READ_ONLY)` and then `ParcelFileDescriptor.open(rightFile, MODE_READ_ONLY)` without cancellation preflight. Existing renderer-open checks occur only afterward. Introduce a small pure JVM-testable descriptor-open preflight for both acquisitions; preserve first descriptor close if second preflight/open fails, error identity, staging and normal output. Add focused regressions; require exact-code/test-SHA draft-PR all-nine-gate CI. PR #1 remains draft/unmerged; main unchanged.
