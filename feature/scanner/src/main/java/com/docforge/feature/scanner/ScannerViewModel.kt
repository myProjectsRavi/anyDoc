package com.docforge.feature.scanner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfCreationOptions
import com.docforge.core.pdf.PdfCreator
import com.docforge.core.pdf.PdfPageSize
import com.docforge.core.pdf.ScanImageExporter
import com.docforge.core.pdf.ScanImageFormat
import com.docforge.core.ui.model.toStableUriRef
import com.docforge.core.ui.model.toStableUriRefList
import com.docforge.core.ui.model.toUriList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScannerViewModel(
    private val pdfCreator: PdfCreator,
    private val scanImageExporter: ScanImageExporter,
    private val historyRepository: HistoryRepository,
    defaultPageSize: PdfPageSize = PdfPageSize.A4
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState(pageSize = defaultPageSize))
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onImageCaptured(uri: Uri) {
        _uiState.update {
            val updated = it.capturedUris.toMutableList()
                .apply { add(uri.toStableUriRef()) }
                .toPersistentList()
            it.copy(
                capturedUris = updated,
                statusMessage = "Captured ${updated.size} page(s)",
                errorMessage = null
            )
        }
    }

    fun onImagesImported(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _uiState.update {
            val updated = (it.capturedUris + uris.toStableUriRefList()).toPersistentList()
            it.copy(
                capturedUris = updated,
                statusMessage = "Added ${uris.size} imported page(s)",
                errorMessage = null
            )
        }
    }

    fun removePage(index: Int) {
        val current = _uiState.value
        if (index !in current.capturedUris.indices) return
        _uiState.update {
            val updated = it.capturedUris.toMutableList().apply { removeAt(index) }.toPersistentList()
            it.copy(capturedUris = updated, statusMessage = "Removed page ${index + 1}")
        }
    }

    fun replacePage(index: Int, uri: Uri, reason: String = "Updated page filter") {
        val current = _uiState.value
        if (index !in current.capturedUris.indices) return
        _uiState.update {
            val updated = it.capturedUris.toMutableList()
            updated[index] = uri.toStableUriRef()
            it.copy(
                capturedUris = updated.toPersistentList(),
                statusMessage = "${reason}: page ${index + 1}",
                errorMessage = null
            )
        }
    }

    fun onOutputNameChanged(name: String) {
        _uiState.update { it.copy(outputName = name) }
    }

    fun onFilterModeChanged(mode: ScanFilterMode) {
        _uiState.update { it.copy(selectedFilterMode = mode) }
    }

    fun onPageSizeChanged(size: PdfPageSize) {
        _uiState.update { it.copy(pageSize = size) }
    }

    fun onExportFormatChanged(format: ScannerExportFormat) {
        _uiState.update {
            it.copy(
                exportFormat = format,
                zipImageOutput = if (format == ScannerExportFormat.PDF) false else it.zipImageOutput
            )
        }
    }

    fun onZipImageOutputChanged(enabled: Boolean) {
        _uiState.update { current ->
            if (current.exportFormat == ScannerExportFormat.PDF) {
                current.copy(zipImageOutput = false)
            } else {
                current.copy(zipImageOutput = enabled)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun exportScan() {
        val state = _uiState.value
        if (state.capturedUris.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Capture or import at least one page first.") }
            return
        }
        if (state.isExporting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    statusMessage = "Exporting scan to ${state.exportFormat.name}...",
                    errorMessage = null
                )
            }

            when (state.exportFormat) {
                ScannerExportFormat.PDF -> {
                    runCatching {
                        pdfCreator.createPdfFromImages(
                            imageUris = state.capturedUris.toUriList(),
                            outputName = state.outputName,
                            options = PdfCreationOptions(pageSize = state.pageSize)
                        )
                    }.onSuccess { result ->
                        persistAndPublishExport(
                            state = state,
                            operation = "Scan -> PDF",
                            exportedPath = result.outputFile.absolutePath,
                            exportedCount = result.pageCount,
                            outputSizeBytes = result.outputSizeBytes,
                            statusLabel = "PDF"
                        )
                    }.onFailure { err ->
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                statusMessage = null,
                                errorMessage = err.message ?: "Export failed"
                            )
                        }
                    }
                }

                ScannerExportFormat.JPG,
                ScannerExportFormat.PNG -> {
                    val imageFormat = if (state.exportFormat == ScannerExportFormat.JPG) {
                        ScanImageFormat.JPG
                    } else {
                        ScanImageFormat.PNG
                    }

                    runCatching {
                        scanImageExporter.export(
                            imageUris = state.capturedUris.toUriList(),
                            outputBaseName = state.outputName,
                            format = imageFormat,
                            zipBundle = state.zipImageOutput
                        )
                    }.onSuccess { result ->
                        val exportedPath = result.bundleZipFile?.absolutePath
                            ?: result.outputFiles.firstOrNull()?.absolutePath
                            ?: ""
                        val statusSuffix = if (result.bundleZipFile != null) " + ZIP" else ""
                        persistAndPublishExport(
                            state = state,
                            operation = "Scan -> ${state.exportFormat.name}${if (result.bundleZipFile != null) " ZIP" else ""}",
                            exportedPath = exportedPath,
                            exportedCount = result.outputFiles.size,
                            outputSizeBytes = result.outputSizeBytes,
                            statusLabel = "${state.exportFormat.name}$statusSuffix"
                        )
                    }.onFailure { err ->
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                statusMessage = null,
                                errorMessage = err.message ?: "Export failed"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun persistAndPublishExport(
        state: ScannerUiState,
        operation: String,
        exportedPath: String,
        exportedCount: Int,
        outputSizeBytes: Long,
        statusLabel: String
    ) {
        viewModelScope.launch {
            historyRepository.insert(
                ConversionRecord(
                    sourceLabel = "Scan pages (${state.capturedUris.size})",
                    outputPath = exportedPath,
                    operation = operation,
                    createdAtMillis = System.currentTimeMillis(),
                    inputCount = state.capturedUris.size,
                    outputSizeBytes = outputSizeBytes
                )
            )
        }
        _uiState.update {
            it.copy(
                isExporting = false,
                lastOutputPath = exportedPath,
                lastOutputCount = exportedCount,
                lastOutputSizeBytes = outputSizeBytes,
                statusMessage = "Scan exported as $statusLabel ($exportedCount page(s))",
                errorMessage = null
            )
        }
    }
}

class ScannerViewModelFactory(
    private val pdfCreator: PdfCreator,
    private val scanImageExporter: ScanImageExporter,
    private val historyRepository: HistoryRepository,
    private val defaultPageSize: PdfPageSize = PdfPageSize.A4
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            return ScannerViewModel(
                pdfCreator = pdfCreator,
                scanImageExporter = scanImageExporter,
                historyRepository = historyRepository,
                defaultPageSize = defaultPageSize
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
