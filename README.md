# DocForge

Offline-first Android document toolkit based on `DocForge_FileConverter_PDF_Blueprint.docx`.

## Current status

Implemented:
- Modular Android project scaffold (`app`, `core`, `feature` modules)
- Blueprint-aligned placeholder modules for upcoming scanner/PDF epic work
- Strict offline posture (`INTERNET` permission intentionally absent)
- Home dashboard + navigation shell
- `Images -> PDF` converter (multi-image, page size options)
- Scanner foundation (CameraX preview, manual capture, gallery import, multi-page queue, scan-to-PDF export)
- OpenCV scanner upgrade (live contour-based edge overlay from camera frames + perspective-corrected page normalization)
- Scanner auto-capture + corner-adjustment UX pass (stable-detection auto shutter, draggable corner handles, manual corner reset)
- Scanner filters with per-page apply (Color/Grayscale/BW/Enhanced)
- Scanner export formats (offline PDF + high-res JPG/PNG output modes)
- Scanner optional ZIP bundle export for JPG/PNG scan outputs
- PDF Merge tool (multi-select, reorder, offline merge export)
- Mixed-source merge support (combine PDFs + images in one merged PDF)
- Merge metadata and bookmarks (set title/author/subject + optional source bookmarks)
- Merge page-size normalization options (keep source size or fit pages to A4/Letter)
- PDF Split/Extract tool (split by page range, extract selected pages into separate PDFs)
- PDF split every N pages (chunk one PDF into evenly-sized groups)
- PDF page reorder/delete workspace (custom page order output + selected page removal)
- PDF page rotate tool (rotate selected pages by 90/180/270 degrees)
- PDF Sign tool (draw signature, place on selected page, export signed PDF)
- Saved signature slots (save/reuse/delete up to 3 local signatures)
- Signature placement drag-and-drop preview on page canvas
- Multi-signature placements in one export (apply multiple signature boxes across pages)
- Apply one placement template to all pages in a PDF signing run
- Persisted signature placement templates (save/load/delete named preset sets)
- PDF Compress tool (high/medium/low presets with estimated output size)
- PDF to TXT tool (extract text to local `.txt`)
- PDF to Images tool (export each page as JPG/PNG/WebP)
- PDF to Images optional ZIP bundle export
- Image format converter (batch convert HEIC/WebP/BMP/TIFF/JPG/PNG inputs to JPG/PNG/WebP outputs)
- Document to PDF converter (DOCX/RTF/CSV/TXT -> PDF)
- Text to PDF quick tool (typed/pasted text with plain/smart formatting + page-size options)
- Video to audio extractor (video -> M4A, plus MP3 passthrough for MP3 source tracks)
- Audio format converter (audio -> M4A/WAV plus MP3/FLAC passthrough-or-encode when device codec exists)
- PDF annotate tool (highlight/text/sticky-note annotations flattened into export)
- PDF freehand draw annotations (draw strokes on selected pages and flatten)
- PDF password tool (protect and unlock with local password operations)
- PDF split by bookmark boundaries (top-level outline chapter splitting)
- Split/organize visual page thumbnails workspace (tap-select pages, visual reorder, and apply to split operations)
- Virtualized on-demand thumbnail rendering for large PDFs (no eager full-document rendering)
- Thumbnail quality presets for split visual workspace (Low/Medium/High)
- Thumbnail prefetch windowing for smoother large-PDF scrolling
- Share-intent smart routing (ACTION_SEND / ACTION_SEND_MULTIPLE -> tool-specific screen prefill by file type)
- Batch queue processing for mixed conversion tasks (sequential offline runner with run/cancel and per-task status)
- Foreground-service queue progress notifications (persistent progress + completion summary notification)
- Live in-queue reordering/editing while running (move queued tasks up/down, update output base names)
- Batch queue task presets (save/load recurring task sets)
- First-run onboarding flow
- Settings screen for defaults (PDF page size, PDF compression, image quality, output folder names)
- Settings-driven default behavior in tools and batch queue (quality/page size/compression)
- Local conversion history (Room)
- Gradle wrapper and build config (AGP 8.5.2, Kotlin 1.9.24)

Not yet implemented (current roadmap subset):
- None. Current roadmap subset is complete.

## Build prerequisites

1. Install Android SDK with platform 34 and build-tools.
2. Set one of:
   - `ANDROID_HOME=/path/to/Android/sdk`
   - `local.properties` with `sdk.dir=/path/to/Android/sdk`
3. Run:

```bash
./gradlew :app:assembleDebug
```

## Next feature slices (in blueprint order)

1. Add placement-template import/export file backup in PDF Sign
2. Add split thumbnail memory cap + LRU eviction for long browsing sessions
3. Add background worker option for very large PDF-to-images exports
