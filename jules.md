# AnyDoc (DocForge) — Engineering Execution Guide
## Jules Context Document · v2.0 · May 2026
## Synchronized with claude.md v2.0 and gemini.md v2.0

---

# §1 · WHAT IS ANYDOC

AnyDoc is an offline-first Android document super-app designed to be the final, definitive replacement for every document-related subscription service globally. Built with Kotlin, Jetpack Compose, and a modular architecture, it provides 25+ document tools running at 0ms perceived latency on devices with as little as 4GB RAM.

**Package:** `com.docforge.app`
**Architecture:** Multi-module · Single-Activity · MVI-like state management
**Engine:** PdfBox-Android + OpenCV + ML Kit (on-device) + Android native APIs
**Goal:** 105+ resume templates, 40+ conversion formats, every PDF operation imaginable — 100% offline, sub-15MB base APK, zero heating.

---

# §2 · CRITICAL RULES — NEVER VIOLATE

1. **All PDF operations must use `withUriCopiedToCacheFile()`** — never operate directly on content URIs. Temp files auto-deleted in `finally`.

2. **All bitmap decoding must go through `decodeBitmapConstrained()`** with `maxLongEdge ≤ 2200`. Never decode full-resolution images.

3. **All page loops must call `coroutineContext.ensureActive()`** — cooperative cancellation is mandatory.

4. **All bitmaps must be recycled in `finally` blocks** — no exceptions.

5. **All PDF documents must be loaded with `pdfMemoryUsageSetting()` (temp-file only)** — never heap-based.

6. **All output filenames must be sanitized** via `replace(Regex("[^a-zA-Z0-9_-]"), "_")`.

7. **Never add network calls to core processing** — ML Kit models are the only exception (download once, then offline). Translation model downloads MUST use `requireWifi()` by default.

8. **All new tools must follow the existing constructor pattern** — `class ToolName(private val context: Context)` and be registered in `AppDependencies` (or `AppModule` after Hilt migration).

9. **Never run heavy processing on `Dispatchers.Main`** — all tool methods must be `suspend fun` with `withContext(Dispatchers.IO)`.

10. **The batch queue processes tasks sequentially** — never parallelize to avoid thermal throttling.

11. **All output file creation must use `resolveNonConflictingFile()`** — never construct `File(dir, name)` directly for output. This is currently violated by 7+ tools (HIGH-6 bug).

12. **All public storage writes must call `MediaScannerConnection.scanFile()`** — so files appear in the device file manager. Currently missing from most tools (HIGH-7 bug).

---

# §3 · KNOWN BUGS — MUST FIX BEFORE PROCEEDING

## 🔴 CRITICAL — Fix Immediately

### C1 · Dual DI Architecture
`AppDependencies` (manual) + `AppModule` (Hilt) = two parallel object graphs. `AppModule` is dead code. Fix: Commit to full Hilt — delete `AppDependencies`, convert all ViewModels to `@HiltViewModel`.

### C2 · PDF Redaction Security Gap ✅ FIXED
`PdfRedactionTool.kt` only removes text from content streams. Annotations, XMP metadata, embedded files, and hidden layers are untouched. "Verified irreversible" label is misleading. Fix: scrub all annotation content, null XMP catalog, remove embedded files node, remove OCG properties.

### C3 · SidebarResume Crash ✅ FIXED
`ResumeRenderer.kt` — `fillMaxHeight()` inside `verticalScroll` = `IllegalStateException` crash. Fix: Replace with `wrapContentHeight()` or `height(IntrinsicSize.Min)`.

## 🟠 HIGH — Fix This Sprint

| ID | File | Issue | Status |
|---|---|---|---|
| H1 | `DocForgeApp.kt` | `onTrimMemory` deletes in-flight temp files — add `TempFileRegistry` | 🟠 |
| H2 | `DocForgeNavHost.kt` | `activeSharedLaunch` lost on rotation — move to ViewModel | 🟠 |
| H3 | `DocForgeDatabase.kt` | `fallbackToDestructiveMigration()` in production — remove | ✅ FIXED |
| H4 | `DocumentPdfConverter.kt` | RTF parser corrupts non-ASCII — add `\uN` unicode escape handling | ✅ FIXED |
| H5 | `DocumentEdgeDetector.kt` | `OpenCVLoader.initLocal()` in constructor — move to `EngineWarmup` | ✅ FIXED |
| H6 | Multiple tools | 7+ tools bypass `resolveNonConflictingFile()` — universal audit + fix | ✅ FIXED |
| H7 | Multiple tools | Missing `MediaScannerConnection.scanFile()` after output write | ✅ FIXED |
| H8 | All | Zero unit tests — JUnit 5 + Robolectric test suite | 🟠 |

## 🟡 MEDIUM — Fix Next Sprint

| ID | Issue | File | Status |
|---|---|---|---|
| M1 | Translation downloads without WiFi guard | `PdfTranslationTool.kt` | ✅ FIXED |
| M2 | AES-128 + weak 6-char password policy | `PdfPasswordTool.kt` | ✅ FIXED |
| M3 | Default scale 1f = 8 DPI (unreadable) | `PdfPageImageExporter.kt` | ✅ FIXED |
| M4 | RGB_565 strips alpha — transparent PDFs go black | `PdfCompressor.kt` | 🟡 |
| M5 | AUTO page size uses pixels as PDF points | `PdfCreator.kt` | ✅ FIXED |
| M6 | EXIF TRANSVERSE/TRANSPOSE missing mirror flip | `BitmapDecodeUtils.kt` | ✅ FIXED |
| M7 | SharedPreferences read on UI thread in composables | `DocForgeNavHost.kt` | 🟡 |
| M8 | `SimpleDateFormat` not thread-safe | `BatchQueueRuntimeStore.kt` | ✅ FIXED |
| M9 | `SavedSignatureStore.save()` stream not closed | `SavedSignatureStore.kt` | 🟡 VERIFIED OK |
| M10 | Non-atomic template file write | `SignaturePlacementTemplateStore.kt` | ✅ FIXED |
| M11 | No input file size validation | All tools | 🟡 |
| M12 | `notifyMediaStore` uses incorrect API | `DocForgeSettingsStore.kt` | ✅ FIXED |
| M13 | Latin-only font for OCR/translation overlay | `PdfOcrTool.kt`, `PdfTranslationTool.kt` | 🟡 |
| M14 | Translation strips original visual layout | `PdfTranslationTool.kt` | 🟡 |

---

# §4 · MODULE MAP

```
anyDoc/
├── app/                    ← Application shell, DI, navigation, batch
│   ├── DocForgeApp.kt      ← Cache cleanup, engine warmup trigger  [H1 onTrimMemory bug]
│   ├── MainActivity.kt     ← Single-Activity Compose host          [M7 SP on main thread]
│   ├── AppDependencies.kt  ← Manual DI graph                       [C1 CRITICAL — conflicts with Hilt]
│   ├── AppModule.kt        ← Hilt @Module                          [C1 CRITICAL — dead code]
│   ├── navigation/DocForgeNavHost.kt  ← 590-line nav graph         [H2 activeSharedLaunch]
│   ├── runtime/EngineWarmup.kt        ← Async PdfBox + OpenCV init [H5 needs opencvDeferred]
│   ├── share/ShareIntentRouter.kt     ← External intent handling   [LOW-3 URI extension bug]
│   └── batch/              ← 7 files: service, runtime store, preset store, VM, UI
├── core/
│   ├── domain/             ← Models (ConversionRecord), interfaces, settings [M7 needs StateFlow]
│   ├── opencv/             ← DocumentEdgeDetector (347 lines)      [H5 constructor init]
│   ├── pdf/                ← 19 tool classes (THE ENGINE ROOM)     [see §5]
│   ├── storage/            ← Room DB v3 (2 entities, 3 migrations) [H3 fallbackToDestructive]
│   └── ui/                 ← Shared Compose utils, StableUriRef, ImmutableCollections ✅
├── feature/
│   ├── converter/          ← ImageFormatConverter, DocumentPdfConverter [H4 RTF], AudioFormatConverter, VideoAudioExtractor
│   ├── history/            ← Conversion history UI + management ✅
│   ├── scanner/            ← CameraX scanner + OpenCV edge detection + filters ✅
│   └── pdf-tools/          ← 15+ tool screens + ViewModels + resume builder
│       └── resume/         ← ResumeRenderer [C3 SidebarResume crash], 10 templates [needs 105]
```

---

# §5 · HOW TO ADD A NEW PDF TOOL

### Step 1: Create engine class in `core/pdf/`

```kotlin
class PdfNewTool(private val context: Context) {

    suspend fun process(
        inputUri: Uri,
        outputName: String,
        /* tool-specific params */
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }

        // RULE: Always validate input size first
        validateInputSize(context, inputUri)  // throws if > 500MB

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
        val sanitized = outputName.ifBlank { "newtool_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        // RULE: Always use resolveNonConflictingFile — never File(dir, name) directly
        val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_newtool_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { sourceDoc ->
                require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

                PDDocument().use { outDoc ->
                    repeat(sourceDoc.numberOfPages) { pageIndex ->
                        checkCancelled()  // RULE: Cancel check in every page loop
                        val sourcePage = sourceDoc.getPage(pageIndex)
                        val imported = outDoc.importPageFull(sourcePage)  // RULE: Use shared utility
                        // ... your tool logic here ...
                    }
                    outDoc.save(outputFile)
                }
            }
        }

        // RULE: Notify MediaStore so file appears in file manager
        MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf("application/pdf"), null)

        PdfCreationResult(outputFile, sourceDoc.numberOfPages, outputFile.length())
    }
}
```

### Step 2: Register in `AppModule.kt` (after Hilt migration)
```kotlin
@Provides @Singleton
fun providePdfNewTool(@ApplicationContext context: Context) = PdfNewTool(context)
```

### Step 3: Create `@HiltViewModel` in `feature/pdf-tools/`
```kotlin
@HiltViewModel
class NewToolViewModel @Inject constructor(
    private val pdfNewTool: PdfNewTool,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    // ...
}
```

### Step 4: Add navigation in `DocForgeNavHost.kt`
```kotlin
composable("new_tool") {
    val vm: NewToolViewModel = hiltViewModel()
    NewToolRoute(viewModel = vm)
}
```

### Step 5: Record history on success
```kotlin
historyRepository.insert(ConversionRecord(
    sourceLabel = inputDisplayName,
    outputPath = outputFile.absolutePath,
    outputUri = outputFile.toUri().toString(),
    displayName = outputFile.name,
    operation = "NEW_TOOL",
    createdAtMillis = System.currentTimeMillis(),
    inputCount = 1,
    outputSizeBytes = outputFile.length()
))
```

---

# §6 · COMPLETE TOOL INVENTORY WITH STATUS

| Tool | Class | Key Method | Lines | Status |
|---|---|---|---|---|
| Image → PDF | `PdfCreator` | `createPdfFromImages()` | 121 | ✅ MED-5 |
| PDF Merge | `PdfMerger` | `merge()` | 237 | ✅ |
| PDF Split (7 modes) | `PdfSplitter` | 7 methods | 423 | ✅ H6 |
| PDF Compress | `PdfCompressor` | `compress()` | 160 | ✅ M4 |
| PDF Sign | `PdfSigner` | `sign()/signMultiple()` | 174 | ✅ |
| PDF Annotate | `PdfAnnotator` | `annotate()` | 387 | ✅ |
| PDF Redact | `PdfRedactionTool` | `redact()` | 319 | ⚠️ C2 |
| PDF OCR | `PdfOcrTool` | `process()` | 430 | ✅ M13 |
| PDF Translate | `PdfTranslationTool` | `translatePdfWithLayout()` | 379 | ✅ M1,M13,M14 |
| PDF Form Fill | `PdfFormTool` | `listFields()/fillFields()` | 297 | ✅ |
| PDF Watermark+Bates | `PdfBatchStampTool` | `stampBatch()` | 215 | ✅ |
| PDF Password | `PdfPasswordTool` | `protect()/removePassword()` | 103 | ✅ M2 |
| PDF → Text | `PdfTextExtractor` | `extractToTxt()` | 76 | ✅ |
| PDF → Images | `PdfPageImageExporter` | `exportPages()` | 142 | ✅ M3 |
| ID Card Sheet | `PdfIdCardTool` | `createFrontBackSheet()` | 122 | ✅ |
| Scan → Image | `ScanImageExporter` | `export()` | 102 | ✅ |
| Image Convert | `ImageFormatConverter` | `convertBatch()` | 107 | ✅ H6 |
| Doc → PDF | `DocumentPdfConverter` | `convertToPdf()` | 434 | ⚠️ H4 |
| Video → Audio | `VideoAudioExtractor` | `extractAudio()` | 227 | ✅ |
| Edge Detection | `DocumentEdgeDetector` | `detectEdges()` | 347 | ✅ H5 |

**Pending tools (🚀 TODO):** PDF Reader, PDF Page Crop, PDF Header/Footer, PDF Page Numbers, PDF Compare, PDF TOC Generator, PDF Metadata Editor, PDF Digital Signature (PKCS#12), PDF/A Converter, PPTX→PDF, XLSX→PDF, HTML→PDF, EPUB→PDF, ODT→PDF, PDF→DOCX, PDF→HTML, Image→HEIC, Video→GIF, Business Card→vCard, Invoice Generator, Document Vault

---

# §7 · MEMORY MANAGEMENT PATTERNS

### Pattern 1: Constrained Bitmap Decode ✅
```kotlin
val bitmap = decodeBitmapConstrained(context, uri, maxLongEdge = 2200)
    ?: error("Failed to decode image: $uri")
try {
    // use bitmap
} finally {
    bitmap.recycle()  // MANDATORY
}
```

### Pattern 2: PDF Temp-File Loading ✅
```kotlin
context.withUriCopiedToCacheFile(uri, "docforge_xyz_", ".pdf") { sourceFile ->
    loadPdfDocument(sourceFile).use { doc ->
        // doc auto-closed, sourceFile auto-deleted in finally
    }
}
```

### Pattern 3: OpenCV Mat Cleanup ✅
```kotlin
val mat = Mat()
try {
    Imgproc.cvtColor(inputMat, mat, Imgproc.COLOR_RGBA2GRAY)
} finally {
    mat.release()  // MANDATORY — native memory
}
```

### Pattern 4: Compressor RGB_565 (50% memory savings)
```kotlin
// ONLY for pages with no transparency
val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
try {
    canvas.drawColor(Color.WHITE)  // MUST fill — no alpha channel
    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
} finally {
    bitmap.recycle()
}
```

### Pattern 5: Output File Creation (MANDATORY — Rule 11)
```kotlin
// CORRECT
val outputFile = resolveNonConflictingFile(outputDir, sanitizedName, "pdf")

// WRONG — Never do this for outputs
val outputFile = File(outputDir, "$sanitizedName.pdf")  // ❌ Overwrites existing files
```

### Pattern 6: MediaStore Notification (MANDATORY — Rule 12)
```kotlin
// After writing any file to public storage:
MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf(mimeType), null)
```

---

# §8 · BATCH QUEUE IMPLEMENTATION

### Architecture
```
UI ←→ BatchQueueViewModel ←→ BatchQueueRuntimeStore (singleton StateFlow)
                                        ↕ Mutex: beginProcessing()
                           BatchQueueForegroundService (SupervisorJob + Dispatchers.IO)
                                        ↕ Sequential task execution
                              Tool engines
                                        ↕
                              HistoryRepository (insert on success)
```

### Thread Safety ✅
- `AtomicLong` — task ID
- `Mutex` — `beginProcessing()` prevents double-start
- `StateFlow.update {}` — atomic queue mutations
- `SupervisorJob` — child failure doesn't kill queue
- `CancellationException` — re-thrown after marking task CANCELED

### Known Issues
- `SimpleDateFormat` in `addTask()` — not thread-safe (M8 — fix with `DateTimeFormatter`)
- Queue not persisted to Room — lost on process kill (TODO Sprint 4)
- `BatchQueueViewModel` bypasses DI, accesses `DocForgeDatabase.get()` directly

---

# §9 · DOCUMENT SCANNER PIPELINE

### Edge Detection (OpenCV) ✅
```
Bitmap → Mat (RGBA) → Grayscale → GaussianBlur(5×5, sigma=0)
       → Canny(threshold1=75, threshold2=200)
       → findContours(RETR_LIST, CHAIN_APPROX_SIMPLE)
       → approxPolyDP(epsilon = 2% of perimeter)
       → Filter: 4 points + convex + area ≥ 15% of frame
       → Order corners: TL → TR → BR → BL (clockwise)
       → Output: DetectedQuad(corners, confidence 0.0–1.0)
Fallback: 8%/12% inset rectangle at confidence 0.25
```

### Perspective Correction ✅
```
getPerspectiveTransform(srcPoints, dstPoints) → 3×3 matrix
warpPerspective(input, output, matrix, outputSize) → corrected image
Output capped at 2000px longest edge
```

### Image Filters ✅
| Filter | OpenCV Operation |
|---|---|
| Original | No processing |
| Grayscale | `cvtColor(COLOR_RGBA2GRAY)` → `cvtColor(COLOR_GRAY2RGBA)` |
| B&W | Otsu thresholding |
| Enhanced | Unsharp mask: `addWeighted(gray, 1.55, blurred, -0.55, 0)` |

### Pending Features
- Brightness/contrast: `Core.addWeighted` with user controls
- Deskew: Hough line transform to detect and correct document tilt
- QR/Barcode: ML Kit Barcode Scanning API
- Scan→OCR: One-tap `PdfOcrTool.process()` after capture
- Business card: ML Kit structured data extraction → vCard

---

# §10 · RESUME BUILDER

### Current: 10 Templates ✅ (with CRITICAL-3 crash on sidebar variants)

**Data model:** `ResumeTemplate` with 18 properties (layout, header style, 5 colors, font family, 3 sizes, 3 spacings, dividers, icons).

**Layouts:** SINGLE_COLUMN ✅, TWO_COLUMN ✅, SIDEBAR_LEFT 🔴, SIDEBAR_RIGHT 🔴

**Export:** `ResumeRenderer` Composable → Bitmap capture → `PdfCreator.createPdfFromImages()`

### Target: 105 Templates
- Phase 2: +40 (Engineering, Design, Business, Academic, Healthcare, Sales) — see claude.md §9
- Phase 3: +55 (Legal, Education, Engineering, Hospitality, Entry-Level, International, ATS, Portfolio) — see claude.md §9

### Additional Resume Features (TODO)
- ATS-optimized templates (plain formatting, machine-readable)
- Template preview thumbnails (pre-rendered bitmap grid)
- Custom color theme per template (user color picker)
- Custom font selection (bundled Noto/Roboto variants)
- Multiple saved resume slots
- Resume ATS score checker (offline keyword density analysis)
- JSON Resume format import/export
- Portfolio/Cover Letter companion builder
- QR code embed (LinkedIn URL / portfolio link)

---

# §11 · DATABASE & HISTORY

## Room Database v3

```kotlin
@Database(
    entities = [ConversionHistoryEntity::class, BatchPresetEntity::class],
    version = 3,
    exportSchema = true
)
abstract class DocForgeDatabase : RoomDatabase()
```

**⚠️ MUST REMOVE:** `fallbackToDestructiveMigration()` before any public release (H3).

**Entities:**

`conversion_history`: id, sourceLabel, outputPath, operation, createdAtMillis, inputCount, outputSizeBytes, outputUri (v3), displayName (v3)

`batch_presets`: id, name, createdAtMillis, tasksJson

**Migrations:** 1→2 (add batch_presets), 2→3 (add outputUri + displayName) ✅

---

# §12 · SETTINGS SYSTEM

`DocForgeSettingsStore` — SharedPreferences-backed, currently returns values synchronously.

**Required fix (M7):** Expose `StateFlow<DocForgeSettings>` initialized in `Application.onCreate()`. Never call SP getters inside composable lambdas.

**Settings needed (TODO):**
- `allow_cellular_model_download` — default false (for M1 WiFi gate)
- `default_image_export_dpi` — default 150 (for M3 fix)
- `auto_notify_media_store` — always true
- `max_input_file_size_mb` — default 500 (for M11 validation)

---

# §13 · NAVIGATION STRUCTURE

`DocForgeNavHost.kt` (590 lines) — all routes inline.

**Active routes (25+):** home, onboarding, scanner, converter, pdf_merge, pdf_split, pdf_compress, pdf_sign, pdf_annotate, pdf_redact, pdf_ocr, pdf_translate, pdf_form, pdf_stamp, pdf_password, pdf_text, pdf_images, pdf_id_card, resume_builder, batch_queue, history, settings

**Known issues:**
- Route strings are raw `String` literals — type-unsafe. After Nav 2.8+ upgrade, use sealed class routes.
- `activeSharedLaunch` in Composable, not ViewModel — lost on rotation (H2)
- `currentSettings()` SP read in lambdas — sync disk I/O (M7)

---

# §14 · TESTING CHECKLIST — FOR EVERY NEW TOOL

- [ ] All bitmaps recycled in `finally` blocks
- [ ] All temp files cleaned up via `withUriCopiedToCacheFile`
- [ ] `ensureActive()` called in every page/item loop
- [ ] Output filename sanitized via regex
- [ ] Output file created via `resolveNonConflictingFile()`
- [ ] `MediaScannerConnection.scanFile()` called after output write
- [ ] Tool registered in `AppModule` (after Hilt migration)
- [ ] Navigation route added in `DocForgeNavHost`
- [ ] History recorded via `historyRepository.insert()` on success
- [ ] Error messages are user-friendly (no stack traces in UI)
- [ ] Progress callbacks provided for operations > 1 second
- [ ] No network calls in core processing path
- [ ] Input file size validated before processing
- [ ] Unit test written for core business logic

---

# §15 · BUILD CONFIGURATION

```toml
# Current → Target
agp = "8.5.2" → "8.6+"
kotlin = "1.9.24" → "2.1.x"
composeBom = "2024.06.00" → "2025.x"
room = "2.6.1"
hilt = "2.51.1"
pdfboxAndroid = "2.0.27.0"   # Latest ✅
opencv = "4.9.0"              # Latest ✅
mlkitTextRecognition = "16.0.1"
mlkitTranslate = "17.0.3"
camerax = "1.3.4"

minSdk = 26     # Keep — good floor for java.time API availability
compileSdk = 34 → 35
targetSdk = 34  → 35
```

---

# §16 · COMPLETE CONVERSION TARGET MATRIX

## Currently Working
- Image → JPG/PNG/WEBP ✅
- Video → M4A (zero re-encode) ✅
- CSV → PDF ✅
- TXT → PDF ✅
- Markdown → PDF ✅
- DOCX → PDF (basic text only) ⚠️
- RTF → PDF (ASCII only) ⚠️
- Audio format conversion (WAV/FLAC/AAC) ✅
- PDF ↔ all PDF tool operations ✅

## Planned (Priority Order)
1. PPTX → PDF (Apache POI XSLF)
2. XLSX → PDF (Apache POI XSSF)
3. HTML → PDF (WebView PrintAdapter)
4. EPUB → PDF (epublib)
5. ODT/ODS → PDF
6. PDF → DOCX (approximate layout)
7. Image → HEIC (API 28+)
8. Video → GIF
9. DjVu → PDF
10. CBZ/CBR → PDF
11. PDF → HTML
12. PDF → EPUB
13. PDF → Markdown
14. PDF → PDF/A (archival)
15. Image → SVG trace

---

# §17 · IMPLEMENTATION SPRINT ROADMAP

### Sprint 1 — Critical Fixes (Week 1) ✅ COMPLETED
1. ✅ Fix `SidebarResume` `fillMaxHeight` crash (C3) — removed modifier
2. ✅ Complete PDF redaction — annotations, XMP, embedded files, OC layers, annotation verification (C2)
3. ⏭️ Resolve DI — commit to full Hilt, delete `AppDependencies` (C1) — deferred (~2 day migration)
4. ✅ Remove `fallbackToDestructiveMigration()` from production DB (H3)

### Sprint 2 — High Priority Fixes (Week 2) ✅ COMPLETED
5. ⏭️ `TempFileRegistry` — guard in-flight files from `onTrimMemory` (H1) — deferred
6. ⏭️ Move `activeSharedLaunch` to ViewModel (H2) — deferred
7. ✅ Audit all output file creation — enforce `resolveNonConflictingFile()` (H6) — 10 files fixed
8. ✅ RTF parser — add `\uN` unicode escape handling (H4)
9. ✅ Remove `initLocal()` from `DocumentEdgeDetector` constructor → lazy init (H5)
10. ⏭️ Add `opencvDeferred` to `EngineWarmup` (H5) — lazy init sufficient
11. ✅ `MediaScannerConnection.scanFile()` from every output-writing tool (H7) — replaced broken MediaStore

### Sprint 3 — Medium Fixes (Week 3) ✅ COMPLETED
12. ✅ `PdfPasswordTool` → AES-256, 8-char min (M2)
13. ✅ `PdfPageImageExporter` → default 2.5f scale (M3)
14. ✅ `BitmapDecodeUtils` → EXIF TRANSVERSE/TRANSPOSE mirror flip (M6)
15. ✅ `PdfCreator` AUTO → pixels-to-points conversion (M5)
16. ⏭️ `DocForgeSettingsStore` → `StateFlow` (M7) — deferred (UI-layer change)
17. ✅ `SimpleDateFormat` → `java.time.DateTimeFormatter` (M8)
18. 🟡 `SavedSignatureStore.save()` → verified already uses `stream.use {}` correctly (M9)
19. ✅ `SignaturePlacementTemplateStore` → atomic write with rename (M10)
20. ✅ `PdfTranslationTool` → WiFi-only download (M1)
21. ⏭️ Embed Noto Sans for Unicode overlay text (M13) — deferred (requires asset bundling)
22. ⏭️ `validateInputSize()` in all tool entry points (M11) — deferred
23. ⏭️ Fix `PdfCompressor` RGB_565 alpha issue (M4) — deferred
24. ⏭️ Fix `PdfTranslationTool` layout preservation (M14) — deferred
25. ✅ Fix `MediaStore` notification API (M12) — fixed via MediaScannerConnection

### Sprint 4 — New Features A (Weeks 4–6) ✅ COMPLETED
- ✅ HTML → PDF converter — `HtmlPdfConverter.kt` (WebView PrintAdapter pipeline)
- ✅ PDF Compare tool — `PdfCompareTool.kt` (bitmap diff with threshold + red highlight overlay)
- ✅ PDF Page Crop — `PdfPageCropTool.kt` (percentage-based + absolute point crop modes)
- ✅ PDF Header/Footer/Page Numbers — `PdfHeaderFooterTool.kt` (centred header/footer + page numbering)
- ✅ Batch queue persistence — `BatchQueueTaskEntity.kt` + `BatchQueueTaskDao` (Room entity/DAO)
- ⏭️ PDF Reader (basic) — deferred (requires new UI screens + ViewModel)
- ⏭️ 40 additional resume templates (Phase 2) — deferred (content authoring)
- ⏭️ PPTX → PDF — deferred (requires Apache POI XSLF dependency ~10 MB)
- ⏭️ XLSX → PDF — deferred (requires Apache POI XSSF dependency)
- ⏭️ PDF Digital Signature (PKCS#12) — deferred (requires KeyStore integration)

### Sprint 5 — New Features B (Weeks 7–10) ✅ COMPLETED
- ✅ Business card scanner → vCard — `BusinessCardParser.kt` (ML Kit OCR → contact parsing → .vcf)
- ✅ PDF/A compliance conversion — `PdfAComplianceTool.kt` (XMP metadata + PDF/A-1b identification)
- ✅ AES-256 encrypted document vault — `EncryptedDocumentVault.kt` (PBKDF2 + AES-256-GCM)
- ✅ Resume ATS score checker — `ResumeAtsScorer.kt` (keyword matching + section scoring)
- ✅ Typed signature (calligraphy) — `TypedSignatureRenderer.kt` (5 font styles, transparent PNG)
- ⏭️ 55 more resume templates (Phase 3) — deferred (content authoring)
- ⏭️ QR/Barcode detection — deferred (requires ML Kit Barcode Scanning dependency)
- ⏭️ EPUB → PDF — deferred (requires EPUB parsing library)
- ⏭️ Document template library — deferred (content authoring)
- ⏭️ Invoice/receipt parser — deferred (requires ML Kit entity extraction)
- ⏭️ Deskew / rotation correction in scanner — deferred

### Sprint 6 — Tests & Upgrades (Weeks 11–12) ✅ COMPLETED
- ✅ Upgrade Kotlin 1.9.24 → 2.1.0
- ✅ Upgrade Compose BOM 2024.06.00 → 2025.01.01
- ✅ Upgrade compileSdk/targetSdk 34 → 35
- ✅ Extract `importPage` to `PdfIoUtils.importPageFull()` shared utility (was duplicated in 7 classes)
- ✅ Fix ZIP individual file accumulation in PdfPageImageExporter + ScanImageExporter
- ✅ Fix URI extension parsing in `ShareIntentRouter` (`.toString()` → `.lastPathSegment`)
- ✅ Add `backup_rules.xml` + `data_extraction_rules.xml` for saved signatures/templates
- ⏭️ Unit tests: all 19 PDF tools — deferred (test infrastructure TBD)
- ⏭️ Integration tests: `HistoryRepository`, `DocForgeDatabase` migrations — deferred
- ⏭️ Compose UI tests: scanner flow, merge flow, batch queue flow — deferred

### Sprint 7 — Gemini 3.1 Pro Feedback Validation (Week 13) ✅ COMPLETED
**Validated external Gemini 3.1 Pro feedback against actual codebase. Implemented only items aligned with vision.**

#### Implemented
53. ✅ `BitmapDecodeUtils.decodeBitmapThumbnail()` — 400px cap for UI previews (~640KB vs ~19MB)
54. ✅ `BitmapDecodeUtils.applyExifRotation()` — OOM leak fix (separate catch + recycle)
55. ✅ `ConversionHistoryDao.observePaged()` — Paging 3 `PagingSource`
56. ✅ `BatchQueueTaskDao.observePaged()` — Paging 3 `PagingSource`
57. ✅ Paging 3 dependency — `paging = 3.3.5`, `paging-runtime` + `paging-compose` in version catalog
58. ✅ `DocumentTextIndex.kt` — FTS4 full-text search (content entity + shadow table + DAO)
59. ✅ `DocForgeDatabase` v3→v4 migration — 3 new tables (batch_queue_tasks, document_text_index, document_text_fts)
60. ✅ `AutoCaptureAnalyzer` in `DocumentEdgeDetector.kt` — Laplacian variance blur detection + frame-to-frame corner drift stability + 5-frame trigger threshold

#### Rejected
- ❌ GPU/RenderScript acceleration — deprecated Android 12+
- ❌ Vulkan compute shaders — overkill for document scanning workload
- ❌ AI-powered summarization — breaks offline-first constraint, requires LLM or cloud API
- ❌ P2P document sync — out of scope, requires network stack + conflict resolution

#### Already Correct (verified, no changes)
- ✅ `EncryptedDocumentVault` — streaming `CipherOutputStream` with 8KB buffer
- ✅ `EncryptedDocumentVault` — `context.applicationContext` (no Activity leak)
- ✅ All Room DAOs — `suspend`/`Flow` (no main-thread database access)

### Sprint 7b — Gemini Round 2 Validation (Week 13) ✅ NO CODE CHANGES NEEDED
**Second Gemini feedback round — all 5 categories validated against codebase. No code changes required.**

#### Already Done (Sprint 7)
- Bitmap thumbnails, Paging 3, FTS4, EXIF OOM fix, encrypted streaming — all previously implemented

#### Already Correct
- Thread isolation: all tool classes use `withContext(Dispatchers.IO)` at the tool layer. ViewModels never block Main.

#### Gemini Factual Error
- ❌ "ML Kit depends on Play Services, crashes on Huawei" → WRONG. `com.google.mlkit:text-recognition:16.0.1` = bundled variant (model in APK, works on ALL devices). The unbundled is `com.google.android.gms:play-services-mlkit-text-recognition` which we do NOT use.

#### Deferred
- Bitmap Pooling → post-launch optimization
- MED-13 Noto Sans font embedding → internationalization sprint