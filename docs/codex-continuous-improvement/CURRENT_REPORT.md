# Cycle 122 — PDF compare output-directory cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E122 / F124 / US-R122-P2-01A
**Activation baseline:** `cc3610234154d8149f914781cb1ec13866d9b235`
**Previous cycle:** Cycle 121 COMPLETE; exact code/test SHA `cc3610234154d8149f914781cb1ec13866d9b235` passed draft-PR Actions `39161742830`, job `117776294771`, all nine configured gates.

## Source evidence
`PdfCompareTool.compare` checks cancellation before renderer acquisition, but after the right renderer opens and page count is read it calls `DocForgeSettingsStore.resolveOutputDirectory` without a new preflight. That resolver reads preferences and may create the configured output folder via `mkdirs()`. Cancellation in the gap can still cause unnecessary directory resolution and creation. Source-level P2 responsiveness concern; no reproduced Android defect or measured delay.

## Acceptance
- Check cancellation immediately before output-directory resolution after renderer acquisition.
- Preserve successful folder selection, symlink validation, resource cleanup, exception identity and output bytes.
- Add focused deterministic JVM regressions for cancellation, success ordering and exception behavior.
- Require exact-code/test-SHA draft-PR CI: six JVM groups, debug APK, unsigned release/R8 and Android lint.
- Do not claim emulator, physical-device, OEM, benchmark, measured latency, peak heap or complete race freedom.

## Next executable step
Add a minimal JVM-testable preflight at the output-directory resolution call in `PdfCompareTool.kt`, add `PdfCompareOutputDirectoryCancellationTest.kt`, commit only on the authorized branch and validate exact SHA. After terminal CI, synchronize all canonical docs and this cycle report.
