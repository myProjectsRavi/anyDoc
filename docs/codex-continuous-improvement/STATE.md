# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never modify or merge `main` in this program.
**Current report:** `2026-10-16_cycle-085`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 085
**Current Epic:** `E085` — Portable staging hard-link checks
**Current Feature:** `F087` — Android link-count fallback when NIO attributes are unavailable
**Current User Story:** `US-R085-P1-01A` — Detect staged hard-link aliases on Android when unix:nlink is unavailable (ACTIVE)
**Last completed cycle:** Cycle 084
**State checkpoint timestamp:** 2026-10-16 UTC

## Canonical checkpoint
- Cycle 083 `E083/F085/US-R083-P1-01A` COMPLETE for its bounded lexical-ancestor guard. Exact code/test SHA `90de62ed6d9d95d06851b639f3cba36589f8539c` passed authoritative draft-PR Actions #726 / API `38898836103`, job `116966393599`, on 2026-10-16 UTC. All eight configured gates SUCCESS: core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, and Android lint.
- Production `f344450b587c7c1b5767904a85fdf783d51205d4` rejects lexical symlink ancestors; three JVM regressions in `80341dc3c40c01abfbecadfa5821a7e1f0da3889`. Android-provided public/app storage bases are canonicalized before appending the configured child in `e4fa660e6fa847e11fc33afa00366d5e48ef52fb`, with an alias JVM regression in `f6dafeaad2a086edbe9496783fcca4f628c84b85`. `90de62ed6d9d95d06851b639f3cba36589f8539c` removed a duplicate suppression annotation that caused CI #724 to fail; #726 succeeded.
- Scope limitation: JVM alias simulation does not prove Android OEM/storage compatibility; no emulator, physical-device, benchmark or cross-process race freedom claimed. Independently mutable ancestor paths remain a TOCTOU limitation. Follow up on-device verification when available.
- Cycle 084 `E084/F086/US-R084-P1-01A` COMPLETE after nine CI gates SUCCESS. Evidence: `DocForgeSettingsStore.resolveOutputDirectory` returns `publicDir` when `publicDir.isDirectory` is true, including when the configured leaf folder is a symlink; fallback `File(...).isDirectory` also follows links. A caller writing directly to such a returned folder could be redirected. The ancestor guard in `PdfIoUtils` protects its own callers but does not make the settings resolver safe for every consumer.
- **Next exact step:** implement Android `Os.lstat(...).st_nlink` fallback for unavailable NIO `unix:nlink`, with deterministic JVM fallback-selection tests and exact-SHA draft-PR CI; preserve unknown-provider limitations.
- **PR #1:** open, draft, unmerged. `main` untouched. Never merge.

## History preservation
- The full original historical checkpoint log through Cycle 082 activation remains permanently recoverable from Git history at commit `5adbc2022c21dff292e39fef469f848e27888876`, path `docs/codex-continuous-improvement/STATE.md`.
- All earlier cycle completion evidence and backlog are retained in `BACKLOG.md`, `VALIDATION.md`, and `reports/`. This compact canonical checkpoint replaces the accumulated historical transcript, not the archived evidence. Never restart a completed cycle without contradictory evidence.
- Original baseline `main` SHA: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`.


## Cycle 084 authoritative completion — 2026-10-16 UTC
- `E084/F086/US-R084-P1-01A` COMPLETE for bounded configured-folder symlink-leaf checks. Exact code/test SHA `0fe410393b950317fcaa390ada107fb4624f080e` passed draft-PR Actions #734 / API `38904369991`, job `116984725783`. All nine gates SUCCESS: domain JVM, PDF JVM, converter JVM, scanner JVM, PDF tools JVM, app JVM, debug APK, unsigned release/R8 and Android lint.
- Code `8fba5e6413d08fdccac81c9bd8eaf2fb3f76effb` rejects linked real/dangling child folders with NOFOLLOW checks; `58b06d0f983d82272a795ad6ec963985c10df110` adds four domain JVM regressions; `e78bea5b565658c3d173de4668e8f2b984950e52` adds the domain CI step. Initial #732 failed compiling the tests due missing JUnit; `0fe410393b950317fcaa390ada107fb4624f080e` adds `testImplementation(libs.junit4)` and #734 passed.
- Residual limitations: path checks are not atomic against concurrent directory swaps; Android OEM/device compatibility and performance are not certified. No physical-device or emulator evidence claimed. PR #1 remains draft/unmerged; `main` unchanged.

## Cycle 085 activation — 2026-10-16 UTC
- `E085/F087/US-R085-P1-01A` ACTIVE / INCOMPLETE before production/test mutation. Evidence: `PdfIoUtils.stagedHardLinkCount` returns null for unsupported `unix:nlink`, and `requireNoKnownStagedHardLinks` allows null; this leaves hard-link aliases undetected on providers without NIO unix attributes. Android `Os.lstat` and `StructStat.st_nlink` are available since API 21; this module minSdk is 26. Baseline branch HEAD `49bf794533caa8d39631c1b53f709e3fa7dcc1d0`; last validated code/test SHA `0fe410393b950317fcaa390ada107fb4624f080e` / PR CI #734 SUCCESS.
- Scope: use `Os.lstat` as a fallback only when the NIO link count is unavailable; preserve existing behavior if both providers are unavailable, add focused JVM tests of fallback selection, and validate exact combined code/test SHA in draft-PR CI. No emulator, device, cross-process race freedom or OEM filesystem guarantees. Keep PR #1 draft and main unchanged.
- Next mutation: update `core/pdf/src/main/java/com/docforge/core/pdf/PdfIoUtils.kt`, add a focused test in `core/pdf/src/test/java/com/docforge/core/pdf/`, inspect CI, then reconcile all canonical docs after terminal CI.
