package com.docforge.feature.scanner

import android.net.Uri
import com.docforge.core.pdf.PdfPageSize

enum class ScanFilterMode {
    COLOR,
    GRAYSCALE,
    BW,
    ENHANCED
}

enum class ScannerExportFormat {
    PDF,
    JPG,
    PNG
}

data class ScannerUiState(
    val capturedUris: List<Uri> = emptyList(),
    val outputName: String = defaultOutputName(),
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val exportFormat: ScannerExportFormat = ScannerExportFormat.PDF,
    val zipImageOutput: Boolean = false,
    val selectedFilterMode: ScanFilterMode = ScanFilterMode.COLOR,
    val isExporting: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputCount: Int = 0,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "scan_${System.currentTimeMillis()}"
