package com.docforge.app

import android.content.Context
import com.docforge.app.settings.AppSettingsRepository
import com.docforge.feature.converter.AudioFormatConverter
import com.docforge.feature.converter.ImageFormatConverter
import com.docforge.feature.converter.DocumentPdfConverter
import com.docforge.feature.converter.TextPdfConverter
import com.docforge.feature.converter.VideoAudioExtractor
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfCreator
import com.docforge.core.pdf.PdfCompressor
import com.docforge.core.pdf.PdfPageImageExporter
import com.docforge.core.pdf.PdfMerger
import com.docforge.core.pdf.PdfAnnotator
import com.docforge.core.pdf.PdfBatchStampTool
import com.docforge.core.pdf.PdfFormTool
import com.docforge.core.pdf.PdfIdCardTool
import com.docforge.core.pdf.PdfOcrTool
import com.docforge.core.pdf.PdfPasswordTool
import com.docforge.core.pdf.PdfRedactionTool
import com.docforge.core.pdf.PdfSigner
import com.docforge.core.pdf.PdfSplitter
import com.docforge.core.pdf.PdfTextExtractor
import com.docforge.core.pdf.PdfTranslationTool
import com.docforge.core.pdf.ScanImageExporter
import com.docforge.feature.pdftools.SavedSignatureStore
import com.docforge.feature.pdftools.SignaturePlacementTemplateStore
import com.docforge.core.storage.db.DocForgeDatabase
import com.docforge.core.storage.repository.LocalHistoryRepository

class AppDependencies(context: Context) {
    private val db = DocForgeDatabase.get(context)

    val settingsRepository: AppSettingsRepository = AppSettingsRepository(context)
    val historyRepository: HistoryRepository = LocalHistoryRepository(db.conversionHistoryDao())
    val pdfCreator: PdfCreator = PdfCreator(context)
    val pdfMerger: PdfMerger = PdfMerger(context)
    val pdfSplitter: PdfSplitter = PdfSplitter(context)
    val pdfSigner: PdfSigner = PdfSigner(context)
    val savedSignatureStore: SavedSignatureStore = SavedSignatureStore(context)
    val placementTemplateStore: SignaturePlacementTemplateStore = SignaturePlacementTemplateStore(context)
    val pdfCompressor: PdfCompressor = PdfCompressor(context)
    val pdfTextExtractor: PdfTextExtractor = PdfTextExtractor(context)
    val pdfPageImageExporter: PdfPageImageExporter = PdfPageImageExporter(context)
    val pdfAnnotator: PdfAnnotator = PdfAnnotator(context)
    val pdfPasswordTool: PdfPasswordTool = PdfPasswordTool(context)
    val scanImageExporter: ScanImageExporter = ScanImageExporter(context)
    val pdfBatchStampTool: PdfBatchStampTool = PdfBatchStampTool(context)
    val pdfOcrTool: PdfOcrTool = PdfOcrTool(context)
    val pdfFormTool: PdfFormTool = PdfFormTool(context)
    val pdfIdCardTool: PdfIdCardTool = PdfIdCardTool(context)
    val pdfTranslationTool: PdfTranslationTool = PdfTranslationTool(context)
    val pdfRedactionTool: PdfRedactionTool = PdfRedactionTool(context)
    val imageFormatConverter: ImageFormatConverter = ImageFormatConverter(context)
    val audioFormatConverter: AudioFormatConverter = AudioFormatConverter(context)
    val documentPdfConverter: DocumentPdfConverter = DocumentPdfConverter(context)
    val textPdfConverter: TextPdfConverter = TextPdfConverter(context)
    val videoAudioExtractor: VideoAudioExtractor = VideoAudioExtractor(context)
}
