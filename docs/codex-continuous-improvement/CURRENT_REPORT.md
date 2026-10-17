# Cycle 093 — Unknown staging identity publication safety

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E093 / F095 / US-R093-P1-01A
**Previous cycle:** Cycle 092 COMPLETE, exact code/test SHA `7036384a7cbd37c7c3ebfbee0f6979a3968235cf` validated by draft-PR Actions #787 / API `39028698857`, job `117385580946`, SUCCESS all nine gates.

## Source evidence
In `PdfIoUtils.kt`, `requireOriginalStagedIdentity` returns immediately when both original identity sources are null, while `publishCheckedStagedOutput` accepts unknown identity continuity. A staged regular-file replacement could be published without ownership proof on a provider exposing neither identity source. This is a code-path concern, not a verified Android exploit.

## Acceptance and boundaries
- First reproduce the null-NIO/null-Android identity scenario with deterministic injected JVM tests, including normal staging and substituted foreign regular files.
- Evaluate compatibility before changing behavior; fail closed on unverified substitution where feasible, preserving foreign entries and original exceptions. Do not silently remove unknown-identity entries.
- Keep publication/cancellation/rollback/collision behavior stable and minimize scope. Do not claim atomic race safety, emulator/device testing or OEM compatibility.
- Obtain authoritative draft-PR CI for exact combined code/test SHA and synchronize canonical docs after terminal CI. PR #1 remains draft/unmerged; main untouched.

## Next executable step
Inspect current null-identity tests and `requireOriginalStagedIdentity` call sites, add a deterministic regression demonstrating the remaining acceptance gap, then choose the smallest safe fix based on evidence.
