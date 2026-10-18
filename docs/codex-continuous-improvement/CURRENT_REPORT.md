# Cycle 107 — PDF compare staged serialization cancellation

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E107 / F109 / US-R107-P2-01A

## Previous validation
Cycle 106 COMPLETE. Exact code/test SHA `3e5f464aff7800dba908c3a746586b89fca32704` passed draft-PR Actions #845/API `39093774353`, job `117576728460`, all nine gates SUCCESS. Docs HEAD `2e543f66900d81b15365f7f891be0c81e1c4ca74` passed API `39095923023`. No device/emulator/benchmark evidence.

## Evidence
`PdfCompareTool.compare` serializes PDF bytes with `pdfDoc.writeTo(FileOutputStream(stagedFile))`, without intra-write coroutine cancellation checks. The staged publisher checks cancellation after the writer returns, but cancellation can be delayed during large writes. This is a source-level risk, not measured on a device.

## Acceptance
Implement a JVM-testable cancellation-checking OutputStream adapter that splits writes into <=64 KiB chunks and checks cancellation before each chunk. Preserve byte fidelity, exception propagation, staging cleanup and stream ownership. Add JVM regressions; validate exact production/test SHA with draft-PR CI. Keep PR #1 draft/unmerged; main untouched.
