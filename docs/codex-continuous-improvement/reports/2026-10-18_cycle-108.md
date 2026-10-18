# Cycle 108 — PDF compare filename sanitization memory safety

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E108 / F110 / US-R108-P2-01A
**Previous cycle:** Cycle 107 `E107/F109/US-R107-P2-01A` COMPLETE: exact production/test SHA `82ceba75aecf724f85d4605f7a304cfa2d71b6ec` passed authoritative draft-PR Actions #852 / API `39097399571`, job `117586609368`, all nine configured gates SUCCESS: core domain/PDF/converter/scanner/PDF-tools/app JVM tests, debug APK, unsigned release/R8, Android lint. Eight focused JVM regressions cover byte fidelity, 64 KiB chunk boundaries, cancellation, invalid ranges, downstream IO exceptions and stream lifecycle. Isolated Kotlin host run also passed 8/8. No Android emulator/physical device/OEM/benchmark or measured cancellation-latency evidence.

## Source evidence and acceptance
`sanitizedCompareOutputBaseName` currently applies `Regex("[^a-zA-Z0-9_-]")` replacement across the entire input before `.take(200)`. For very large strings, the output is short but the intermediate replacement can be huge. This is a source-level performance/memory risk, not device-measured OOM. A naive `.take(200).replace(...)` may change output at a UTF-16 surrogate boundary, since Java regex processes Unicode code points; preserve ordinary names, blank fallback, ASCII replacement policy, Unicode/surrogate behavior and exact 200-character cap. Prove semantics with focused host tests before code mutation, then implement bounded scanning and exact-SHA draft-PR CI.

## Next action
Compare old/new semantics on ASCII, Unicode code points, surrogate pairs at the 200-character boundary, whitespace-only fallback, long inputs, and collision-safe publication. Implement only the smallest proven compatible fix, add JVM regressions, run authoritative exact-SHA CI and reconcile canonical documents. PR #1 draft/unmerged; main untouched.
