# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never modify or merge `main` in this program.
**Current report:** `2026-10-16_cycle-084`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 084
**Current Epic:** `E084` — Settings output-folder symlink integrity
**Current Feature:** `F086` — Reject linked output-folder leaves at resolution
**Current User Story:** `US-R084-P1-01A` — Prevent settings resolver from returning symlinked output folders (ACTIVE)
**Last completed cycle:** Cycle 083
**State checkpoint timestamp:** 2026-10-16 UTC

## Canonical checkpoint
- Cycle 083 `E083/F085/US-R083-P1-01A` COMPLETE for its bounded lexical-ancestor guard. Exact code/test SHA `90de62ed6d9d95d06851b639f3cba36589f8539c` passed authoritative draft-PR Actions #726 / API `38898836103`, job `116966393599`, on 2026-10-16 UTC. All eight configured gates SUCCESS: core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, and Android lint.
- Production `f344450b587c7c1b5767904a85fdf783d51205d4` rejects lexical symlink ancestors; three JVM regressions in `80341dc3c40c01abfbecadfa5821a7e1f0da3889`. Android-provided public/app storage bases are canonicalized before appending the configured child in `e4fa660e6fa847e11fc33afa00366d5e48ef52fb`, with an alias JVM regression in `f6dafeaad2a086edbe9496783fcca4f628c84b85`. `90de62ed6d9d95d06851b639f3cba36589f8539c` removed a duplicate suppression annotation that caused CI #724 to fail; #726 succeeded.
- Scope limitation: JVM alias simulation does not prove Android OEM/storage compatibility; no emulator, physical-device, benchmark or cross-process race freedom claimed. Independently mutable ancestor paths remain a TOCTOU limitation. Follow up on-device verification when available.
- Cycle 084 `E084/F086/US-R084-P1-01A` ACTIVE / INCOMPLETE, activated BEFORE first code/test mutation. Evidence: `DocForgeSettingsStore.resolveOutputDirectory` returns `publicDir` when `publicDir.isDirectory` is true, including when the configured leaf folder is a symlink; fallback `File(...).isDirectory` also follows links. A caller writing directly to such a returned folder could be redirected. The ancestor guard in `PdfIoUtils` protects its own callers but does not make the settings resolver safe for every consumer.
- **Next exact mutation:** add a narrowly bounded NOFOLLOW symlink-leaf rejection to public and fallback output-folder selection without canonicalizing the child; add deterministic core-domain JVM coverage for symlinked real/dangling child folders and ordinary folder selection; run authoritative exact-SHA PR CI. Inspect existing tests/build before editing.
- **PR #1:** open, draft, unmerged. `main` untouched. Never merge.

## History preservation
- The full original historical checkpoint log through Cycle 082 activation remains permanently recoverable from Git history at commit `5adbc2022c21dff292e39fef469f848e27888876`, path `docs/codex-continuous-improvement/STATE.md`.
- All earlier cycle completion evidence and backlog are retained in `BACKLOG.md`, `VALIDATION.md`, and `reports/`. This compact canonical checkpoint replaces the accumulated historical transcript, not the archived evidence. Never restart a completed cycle without contradictory evidence.
- Original baseline `main` SHA: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`.
