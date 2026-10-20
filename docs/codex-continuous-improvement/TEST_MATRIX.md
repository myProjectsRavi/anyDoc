# Test Matrix

| Area | Unit | Robolectric | Instrumentation/Compose | Emulator | Status |
|---|---|---|---|---|---|
| Safe output allocation | shared resolver test added | candidate | candidate | not needed | CI pending |
| PDFBox initialization | concurrency/retry tests added | not required for helper | integration init test desirable | desirable | CI pending |
| Temp-file registry | lifecycle test added | app cleanup test desirable | cancellation integration desirable | desirable | CI pending |
| Room migrations 1→4 | missing | recommended | optional | optional | NOT_STARTED |
| PDF merge/split | missing | possible fixtures | desirable | desirable | NOT_STARTED |
| Redaction verification | missing | possible generated PDFs | desirable | optional | NOT_STARTED |
| OCR | helper tests missing | useful | required for end-to-end | required | NOT_STARTED |
| Scanner edge detection | pure/native tests missing | limited | CameraX UI flow | required where practical | NOT_STARTED |
| Share intent routing | missing | highly suitable | navigation regression | desirable | NOT_STARTED |
| Batch queue state machine | missing | highly suitable | service lifecycle | required where practical | NOT_STARTED |
| Settings/navigation | missing | suitable | Compose state restoration | desirable | NOT_STARTED |
| Accessibility | n/a | n/a | semantics/touch targets | desirable | NOT_STARTED |
| Large inputs / malformed files | generated fixtures missing | mixed | selected flows | desirable | NOT_STARTED |
| Release/R8 | n/a | n/a | release smoke | required where practical | NOT_STARTED |

## Current automated gate
The newly added branch workflow runs:
1. `:core:pdf:testDebugUnitTest`
2. `:app:assembleDebug`
3. `:app:lintDebug`

Future CI expansion should add Room migration tests, selected instrumentation/emulator jobs, release/R8 build sanity, and reproducibility checks without making the normal feedback loop unnecessarily slow.
