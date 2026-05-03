# AnyDoc (DocForge) — Complete Architectural Blueprint
## Claude Context Document · v1.0 · May 2026

---

# §1 · VISION & NON-NEGOTIABLE CONSTRAINTS

**Mission:** Build the ultimate offline-first Android document super-app that replaces every document-related subscription worldwide — achieving 0ms perceived latency, zero network dependency, sub-15MB APK, and flawless performance on 4GB RAM devices without heating.

| Constraint | Target | Enforcement |
|---|---|---|
| Latency | 0ms perceived UI, <50ms for any tool launch | Async engine warmup, Compose stability, IO-only heavy work |
| RAM ceiling | ≤4GB device comfort | RGB_565 bitmaps, temp-file PDF loading, bitmap recycling, 1800px cap |
| APK size | <15MB | R8 full mode, no bundled ML models, tree-shaking |
| Network | 100% offline for core features | No REST calls; ML Kit models download once then offline |
| Heating | Zero sustained thermal throttle | SupervisorJob scoping, sequential batch, bitmap.recycle() |

---

# §2 · PROJECT STRUCTURE & MODULE GRAPH

```
anyDoc/
├── app/                          # Application shell, DI, navigation, batch queue
│   ├── DocForgeApp.kt            # Application class — cache cleanup, StrictMode
│   ├── MainActivity.kt           # Single-Activity Compose host
│   ├── AppDependencies.kt        # Manual DI graph (no Hilt in features)
│   ├── AppModule.kt              # Hilt @Module for app-level bindings
│   ├── navigation/
│   │   └── DocForgeNavHost.kt    # Central nav graph (590 lines)
│   ├── runtime/
│   │   └── EngineWarmup.kt       # Async PdfBox + OpenCV pre-init
│   ├── share/
│   │   └── ShareIntentRouter.kt  # External share → feature routing
│   └── batch/                    # Foreground service batch queue (7 files)
├── core/
│   ├── domain/                   # Pure Kotlin models + interfaces
│   │   ├── model/ConversionRecord.kt
│   │   ├── repository/HistoryRepository.kt
│   │   └── settings/DocForgeSettings.kt
│   ├── opencv/                   # OpenCV wrapper — edge detect + perspective
│   │   └── DocumentEdgeDetector.kt (347 lines)
│   ├── pdf/                      # 19 PDF tool classes — the engine room
│   │   ├── PdfBoxInit.kt         # AtomicBoolean singleton init guard
│   │   ├── PdfIoUtils.kt         # URI→cache, temp file, MemoryUsageSetting
│   │   ├── BitmapDecodeUtils.kt  # EXIF-aware constrained decode
│   │   ├── PdfCreator.kt         # Images→PDF (Android PdfDocument API)
│   │   ├── PdfMerger.kt          # Multi-source merge with bookmarks
│   │   ├── PdfSplitter.kt        # 7 split modes (423 lines)
│   │   ├── PdfCompressor.kt      # JPEG rasterization compression
│   │   ├── PdfSigner.kt          # Multi-placement signature overlay
│   │   ├── PdfAnnotator.kt       # Highlight/text/sticky/freehand (387 lines)
│   │   ├── PdfRedactionTool.kt   # Content-stream level redaction + verify
│   │   ├── PdfOcrTool.kt         # ML Kit OCR → searchable PDF (430 lines)
│   │   ├── PdfTranslationTool.kt # OCR + ML Kit Translate overlay (379 lines)
│   │   ├── PdfFormTool.kt        # AcroForm read/fill/build (297 lines)
│   │   ├── PdfBatchStampTool.kt  # Watermark + Bates numbering
│   │   ├── PdfPasswordTool.kt    # 128-bit AES encrypt/decrypt
│   │   ├── PdfTextExtractor.kt   # PDFTextStripper → .txt
│   │   ├── PdfPageImageExporter.kt # PDF→JPG/PNG/WEBP + ZIP bundle
│   │   ├── PdfIdCardTool.kt      # Front+back ID card → single page
│   │   └── ScanImageExporter.kt  # Scan pages → image files + ZIP
│   ├── storage/                  # Room DB (v3), DAOs, migrations
│   │   └── db/DocForgeDatabase.kt
│   └── ui/                       # Shared Compose utilities + ImmutableCollections
├── feature/
│   ├── converter/                # Image/Doc/Video conversion engines + UI
│   │   ├── ImageFormatConverter.kt
│   │   ├── DocumentPdfConverter.kt (434 lines — DOCX/RTF/CSV/TXT parser)
│   │   └── VideoAudioExtractor.kt (MediaExtractor/MediaMuxer)
│   ├── history/                  # Conversion history list + management
│   ├── scanner/                  # CameraX document scanner + filters
│   └── pdf-tools/                # 15+ PDF tool screens + ViewModels
│       ├── resume/               # Resume builder (10 templates, Compose renderer)
│       ├── SavedSignatureStore.kt
│       └── SignaturePlacementTemplateStore.kt
└── gradle/libs.versions.toml    # Version catalog (77 lines)
```

**Module dependency rule:** `feature/*` → `core/*` → pure Kotlin. No feature↔feature dependency. App wires everything via `AppDependencies`.

---

# §3 · DEPENDENCY INJECTION & WIRING

## 3.1 — Hybrid DI Strategy

Hilt is applied **only at the app module level** (`@HiltAndroidApp`, `@AndroidEntryPoint`). Feature modules receive dependencies through **manual constructor injection** via `AppDependencies`:

```kotlin
class AppDependencies(context: Context) {
    val pdfCreator = PdfCreator(context)
    val pdfMerger = PdfMerger(context)
    val pdfSplitter = PdfSplitter(context)
    val pdfCompressor = PdfCompressor(context)
    val pdfSigner = PdfSigner(context)
    val pdfAnnotator = PdfAnnotator(context)
    val pdfRedactionTool = PdfRedactionTool(context)
    val pdfOcrTool = PdfOcrTool(context)
    val pdfTranslationTool = PdfTranslationTool(context)
    val pdfFormTool = PdfFormTool(context)
    val pdfBatchStampTool = PdfBatchStampTool(context)
    val pdfPasswordTool = PdfPasswordTool(context)
    val pdfTextExtractor = PdfTextExtractor(context)
    val pdfPageImageExporter = PdfPageImageExporter(context)
    val pdfIdCardTool = PdfIdCardTool(context)
    val scanImageExporter = ScanImageExporter(context)
    val imageFormatConverter = ImageFormatConverter(context)
    val documentPdfConverter = DocumentPdfConverter(context)
    val videoAudioExtractor = VideoAudioExtractor(context)
    val edgeDetector = DocumentEdgeDetector()
    val historyRepository: HistoryRepository = LocalHistoryRepository(db.conversionHistoryDao())
}
```

**Design rationale:** Avoids Hilt's annotation processing overhead in feature modules. Each tool class is a plain Kotlin class with a `Context` constructor — zero reflection, zero proxy, instant construction.

## 3.2 — ViewModel Factory Pattern

All feature ViewModels use custom `ViewModelProvider.Factory` instances created in `DocForgeNavHost`, receiving dependencies from `AppDependencies`:

```kotlin
composable("scanner") {
    val vm: ScannerViewModel = viewModel(factory = ScannerViewModelFactory(
        pdfCreator = deps.pdfCreator,
        scanImageExporter = deps.scanImageExporter,
        historyRepository = deps.historyRepository
    ))
    ScannerRoute(viewModel = vm)
}
```

---

# §4 · ENGINE WARMUP — SUB-MILLISECOND STARTUP

```kotlin
object EngineWarmup {
    private val pdfReady = AtomicBoolean(false)
    private val opencvReady = AtomicBoolean(false)
    val pdfDeferred = CompletableDeferred<Unit>()

    fun warmup(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            if (pdfReady.compareAndSet(false, true)) {
                PdfBoxInit.ensure(context)  // PDFBoxResourceLoader.init() — once per process
                pdfDeferred.complete(Unit)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            if (opencvReady.compareAndSet(false, true)) {
                OpenCVLoader.initLocal()  // Native .so loading
            }
        }
    }
}
```

Called from `DocForgeApp.onCreate()`. By the time user navigates to any tool, both engines are ready. `AtomicBoolean` + `CompletableDeferred` ensures exactly-once initialization with zero contention.

---

# §5 · CORE PDF ENGINE — DESIGN DECISIONS

## 5.1 — Memory-Safe PDF Loading

Every PDF operation follows the same pattern:
1. **Copy URI to cache** via `copyUriToCacheFile()` (8MB chunked `FileChannel.transferFrom`)
2. **Load with `MemoryUsageSetting.setupTempFileOnly()`** — PdfBox spills to disk instead of heap
3. **Process with cancellation checks** — `coroutineContext.ensureActive()` on every page loop
4. **Clean up in `finally`** — `Mat.release()`, `bitmap.recycle()`, temp file deletion

```kotlin
internal fun loadPdfDocument(file: File): PDDocument {
    return PDDocument.load(file, pdfMemoryUsageSetting())  // Temp-file only
}
```

## 5.2 — Bitmap Pipeline

`BitmapDecodeUtils.kt` is the universal image ingestion point:
- **API 28+:** `ImageDecoder` with `ALLOCATOR_SOFTWARE` — handles EXIF natively
- **API <28:** `BitmapFactory` with computed `inSampleSize` + manual EXIF rotation
- **Max long edge: 1800-2200px** — prevents OOM on 4GB devices
- All bitmaps are `ARGB_8888` for quality, except compressor which uses `RGB_565` for 50% memory savings

## 5.3 — Output File Resolution

Every tool uses `resolveNonConflictingFile()` to prevent overwrites:
```kotlin
fun resolveNonConflictingFile(directory: File, baseName: String, extension: String): File {
    val candidate = File(directory, "$baseName.$extension")
    if (!candidate.exists()) return candidate
    var counter = 1
    while (true) {
        val numbered = File(directory, "${baseName}_$counter.$extension")
        if (!numbered.exists()) return numbered
        counter++
    }
}
```

Output directories are configurable per bucket (Documents/Pictures/Music) via `DocForgeSettingsStore`.

---

# §6 · FEATURE CATALOG — EVERY TOOL EXPLAINED

## 6.1 — Document Scanner (`feature/scanner`)

| Component | Role |
|---|---|
| `ScannerScreen.kt` | CameraX preview + capture + gallery import |
| `ScannerViewModel.kt` | Page management, filter application, export orchestration |
| `ScannerUiState.kt` | Immutable state with `PersistentList<StableUriRef>` |

**Edge Detection:** `DocumentEdgeDetector` (OpenCV) performs:
1. Grayscale conversion → GaussianBlur(5×5) → Canny(75,200)
2. `findContours` → `approxPolyDP` (2% perimeter) → filter 4-point convex hulls
3. Area ratio filter (≥15% of frame) → corner ordering (top-left CW)
4. Fallback: 8%/12% inset rectangle at confidence 0.25

**Perspective Correction:** 4-point homography via `Imgproc.getPerspectiveTransform` + `warpPerspective`. Output capped at 2000px longest edge.

**Image Filters (all OpenCV):**
- Grayscale: `COLOR_RGBA2GRAY` → `COLOR_GRAY2RGBA`
- B&W: Otsu's thresholding (auto) or fixed threshold
- Enhanced: Unsharp mask — `addWeighted(gray, 1.55, blurred, -0.55, 0)`

**Export formats:** PDF (via `PdfCreator`), JPG, PNG (via `ScanImageExporter`), with optional ZIP bundling.

## 6.2 — Document Converter (`feature/converter`)

### ImageFormatConverter
Batch converts images between JPG/PNG/WEBP with quality control (10-100) and scale factor (0.2x-3x). Uses `decodeBitmapConstrained` for safe memory.

### DocumentPdfConverter (434 lines)
Converts DOCX, RTF, CSV, TXT → PDF using **zero external dependencies**:

- **DOCX:** `ZipInputStream` → parse `word/document.xml` via `XmlPullParser`. Handles paragraphs, bold/italic runs, tables (`<w:tbl>`), list items (`<w:numPr>`), embedded image detection.
- **RTF:** Regex-based stripping of control words (`\\par`, `\\tab`, `\\'XX` hex escapes)
- **CSV:** Custom RFC-4180 compliant parser with quoted field support → pipe-delimited layout
- **TXT:** Direct line extraction

All text is rendered to PDF via Android's `PdfDocument` API with word-wrapping via `Paint.breakText()`.

### VideoAudioExtractor
Extracts audio from video using `MediaExtractor` + `MediaMuxer`:
- **M4A output:** Direct mux to MPEG-4 container (zero re-encoding)
- **MP3 output:** Passthrough only if source audio is already MP3 (no transcoding = no quality loss)
- Buffer size: `KEY_MAX_INPUT_SIZE` or 256KB fallback

## 6.3 — PDF Tools Suite (`feature/pdf-tools` + `core/pdf`)

### PdfMerger
- Accepts mixed PDF + image inputs (auto-detected by MIME/extension)
- Images rendered as full pages with fit-center scaling
- **Bookmarks:** Auto-generates `PDDocumentOutline` with source file labels
- **Metadata:** Optional title/author/subject injection
- Page size modes: KEEP_SOURCE, A4_FIT, LETTER_FIT

### PdfSplitter (423 lines — 7 operations)
1. `splitByRange` — extract page range to single PDF
2. `extractPages` — individual pages to separate PDFs
3. `splitEveryNPages` — chunk into N-page segments
4. `splitByBookmarks` — split at `PDDocumentOutline` boundaries
5. `reorderPages` — arbitrary page reordering
6. `deletePages` — remove pages, keep remainder
7. `applyWorkspaceEdits` — combined reorder + per-page rotation

### PdfCompressor
**Strategy:** JPEG rasterization — renders each page via `PdfRenderer` at configurable DPI, then embeds as JPEG via `JPEGFactory`:

| Level | DPI | JPEG Quality | Est. Ratio |
|---|---|---|---|
| HIGH | 150 | 82% | ~65% |
| MEDIUM | 120 | 70% | ~45% |
| LOW | 96 | 58% | ~30% |

Uses `RGB_565` (no alpha needed for documents) = 50% less memory per page. Progress callback for UI.

### PdfSigner
- Multi-placement: single signature bitmap applied to multiple positions across pages
- Position specified as ratio-based coordinates (xRatio, yRatio, widthRatio) for resolution independence
- Signature embedded as `LosslessFactory` PNG image in PDPageContentStream APPEND mode

### PdfAnnotator (387 lines)
Four annotation types, all ratio-based positioning:
- **Highlight:** Semi-transparent rectangle (55% white blend)
- **Text Box:** Background fill + border + word-wrapped text (max 4 lines)
- **Sticky Note:** Yellow square icon + adjacent text label
- **Freehand:** Stroke path with break points for multi-segment drawing

### PdfRedactionTool (319 lines)
**True content-stream redaction** — not just visual overlay:
1. Parse page content stream via `PDFStreamParser`
2. Match text-showing operators (`Tj`, `TJ`, `'`, `"`) against redaction terms
3. Remove matching operators entirely from token stream
4. Rewrite content stream with `ContentStreamWriter` + FLATE_DECODE compression
5. Scrub form field values matching terms
6. Scrub document metadata (`PDDocumentInformation` reset)
7. **Verification pass:** Re-extract text with `PDFTextStripper`, confirm zero remaining matches. Deletes output file on failure.

### PdfOcrTool (430 lines)
- **PDF input:** Renders pages at 1800px via `PdfRenderer` → ML Kit `TextRecognition`
- **Image input:** Direct bitmap → ML Kit
- **Text output:** Structured `=== Page N ===` format
- **Searchable PDF:** Invisible text layer (`RenderingMode.NEITHER`) overlaid on original pages with position-mapped coordinates

### PdfTranslationTool (379 lines)
Pipeline: OCR → ML Kit Translate → white-rectangle overlay + translated text:
1. Render pages → extract text lines with bounding boxes
2. Download translation model if needed (one-time, then offline)
3. Batch translate (24 lines/batch) with progress callbacks
4. For each line: draw white rect over original → draw translated text at same position
5. Word-wrapping with `PDType1Font.HELVETICA.getStringWidth()` measurement

### PdfFormTool (297 lines)
- **List fields:** Reads AcroForm → maps to TEXT/CHECKBOX/RADIO/CHOICE types
- **Fill fields:** Type-aware value setting (checkbox: true/yes/1, radio: by index or export value)
- **Add text field:** Creates `PDTextField` + `PDAnnotationWidget` with ratio-based positioning
- **Flatten option:** Bakes form values into content stream (non-editable)

### PdfBatchStampTool
- **Watermark:** Diagonal text via rotation matrix (`Matrix.getRotateInstance`), configurable gray level
- **Bates numbering:** Sequential numbering across multiple PDFs with prefix + zero-padding
- Processes multiple input PDFs in sequence with continuous Bates counter

### PdfPasswordTool
- **Protect:** 128-bit AES encryption via `StandardProtectionPolicy`
- **Unlock:** Password-based decryption + `setAllSecurityToBeRemoved(true)`
- Minimum 6-character password enforcement

### PdfIdCardTool
Creates single-page front+back ID card layout using Android `PdfDocument` API with fit-center scaling and labeled sections.

### PdfPageImageExporter
Exports PDF pages as JPG/PNG/WEBP images with configurable scale factor. Optional ZIP bundling via `ZipOutputStream`.

---

# §7 · RESUME BUILDER

## 7.1 — Template System

10 bundled templates across 6 categories (Engineer, Designer, Manager, Academic, Sales, General) and 5 styles (Modern, Classic, Minimal, Creative, Executive):

Each `ResumeTemplate` is a pure data class with 18 configurable properties:
- Layout: `SINGLE_COLUMN`, `TWO_COLUMN`, `SIDEBAR_LEFT`, `SIDEBAR_RIGHT`
- Header: `TOP_LEFT`, `TOP_CENTER`, `BANNER`, `SIDEBAR_HEADER`
- Colors: primary, accent, background, text, subtitle
- Typography: name/section/body font sizes, font family
- Spacing: section, item, page margin
- Features: dividers, section icons

## 7.2 — Renderer Architecture

`ResumeRenderer.kt` (326 lines) is a pure Compose composable that interprets template data:
- Layout dispatch: 4 layout composables (single, two-column, sidebar-left, sidebar-right)
- Shared building blocks: `ResumeHeader`, `SectionTitle`, `ExperienceSection`, `EducationSection`, `SkillsSection`, `BulletSection`
- Skills rendered as chip-style `FlowRow` with bordered items
- Experience entries use `AnnotatedString` for inline bold/accent styling
- Composable output → captured to Bitmap → PDF for export

---

# §8 · BATCH QUEUE SYSTEM

## 8.1 — Architecture

```
BatchQueueViewModel → BatchQueueRuntimeStore (singleton) → BatchQueueForegroundService
                    → BatchQueuePresetStore (Room)
```

**6 task types:** Images→PDF, Images→JPG, PDF Merge, PDF Compress, Doc→PDF, Video→M4A

## 8.2 — RuntimeStore (singleton, thread-safe)

- `MutableStateFlow<BatchQueueUiState>` for reactive UI
- `AtomicLong` task ID counter
- `Mutex` for `beginProcessing()` to prevent double-start
- Task lifecycle: QUEUED → RUNNING → SUCCESS/FAILED/CANCELED
- Operations: add, remove, move up/down, update output name, clear, replace with preset

## 8.3 — ForegroundService

- `startForeground()` called within 5s of `startForegroundService()` (Android requirement)
- `SupervisorJob + Dispatchers.IO` scoped coroutine
- Sequential task execution with per-task try/catch (failure doesn't stop queue)
- `CancellationException` propagated correctly (re-thrown after marking task)
- Progress notification via `NotificationManagerCompat` with progress bar
- Completion notification with success/failure summary
- History recording via `historyRepository.insert()` for each successful task

## 8.4 — Preset System

Presets stored in Room (`BatchPresetEntity`) with JSON-serialized task list. Supports save, load (replaces queue), delete.

---

# §9 · STORAGE LAYER

## Room Database (v3)

```kotlin
@Database(entities = [ConversionHistoryEntity, BatchPresetEntity], version = 3)
abstract class DocForgeDatabase : RoomDatabase()
```

**Migrations:**
- 1→2: Added `batch_presets` table
- 2→3: Added `outputUri` + `displayName` columns to `conversion_history`

**Singleton pattern:** `@Volatile` + `synchronized` double-checked locking.

**HistoryRepository interface:**
```kotlin
fun observeRecent(limit: Int = 20): Flow<List<ConversionRecord>>
suspend fun insert(record: ConversionRecord)
suspend fun deleteById(id: Long)
suspend fun deleteAll()
```

---

# §10 · SETTINGS & OUTPUT MANAGEMENT

`DocForgeSettingsStore` (SharedPreferences-backed):

| Key | Default | Purpose |
|---|---|---|
| `onboarding_completed` | false | First-launch flow gate |
| `default_pdf_page_size` | A4 | PDF creation default |
| `default_pdf_compression` | MEDIUM | Compressor default |
| `default_image_quality` | 90 | Image export quality (10-100) |
| `documents_folder_name` | DocForge | Output subfolder in Documents |
| `images_folder_name` | DocForge | Output subfolder in Pictures |
| `audio_folder_name` | DocForge | Output subfolder in Music |

**Output resolution:** Public storage → app-specific fallback → internal files. MediaStore notification on API 29+ for file manager visibility.

---

# §11 · NAVIGATION & SHARE INTENT ROUTING

`DocForgeNavHost.kt` (590 lines) — centralized Compose Navigation graph. Every feature is a `composable()` destination receiving its ViewModel factory from `AppDependencies`.

`ShareIntentRouter` handles incoming `ACTION_SEND`/`ACTION_SEND_MULTIPLE`:
- MIME detection → route to appropriate converter/tool
- Supports files shared from Gmail, WhatsApp, file managers, etc.

---

# §12 · BUILD & PERFORMANCE CONFIGURATION

**Gradle (libs.versions.toml):**
- AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06.00
- minSdk 26, compileSdk/targetSdk 34
- Room 2.6.1 with KSP, Hilt 2.51.1
- PdfBox-Android 2.0.27.0, OpenCV 4.9.0
- ML Kit Text Recognition 16.0.1, Translate 17.0.3
- CameraX 1.3.4, Compose Navigation 2.7.7

**R8/ProGuard:** Full shrinking enabled in release builds for minimal APK size.

**Compose Compiler Metrics:** Optional via `enableComposeCompilerMetrics` Gradle property for stability auditing.

**Baseline Profiles:** Module configured for startup optimization.

---

# §13 · KEY DESIGN PATTERNS

1. **Result type everywhere:** All mutable operations return `Result<T>` for explicit error handling
2. **Cooperative cancellation:** Every page loop calls `coroutineContext.ensureActive()`
3. **Resource cleanup in finally:** Bitmaps, Mats, PDDocuments, temp files
4. **Ratio-based positioning:** All PDF overlay tools use 0-1 ratios for resolution independence
5. **Import page pattern:** Every PDF tool copies page properties (rotation, mediaBox, cropBox, resources)
6. **Sanitized filenames:** Regex `[^a-zA-Z0-9_-]` → `_` universally applied
7. **Progress callbacks:** Long operations expose `(current, total)` lambdas for UI feedback
8. **ImmutableCollections:** Scanner uses `PersistentList` for Compose stability

---

# §14 · TECHNOLOGY STACK SUMMARY

| Layer | Technology | Rationale |
|---|---|---|
| UI | Jetpack Compose + Material3 | Declarative, performant, modern |
| Navigation | Compose Navigation | Type-safe, single-activity |
| DI | Hilt (app) + Manual (features) | Minimal overhead, fast construction |
| Database | Room (SQLite) | Offline-first, type-safe queries |
| PDF Engine | PdfBox-Android | Full PDF manipulation, no server |
| PDF Rendering | Android PdfRenderer | Native, zero-dependency page rendering |
| Computer Vision | OpenCV 4.9 | Edge detection, perspective correction |
| OCR | ML Kit Text Recognition | On-device, offline after model download |
| Translation | ML Kit Translate | On-device, offline after model download |
| Camera | CameraX | Lifecycle-aware, modern camera API |
| Media | MediaExtractor/MediaMuxer | Native audio extraction, zero re-encoding |
| Concurrency | Kotlin Coroutines | Structured concurrency, cancellation |
| Image Loading | BitmapFactory/ImageDecoder | Native Android, EXIF-aware |
