# Cycle 121 — PDF compare input cache-copy cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E121 / F123 / US-R121-P2-01A
**Activation baseline:** `8977edb8e6a6212eebd3299dc2ab945b8e0c4f74`
**Previous cycle:** Cycle 120 COMPLETE; exact code/test SHA `8977edb8e6a6212eebd3299dc2ab945b8e0c4f74` passed draft-PR Actions `39157338513`, job `117763175051`, all nine configured gates.

## Source evidence
`PdfCompareTool.compare` invokes `withUriCopiedToCacheFile` for the first input and then the second without checking cancellation immediately before either copy. `copyUriToCacheFile` does preflight, creates a temporary file and opens a URI before its first chunk-level cancellation check. Cancellation after the left copy but before the right copy can therefore begin avoidable second-input setup. Source-level P2 responsiveness concern; no reproduced Android defect or measured delay.

## Acceptance
- Check cancellation immediately before starting each PDF input cache copy, including the gap between left and right copies.
- Preserve `withUriCopiedToCacheFile` scoped cleanup, error identity, successful output bytes and existing cache-space checks.
- Add focused deterministic host JVM regressions for cancelled-first, cancelled-second, normal ordering and exceptions.
- Require exact-code/test-SHA draft-PR CI: six JVM groups, debug APK, unsigned release/R8, Android lint.
- Do not claim emulator, physical-device, OEM, benchmark, measured latency, peak heap or complete race freedom.

## Next executable step
Implement the smallest JVM-testable preflight at the two cache-copy call sites in `PdfCompareTool.kt`, add `PdfCompareCacheCopyCancellationTest.kt`, commit only on the authorized branch and validate exact SHA. After terminal CI, synchronize all canonical docs and this cycle report.
