package com.docforge.feature.pdftools

import android.net.Uri
import com.docforge.core.pdf.PdfCompressionLevel

data class PdfCompressUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val inputSizeBytes: Long? = null,
    val pageCount: Int? = null,
    val outputName: String = defaultOutputName(),
    val compressionLevel: PdfCompressionLevel = PdfCompressionLevel.MEDIUM,
    val isProcessing: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "compressed_${System.currentTimeMillis()}"
