# Cycle 120 — PDF compare cached-descriptor cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E120 / F122 / US-R120-P2-01A
**Activation baseline:** `64967addfc3ebbd56506220899ad5ed0b70dcca9`
**Previous cycle:** Cycle 119 COMPLETE; exact code/test SHA `64967addfc3ebbd56506220899ad5ed0b70dcca9` passed authoritative draft-PR Actions #898 / API `39147699102`, job `117734802444`, all nine gates.

## Source evidence
After both PDF URIs have been copied to cache, `PdfCompareTool.compare` enters `withOpenedCloseables` and opens left then right `ParcelFileDescriptor` without a fresh cancellation check before either acquisition. The next checks occur when opening renderers. Cancellation between the two descriptor acquisitions can still open the right descriptor unnecessarily. Existing `PdfCompareDescriptorLifecycleTest` covers resource ownership but not preflight cancellation. This is a source-level P2 responsiveness concern, not a reproduced Android defect.

## Acceptance
- Check cancellation before each non-null cached descriptor acquisition, including between left and right.
- Preserve `withOpenedCloseables` cleanup, first-descriptor closure when second preflight/open fails, original exception identity and suppression behavior.
- Preserve normal comparison, missing-page overlay, staged output/collision/no-overwrite, bitmap recycling and heap budget.
- Add deterministic host JVM regressions for cancelled-first/no-open, cancelled-second/first-close, normal ordering, thrown opens, and cleanup exceptions.
- Require exact-code/test-SHA draft-PR CI: six JVM groups, debug APK, unsigned release/R8 and Android lint.
- Do not claim Android device, OEM, benchmark, cancellation latency, peak heap or complete race freedom.

## Next executable step
Add the smallest pure JVM-testable descriptor-open cancellation preflight in `PdfCompareTool.kt` and focused `PdfCompareDescriptorCancellationTest.kt`. Preserve existing descriptor lifecycle semantics. Commit on the authorized branch with expected-SHA non-force CAS, validate exact HEAD in draft-PR CI, and reconcile canonical docs only after terminal green.
