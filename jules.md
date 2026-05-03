# AnyDoc (DocForge) — Engineering Execution Guide
## Jules Context Document · v1.0 · May 2026

---

# §1 · WHAT IS ANYDOC

AnyDoc is an offline-first Android document super-app designed to be the final, definitive replacement for every document-related subscription service globally. Built with Kotlin, Jetpack Compose, and a modular architecture, it provides 25+ document tools running at 0ms perceived latency on devices with as little as 4GB RAM.

**Package:** `com.docforge.app`
**Architecture:** Multi-module · Single-Activity · MVI-like state management
**Engine:** PdfBox-Android + OpenCV + ML Kit (on-device) + Android native APIs

---

# §2 · CRITICAL RULES — NEVER VIOLATE

1. **All PDF operations must use `withUriCopiedToCacheFile()`** — never operate directly on content URIs. Temp files auto-deleted in `finally`.

2. **All bitmap decoding must go through `decodeBitmapConstrained()`** with `maxLongEdge ≤ 2200`. Never decode full-resolution images.

3. **All page loops must call `coroutineContext.ensureActive()`** — cooperative cancellation is mandatory.

4. **All bitmaps must be recycled in `finally` blocks** — no exceptions.

5. **All PDF documents must be loaded with `pdfMemoryUsageSetting()` (temp-file only)** — never heap-based.

6. **All output filenames must be sanitized** via `replace(Regex("[^a-zA-Z0-9_-]"), "_")`.

7. **Never add network calls to core processing** — ML Kit models are the only exception (download once, then offline).

8. **All new tools must follow the existing constructor pattern** — `class ToolName(private val context: Context)` and be registered in `AppDependencies`.

9. **Never run heavy processing on `Dispatchers.Main`** — all tool methods must be `suspend fun` with `withContext(Dispatchers.IO)`.

10. **The batch queue processes tasks sequentially** — never parallelize to avoid thermal throttling.

---

# §3 · MODULE MAP

```
anyDoc/
├── app/                    ← Application shell, DI, navigation, batch
│   ├── DocForgeApp.kt      ← Cache cleanup, engine warmup trigger
│   ├── MainActivity.kt     ← Single-Activity Compose host
│   ├── AppDependencies.kt  ← Manual DI graph for all tool engines
│   ├── AppModule.kt        ← Hilt @Module for framework bindings
│   ├── navigation/DocForgeNavHost.kt  ← 590-line nav graph
│   ├── runtime/EngineWarmup.kt        ← Async PdfBox + OpenCV init
│   ├── share/ShareIntentRouter.kt     ← External intent handling
│   └── batch/              ← 7 files: service, runtime store, preset store, VM, UI
├── core/
│   ├── domain/             ← Models (ConversionRecord), interfaces (HistoryRepository), settings
│   ├── opencv/             ← DocumentEdgeDetector (347 lines)
│   ├── pdf/                ← 19 tool classes (THE ENGINE ROOM)
│   ├── storage/            ← Room DB v3 (2 entities, 3 migrations)
│   └── ui/                 ← Shared Compose utils, StableUriRef, ImmutableCollections
├── feature/
│   ├── converter/          ← ImageFormatConverter, DocumentPdfConverter, VideoAudioExtractor
│   ├── history/            ← Conversion history UI + management
│   ├── scanner/            ← CameraX scanner + OpenCV edge detection + filters
│   └── pdf-tools/          ← 15+ tool screens + ViewModels + resume builder
```

---

# §4 · HOW TO ADD A NEW PDF TOOL

### Step 1: Create the engine class in `core/pdf/`

```kotlin
class PdfNewTool(private val context: Context) {

    suspend fun process(
        inputUri: Uri,
        outputName: String,
        /* tool-specific params */
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
        val sanitized = outputName.ifBlank { "newtool_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_newtool_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { sourceDoc ->
                require(sourceDoc.numberOfPages > 0) { "Input PDF has no pages." }

                PDDocument().use { outDoc ->
                    repeat(sourceDoc.numberOfPages) { pageIndex ->
                        checkCancelled()
                        val sourcePage = sourceDoc.getPage(pageIndex)
                        val imported = importPage(outDoc, sourcePage)
                        // ... your tool logic here ...
                    }
                    outDoc.save(outputFile)
                }

                PdfCreationResult(
                    outputFile = outputFile,
                    pageCount = sourceDoc.numberOfPages,
                    outputSizeBytes = outputFile.length()
                )
            }
        }
    }

    private fun importPage(outDoc: PDDocument, sourcePage: PDPage): PDPage {
        val imported = outDoc.importPage(sourcePage)
        imported.rotation = sourcePage.rotation
        imported.mediaBox = sourcePage.mediaBox
        imported.cropBox = sourcePage.cropBox
        imported.resources = sourcePage.resources
        return imported
    }
}
```

### Step 2: Register in `AppDependencies.kt`
```kotlin
val pdfNewTool = PdfNewTool(context)
```

### Step 3: Create ViewModel + Factory in `feature/pdf-tools/`
```kotlin
class NewToolViewModel(private val pdfNewTool: PdfNewTool, ...) : ViewModel() { ... }

class NewToolViewModelFactory(private val pdfNewTool: PdfNewTool, ...) : ViewModelProvider.Factory { ... }
```

### Step 4: Add navigation in `DocForgeNavHost.kt`
```kotlin
composable("new_tool") {
    val vm: NewToolViewModel = viewModel(factory = NewToolViewModelFactory(deps.pdfNewTool, ...))
    NewToolRoute(viewModel = vm)
}
```

---

# §5 · COMPLETE TOOL INVENTORY WITH ENTRY POINTS

| Tool | Class | Key Method | Lines |
|---|---|---|---|
| Image → PDF | `PdfCreator` | `createPdfFromImages()` | 121 |
| PDF Merge | `PdfMerger` | `merge()` | 237 |
| PDF Split (7 modes) | `PdfSplitter` | `splitByRange/extractPages/splitEveryNPages/splitByBookmarks/reorderPages/deletePages/applyWorkspaceEdits` | 423 |
| PDF Compress | `PdfCompressor` | `compress()` | 160 |
| PDF Sign | `PdfSigner` | `sign()/signMultiple()` | 174 |
| PDF Annotate | `PdfAnnotator` | `annotate()` | 387 |
| PDF Redact | `PdfRedactionTool` | `redact()` | 319 |
| PDF OCR | `PdfOcrTool` | `process()` | 430 |
| PDF Translate | `PdfTranslationTool` | `translatePdfWithLayout()` | 379 |
| PDF Form Fill | `PdfFormTool` | `listFields()/fillFields()/addTextField()` | 297 |
| PDF Watermark+Bates | `PdfBatchStampTool` | `stampBatch()` | 215 |
| PDF Password | `PdfPasswordTool` | `protect()/removePassword()` | 103 |
| PDF → Text | `PdfTextExtractor` | `extractToTxt()` | 76 |
| PDF → Images | `PdfPageImageExporter` | `exportPages()` | 142 |
| ID Card Sheet | `PdfIdCardTool` | `createFrontBackSheet()` | 122 |
| Scan → Image | `ScanImageExporter` | `export()` | 102 |
| Image Convert | `ImageFormatConverter` | `convertBatch()` | 107 |
| Doc → PDF | `DocumentPdfConverter` | `convertToPdf()` | 434 |
| Video → Audio | `VideoAudioExtractor` | `extractAudio()` | 227 |
| Edge Detection | `DocumentEdgeDetector` | `detectEdges()/correctPerspective()` | 347 |

---

# §6 · MEMORY MANAGEMENT PATTERNS

### Pattern 1: Constrained Bitmap Decode
```kotlin
// ALWAYS use this — never BitmapFactory.decodeStream() directly
val bitmap = decodeBitmapConstrained(context, uri, maxLongEdge = 2200)
    ?: error("Failed to decode image")
try {
    // use bitmap
} finally {
    bitmap.recycle()
}
```

### Pattern 2: PDF Temp-File Loading
```kotlin
context.withUriCopiedToCacheFile(uri, prefix = "docforge_xyz_", suffix = ".pdf") { sourceFile ->
    loadPdfDocument(sourceFile).use { doc ->
        // doc auto-closed, sourceFile auto-deleted
    }
}
```

### Pattern 3: OpenCV Mat Cleanup
```kotlin
val mat = Mat()
try {
    Imgproc.cvtColor(inputMat, mat, Imgproc.COLOR_RGBA2GRAY)
    // use mat
} finally {
    mat.release()
}
```

### Pattern 4: Compressor RGB_565 (50% memory savings)
```kotlin
val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)  // Not ARGB_8888
try {
    canvas.drawColor(Color.WHITE)  // Must paint white — RGB_565 has no alpha
    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
} finally {
    bitmap.recycle()
}
```

---

# §7 · BATCH QUEUE IMPLEMENTATION

### Architecture
```
UI ←→ BatchQueueViewModel ←→ BatchQueueRuntimeStore (singleton StateFlow)
                                        ↕
                           BatchQueueForegroundService (SupervisorJob + Dispatchers.IO)
                                        ↕
                              Tool engines (PdfCreator, PdfMerger, etc.)
                                        ↕
                              HistoryRepository (Room insert on success)
```

### Task Types
| Type | Engine Used |
|---|---|
| `IMAGES_TO_PDF` | `PdfCreator` |
| `IMAGES_TO_JPG` | `ImageFormatConverter` |
| `PDF_MERGE` | `PdfMerger` |
| `PDF_COMPRESS` | `PdfCompressor` |
| `DOCUMENT_TO_PDF` | `DocumentPdfConverter` |
| `VIDEO_TO_AUDIO` | `VideoAudioExtractor` |

### Thread Safety
- **Task ID:** `AtomicLong` — guaranteed unique across all threads
- **Queue state:** `MutableStateFlow` — atomic updates via `update {}` lambda
- **Processing gate:** `Mutex` in `beginProcessing()` — prevents double-start race
- **Service scope:** `SupervisorJob` — child failure doesn't cancel siblings
- **Cancellation:** `CancellationException` re-thrown after marking task as CANCELED

### Preset System
Presets stored in Room (`BatchPresetEntity`) with `tasksJson` column containing Gson-serialized task list. `BatchPresetDao` provides `getAll()`, `insert()`, `deleteById()`.

---

# §8 · DOCUMENT SCANNER PIPELINE

### Edge Detection (OpenCV)
```
Bitmap → Mat (RGBA) → Grayscale → GaussianBlur(5×5, sigma=0)
       → Canny(threshold1=75, threshold2=200)
       → findContours(RETR_LIST, CHAIN_APPROX_SIMPLE)
       → approxPolyDP(epsilon = 2% of perimeter)
       → Filter: 4 points + convex + area ≥ 15% of frame
       → Order corners: TL → TR → BR → BL (clockwise)
       → Output: DetectedQuad(corners, confidence 0.0–1.0)
```

### Perspective Correction
```
getPerspectiveTransform(srcPoints, dstPoints) → 3×3 transform matrix
warpPerspective(input, output, matrix, outputSize) → corrected image
Output capped at 2000px longest edge
```

### Image Filters (all in ScannerViewModel)
| Filter | OpenCV Operation |
|---|---|
| Original | No processing |
| Grayscale | `cvtColor(COLOR_RGBA2GRAY)` → `cvtColor(COLOR_GRAY2RGBA)` |
| B&W | Otsu thresholding or fixed threshold |
| Enhanced | Unsharp mask: `addWeighted(gray, 1.55, blurred, -0.55, 0)` |

### Export Formats
- **PDF:** `PdfCreator.createPdfFromImages()` with page size option
- **JPG/PNG:** `ScanImageExporter.export()` with optional ZIP bundling

---

# §9 · CONVERTER ENGINES

### DocumentPdfConverter (434 lines)
**Input detection chain:** Extension → MIME type → content sniffing (RTF header `{\rtf`, DOCX ZIP magic `PK` + `word/document.xml` entry, CSV comma density heuristic)

**DOCX parser:** `ZipInputStream` → `XmlPullParser` on `word/document.xml`. Handles:
- Paragraphs (`<w:p>`) with bold/italic run properties
- Tables (`<w:tbl>` → `<w:tr>` → `<w:tc>`) rendered as pipe-delimited text
- List items (`<w:numPr>`) prefixed with `• `
- Embedded image detection (counts files in `word/media/`)

**RTF parser:** Regex-based control word stripping. Handles `\par`, `\tab`, hex escapes (`\'XX`).

**CSV parser:** RFC-4180 compliant with quoted field and escaped quote (`""`) support.

**PDF rendering:** Android `PdfDocument` API with `Paint.breakText()` for word wrapping. Page size: A4 (595×842).

### VideoAudioExtractor (227 lines)
- **M4A:** `MediaExtractor` → `MediaMuxer(MPEG_4)` — zero re-encoding
- **MP3:** Direct byte stream copy — only works if source audio is already MPEG
- Buffer size: `KEY_MAX_INPUT_SIZE` or 256KB fallback
- Returns `AudioTrackInfo` with MIME, duration, sample rate, channel count

---

# §10 · RESUME BUILDER

### Template Data Model
```kotlin
data class ResumeTemplate(
    val id: String, val name: String,
    val category: TemplateCategory,      // ENGINEER, DESIGNER, MANAGER, ACADEMIC, SALES, GENERAL
    val style: TemplateStyle,            // MODERN, CLASSIC, MINIMAL, CREATIVE, EXECUTIVE
    val layout: TemplateLayout,          // SINGLE_COLUMN, TWO_COLUMN, SIDEBAR_LEFT, SIDEBAR_RIGHT
    val primaryColor: Color, val accentColor: Color, val backgroundColor: Color,
    val textColor: Color, val subtitleColor: Color,
    val fontFamily: FontFamily,
    val nameFontSize: TextUnit, val sectionTitleFontSize: TextUnit, val bodyFontSize: TextUnit,
    val sectionSpacing: Dp, val itemSpacing: Dp, val pageMargin: Dp,
    val showDividers: Boolean, val showSectionIcons: Boolean,
    val headerStyle: HeaderStyle         // TOP_LEFT, TOP_CENTER, BANNER, SIDEBAR_HEADER
)
```

### 10 Bundled Templates
| # | Name | Category | Layout | Style |
|---|---|---|---|---|
| 1 | Modern Engineer | Engineer | Single Column | Modern |
| 2 | Classic Executive | Manager | Single Column | Executive |
| 3 | Minimal Designer | Designer | Single Column | Minimal |
| 4 | Two-Column Manager | Manager | Sidebar Left | Modern |
| 5 | Academic Researcher | Academic | Single Column | Classic |
| 6 | Bold Sales | Sales | Single Column | Creative |
| 7 | Clean General | General | Single Column | Modern |
| 8 | Creative Sidebar | Designer | Sidebar Right | Creative |
| 9 | Compact Tech | Engineer | Two Column | Minimal |
| 10 | Elegant Professional | General | Single Column | Executive |

---

# §11 · DATABASE SCHEMA

```sql
-- Table 1: conversion_history (v1, extended in v2→v3)
CREATE TABLE conversion_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    sourceLabel TEXT NOT NULL,
    outputPath TEXT NOT NULL,
    operation TEXT NOT NULL,
    createdAtMillis INTEGER NOT NULL,
    inputCount INTEGER NOT NULL DEFAULT 0,
    outputSizeBytes INTEGER NOT NULL DEFAULT 0,
    outputUri TEXT,         -- Added in migration 2→3
    displayName TEXT        -- Added in migration 2→3
);

-- Table 2: batch_presets (added in migration 1→2)
CREATE TABLE batch_presets (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    createdAtMillis INTEGER NOT NULL,
    tasksJson TEXT NOT NULL
);
```

---

# §12 · SETTINGS SYSTEM

`DocForgeSettingsStore` — SharedPreferences-backed singleton:

| Setting | Type | Default | Used By |
|---|---|---|---|
| `onboarding_completed` | Boolean | false | App startup gate |
| `default_pdf_page_size` | Enum | A4 | PdfCreator, Scanner |
| `default_pdf_compression` | Enum | MEDIUM | PdfCompressor |
| `default_image_quality` | Int | 90 | ImageFormatConverter |
| `documents_folder_name` | String | "DocForge" | All PDF tools |
| `images_folder_name` | String | "DocForge" | Image converter |
| `audio_folder_name` | String | "DocForge" | VideoAudioExtractor |

**Output directory resolution:** Public storage (`Environment.getExternalStoragePublicDirectory`) → app-specific external → internal files directory. MediaStore notification for file manager visibility.

---

# §13 · NAVIGATION STRUCTURE

Central `DocForgeNavHost.kt` (590 lines) defines all routes:

| Route | Feature | ViewModel Factory |
|---|---|---|
| `home` | Dashboard with tool grid | — |
| `scanner` | Document scanner | `ScannerViewModelFactory` |
| `converter` | File converter | `ConverterViewModelFactory` |
| `pdf_merge` | PDF merger | `PdfMergeViewModelFactory` |
| `pdf_split` | PDF splitter (7 modes) | `PdfSplitViewModelFactory` |
| `pdf_compress` | PDF compressor | `PdfCompressViewModelFactory` |
| `pdf_sign` | PDF signer | `PdfSignViewModelFactory` |
| `pdf_annotate` | PDF annotator | `PdfAnnotateViewModelFactory` |
| `pdf_redact` | PDF redaction | `PdfRedactViewModelFactory` |
| `pdf_ocr` | OCR tool | `PdfOcrViewModelFactory` |
| `pdf_translate` | Translation | `PdfTranslateViewModelFactory` |
| `pdf_form` | Form tool | `PdfFormViewModelFactory` |
| `pdf_stamp` | Watermark + Bates | `PdfStampViewModelFactory` |
| `pdf_password` | Encrypt/decrypt | `PdfPasswordViewModelFactory` |
| `pdf_text` | Text extraction | `PdfTextViewModelFactory` |
| `pdf_images` | Page → image export | `PdfImagesViewModelFactory` |
| `pdf_id_card` | ID card sheet | `PdfIdCardViewModelFactory` |
| `resume_builder` | Resume builder | `ResumeViewModelFactory` |
| `batch_queue` | Batch queue | `BatchQueueViewModelFactory` |
| `history` | Conversion history | `HistoryViewModelFactory` |
| `settings` | App settings | — |

---

# §14 · KEY DEPENDENCIES

```toml
# From gradle/libs.versions.toml
pdfbox-android = "2.0.27.0"      # PDF manipulation engine
opencv = "4.9.0"                  # Computer vision
mlkit-text = "16.0.1"             # On-device OCR
mlkit-translate = "17.0.3"        # On-device translation
camerax = "1.3.4"                 # Camera API
compose-bom = "2024.06.00"        # Compose framework
room = "2.6.1"                    # Local database
hilt = "2.51.1"                   # Dependency injection
kotlinx-collections = "0.3.7"    # Immutable collections for Compose
```

---

# §15 · SHARE INTENT HANDLING

`ShareIntentRouter` processes `ACTION_SEND` and `ACTION_SEND_MULTIPLE`:

1. Extract URIs from intent extras
2. Detect MIME type from content resolver
3. Route to appropriate tool:
   - `application/pdf` → PDF tools menu
   - `image/*` → Scanner or converter
   - `video/*` → Video audio extractor
   - `text/*`, `application/msword`, etc. → Document converter
4. Pass URIs to target ViewModel via navigation arguments

---

# §16 · TESTING & QUALITY CHECKLIST

When modifying or adding features, verify:

- [ ] All bitmaps recycled in `finally` blocks
- [ ] All temp files cleaned up via `withUriCopiedToCacheFile`
- [ ] `ensureActive()` called in every page/item loop
- [ ] Output filename sanitized
- [ ] Tool registered in `AppDependencies`
- [ ] Navigation route added in `DocForgeNavHost`
- [ ] History recording via `historyRepository.insert()` on success
- [ ] Error messages are user-friendly (no stack traces)
- [ ] Progress callbacks provided for operations > 1 second
- [ ] No network calls in core processing path
