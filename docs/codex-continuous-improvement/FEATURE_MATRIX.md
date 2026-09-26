# Feature Matrix

This matrix is intentionally evidence-oriented. “Needs audit” means the feature exists in current repository/README/verified wiring but its full correctness/performance surface has not yet been exhaustively validated in Cycle 001.

| Feature | Main implementation | UX | Correctness / safety | Test coverage | Performance / memory | Cycle 001 |
|---|---|---|---|---|---|---|
| Home + PDF/Image hubs | app navigation/home/hub screens | needs audit | routing needs audit | none verified | Compose metrics needed | inventory |
| Scanner | `feature/scanner`, `core/opencv` | needs audit | native cleanup patterns present; full flow needs audit | none verified | 4 GB / CameraX profiling needed | pending |
| Images → PDF | `PdfCreator` | needs audit | non-conflicting output verified | indirect resolver test only | bitmap baseline needed | pending |
| PDF merge | `PdfMerger` | needs audit | non-conflicting output verified | none verified | large-page-count baseline needed | pending |
| PDF split/extract | `PdfSplitter` | needs audit | collision paths patched | CI pending | large-page-count baseline needed | P0 fix |
| PDF compress | `PdfCompressor` | trade-off must be clear | output allocation safe; rasterization changes semantics | none verified | high bitmap risk; benchmark needed | pending |
| PDF sign | `PdfSigner` + saved signatures | needs audit | visual-signature semantics documented | none verified | preview/export baseline needed | pending |
| PDF annotate | `PdfAnnotator` | needs audit | output allocation safe | none verified | needs audit | pending |
| PDF redact | `PdfRedactionTool` | disclaimer critical | collision path patched; forensic behavior requires regression suite | none verified | needs audit | P0 fix + follow-up |
| PDF OCR | `PdfOcrTool` | needs audit | collisions patched; Latin-only searchable layer remains | none verified | 1800px/page bitmap path; benchmark needed | P0 fix + P3 |
| PDF password | `PdfPasswordTool` | needs audit | non-conflicting output verified | none verified | needs audit | pending |
| PDF → text | `PdfTextExtractor` | needs audit | collision patched | none verified | large text baseline needed | P0 fix |
| PDF → images | `PdfPageImageExporter` | needs audit | per-page safe; ZIP collision patched | none verified | bitmap + ZIP disk baseline needed | P0 fix |
| ID card sheet | `PdfIdCardTool` | needs audit | collision patched | none verified | needs audit | P0 fix |
| Batch stamp | `PdfBatchStampTool` | needs audit | collision patched | none verified | batch baseline needed | P0 fix |
| PDF compare | `PdfCompareTool` | needs audit | safe allocator verified | none verified | raster diff cost needs benchmark | pending |
| PDF crop | `PdfPageCropTool` | needs audit | safe allocator verified | none verified | needs audit | pending |
| Header/footer/page no. | `PdfHeaderFooterTool` | needs audit | safe allocator verified | none verified | needs audit | pending |
| PDF/A conversion | `PdfAComplianceTool` | needs audit | safe allocator verified; standards conformance needs dedicated validation | none verified | needs audit | pending |
| Image format conversion | `ImageFormatConverter` | needs audit | safe per-item allocation verified | none verified | 2200px constrained decode | pending |
| Text → PDF | `TextPdfConverter` | needs audit | collision patched | none verified | long-text baseline needed | P0 fix |
| Document → PDF | `DocumentPdfConverter` | fidelity limitations | safe allocator verified | none verified | DOCX/RTF/CSV large input audit needed | pending |
| Audio conversion | `AudioFormatConverter` | device-codec errors need UX audit | overwrite fixed; PCM temp protected | none verified | temp-disk/codec baseline needed | P0/P1 fix |
| Video → audio | `VideoAudioExtractor` | MP3 passthrough limitation needs clarity | overwrite fixed | none verified | buffer/duration baseline needed | P0 fix |
| Batch queue | app batch service/store | needs audit | foreground/lifecycle recovery needs audit | none verified | sequential design; benchmark needed | P1 pending |
| History | Room repository / feature history | needs audit | explicit migrations verified | migration tests needed | paging baseline needed | pending |
| Vault | app vault screen / storage implementation | needs audit | security review required | none verified | needs audit | pending |
| Resume builder | pdf-tools resume | needs audit | prior crash fix not revalidated this run | none verified | Compose/export memory needed | pending |
| Settings | app settings repository | needs audit | synchronous reads in NavHost verified | none verified | cold-start/jank risk | P3 pending |
| Share into AnyDoc | share router + NavHost | needs audit | recreation state loss verified | none verified | low | P1 pending |

## Next matrix expansion
Future runs must add exact entry points, file lists, benchmark identifiers, memory-risk ratings, known bugs, and measured results for every row.
