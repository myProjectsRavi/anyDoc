package com.docforge.app.di

import android.content.Context
import com.docforge.app.settings.AppSettingsRepository
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.*
import com.docforge.core.storage.db.BatchPresetDao
import com.docforge.core.storage.db.ConversionHistoryDao
import com.docforge.core.storage.db.DocForgeDatabase
import com.docforge.core.storage.repository.LocalHistoryRepository
import com.docforge.feature.converter.*
import com.docforge.feature.pdftools.SavedSignatureStore
import com.docforge.feature.pdftools.SignaturePlacementTemplateStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DocForgeDatabase =
        DocForgeDatabase.get(context)

    @Provides
    fun provideConversionHistoryDao(db: DocForgeDatabase): ConversionHistoryDao =
        db.conversionHistoryDao()

    @Provides
    fun provideBatchPresetDao(db: DocForgeDatabase): BatchPresetDao =
        db.batchPresetDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): AppSettingsRepository =
        AppSettingsRepository(context)

    @Provides
    @Singleton
    fun provideHistoryRepository(dao: ConversionHistoryDao): HistoryRepository =
        LocalHistoryRepository(dao)

    @Provides
    @Singleton
    fun providePdfCreator(@ApplicationContext context: Context): PdfCreator = PdfCreator(context)

    @Provides
    @Singleton
    fun providePdfMerger(@ApplicationContext context: Context): PdfMerger = PdfMerger(context)

    @Provides
    @Singleton
    fun providePdfSplitter(@ApplicationContext context: Context): PdfSplitter = PdfSplitter(context)

    @Provides
    @Singleton
    fun providePdfSigner(@ApplicationContext context: Context): PdfSigner = PdfSigner(context)

    @Provides
    @Singleton
    fun provideSavedSignatureStore(@ApplicationContext context: Context): SavedSignatureStore =
        SavedSignatureStore(context)

    @Provides
    @Singleton
    fun provideSignaturePlacementTemplateStore(@ApplicationContext context: Context): SignaturePlacementTemplateStore =
        SignaturePlacementTemplateStore(context)

    @Provides
    @Singleton
    fun providePdfCompressor(@ApplicationContext context: Context): PdfCompressor = PdfCompressor(context)

    @Provides
    @Singleton
    fun providePdfTextExtractor(@ApplicationContext context: Context): PdfTextExtractor = PdfTextExtractor(context)

    @Provides
    @Singleton
    fun providePdfPageImageExporter(@ApplicationContext context: Context): PdfPageImageExporter = PdfPageImageExporter(context)

    @Provides
    @Singleton
    fun providePdfAnnotator(@ApplicationContext context: Context): PdfAnnotator = PdfAnnotator(context)

    @Provides
    @Singleton
    fun providePdfPasswordTool(@ApplicationContext context: Context): PdfPasswordTool = PdfPasswordTool(context)

    @Provides
    @Singleton
    fun provideScanImageExporter(@ApplicationContext context: Context): ScanImageExporter = ScanImageExporter(context)

    @Provides
    @Singleton
    fun providePdfBatchStampTool(@ApplicationContext context: Context): PdfBatchStampTool = PdfBatchStampTool(context)

    @Provides
    @Singleton
    fun providePdfOcrTool(@ApplicationContext context: Context): PdfOcrTool = PdfOcrTool(context)

    @Provides
    @Singleton
    fun providePdfFormTool(@ApplicationContext context: Context): PdfFormTool = PdfFormTool(context)

    @Provides
    @Singleton
    fun providePdfIdCardTool(@ApplicationContext context: Context): PdfIdCardTool = PdfIdCardTool(context)

    @Provides
    @Singleton
    fun providePdfRedactionTool(@ApplicationContext context: Context): PdfRedactionTool = PdfRedactionTool(context)

    @Provides
    @Singleton
    fun provideImageFormatConverter(@ApplicationContext context: Context): ImageFormatConverter = ImageFormatConverter(context)

    @Provides
    @Singleton
    fun provideAudioFormatConverter(@ApplicationContext context: Context): AudioFormatConverter = AudioFormatConverter(context)

    @Provides
    @Singleton
    fun provideDocumentPdfConverter(@ApplicationContext context: Context): DocumentPdfConverter = DocumentPdfConverter(context)

    @Provides
    @Singleton
    fun provideTextPdfConverter(@ApplicationContext context: Context): TextPdfConverter = TextPdfConverter(context)

    @Provides
    @Singleton
    fun provideVideoAudioExtractor(@ApplicationContext context: Context): VideoAudioExtractor = VideoAudioExtractor(context)
}
