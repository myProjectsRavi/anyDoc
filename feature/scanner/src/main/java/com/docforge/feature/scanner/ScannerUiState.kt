package com.docforge.feature.scanner

import androidx.compose.runtime.Immutable
import com.docforge.core.ui.model.StableUriRef
import com.docforge.core.pdf.PdfPageSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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

@Immutable
data class ScannerUiState(
    val capturedUris: ImmutableList<StableUriRef> = persistentListOf(),
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

private fun defaultOutputName(): String {
    val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
    return "scan_${sdf.format(java.util.Date())}"
}
