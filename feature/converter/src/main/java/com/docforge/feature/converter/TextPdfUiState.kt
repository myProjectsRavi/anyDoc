package com.docforge.feature.converter

import com.docforge.core.pdf.PdfPageSize

data class TextPdfUiState(
    val title: String = "",
    val text: String = "",
    val outputName: String = defaultTextPdfOutputName(),
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val formatMode: TextPdfFormatMode = TextPdfFormatMode.SMART,
    val isProcessing: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val lastPageCount: Int? = null,
    val lastParagraphCount: Int? = null,
    val lastCharacterCount: Int? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultTextPdfOutputName(): String = "text_${System.currentTimeMillis()}"
