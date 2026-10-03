# Performance Baseline — Cycle 001

## Environment
- Target: Android minSdk 26, compile/target SDK 35, Java 17.
- Important device class: approximately 4 GB total RAM.
- Physical device available to this automation: none.
- Current ChatGPT/Codex container: repository clone blocked by DNS resolution for GitHub.
- Remote authoritative environment: GitHub Actions, newly configured but current run not yet observed as passing.

## Source-level observations, not benchmark claims
- PDF loading uses PDFBox temp-file-backed memory settings.
- PDF URI ingestion uses an 8 MiB channel transfer chunk.
- Image decode utility constrains long edge (default 2200px; thumbnail 400px).
- PDF compression creates one ARGB_8888 page bitmap at a time, which bounds page concurrency but can still be expensive for large page dimensions/render DPI.
- OCR renders one page at a time and caps its long edge around 1800px.
- Batch processing is designed sequentially, reducing concurrent memory pressure.
- OpenCV paths explicitly release native Mat objects in fetched scanner code.
- Audio conversion may create large PCM temp files; disk-space and input-duration guards are not yet established.

## Baselines to establish
- cold/warm startup;
- first navigation to PDF/scanner;
- PDF open/copy latency by 10/100/500 MB inputs;
- merge/split 10/100/500 pages;
- compression at each quality level;
- OCR 1/10/50 pages;
- scanner edge detection/frame;
- image batch 1/10/100 images;
- audio conversion by duration;
- batch cancellation latency;
- peak managed/native memory and temporary disk use.

No “faster”, “5% improved”, “zero lag”, or similar claim is allowed until measurements exist.
