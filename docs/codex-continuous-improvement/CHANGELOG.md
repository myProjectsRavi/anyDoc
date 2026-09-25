# Continuous Improvement Changelog

## Cycle 001 — 2026-09-25

### Safety fixes implemented, CI pending
- Replaced destructive same-name output handling in `VideoAudioExtractor` and `AudioFormatConverter` with non-conflicting output allocation.
- Extended the same non-destructive allocation rule to verified direct-output paths in PDF split, irreversible redaction, OCR text/searchable PDF, batch stamping, PDF text extraction, page-image ZIP bundles, ID-card PDF generation, scanner ZIP bundles, and text-to-PDF.
- Reworked PDFBox initialization so the success state is published only after initialization actually succeeds; a failed initialization remains retryable and concurrent callers serialize.
- Added a process-local active-temp-file registry shared by core PDF, converter, and app cleanup code.
- Protected PDF URI-copy temp files for their complete active lifetime.
- Protected audio PCM temp files for their complete active lifetime.
- Updated `DocForgeApp` cache trimming to skip active temporary files.
- Added JVM regression tests for one-time initialization, retry after initialization failure, active temp-file registry lifecycle, and non-destructive output naming.
- Added feature-branch GitHub Actions workflow for core PDF unit tests, debug APK assembly, unsigned release/R8 assembly, and Android lint.
- Removed the production release build's explicit debug signing fallback; release signing is now external by design.
- Replaced Compose-owned shared-launch buffering with a Hilt Activity ViewModel backed by SavedStateHandle, and defer consumption until the target screen accepts the shared URIs. This prevents recreation replay/duplication while keeping a pending launch durable.

### Validation state
- ChatGPT/Codex sandbox clone is blocked by DNS/network resolution for `github.com` in the current execution environment.
- GitHub connector reports no combined status/check result yet for the latest feature-branch commit.
- Therefore the code above remains `CI_PENDING`; no CI pass is claimed.


## Run 002 — 2026-09-25

- Added reusable failure-safe staged output publishing in `PdfIoUtils.kt`.
- Migrated PDF compression, PDF merge, and searchable OCR PDFs to staged final publication.
- Added staged-output success/failure regression tests.
- Completed a recursive current-branch source-tree collision audit.
- Fixed missed destructive same-name business-card vCard export.
- Hardened business-card OCR bitmap/recognizer cleanup.
- Reconfirmed sandbox DNS prevents local clone/Gradle execution.
- GitHub Actions run #60 passed core PDF unit tests and was still building the debug APK at the last checkpoint.
