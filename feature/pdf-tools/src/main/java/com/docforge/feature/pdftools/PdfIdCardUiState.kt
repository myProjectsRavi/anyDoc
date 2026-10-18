package com.docforge.feature.pdftools

import android.net.Uri
import com.docforge.core.pdf.PdfPageSize

data class PdfIdCardUiState(
    val frontUri: Uri? = null,
    val backUri: Uri? = null,
    val frontLabel: String? = null,
    val backLabel: String? = null,
    val outputName: String = "id_card_sheet",
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null
)
