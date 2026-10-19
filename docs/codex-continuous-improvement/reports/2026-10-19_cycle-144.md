# Cycle 144 — Cancellation before compression output-directory resolution

**Status:** ACTIVE / INCOMPLETE
**Branch:** `codex/anydoc-continuous-improvement`
**Epic / Feature / Story:** E144/F146/US-R144-P2-01A
**Activation baseline:** `10fef67dc697a0046dce0fe5d628a8dc9c55e39f`
**Last validated code/test SHA:** `10fef67dc697a0046dce0fe5d628a8dc9c55e39f`, authoritative draft-PR Actions #984 / API `39335370125`, job `118343523855`, all nine configured gates SUCCESS (Cycle 143).

## Source-backed risk
At exact validated SHA `10fef67dc697a0046dce0fe5d628a8dc9c55e39f`, `PdfCompressor.compress` invokes `PdfBoxInit.ensure(context)` and then `DocForgeSettingsStore.resolveOutputDirectory` without a fresh cancellation preflight between them. If initialization cancels the coroutine (or cancellation occurs during it), output-directory resolution may still access preferences or create directories before the later staging/copy checks. This is a source-backed P2 responsiveness/side-effect concern, not a reproduced device failure.

## Acceptance
- Recheck coroutine cancellation immediately after `PdfBoxInit.ensure(context)` and before `DocForgeSettingsStore.resolveOutputDirectory`.
- Introduce only the smallest JVM-testable preflight seam; preserve settings resolution, invalid output names, symlink protections, staging/collision/no-overwrite, output bytes and exception identity.
- Add deterministic core PDF JVM tests for cancellation after initialization skipping directory resolution, normal ordering/result, preflight failure identity, directory-resolution failure identity, and repeated success.
- Require authoritative exact-code/test-SHA draft-PR CI with all nine configured gates before closing Cycle 144.
- Keep PR #1 draft/unmerged and main unchanged. Host JVM tests do not prove device/OEM behavior, benchmark, peak heap or race freedom.

## Next executable step
Freshly inspect `PdfCompressor.kt`, `DocForgeSettingsStore.resolveOutputDirectory` and existing compressor JVM tests; implement bounded post-init pre-directory cancellation preflight on `codex/anydoc-continuous-improvement`, commit source and focused tests via non-force expected-SHA CAS, then inspect exact-code/test-SHA authoritative draft-PR CI.
