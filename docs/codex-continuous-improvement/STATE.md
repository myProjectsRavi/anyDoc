# AnyDoc Continuous Improvement State

**Canonical state:** this file
**Branch:** `codex/anydoc-continuous-improvement`
**Main safety rule:** never modify or merge `main` in this program.
**Current report:** `2026-10-16_cycle-083`
**Report status:** ACTIVE / INCOMPLETE
**Cycle:** 083
**Current Epic:** `E083` — Output-directory ancestry integrity
**Current Feature:** `F085` — Bound ancestor symlink redirection
**Current User Story:** `US-R083-P1-01A` — Prevent unsafe staged output redirection through symlinked ancestors (ACTIVE)
**Last completed cycle:** Cycle 082
**State checkpoint timestamp:** 2026-10-16 UTC

## Canonical checkpoint
- Cycle 082 `E082/F084/US-R082-P1-01A` COMPLETE. Exact code/test SHA `5adbc2022c21dff292e39fef469f848e27888876` passed authoritative draft-PR GitHub Actions #708 / API `38887203020` on 2026-10-16 UTC. Job `116928271059` completed SUCCESS, with core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8 and Android lint all passing.
- Four committed JVM regressions cover normal publication, known hard-link rejection, substitution before rename, and unavailable hard-link count. `unix:nlink` is optional on Android filesystems; an unknown count does not prove safety. No emulator, physical device, benchmark, universal hard-link protection or race-free claim.
- Cycle 083 `E083/F085/US-R083-P1-01A` ACTIVE / INCOMPLETE. Source evidence: `requireNonSymlinkOutputDirectory` checks only the leaf output directory. A symbolic-link ancestor can redirect staging into another directory. Do not blanket-reject legitimate Android storage aliases without compatibility evidence.
- **Cycle 083 candidate (NOT COMPLETE):** commit `f344450b587c7c1b5767904a85fdf783d51205d4` adds lexical ancestor symlink rejection; commit `80341dc3c40c01abfbecadfa5821a7e1f0da3889` adds three JVM regressions. PR Actions run #718 / API `38897540514` was pending at checkpoint. This blanket lexical rejection may break Android's legitimate symlinked storage aliases; audit call sites and Android storage compatibility before considering it production-ready. No sandbox Android build or emulator was run here.
- **Cycle 083 compatibility follow-up (NOT COMPLETE):** `e4fa660e6fa847e11fc33afa00366d5e48ef52fb` resolves only Android-provided public/app storage roots to canonical paths before appending the application output folder. `f6dafeaad2a086edbe9496783fcca4f628c84b85` adds a JVM regression for a symlinked platform base while preserving rejection of a symlinked child output folder. No instrumented Android-device compatibility result is claimed. PR Actions #723 / API `38898448736` was pending at checkpoint; #719 was still in progress and earlier superseded runs were cancelled.
- **Next exact step:** inspect latest exact-SHA PR Actions, fix all failing gates, verify Android storage path compatibility with emulator/device when possible, and only then synchronize report/backlog/validation and close Cycle 083. Preserve draft PR, do not merge main.
- **Code HEAD at activation:** `5adbc2022c21dff292e39fef469f848e27888876`; subsequent documentation-only commits do not represent new production validation. Confirm actual branch HEAD and CI on every run.
- **PR #1:** open, draft, unmerged. `main` untouched. Never merge.

## History preservation
- The full original historical checkpoint log through Cycle 082 activation remains permanently recoverable from Git history at commit `5adbc2022c21dff292e39fef469f848e27888876`, path `docs/codex-continuous-improvement/STATE.md`.
- All earlier cycle completion evidence and backlog are retained in `BACKLOG.md`, `VALIDATION.md`, and `reports/`. This compact canonical checkpoint replaces the accumulated historical transcript, not the archived evidence. Never restart a completed cycle without contradictory evidence.
- Original baseline `main` SHA: `a2c484b025b1dafb21c9f75bd6e5deb742341f5f`.
