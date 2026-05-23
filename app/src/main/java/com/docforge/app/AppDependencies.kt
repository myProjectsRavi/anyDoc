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
import com.docforge.core.pdf.ScanImageExporter
import com.docforge.feature.pdftools.SavedSignatureStore
import com.docforge.feature.pdftools.SignaturePlacementTemplateStore
import com.docforge.core.storage.db.DocForgeDatabase
import com.docforge.core.storage.repository.LocalHistoryRepository

class AppDependencies(private val context: Context) {
    private val db by lazy { DocForgeDatabase.get(context) }

    val settingsRepository: AppSettingsRepository by lazy { AppSettingsRepository(context) }
    val historyRepository: HistoryRepository by lazy { LocalHistoryRepository(db.conversionHistoryDao()) }
    val pdfCreator: PdfCreator by lazy { PdfCreator(context) }
    val pdfMerger: PdfMerger by lazy { PdfMerger(context) }
    val pdfSplitter: PdfSplitter by lazy { PdfSplitter(context) }
    val pdfSigner: PdfSigner by lazy { PdfSigner(context) }
    val savedSignatureStore: SavedSignatureStore by lazy { SavedSignatureStore(context) }
    val placementTemplateStore: SignaturePlacementTemplateStore by lazy { SignaturePlacementTemplateStore(context) }
    val pdfCompressor: PdfCompressor by lazy { PdfCompressor(context) }
    val pdfTextExtractor: PdfTextExtractor by lazy { PdfTextExtractor(context) }
    val pdfPageImageExporter: PdfPageImageExporter by lazy { PdfPageImageExporter(context) }
    val pdfAnnotator: PdfAnnotator by lazy { PdfAnnotator(context) }
    val pdfPasswordTool: PdfPasswordTool by lazy { PdfPasswordTool(context) }
    val scanImageExporter: ScanImageExporter by lazy { ScanImageExporter(context) }
    val pdfBatchStampTool: PdfBatchStampTool by lazy { PdfBatchStampTool(context) }
    val pdfOcrTool: PdfOcrTool by lazy { PdfOcrTool(context) }
    val pdfFormTool: PdfFormTool by lazy { PdfFormTool(context) }
    val pdfIdCardTool: PdfIdCardTool by lazy { PdfIdCardTool(context) }
    val pdfRedactionTool: PdfRedactionTool by lazy { PdfRedactionTool(context) }
    val imageFormatConverter: ImageFormatConverter by lazy { ImageFormatConverter(context) }
    val audioFormatConverter: AudioFormatConverter by lazy { AudioFormatConverter(context) }
    val documentPdfConverter: DocumentPdfConverter by lazy { DocumentPdfConverter(context) }
    val textPdfConverter: TextPdfConverter by lazy { TextPdfConverter(context) }
    val videoAudioExtractor: VideoAudioExtractor by lazy { VideoAudioExtractor(context) }
}
