# AnyDoc Continuous Improvement Validation

## Historical evidence
The complete earlier validation log remains in Git history at commit `7593c9044a443d8a939ed18d4d7ff75ab7220fb6`, file `docs/codex-continuous-improvement/VALIDATION.md`. Previous cycle reports and `BACKLOG.md` also preserve completion evidence. This document retains the latest validation checkpoints; it does not invalidate prior successful tests. Never infer emulator, physical-device or benchmark results from JVM/CI gates.

## Cycle 079 authoritative validation / Cycle 080 activation — 2026-10-15 UTC
- Code/test SHA: `c4b2420499add93a3ea41a7027cdacf9414b901a`.
- Draft-PR Actions #691 / API `38803553526`: SUCCESS, job `116659166895`; core PDF, converter, scanner, PDF tools signature, app JVM tests, debug APK, unsigned release/R8, Android lint all SUCCESS. Push #690 / API `38803546956` CANCELLED by concurrency and not used as test evidence.
- Seven core PDF JVM staging-entry regression tests committed with NOFOLLOW_LINKS validation. No physical device, emulator, benchmark, cross-process race or hard-link safety claim.
- Cycle 080 ACTIVE before production mutation: `E080/F082/US-R080-P1-01A`. Source: shallow `stagedFile.delete()` leaves a nonempty substituted staging directory. Next: no-follow cleanup and focused JVM regressions; exact-SHA PR CI required. PR #1 remains draft; main untouched.


## Cycle 081 authoritative validation / Cycle 082 activation — 2026-10-16 UTC
- Cycle 081 `E081/F083/US-R081-P1-01A` COMPLETE. Exact code/test commit `cf43b166f91f2f02838461121288b84b48b38ba2` passed authoritative draft-PR Actions #705 / API `38883604993` on 2026-10-16 UTC: core PDF JVM, converter JVM, scanner JVM, PDF-tools JVM, app JVM, debug APK, unsigned release/R8 and Android lint all SUCCESS. `publishCheckedStagedOutput` revalidates the renamed output with NOFOLLOW_LINKS, compares available source/published file keys, and cleans up a substituted symlink or directory on detected failure. Six committed JVM regressions cover absolute/dangling symlink swaps, replacement regular file identity when available, substituted nested directory, normal publication, and existing destination. Paired push #704 was cancelled due to concurrency, not a failure. This is a bounded mitigation, not total TOCTOU, ancestor-swap, or hard-link protection. No emulator/physical-device/benchmark evidence.
- Cycle 082 `E082/F084/US-R082-P1-01A` ACTIVE / INCOMPLETE: staged output validation checks regular-file type and optional fileKey, but an adversary can replace the staged path with a hard link to another regular file. The moved output can then alias an unrelated file even though both checks pass. Next: verify whether reliable hard-link counts or secure file handle protections are available on Android API 26+, reproduce the alias case in deterministic core PDF JVM tests, implement only a compatible fail-closed or otherwise narrowly bounded mitigation, and validate exact SHA in authoritative draft-PR CI. Preserve normal staging/collision behavior. Track residual ancestor substitution and post-check races separately; do not claim race freedom or protection absent evidence.
- No Cycle 082 production/test commit or CI validation claimed at activation.

## Cycle 082 authoritative completion — 2026-10-16 UTC
- Story `E082/F084/US-R082-P1-01A` COMPLETE. Exact code/test SHA `5adbc2022c21dff292e39fef469f848e27888876` passed draft-PR Actions #708 / API `38887203020` (job `116928271059`) on 2026-10-16 UTC, all eight configured gates SUCCESS: core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, Android lint.
- Four committed core PDF JVM regressions cover ordinary publication, known hard-link rejection, pre-rename substitution and unavailable-count semantics. No device, emulator, benchmark or complete TOCTOU protection evidence. `unix:nlink` unsupported providers remain a residual risk.
- Cycle 083 `E083/F085/US-R083-P1-01A` ACTIVE / INCOMPLETE. Next: reproduce ancestor-symlink redirection, assess Android storage path compatibility and validate only a bounded compatible fix by exact-SHA draft-PR CI.
