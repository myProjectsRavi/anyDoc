# AnyDoc (DocForge) — Deep Implementation Reference
## Gemini Context Document · v2.0 · May 2026
## Synchronized with claude.md v2.0 — same bug tracking, same roadmap

---

# §1 · PROJECT IDENTITY

**Package:** `com.docforge.app`
**App Name:** AnyDoc (internal codename: DocForge)
**Architecture:** Multi-module Android (Kotlin) · Single-Activity Compose · Offline-First
**Target:** Replace every document subscription globally with a free, private, 0ms-latency super-app

---

# §2 · STRATEGIC PERFORMANCE MANDATES

### The 5 Unbreakable Rules

1. **Zero perceived latency** — `PdfBox` + `OpenCV` init fires asynchronously in `Application.onCreate()` via `EngineWarmup`. By navigation time engines are hot. `CompletableDeferred<Unit>` gates consumers. ✅

2. **4GB RAM survival** — Every bitmap path enforces `maxLongEdge = 1800–2200px`. PDF loading uses `MemoryUsageSetting.setupTempFileOnly()`. Compressor uses `RGB_565`. Explicit `bitmap.recycle()` in every `finally`. ✅

3. **Zero heating** — Sequential batch execution (never parallel heavy tasks). `SupervisorJob` scoping. All IO on `Dispatchers.IO`. ✅

4. **100% offline core** — No REST APIs. PdfBox, OpenCV, Android PdfRenderer are local. ML Kit models download once then operate offline. ✅
   - **Exception gap:** Translation model downloads without WiFi-only guard — 🟡 MED-1

5. **Minimal APK** — R8 full mode with aggressive tree-shaking. No bundled ML models. ✅

---

# §3 · BUGS & VULNERABILITIES — COMPLETE LIST

## Critical (Fix Before Any Release)

### CRITICAL-1 · Dual DI Architecture
**Files:** `app/AppDependencies.kt` + `app/AppModule.kt`
`AppDependencies` (manual) and `AppModule` (Hilt) both instantiate the same tool objects creating two parallel object graphs. `DocForgeNavHost` uses `AppDependencies` — making `AppModule` dead code at runtime. Any Hilt-injected component gets a DIFFERENT instance than NavHost-wired ViewModels.
**Fix:** Commit to full Hilt. Delete `AppDependencies`. Convert all ViewModels to `@HiltViewModel`. Use `hiltViewModel()` in NavHost.
**Status:** 🔴 NOT FIXED

### CRITICAL-2 · PDF Redaction Does Not Scrub Annotations/XMP
**File:** `core/pdf/PdfRedactionTool.kt`
Content-stream text removal only. Annotations (sticky notes, highlights, form fields), XMP metadata, embedded attachments, and hidden layers are untouched. `PDFTextStripper` verification also only reads content streams — false security confidence.
**Fix:** Delete matching annotations, null XMP catalog metadata, remove embedded files node, remove OCG/optional content. Extend verification to also scan annotation text.
**Status:** ✅ FIXED

### CRITICAL-3 · SidebarResume fillMaxHeight Inside verticalScroll
**File:** `feature/pdf-tools/resume/ResumeRenderer.kt`
`fillMaxHeight()` inside `verticalScroll` = unbounded height = `IllegalStateException` crash at runtime when any sidebar template is selected.
**Fix:** Replace `fillMaxHeight()` with `wrapContentHeight()` or `height(IntrinsicSize.Min)`.
**Status:** ✅ FIXED

## High

| ID | File | Issue | Fix | Status |
|---|---|---|---|---|
| HIGH-1 | `DocForgeApp.kt` | `onTrimMemory` deletes in-flight temp files | `TempFileRegistry` singleton to track active paths | 🟠 NOT FIXED |
| HIGH-2 | `DocForgeNavHost.kt` | `activeSharedLaunch` lost on rotation (in Composable, not ViewModel) | Move to `MainActivity` ViewModel | 🟠 NOT FIXED |
| HIGH-3 | `DocForgeDatabase.kt` | `fallbackToDestructiveMigration()` silently wipes all user data | Remove from production; explicit migrations only | ✅ FIXED |
| HIGH-4 | `DocumentPdfConverter.kt` | RTF parser corrupts non-ASCII (no `\uN` unicode handling) | Apache POI RTFEditorKit or at minimum `\uN` escape handling | ✅ FIXED |
| HIGH-5 | `DocumentEdgeDetector.kt` | `OpenCVLoader.initLocal()` in constructor = potential main thread block | Remove from constructor; use `EngineWarmup.awaitOpenCv()` | ✅ FIXED |
| HIGH-6 | Multiple tools | 7+ tools bypass `resolveNonConflictingFile()` — silently overwrite files | Audit all `File(outputDir,` patterns; enforce `resolveNonConflictingFile()` universally | ✅ FIXED |
| HIGH-7 | Multiple tools | `MediaScannerConnection.scanFile()` not called — output files invisible in file manager | Call from every public-storage output tool | ✅ FIXED |
| HIGH-8 | All | Zero unit tests for any business logic | JUnit 5 + Robolectric test suite | 🟠 NOT STARTED |

## Medium

| ID | File | Issue | Fix | Status |
|---|---|---|---|---|
| MED-1 | `PdfTranslationTool.kt` | Translation model downloads without WiFi constraint (~30–100MB) | `DownloadConditions.Builder().requireWifi().build()` + settings toggle | ✅ FIXED |
| MED-2 | `PdfPasswordTool.kt` | AES-128, 6-char min password, owner=user password | AES-256, 8-char min, random owner password | ✅ FIXED |
| MED-3 | `PdfPageImageExporter.kt` | Default scale 1f = 8 DPI (unreadable output) | Default 2.5f (~150 DPI) | ✅ FIXED |
| MED-4 | `PdfCompressor.kt` | RGB_565 strips alpha — transparent PDFs render black | ARGB_8888 for pages with potential transparency | 🟡 NOT FIXED |
| MED-5 | `PdfCreator.kt` | AUTO page size uses pixels as PDF points (4000×3000px → 139" wide page) | Convert via `width * 72f / bitmap.density` | ✅ FIXED |
| MED-6 | `BitmapDecodeUtils.kt` | EXIF TRANSVERSE/TRANSPOSE missing mirror flip | Add `matrix.postScale(-1f, 1f)` for flip variants | ✅ FIXED |
| MED-7 | `DocForgeNavHost.kt` + `MainActivity.kt` | SharedPreferences read synchronously in composables | Expose `StateFlow<Settings>` from `Application.onCreate()` | 🟡 NOT FIXED |
| MED-8 | `BatchQueueRuntimeStore.kt` + `ScannerUiState.kt` | `SimpleDateFormat` not thread-safe | Replace with `java.time.DateTimeFormatter` (API 26+) | ✅ FIXED |
| MED-9 | `SavedSignatureStore.kt` | `compress()` stream not closed in `finally` | Wrap in `FileOutputStream(file).use { }` | 🟡 VERIFIED OK |
| MED-10 | `SignaturePlacementTemplateStore.kt` | Non-atomic file write (process kill = corrupt JSON) | Write to `.tmp` then `renameTo()` | ✅ FIXED |
| MED-11 | All tools | No input file size validation (2GB file = OOM crash) | `validateInputSize()` in `PdfIoUtils` checking `ContentResolver.statSize` | 🟡 NOT FIXED |
| MED-12 | `DocForgeSettingsStore.kt` | `notifyMediaStore` uses incorrect MediaStore API | Replace with `MediaScannerConnection.scanFile()` | ✅ FIXED |
| MED-13 | `PdfOcrTool.kt`, `PdfTranslationTool.kt` | `PDType1Font.HELVETICA` Latin-only — non-Latin overlay text renders as boxes | Embed Noto Sans; load via `PDType0Font.load()` | 🟡 NOT FIXED |
| MED-14 | `PdfTranslationTool.kt` | Translation strips original visual layout (white rect over original) | Render original page as background image; overlay text on top | 🟡 NOT FIXED |

## Low

| ID | Issue | Fix | Status |
|---|---|---|---|
| LOW-1 | Kotlin 1.9.24, Compose BOM 2024.06.00, Nav 2.7.7 outdated | Upgrade to Kotlin 2.1.x, Compose BOM 2025.x, Nav 2.8+ | 🟢 NOT DONE |
| LOW-2 | `importPage()` duplicated in 5+ tool classes | Extract to `PdfIoUtils.importPageFull()` | 🟢 NOT DONE |
| LOW-3 | URI extension parsing bug in `ShareIntentRouter` | Use `uri.lastPathSegment?.substringAfterLast('.')` | 🟢 NOT DONE |
| LOW-4 | ZIP output accumulates individual files alongside ZIP | Delete individual files after successful ZIP | 🟢 NOT DONE |
| LOW-5 | `PdfTextExtractor` platform default charset | `extracted.toByteArray(Charsets.UTF_8)` | ✅ FIXED |
| LOW-6 | `PdfIdCardTool` "Front"/"Back" labels may clip on small pages | Clamp label Y position | 🟢 NOT DONE |
| LOW-7 | `AppModule` dead code until DI resolved | Remove after CRITICAL-1 | 🟢 BLOCKED |
| LOW-8 | Saved signatures/templates not backed up on reinstall | Add `backup_rules.xml` | 🟢 NOT DONE |

---

# §4 · MODULE ARCHITECTURE

## 4.1 — Dependency Flow

```
app ──────→ feature/scanner
        ├─→ feature/converter
        ├─→ feature/pdf-tools
        ├─→ feature/history
        └─→ core/domain
             core/pdf       (19 tool classes)
             core/opencv    (DocumentEdgeDetector)
             core/storage   (Room DB v3)
             core/ui        (shared Compose utilities)
```

**Rule:** Features never depend on each other. App module is the only integration point.

## 4.2 — Dependency Injection

**Current state (broken hybrid):**

| Layer | Current Strategy | Problem |
|---|---|---|
| App module | Hilt `@HiltAndroidApp` + `AppModule` | `AppModule` bindings never consumed at runtime |
| Feature modules | Manual via `AppDependencies` | Two object graphs in memory |
| ViewModels | Custom `ViewModelProvider.Factory` | 25+ factory boilerplate classes |

**Target state (full Hilt):**

| Layer | Target Strategy | Benefit |
|---|---|---|
| App module | Hilt `@HiltAndroidApp` + `AppModule` | Single source of truth |
| Feature modules | `@HiltViewModel` + `@Inject` | Zero annotation processing overhead |
| ViewModels | `hiltViewModel()` in NavHost | Eliminates all factory classes |
| Core tools | `@Singleton @Provides` in `AppModule` | Guaranteed single instance per process |

---

# §5 · ENGINE WARMUP SYSTEM

```kotlin
object EngineWarmup {
    private val pdfReady = AtomicBoolean(false)
    private val opencvReady = AtomicBoolean(false)
    val pdfDeferred = CompletableDeferred<Unit>()
    val opencvDeferred = CompletableDeferred<Unit>()  // TODO: Add this

    fun warmup(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            if (pdfReady.compareAndSet(false, true)) {
                PdfBoxInit.ensure(context)  // PDFBoxResourceLoader.init() ~200ms cold
                pdfDeferred.complete(Unit)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            if (opencvReady.compareAndSet(false, true)) {
                OpenCVLoader.initLocal()   // Native .so load ~150ms cold
                opencvDeferred.complete(Unit)  // TODO: Add this
            }
        }
    }

    fun isOpenCvReady() = opencvReady.get()
}
```

**Issue:** `opencvDeferred` does not exist yet — `DocumentEdgeDetector` consumers cannot await readiness. Add as part of HIGH-5 fix.

---

# §6 · CORE PDF ENGINE — COMPLETE API

## 6.1 — IO Utilities (`PdfIoUtils.kt`) ✅

```kotlin
inline fun <T> Context.withUriCopiedToCacheFile(
    uri: Uri, prefix: String, suffix: String, block: (File) -> T
): T {
    val tempFile = copyUriToCacheFile(uri, prefix, suffix)
    try { return block(tempFile) }
    finally { tempFile.delete() }  // Auto-cleanup, even on exception
}
```

`pdfMemoryUsageSetting()` = `MemoryUsageSetting.setupTempFileOnly()` — always used, prevents heap OOM.

**TODO:** `validateInputSize(context, uri, maxBytes)` — check before copying (MED-11).

## 6.2 — Bitmap Pipeline (`BitmapDecodeUtils.kt`) — Mostly Correct

**API 28+ (ImageDecoder):** `ALLOCATOR_SOFTWARE` forces software bitmap (correct for all downstream use).
**API <28 (BitmapFactory):** `inSampleSize` power-of-2 + EXIF rotation via `Matrix`.

**Bug:** EXIF `ORIENTATION_TRANSVERSE`/`ORIENTATION_TRANSPOSE` missing `matrix.postScale(-1f, 1f)` — see MED-6.

Max long edge: 1800–2200px everywhere. All bitmaps `ARGB_8888` except `PdfCompressor` which uses `RGB_565`.

## 6.3 — Output File Resolution

```kotlin
fun resolveNonConflictingFile(directory: File, baseName: String, extension: String): File
```

**Must be used by ALL tools** — currently bypassed by 7+ tools (HIGH-6). Audit required.

---

# §7 · TOOL-BY-TOOL STATUS

| Tool | Class | Lines | Status | Key Open Issue |
|---|---|---|---|---|
| Image → PDF | `PdfCreator` | 121 | ✅ | MED-5 pixel/point confusion |
| PDF Merge | `PdfMerger` | 237 | ✅ | — |
| PDF Split (7 modes) | `PdfSplitter` | 423 | ✅ | HIGH-6 splitByRange overwrite |
| PDF Compress | `PdfCompressor` | 160 | ✅ | MED-4 RGB_565 alpha |
| PDF Sign | `PdfSigner` | 174 | ✅ | — |
| PDF Annotate | `PdfAnnotator` | 387 | ✅ | Burned-in, not real PDF annotations |
| PDF Redact | `PdfRedactionTool` | 319 | ⚠️ PARTIAL | CRITICAL-2 annotation/XMP gap |
| PDF OCR | `PdfOcrTool` | 430 | ✅ | MED-13 Latin font only |
| PDF Translate | `PdfTranslationTool` | 379 | ✅ | MED-1 WiFi, MED-13 font, MED-14 layout |
| PDF Form Fill | `PdfFormTool` | 297 | ✅ | — |
| PDF Watermark+Bates | `PdfBatchStampTool` | 215 | ✅ | — |
| PDF Password | `PdfPasswordTool` | 103 | ✅ | MED-2 AES-128 + weak passwords |
| PDF → Text | `PdfTextExtractor` | 76 | ✅ | LOW-5 charset |
| PDF → Images | `PdfPageImageExporter` | 142 | ✅ | MED-3 default 8 DPI |
| ID Card Sheet | `PdfIdCardTool` | 122 | ✅ | LOW-6 label clipping |
| Scan → Image | `ScanImageExporter` | 102 | ✅ | LOW-4 ZIP accumulation |
| Image Convert | `ImageFormatConverter` | 107 | ✅ | HIGH-6 overwrite |
| Doc → PDF | `DocumentPdfConverter` | 434 | ⚠️ PARTIAL | HIGH-4 RTF non-ASCII |
| Video → Audio | `VideoAudioExtractor` | 227 | ✅ | MP3 passthrough only |
| Edge Detection | `DocumentEdgeDetector` | 347 | ✅ | HIGH-5 constructor init |

---

# §8 · FEATURE CATALOG — SCANNER

**Edge Detection Pipeline:**
```
Bitmap → Mat(RGBA) → Grayscale → GaussianBlur(5×5) → Canny(75,200)
       → findContours(RETR_LIST) → approxPolyDP(2% perimeter)
       → filter(4pts + convex + area≥15%) → order corners TL→TR→BR→BL
       → DetectedQuad(corners, confidence 0.0–1.0)
Fallback: 8%/12% inset rectangle at confidence 0.25
```

**Perspective Correction:** `getPerspectiveTransform` → `warpPerspective` → cap 2000px longest edge. ✅

**Image Filters:** Grayscale (`COLOR_RGBA2GRAY`), B&W (Otsu threshold), Enhanced (unsharp mask `addWeighted(gray, 1.55, blurred, -0.55, 0)`). ✅

**Pending Scanner Features:** Brightness/contrast adjust, Deskew (Hough lines), QR detection, Scan→OCR, Business card→vCard, Magic erase (GrabCut).

---

# §9 · FEATURE CATALOG — CONVERTER

## DocumentPdfConverter (434 lines) — ⚠️ PARTIAL

**Input detection:** Extension → MIME type → content sniffing (RTF header `{\rtf`, DOCX ZIP+`word/document.xml`, CSV comma density).

**DOCX parser:** `ZipInputStream` → `XmlPullParser` on `word/document.xml`. Handles paragraphs, bold/italic runs, tables (pipe-delimited), list items, embedded image count. ✅ For basic text.

**RTF parser:** Regex-based control word stripping. ✅ ASCII only. 🟠 Non-ASCII corrupted (HIGH-4).

**CSV parser:** RFC-4180 compliant with quoted field + escaped quote support. ✅

**PDF rendering:** Android `PdfDocument` API + `Paint.breakText()` word-wrap. A4 (595×842pts). ✅

## VideoAudioExtractor (227 lines) ✅
- M4A: `MediaExtractor` → `MediaMuxer(MPEG_4)` — zero re-encoding
- MP3: Direct byte copy — passthrough only (no encode)
- Buffer: `KEY_MAX_INPUT_SIZE` or 256KB fallback

## AudioFormatConverter (720 lines) ✅ with caveats
- WAV output: RIFF header construction, raw PCM
- FLAC/MP3: Requires device hardware encoder (`MediaCodecList` check)
- `decodeToPcmFile` — ~30MB temp for 3-min stereo audio; no size guard (MED-11)

---

# §10 · BATCH QUEUE SYSTEM

## Architecture
```
UI ←→ BatchQueueViewModel ←→ BatchQueueRuntimeStore (singleton StateFlow)
                                        ↕ Mutex-guarded beginProcessing()
                           BatchQueueForegroundService (SupervisorJob + Dispatchers.IO)
                                        ↕
                              Tool engines (PdfCreator, PdfMerger, ...)
                                        ↕
                              HistoryRepository (Room insert on success)
```

## Thread Safety ✅
- `AtomicLong` for task ID generation
- `Mutex` guards `beginProcessing()` — prevents double-start race
- `StateFlow.update {}` for all queue mutations — atomic
- `SupervisorJob` — child failure doesn't cancel queue

## Known Issues
- `SimpleDateFormat` in `addTask()` — not thread-safe (MED-8)
- Queue state lost on process kill — no Room persistence (TODO Sprint 4)
- `BatchQueueViewModel` accesses DB directly via `DocForgeDatabase.get(application)` bypassing DI

## Task Types

| Type | Engine | Status |
|---|---|---|
| `IMAGES_TO_PDF` | `PdfCreator` | ✅ |
| `IMAGES_TO_JPG` | `ImageFormatConverter` | ✅ |
| `PDF_MERGE` | `PdfMerger` | ✅ |
| `PDF_COMPRESS` | `PdfCompressor` | ✅ |
| `DOCUMENT_TO_PDF` | `DocumentPdfConverter` | ✅ |
| `VIDEO_TO_AUDIO` | `VideoAudioExtractor` | ✅ |

---

# §11 · RESUME BUILDER SYSTEM

## Current: 10 Templates ✅ (with CRITICAL-3 sidebar crash)

**Template model:** 18 properties — layout, header style, 5 colors, font family, 3 font sizes, 3 spacing values, dividers, section icons.

**Layout engine (`ResumeRenderer.kt`, 326 lines):**
- `SingleColumnResume` ✅
- `TwoColumnResume` ✅
- `SidebarLeftResume` — 🔴 CRASH: `fillMaxHeight` inside `verticalScroll`
- `SidebarRightResume` — 🔴 CRASH: same issue

**Export:** Compose → Bitmap capture → `PdfCreator` → PDF file.

## Target: 105 Templates (see §12)

---

# §12 · RESUME TEMPLATE EXPANSION — 105 TOTAL

### Currently Implemented (10) ✅
1–10: See claude.md §9 Phase 1

### Phase 2 — 40 More Templates (TODO)
Categories: Engineering & Tech (10), Design & Creative (8), Business & Management (8), Academic & Research (6), Healthcare (4), Sales & Marketing (4)

### Phase 3 — 55 More Templates (TODO)
Categories: Legal & Finance (8), Education (6), Non-Software Engineering (6), Hospitality (5), Entry-Level & Student (8), International (5), ATS-Optimized (6), Portfolio & Special (11)

**Full template list:** See claude.md §9 for complete names, layouts, and styles.

---

# §13 · STORAGE LAYER

## Room Database (v3) — ⚠️ fallbackToDestructiveMigration present

```kotlin
@Database(entities = [ConversionHistoryEntity, BatchPresetEntity], version = 3)
abstract class DocForgeDatabase : RoomDatabase()
```

**Migrations:** 1→2 added `batch_presets`. 2→3 added `outputUri` + `displayName` to `conversion_history`. ✅

**CRITICAL:** `fallbackToDestructiveMigration()` must be removed before any public release (HIGH-3).

**Singleton:** `@Volatile` + `synchronized` double-checked locking. ✅

**Schema:**
```sql
-- conversion_history (v3)
id INTEGER PK AUTOINCREMENT, sourceLabel TEXT, outputPath TEXT,
operation TEXT, createdAtMillis INTEGER, inputCount INTEGER DEFAULT 0,
outputSizeBytes INTEGER DEFAULT 0, outputUri TEXT, displayName TEXT

-- batch_presets (v2+)
id INTEGER PK AUTOINCREMENT, name TEXT, createdAtMillis INTEGER, tasksJson TEXT
```

---

# §14 · SETTINGS SYSTEM

`DocForgeSettingsStore` — SharedPreferences-backed

| Key | Default | Status |
|---|---|---|
| `onboarding_completed` | false | ✅ |
| `default_pdf_page_size` | A4 | ✅ |
| `default_pdf_compression` | MEDIUM | ✅ |
| `default_image_quality` | 90 | ✅ |
| `documents_folder_name` | "DocForge" | ✅ |
| `images_folder_name` | "DocForge" | ✅ |
| `audio_folder_name` | "DocForge" | ✅ |
| **`allow_cellular_model_download`** | false | 🚀 TODO (MED-1) |

**Issue:** `currentSettings()` called synchronously in composables (MED-7). Fix: expose `StateFlow<DocForgeSettings>`.

---

# §15 · NAVIGATION STRUCTURE

`DocForgeNavHost.kt` (590 lines) — all 25+ routes inline. Should be split by feature as the codebase grows.

**Active routes:** home, scanner, converter, pdf_merge, pdf_split, pdf_compress, pdf_sign, pdf_annotate, pdf_redact, pdf_ocr, pdf_translate, pdf_form, pdf_stamp, pdf_password, pdf_text, pdf_images, pdf_id_card, resume_builder, batch_queue, history, settings, onboarding

**Issues:** Route strings are raw strings (type-unsafe), `activeSharedLaunch` lives in composable (HIGH-2), `currentSettings()` SP read in lambdas (MED-7).

---

# §16 · BUILD CONFIGURATION

```toml
# Current (needs upgrades)
agp = "8.5.2"             # → 8.6+
kotlin = "1.9.24"          # → 2.1.x (K2 compiler)
composeBom = "2024.06.00"  # → 2025.x
room = "2.6.1"
hilt = "2.51.1"
pdfboxAndroid = "2.0.27.0" # Latest ✅
opencv = "4.9.0"            # Latest stable ✅
mlkitTextRecognition = "16.0.1"
mlkitTranslate = "17.0.3"
camerax = "1.3.4"
minSdk = 26     # Good floor
targetSdk = 34  # → 35
```

---

# §17 · WHAT'S IMPLEMENTED SUPERBLY

The following patterns are engineering excellence and must never be regressed:

1. `withUriCopiedToCacheFile` + auto `finally` cleanup
2. `coroutineContext.ensureActive()` in every page/item loop
3. `Mat.release()` + `bitmap.recycle()` in every `finally`
4. `MemoryUsageSetting.setupTempFileOnly()` universally applied
5. `EngineWarmup` parallel async pre-warm of both engines
6. `AtomicBoolean` + `CompletableDeferred` exactly-once init
7. `StableUriRef` + `PersistentList` for Compose stability
8. Sequential batch queue with `SupervisorJob` — no thermal throttle
9. True content-stream redaction in `PdfRedactionTool`
10. Invisible text OCR overlay in `PdfOcrTool`
11. PDF outline (bookmarks) auto-generated in `PdfMerger`
12. Bates numbering continuous counter across multi-file batches
13. RFC-4180 compliant CSV parser
14. Room migrations with explicit DDL (no schema guessing)
15. `Result<T>` return type from all operations
16. Ratio-based coordinates for all PDF overlay tools
17. `resolveNonConflictingFile()` pattern (where consistently applied)
18. `RGB_565` in compressor — 50% memory savings
19. Progress callbacks `(current, total)` on all long operations
20. `Repository` interface pattern for clean domain separation

---

# §18 · IMPLEMENTATION SPRINT PLAN

### Sprint 1 (Week 1) — Critical Fixes ✅ COMPLETED
- ✅ Fix `SidebarResume` crash (removed `fillMaxHeight()`)
- ✅ Complete PDF redaction (annotations, XMP, embedded files, OC layers, verification)
- ⏭️ Commit to full Hilt DI architecture — deferred (~2 day migration)
- ✅ Remove `fallbackToDestructiveMigration()`

### Sprint 2 (Week 2) — High Priority Fixes ✅ COMPLETED
- ⏭️ `TempFileRegistry` for in-flight file protection — deferred
- ⏭️ Move `activeSharedLaunch` to ViewModel — deferred
- ✅ Universal `resolveNonConflictingFile()` audit (10 files fixed)
- ✅ RTF unicode escape handling (`\uN`)
- ✅ Remove OpenCV init from constructor (lazy init)
- ✅ `MediaScannerConnection.scanFile()` everywhere (replaced broken MediaStore API)

### Sprint 3 (Week 3) — Medium Fixes ✅ COMPLETED
- ✅ AES-256, 8-char min password
- ✅ PdfPageImageExporter default 2.5f scale
- ✅ EXIF TRANSVERSE/TRANSPOSE flip fix
- ✅ PdfCreator AUTO pixels-to-points
- ✅ `SimpleDateFormat` → `java.time` everywhere
- ✅ Atomic signature template write
- ✅ WiFi-only translation download

### Sprint 4 (Weeks 4–6) — New Features A ✅ COMPLETED
- ✅ HTML → PDF converter (`HtmlPdfConverter.kt` — WebView PrintAdapter)
- ✅ PDF Compare tool (`PdfCompareTool.kt` — bitmap diff with red highlights)
- ✅ PDF Page Crop (`PdfPageCropTool.kt` — percentage + absolute point modes)
- ✅ PDF Header/Footer/Page Numbers (`PdfHeaderFooterTool.kt`)
- ✅ Batch queue persistence (`BatchQueueTaskEntity` + `BatchQueueTaskDao` Room DAO)
- ⏭️ PDF Reader, PPTX/XLSX → PDF, Digital Signature, 40 templates — deferred

### Sprint 5 (Weeks 7–10) — New Features B ✅ COMPLETED
- ✅ Business card scanner → vCard (`BusinessCardParser.kt` — ML Kit OCR + vCard export)
- ✅ PDF/A compliance conversion (`PdfAComplianceTool.kt` — XMP metadata + PDF/A-1b ID)
- ✅ AES-256 encrypted document vault (`EncryptedDocumentVault.kt` — PBKDF2 + AES-GCM)
- ✅ Resume ATS score checker (`ResumeAtsScorer.kt` — keyword matching + section analysis)
- ✅ Typed signature calligraphy (`TypedSignatureRenderer.kt` — 5 font styles)
- ⏭️ 55 templates, QR/Barcode, EPUB → PDF, Invoice parser — deferred

### Sprint 6 (Weeks 11–12) — Tests & Upgrades ✅ COMPLETED
- ✅ Kotlin 1.9.24 → 2.1.0, Compose BOM 2024.06.00 → 2025.01.01, compileSdk/targetSdk 34 → 35
- ✅ Extract `importPage` to `PdfIoUtils.importPageFull()` shared utility
- ✅ Fix ZIP individual file accumulation (PdfPageImageExporter + ScanImageExporter)
- ✅ Fix URI extension extraction in ShareIntentRouter (`.toString()` → `.lastPathSegment`)
- ✅ Add `backup_rules.xml` + `data_extraction_rules.xml` for signatures/templates
- ⏭️ Unit tests, integration tests, UI tests — deferred (test infrastructure TBD)

### Sprint 7 (Week 13) — Gemini 3.1 Pro Feedback Validation ✅ COMPLETED
**External review validated against codebase. Only changes aligned with offline-first / 0ms latency / 4GB RAM vision implemented.**

#### Implemented
- ✅ Bitmap thumbnail tier — `decodeBitmapThumbnail()` with 400px cap (640KB vs 19MB per preview)
- ✅ EXIF rotation OOM leak fix — separate `OutOfMemoryError` catch + `bitmap.recycle()`
- ✅ Paging 3 support — `PagingSource` added to `ConversionHistoryDao` + `BatchQueueTaskDao`
- ✅ Paging 3 library — `paging = 3.3.5` in version catalog, `paging-runtime` + `paging-compose`
- ✅ Full-Text Search (FTS4) — `DocumentTextIndex.kt` (content entity + FTS shadow table + DAO with snippet search)
- ✅ Database migration v3→v4 — `batch_queue_tasks`, `document_text_index`, `document_text_fts` tables
- ✅ Auto-capture analyzer — `AutoCaptureAnalyzer` (Laplacian blur + frame stability, 5-frame trigger)

#### Rejected
- ❌ GPU/RenderScript → deprecated Android 12+
- ❌ Vulkan shaders → overkill for document scanning
- ❌ AI summarization → breaks offline-first, requires LLM/cloud
- ❌ P2P sync → out of scope, requires network stack

#### Already Correct (no changes)
- ✅ `EncryptedDocumentVault` streaming encryption (8KB CipherOutputStream)
- ✅ `EncryptedDocumentVault` uses `applicationContext` (no leak)
- ✅ All Room DAOs use `suspend`/`Flow` (no main-thread access)

### Sprint 7b (Week 13) — Gemini Round 2 Validation ✅ NO CODE CHANGES NEEDED
**All claims validated — already done, already correct, or factually wrong.**

- ✅ Already done (Sprint 7): Bitmap thumbnails, Paging 3, FTS4, EXIF OOM fix, encrypted streaming
- ✅ Already correct: Thread isolation — all tools use `withContext(Dispatchers.IO)` internally
- ❌ Gemini wrong: "ML Kit depends on Play Services" → we use `com.google.mlkit:text-recognition:16.0.1` (bundled, offline, all devices)
- 🟡 Deferred: Bitmap Pooling (post-launch), MED-13 Noto Sans (internationalization sprint)