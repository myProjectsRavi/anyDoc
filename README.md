# 🛡️ AnyDoc

<div align="center">
  <h3><strong>The Ultimate, God-Tier Document & Media Toolkit for Android</strong></h3>
  <p><em>100% Free. 100% Offline. 100% Private. Zero Subscriptions. Zero Ads. Zero Latency.</em></p>
</div>

---

## 🌟 Why AnyDoc?

Tired of subscription traps, cloud-dependent lag, and invasive privacy policies just to merge a PDF or scan a document? **AnyDoc** is engineered to be the final document app you will ever need. 

Built with an **uncompromising offline-first architecture** (the `INTERNET` permission is intentionally absent from the manifest), AnyDoc processes massive files directly on your device's hardware. Even on a 4GB RAM device, it delivers a blisteringly fast, 0ms-latency experience.

Your data never leaves your phone. Ever.

---

## 🚀 Core Pillars

- **🚫 Zero Subscriptions:** Completely free forever. No paywalls, no "premium" features.
- **🔒 Absolute Privacy:** No tracking, no analytics, no cloud uploads. What happens on your device, stays on your device.
- **⚡ Supercharged Performance:** God-tier memory management. Native OpenCV & PDFBox integrations tear through 500MB PDFs and 48MP raw photos without breaking a sweat or crashing.
- **🔋 Offline Forever:** Works flawlessly on an airplane, in a tunnel, or completely off the grid.

---

## 🛠️ Feature Arsenal

### 📄 PDF Mastery
*   **Merge PDFs & Images:** Combine multiple PDFs and images into a single document. Normalize page sizes and inject custom metadata/bookmarks.
*   **Split & Extract:** Split by page ranges, extract specific pages, chunk every *N* pages, or intelligently split by top-level outline bookmarks.
*   **Sign & Fill:** Draw your signature, save up to 3 presets, and drag-and-drop them onto pages. Create **Placement Templates** to automate signing massive contracts across multiple pages.
*   **Compress PDFs:** Shrink massive PDFs using native smart compression presets (High/Medium/Low) without destroying vector quality.
*   **Annotate & Draw:** Highlight, add text, drop sticky notes, or freehand draw directly onto PDF pages.
*   **Protect & Unlock:** Encrypt sensitive documents with military-grade AES-128 passwords, or strip passwords from unlocked files.
*   **Page Workspace:** Visually reorder, rotate (90/180/270°), and delete pages using a highly optimized, memory-safe thumbnail grid.

### 📸 Next-Gen Document Scanner
*   **Live Edge Detection:** Powered by OpenCV. Accurately detects document bounds in real-time through the camera viewfinder.
*   **Auto-Capture & Perspective Correction:** Automatically snaps when stable and perfectly flattens skewed pages.
*   **Pro Filters:** Apply Color, Grayscale, B&W, or Enhanced filters per page.
*   **Flexible Exports:** Export your scans as an offline PDF, high-res JPG/PNG gallery, or a neat ZIP bundle.

### 🔄 Universal Media Converter
*   **Document to PDF:** Convert DOCX, RTF, CSV, and TXT files instantly into formatted PDFs.
*   **Text to PDF:** Type or paste text and generate a PDF with plain/smart formatting and custom page sizes.
*   **PDF to Text & Images:** Extract raw text from PDFs to a local `.txt` file, or render pages into JPG/PNG/WebP images (with ZIP export).
*   **Image Format Converter:** Batch convert heavy HEIC, BMP, TIFF, WebP, JPG, or PNG files into optimized formats.
*   **Video to Audio:** Strip audio tracks from massive video files (MP4/MKV -> M4A/MP3).
*   **Audio Transcoder:** Convert and encode audio formats natively (M4A, WAV, MP3, FLAC).

### ⚙️ Power-User Workflows
*   **Batch Queue Runner:** Queue up 50 different conversion tasks and let them run sequentially in the background with persistent progress notifications.
*   **Live Queue Editing:** Reorder or cancel tasks while the batch runner is active.
*   **Smart Share Routing:** Send any file to AnyDoc from other apps, and it will auto-route to the correct tool based on the file type.
*   **Persistent History:** Keep track of your local conversions seamlessly via local database.

---

## 🏗️ Technical Architecture (For the Geeks)

*   **100% Native PDF Manipulation:** Uses `pdfbox-android` for instantaneous, vector-preserving PDF merges, splits, and edits. No destructive Bitmap rasterization.
*   **OOM-Proof Memory Management:** Utilizes stream-based processing, constrained memory-mapped I/O, and strict hardware-aware decode bounds. It will never crash, even when processing massive PDFs or camera frames on constrained 4GB RAM devices.
*   **Modern Android Stack:** Kotlin, Jetpack Compose UI, Coroutines/Flow, CameraX, Room, and an ultra-clean modular architecture (`app`, `core`, `feature`).

---

## 📦 Build Prerequisites

1. Install Android SDK with platform 34 and build-tools.
2. Set your Android SDK path in `local.properties` (`sdk.dir=/path/to/Android/sdk`) or via `ANDROID_HOME`.
3. Run the following command:

```bash
./gradlew :app:assembleDebug
```

---
<div align="center">
  <p>Built with passion to replace corporate bloatware. Enjoy your digital freedom.</p>
</div>
