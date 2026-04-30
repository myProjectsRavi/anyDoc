package com.docforge.feature.pdftools

import android.net.Uri

data class PdfTranslateUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val outputName: String = "translated_pdf",
    val sourceLanguageTag: String = "en",
    val targetLanguageTag: String = "es",
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val progressStage: String? = null,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val translatedLineCount: Int? = null
)
