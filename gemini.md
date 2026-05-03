# AnyDoc (DocForge) — Deep Implementation Reference
## Gemini Context Document · v1.0 · May 2026

---

# §1 · PROJECT IDENTITY

**Package:** `com.docforge.app`
**App Name:** AnyDoc (internal codename: DocForge)
**Architecture:** Multi-module Android (Kotlin) · Single-Activity Compose · Offline-First
**Target:** Replace every document subscription globally with a free, private, 0ms-latency super-app

---

# §2 · STRATEGIC PERFORMANCE MANDATES

### The 5 Unbreakable Rules

1. **Zero perceived latency** — Native library init (`PdfBox`, `OpenCV`) fires asynchronously in `Application.onCreate()` via `EngineWarmup`. By navigation time, engines are hot. `CompletableDeferred<Unit>` gates consumers.

2. **4GB RAM survival** — Every bitmap path enforces `maxLongEdge = 1800–2200px`. PDF loading uses `MemoryUsageSetting.setupTempFileOnly()` to spill to disk. Compressor uses `RGB_565` (2 bytes/pixel vs 4). Explicit `bitmap.recycle()` in every `finally` block.

3. **Zero heating** — Sequential batch execution (never parallel heavy tasks). `SupervisorJob` scoping prevents runaway coroutines. All IO on `Dispatchers.IO`, never main thread.

4. **100% offline core** — No REST APIs. PdfBox, OpenCV, Android PdfRenderer are local. ML Kit models download once then operate offline. Translation models cached after first download.

5. **Minimal APK** — R8 full mode with aggressive tree-shaking. No bundled ML models. Feature modules only import what they use from core.

---

# §3 · MODULE ARCHITECTURE

## 3.1 — Dependency Flow

```
app ──────→ feature/scanner
        ├─→ feature/converter
        ├─→ feature/pdf-tools
        ├─→ feature/history
        └─→ core/domain
             core/pdf       (19 tool classes)
             core/opencv    (DocumentEdgeDetector)
             core/storage   (Room DB)
             core/ui        (shared Compose utilities)
```

**Rule:** Features never depend on each other. App module is the only integration point. Core modules are pure engines with no UI knowledge.

## 3.2 — Dependency Injection

**Hybrid approach for maximum performance:**

| Layer | Strategy | Why |
|---|---|---|
| App module | Hilt `@HiltAndroidApp` + `@Module` | Android framework integration |
| Feature modules | Manual via `AppDependencies` | Zero annotation processing, instant construction |
| ViewModels | Custom `ViewModelProvider.Factory` | No Hilt ViewModel injection overhead |
| Core tools | Plain Kotlin classes with `Context` param | Zero reflection, zero proxy generation |

`AppDependencies.kt` constructs every tool engine eagerly — all are lightweight (just store Context reference). Actual heavy work happens lazily on first use.

---

# §4 · ENGINE WARMUP SYSTEM

```kotlin
object EngineWarmup {
    private val pdfReady = AtomicBoolean(false)
    private val opencvReady = AtomicBoolean(false)
    val pdfDeferred = CompletableDeferred<Unit>()

    fun warmup(context: Context) {
        // PdfBox: loads font cache, resource tables (~200ms cold)
        CoroutineScope(Dispatchers.IO).launch {
            if (pdfReady.compareAndSet(false, true)) {
                PdfBoxInit.ensure(context)
                pdfDeferred.complete(Unit)
            }
        }
        // OpenCV: loads native .so (~150ms cold)
        CoroutineScope(Dispatchers.IO).launch {
            if (opencvReady.compareAndSet(false, true)) {
                OpenCVLoader.initLocal()
            }
        }
    }
}
```

**PdfBoxInit** uses its own `AtomicBoolean` guard with `synchronized` block inside — double protection against concurrent init from different call sites.

---

# §5 · CORE PDF ENGINE — COMPLETE API

## 5.1 — IO Utilities (`PdfIoUtils.kt`)

**URI → Cache File Pattern:**
```kotlin
inline fun <T> Context.withUriCopiedToCacheFile(
    uri: Uri, prefix: String, suffix: String, block: (File) -> T
): T {
    val tempFile = copyUriToCacheFile(uri, prefix, suffix)
    try { return block(tempFile) }
    finally { tempFile.delete() }
}
```
- `FileChannel.transferFrom()` with 8MB chunk for efficient large file copy
- Temp file auto-deleted in `finally` — zero disk accumulation
- Every tool uses this pattern — never operates directly on content URIs

**PDF Loading:**
```kotlin
fun loadPdfDocument(file: File, password: String? = null): PDDocument {
    return if (password != null) {
        PDDocument.load(file, password, pdfMemoryUsageSetting())
    } else {
        PDDocument.load(file, pdfMemoryUsageSetting())
    }
}
```
`pdfMemoryUsageSetting()` returns `MemoryUsageSetting.setupTempFileOnly()` — PdfBox uses temp files instead of heap for large objects.

## 5.2 — Bitmap Pipeline (`BitmapDecodeUtils.kt`)

Two code paths for maximum device coverage:

**API 28+ (ImageDecoder):**
```kotlin
ImageDecoder.createSource(context.contentResolver, uri)
ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE  // Force software bitmap
    if (info.size.width > maxLongEdge || info.size.height > maxLongEdge) {
        decoder.setTargetSampleSize(computeSampleSize(info.size, maxLongEdge))
    }
}
```

**API <28 (BitmapFactory):**
1. Decode bounds only (`inJustDecodeBounds = true`)
2. Compute `inSampleSize` as power-of-2 down-sampling
3. Full decode with computed sample size
4. Read EXIF orientation → apply rotation via `Matrix`

Both paths output a bitmap guaranteed to be ≤ `maxLongEdge` pixels on longest dimension.

## 5.3 — Complete Tool Inventory

### PdfCreator
- Input: `List<Uri>` (images) + `PdfCreationOptions`
- Uses Android `PdfDocument` API (not PdfBox) for creation
- Page sizes: A4 (595×842), Letter (612×792), Legal (612×1008), Auto (image dimensions)
- Images fit-centered with aspect ratio preservation

### PdfMerger (237 lines)
- Mixed input: PDFs + images auto-detected by MIME type or extension
- Image extensions supported: jpg, jpeg, png, webp, heic, heif, bmp, tif, tiff
- Auto-generates PDF outline (bookmarks) from source file names
- Metadata injection: title, author, subject
- Page size modes for image pages: KEEP_SOURCE, A4_FIT, LETTER_FIT

### PdfSplitter (423 lines) — 7 Operations
| Method | Description |
|---|---|
| `splitByRange` | Extract contiguous page range |
| `extractPages` | Individual pages → separate PDFs |
| `splitEveryNPages` | Chunk into N-page segments |
| `splitByBookmarks` | Split at outline boundaries |
| `reorderPages` | Arbitrary page reordering |
| `deletePages` | Remove specific pages |
| `applyWorkspaceEdits` | Combined reorder + per-page rotation |

All methods preserve page properties (rotation, mediaBox, cropBox, resources) via `importPage()`.

### PdfCompressor (160 lines)
**Rasterization-based compression pipeline:**
1. Open source with `PdfRenderer` (Android native)
2. Render each page to `RGB_565` bitmap at target DPI
3. Paint white background (PDF default)
4. Embed as JPEG via `JPEGFactory.createFromImage(doc, bitmap, quality)`
5. Write to new PdfBox document

Trade-off: Produces smaller files but text becomes non-selectable. Ideal for scanned documents.

### PdfSigner (174 lines)
- `sign()` — single placement convenience method
- `signMultiple()` — multiple placements per document
- Position is ratio-based: `xRatio`, `yRatio`, `widthRatio` (0.0–1.0)
- Width clamped to 10%–80% of page, height computed from aspect ratio
- Signature embedded as `LosslessFactory` PNG in APPEND mode content stream

### PdfAnnotator (387 lines)
Four annotation types rendered directly to content streams:

**Highlight:** Semi-transparent fill rectangle. Color blended 55% toward white for readability.

**Text Box:** Background fill (15% white blend) + 1px border + word-wrapped text. Font: Helvetica, size auto-scaled from box height (9–14pt), max 4 lines.

**Sticky Note:** 24–72px yellow square + optional adjacent text (2 lines max).

**Freehand:** Continuous stroke path with break points. Line width scales with page size (0.25% of width, min 1.5pt).

### PdfRedactionTool (319 lines)
**The most security-critical tool — true content removal, not overlay:**

1. **Parse:** `PDFStreamParser` tokenizes each page's content stream
2. **Match:** Text-showing operators (`Tj`, `TJ`, `'`, `"`) checked against term list
3. **Remove:** Matching operator + operands stripped from token list
4. **Rewrite:** Clean tokens written via `ContentStreamWriter` with FLATE_DECODE
5. **Forms:** `PDAcroForm` fields with matching values cleared + `refreshAppearances()`
6. **Metadata:** `PDDocumentInformation` replaced with empty instance, XMP metadata nulled
7. **Verify:** Reload output, extract all text with `PDFTextStripper`, check for any remaining term matches. **Deletes output file on verification failure.**

### PdfOcrTool (430 lines)
- **PDF path:** Render via `PdfRenderer` at 1800px → ML Kit `TextRecognition` → structured text + bounding boxes
- **Image path:** Direct bitmap decode → ML Kit
- **Searchable PDF:** Overlays invisible text (`RenderingMode.NEITHER`, Helvetica) at position-mapped coordinates. Font size derived from bounding box height (8–18pt for PDF pages, 8–24pt for images).
- **Text sanitization:** Null char removal + whitespace normalization

### PdfTranslationTool (379 lines)
Full pipeline: OCR → Translate → Layout-Preserving Overlay

1. Render pages → ML Kit text detection → bounding boxes
2. Download ML Kit translation model if needed (`DownloadConditions.Builder().build()`)
3. Batch translate (24 lines per batch, configurable)
4. For each translated line:
   - Draw white rectangle over original text position
   - Draw translated text with word-wrapping (max 3 lines)
   - Font size: bounding box height clamped 8–18pt

### PdfFormTool (297 lines)
- **Field type mapping:** PDTextField→TEXT, PDCheckBox→CHECKBOX, PDRadioButton→RADIO, PDChoice→CHOICE
- **Smart fill:** Checkbox accepts true/yes/1; Radio accepts index or export value name
- **Field creation:** `PDAnnotationWidget` + `PDTextField` with ratio-based positioning
- **AcroForm bootstrap:** Creates form with Helvetica default resource if none exists
- **Flatten:** Bakes field values into content stream for non-editable output

### PdfBatchStampTool (215 lines)
- **Watermark:** Diagonal text rendered via `Matrix.getRotateInstance(angle, centerX, centerY)`. Font size = 6.5% of page diagonal (clamped 28–72pt). Gray level configurable (90–230).
- **Bates numbering:** Sequential counter across multiple files. Format: `{prefix}{zero-padded number}`. Rendered bottom-right in Helvetica.

### PdfPasswordTool (103 lines)
- **Protect:** `StandardProtectionPolicy` with 128-bit key length, separate user/owner passwords
- **Unlock:** Load with password → `setAllSecurityToBeRemoved(true)` → save
- **Detect:** `isEncrypted` check with fallback (returns true on load failure — safe default)

### PdfTextExtractor (76 lines)
`PDFTextStripper` with `sortByPosition = true` for logical text order. Falls back to `[No extractable text found]` placeholder.

### PdfPageImageExporter (142 lines)
- Renders via `PdfRenderer` with configurable scale factor
- Format: JPG/PNG/WEBP (API 30+: WEBP_LOSSY, <30: WEBP legacy)
- Optional ZIP bundling with 8KB buffer `copyTo`

### PdfIdCardTool (122 lines)
Uses Android `PdfDocument` API (not PdfBox) for lightweight ID card sheet generation. Two-image layout with margin/gap calculations and "Front"/"Back" labels.

### ScanImageExporter (102 lines)
Exports scan page URIs to JPG/PNG files with optional ZIP bundling. Uses `decodeBitmapConstrained` at 2000px cap.

---

# §6 · BATCH QUEUE — DETAILED IMPLEMENTATION

## 6.1 — Task Model

```kotlin
data class BatchQueueTask(
    val id: Long,
    val type: BatchQueueTaskType,
    val inputUris: PersistentList<StableUriRef>,
    val outputName: String,
    val status: BatchQueueTaskStatus,
    val progress: Float = 0f,
    val resultMessage: String? = null
)

enum class BatchQueueTaskType {
    IMAGES_TO_PDF, IMAGES_TO_JPG, PDF_MERGE,
    PDF_COMPRESS, DOCUMENT_TO_PDF, VIDEO_TO_AUDIO
}

enum class BatchQueueTaskStatus {
    QUEUED, RUNNING, SUCCESS, FAILED, CANCELED
}
```

## 6.2 — RuntimeStore (364 lines, singleton)

**Thread safety:** `AtomicLong` for ID generation, `Mutex` for `beginProcessing()`, `StateFlow` for reactive UI updates.

**Key operations:**
- `addTask()` — appends to queue, assigns unique ID
- `moveUp()/moveDown()` — reorder queued tasks
- `removeTask()` — remove if not running
- `replaceFromPreset()` — clear + load saved preset
- `beginProcessing()` — mutex-guarded, marks first QUEUED as RUNNING
- `advanceToNextOrFinish()` — marks next QUEUED as RUNNING or returns null

## 6.3 — ForegroundService (357 lines)

**Lifecycle:**
1. `onStartCommand()` → `startForeground()` with notification
2. Launch `supervisorScope` coroutine on `Dispatchers.IO`
3. Loop: `beginProcessing()` → execute task → record history → `advanceToNextOrFinish()`
4. Each task wrapped in `try/catch` — `CancellationException` re-thrown, others mark FAILED
5. `stopSelf()` when queue empty

**Android compliance:** `startForeground()` called within 5 seconds of `startForegroundService()`. Notification channel created with proper importance level.

---

# §7 · DOCUMENT SCANNER IMPLEMENTATION

## 7.1 — OpenCV Edge Detection Pipeline

```
Input frame → Grayscale → GaussianBlur(5×5) → Canny(75,200)
            → findContours(RETR_LIST) → approxPolyDP(2% perimeter)
            → filter(4 points, convex, area ≥ 15% frame)
            → order corners (TL→TR→BR→BL clockwise)
            → output DetectedQuad(corners, confidence)
```

**Fallback:** If no valid quadrilateral found, returns inset rectangle (8%/12% margins) with confidence 0.25.

## 7.2 — Perspective Correction

```kotlin
val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
Imgproc.warpPerspective(inputMat, outputMat, transform, outputSize)
```

Output dimensions capped at 2000px longest edge. Native `Mat` objects explicitly released in `finally` blocks.

## 7.3 — Scanner ViewModel State

Uses `PersistentList<StableUriRef>` (kotlinx.collections.immutable) for Compose stability. `StableUriRef` wraps `Uri` with stable identity for efficient recomposition.

---

# §8 · RESUME BUILDER SYSTEM

**10 templates** defined as pure `ResumeTemplate` data classes with 18 properties each. Categories: Engineer, Designer, Manager, Academic, Sales, General. Styles: Modern, Classic, Minimal, Creative, Executive.

**Layout engine (`ResumeRenderer.kt`, 326 lines):**
- 4 layout composables: SingleColumn, TwoColumn, SidebarLeft, SidebarRight
- Header styles: TopLeft, TopCenter, Banner (colored background), SidebarHeader
- Skills rendered as `FlowRow` chips with accent-colored borders
- Experience uses `AnnotatedString` for inline formatting

**Data model (`ResumeData`):** fullName, jobTitle, email, phone, location, summary, experience[], education[], skills[], certifications[], languages[], links[].

**Export:** Compose → Bitmap capture → PdfCreator → PDF file.

---

# §9 · DATABASE & HISTORY

**Room Database v3** with 2 entities:

| Entity | Columns |
|---|---|
| `ConversionHistoryEntity` | id, sourceLabel, outputPath, operation, createdAtMillis, inputCount, outputSizeBytes, outputUri, displayName |
| `BatchPresetEntity` | id, name, createdAtMillis, tasksJson |

**Migrations:** 1→2 added batch_presets table. 2→3 added outputUri + displayName to history.

**Repository pattern:** `HistoryRepository` interface → `LocalHistoryRepository` implementation with `Flow<List<ConversionRecord>>` for reactive UI.

---

# §10 · BUILD CONFIGURATION

```toml
# Key versions from libs.versions.toml
agp = "8.5.2"
kotlin = "1.9.24"
composeBom = "2024.06.00"
room = "2.6.1"
hilt = "2.51.1"
pdfboxAndroid = "2.0.27.0"
opencv = "4.9.0"
mlkitTextRecognition = "16.0.1"
mlkitTranslate = "17.0.3"
camerax = "1.3.4"

# SDK targets
minSdk = 26
compileSdk = 34
targetSdk = 34
```

**Build optimizations:**
- R8 full mode in release
- Compose compiler metrics via gradle property
- Baseline profiles module configured
- KSP for Room + Hilt annotation processing
