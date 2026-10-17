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


## Cycle 083 authoritative completion / Cycle 084 activation — 2026-10-16 UTC
- `E083/F085/US-R083-P1-01A` COMPLETE for bounded lexical ancestor mitigation. Exact code/test SHA `90de62ed6d9d95d06851b639f3cba36589f8539c` passed draft-PR Actions #726 / API `38898836103`, job `116966393599`: core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, Android lint SUCCESS.
- Prior #724 / API `38898512930` FAILED at Kotlin compilation due duplicate suppression annotation in `DocForgeSettings.kt`; repaired in `90de62ed6d9d95d06851b639f3cba36589f8539c`. Superseded/cancelled workflow runs are not acceptance evidence.
- Four committed core PDF JVM regressions cover symlinked ancestors, relative ancestor link rejection, normal collision numbering and canonicalized platform-root alias simulation. Android storage alias compatibility on physical device/emulator remains unverified; no cross-process race guarantee.
- `E084/F086/US-R084-P1-01A` ACTIVE / INCOMPLETE before code mutation: output settings resolver can accept symlinked child folder via `isDirectory`. Next: NOFOLLOW leaf guard, focused domain JVM tests, exact-SHA PR CI; draft PR stays open, main untouched.


## Cycle 084 authoritative completion — 2026-10-16 UTC
- `E084/F086/US-R084-P1-01A` COMPLETE for bounded configured-folder symlink-leaf checks. Exact code/test SHA `0fe410393b950317fcaa390ada107fb4624f080e` passed draft-PR Actions #734 / API `38904369991`, job `116984725783`. All nine gates SUCCESS: domain JVM, PDF JVM, converter JVM, scanner JVM, PDF tools JVM, app JVM, debug APK, unsigned release/R8 and Android lint.
- Code `8fba5e6413d08fdccac81c9bd8eaf2fb3f76effb` rejects linked real/dangling child folders with NOFOLLOW checks; `58b06d0f983d82272a795ad6ec963985c10df110` adds four domain JVM regressions; `e78bea5b565658c3d173de4668e8f2b984950e52` adds the domain CI step. Initial #732 failed compiling the tests due missing JUnit; `0fe410393b950317fcaa390ada107fb4624f080e` adds `testImplementation(libs.junit4)` and #734 passed.
- Residual limitations: path checks are not atomic against concurrent directory swaps; Android OEM/device compatibility and performance are not certified. No physical-device or emulator evidence claimed. PR #1 remains draft/unmerged; `main` unchanged.

## Cycle 085 activation — 2026-10-16 UTC
- `US-R085-P1-01A` ACTIVE / INCOMPLETE before production/test mutation; baseline branch HEAD `49bf794533caa8d39631c1b53f709e3fa7dcc1d0`. Previous Cycle 084 exact code/test SHA `0fe410393b950317fcaa390ada107fb4624f080e` passed draft-PR CI #734 / API `38904369991`, nine gates.
- Local host Kotlin/JVM link-count selection and Linux hard-link probe passed 5/5; isolated host evidence only, not Android CI or OEM validation. Next: Android `Os.lstat` fallback, focused core PDF tests, exact-SHA CI. Do not claim race freedom.

## Cycle 085 authoritative completion — 2026-10-16 UTC
- `E085/F087/US-R085-P1-01A` COMPLETE. Exact production/test SHA `af624de8ee4f7e1a50dd1be12e2a460aec2b267a` passed authoritative draft-PR Actions #740 / API `38909368135`, job `117001519196`, all nine gates SUCCESS: domain JVM, core PDF JVM, converter JVM, scanner JVM, PDF tools JVM, app JVM, debug APK, unsigned release/R8 and Android lint. Push #739 was cancelled by concurrency and is not validation evidence.
- Production `PdfIoUtils.stagedHardLinkCount` now falls back to Android `Os.lstat(...).st_nlink` when NIO `unix:nlink` is unavailable. Four committed JVM fallback-selection tests passed in core PDF CI. Host-only Kotlin probe 5/5 passed with synthetic Android stubs, not device validation.
- Residual: if both providers cannot report a link count, unknown remains unknown; neither lookup eliminates filesystem TOCTOU races. No emulator, physical-device, OEM compatibility or benchmark evidence claimed. PR #1 draft/unmerged; main unchanged.

## Cycle 086 activation — 2026-10-16 UTC
- `E086/F088/US-R086-P1-01A` ACTIVE before production/test mutation. Host-only Kotlin/JVM rollback replacement reproduction passed 4/4 assertions; it is not Android CI. Baseline code/test SHA `af624de8ee4f7e1a50dd1be12e2a460aec2b267a` passed authoritative PR CI #740 / API `38909368135`. Next: identity-aware rollback and focused core PDF JVM tests, exact-SHA CI. No device/emulator/race-freedom claim.


## Cycle 086 authoritative completion / Cycle 087 activation — 2026-10-16 UTC
- Exact code/test SHA `58a62f6c1e277cab3e921c8e1c78c07cfd069e39` passed authoritative draft-PR GitHub Actions #743 / API `38915971783`, job `117025709017`, on 2026-10-16 UTC. All nine gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8, and Android lint. Paired push #742 was cancelled and is not validation evidence. Six committed core PDF JVM regressions cover unchanged output, replacement regular file, symlink, directory, unknown identity, and missing output. No emulator, physical device, benchmark or atomic race-freedom evidence.
- `E087/F089/US-R087-P0-01A` ACTIVE / INCOMPLETE before code mutation. Repro: post-move validation failure recursively cleans the candidate, including a substituted directory's children. Earlier isolated Kotlin/JVM probe passed 5/5; not Android CI. Next: narrow non-destructive cleanup and deterministic core PDF tests, authoritative exact-SHA CI. No device/emulator/benchmark or complete TOCTOU guarantee.


## Cycle 087 validation and closure — 2026-10-16 UTC
- `E087/F089/US-R087-P0-01A` COMPLETE for bounded identity-aware, non-recursive failed-publication cleanup. Exact code/test head `b455c28156d5566d788e09876ed127df10e44f13` passed draft-PR Actions #748 / API `38943994491`, job `117118776491`. All nine configured gates SUCCESS: core domain, core PDF, converter, scanner, PDF tools, app JVM, debug APK, unsigned release/R8 and Android lint.
- Source `publishCheckedStagedOutput` now attempts `rollbackPublishedOutputIfUnchanged(candidate, sourceAttributes.fileKey())` after failed post-move validation rather than recursively deleting a substituted directory; file-key unknown or mismatched entries remain in place. JVM swap regressions confirm preservation of substituted nonempty directory children, links and regular files, alongside normal publication and occupied-name behavior. Final hard-link alias expectation adjusted in `b455c281` after #746 failure; #748 passed.
- Remaining limitations: foreign entries may remain visible at the candidate path after rejection; no atomic filesystem identity-and-unlink, full TOCTOU safety, Android emulator/device evidence or race-free guarantee. No reviewer approval implied. PR #1 draft/unmerged; `main` unchanged.

## Cycle 089 authoritative completion / Cycle 090 activation — 2026-10-17 UTC
- Exact code/test SHA `9c6ac1a5d87b136449d596e5ce9ae8b31e287191` passed draft-PR GitHub Actions API `38988957597`, job `117267167769`, nine gates SUCCESS: domain JVM, PDF JVM, converter JVM, scanner JVM, PDF tools JVM, app JVM, debug APK, unsigned release/R8, Android lint. Paired push API `38988952406` CANCELLED, not acceptance evidence.
- Three committed core PDF JVM identity-continuity tests. Bounded original NIO fileKey comparisons at writer, batch and publication boundaries. Unknown keys bypass the guard; checks are path-based and non-atomic. No emulator, physical device, benchmark or cross-process race-freedom evidence.
- Cycle 090 `E090/F092/US-R090-P1-01A` ACTIVE before production mutation; investigate portable Android `Os.lstat` identity fallback with JVM-injected readers and exact-SHA CI. PR #1 draft/unmerged; main untouched.

## Cycle 090 validation / Cycle 091 activation — 2026-10-17 UTC
- Cycle 090 `E090/F092/US-R090-P1-01A` COMPLETE. Exact code/test SHA `8d6d1fb2e7c3567d174675f8324226002aef2bcc`; authoritative draft-PR Actions #766 / API `38991137058`, job `117273629538`, SUCCESS all nine gates (core domain/PDF/converter/scanner/PDF-tools/app JVM, debug APK, unsigned release/R8, Android lint). Paired push #765 was cancelled, not acceptance evidence. Four core PDF JVM injected Android identity tests; no device proof. JVM tests inject/simulate Android identity; no emulator, physical-device, OEM/provider, benchmark, atomic check/delete or complete TOCTOU/race-freedom evidence. Unknown identities remain unproven.
- Cycle 091 `E091/F093/US-R091-P1-01A` ACTIVE before production mutation: cleanup/rollback skip deletion when NIO key null even if Android identity known. Next: NOFOLLOW ownership-aware Android fallback and deterministic core PDF JVM regressions, authoritative exact-code/test-SHA PR CI. PR #1 draft/unmerged; `main` untouched.

## Cycle 091 validated candidate, acceptance still open — 2026-10-17 UTC
- Exact production/test SHA `2552fc0137e584de5e134eecc9821f8d76544a67` passed authoritative draft-PR Actions #770 / API `39002954473`, job `117309527721`, on 2026-10-17 UTC. All nine configured gates SUCCESS: core domain, PDF, converter, scanner, PDF-tools and app JVM tests, debug APK, unsigned release/R8, and Android lint. Paired push #769 / API `39002951021` was cancelled by concurrency and is not validation evidence.
- Six committed core PDF JVM methods exercise Android fallback cleanup/rollback when the NIO key is simulated absent, foreign regular replacement preservation, symlink/directory preservation, missing/unknown identity fail-closed and NIO precedence. CI's core PDF test gate passed; no exact individual test execution counts or device proof inferred.
- Remaining acceptance gap: direct injected JVM tests cover fallback ownership, substitution, links/directories, missing/unknown identity and NIO precedence, but no new end-to-end batch-rollback or staging-cancellation regression was added for the null-key scenario. Keep Cycle 091 ACTIVE until these paths are covered or explicitly scoped/deferred with evidence. Android OEM/provider compatibility, emulator/device behavior and atomic check/unlink remain unverified.
