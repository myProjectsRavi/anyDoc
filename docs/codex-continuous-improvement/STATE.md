# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never modify or merge `main`.
**Current report:** `2026-10-18_cycle-108`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 108
**Current Epic:** `E108` — PDF compare filename sanitization memory safety
**Current Feature:** `F110` — Bound work and allocation for very long output names
**Current User Story:** `US-R108-P2-01A` — Avoid full-input replacement allocation while preserving Unicode filename semantics (ACTIVE)
**Last completed cycle:** Cycle 107
**State checkpoint timestamp:** 2026-10-18 UTC
**Starting HEAD:** `82ceba75aecf724f85d4605f7a304cfa2d71b6ec`

## Canonical checkpoint
- Cycle 107 `E107/F109/US-R107-P2-01A` COMPLETE: exact production/test SHA `82ceba75aecf724f85d4605f7a304cfa2d71b6ec` passed authoritative draft-PR Actions #852 / API `39097399571`, job `117586609368`, all nine configured gates SUCCESS: core domain/PDF/converter/scanner/PDF-tools/app JVM tests, debug APK, unsigned release/R8, Android lint. Eight focused JVM regressions cover byte fidelity, 64 KiB chunk boundaries, cancellation, invalid ranges, downstream IO exceptions and stream lifecycle. Isolated Kotlin host run also passed 8/8. No Android emulator/physical device/OEM/benchmark or measured cancellation-latency evidence.
- Cycle 106 `E106/F108/US-R106-P2-01A` COMPLETE: code/test SHA `3e5f464aff7800dba908c3a746586b89fca32704` passed draft-PR Actions #845 / API `39093774353`, job `117576728460`, all nine gates. No device evidence.
- Cycle 108 `E108/F110/US-R108-P2-01A` ACTIVE before production/test mutation. Source: `sanitizedCompareOutputBaseName` currently calls `outputName.ifBlank(fallback).replace(Regex("[^a-zA-Z0-9_-]"), "_").take(200)`, allocating a full-length replacement string before truncation. Very long user input may waste memory/CPU. Audit UTF-16 surrogate/codepoint replacement semantics and blank fallback; implement a bounded prefix sanitizer that exactly preserves output for ordinary, Unicode and surrogate-pair inputs; add focused JVM tests and exact-SHA draft-PR CI. Source-level risk, not measured OOM.
- **Next exact action:** prove equivalence for surrogate pairs and blank fallback; implement a bounded sanitizer only after deterministic host evidence, then exact-SHA draft-PR CI and canonical reconciliation.
- **PR #1:** open/draft/unmerged; main untouched.

## History preservation
- Previous Cycle 107 STATE.md at `82ceba75aecf724f85d4605f7a304cfa2d71b6ec`. Earlier history remains in BACKLOG.md, VALIDATION.md and cycle reports. Original main SHA `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`.
