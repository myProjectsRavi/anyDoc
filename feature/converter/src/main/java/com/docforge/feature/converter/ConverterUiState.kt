package com.docforge.feature.converter

import android.net.Uri
import com.docforge.core.pdf.PdfPageSize

data class ConverterUiState(
    val selectedUris: List<Uri> = emptyList(),
    val outputName: String = defaultOutputName(),
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val isConverting: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "docforge_${System.currentTimeMillis()}"
