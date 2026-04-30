package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfPageImageExporter
import com.docforge.core.pdf.PdfPageImageFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfPageImageViewModel(
    private val historyRepository: HistoryRepository,
    private val pdfPageImageExporter: PdfPageImageExporter,
    defaultJpegQuality: Int = 90
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PdfPageImageUiState(jpegQualityInput = defaultJpegQuality.coerceIn(10, 100).toString())
    )
    val uiState: StateFlow<PdfPageImageUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                pageCount = null,
                isProcessing = false,
                lastOutputPaths = emptyList(),
                lastOutputSizeBytes = null,
                statusMessage = "Loading PDF details...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching { pdfPageImageExporter.getPageCount(uri) }
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            pageCount = count,
                            statusMessage = "Loaded PDF ($count page(s))",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            pageCount = null,
                            statusMessage = null,
                            errorMessage = err.message ?: "Failed to read PDF"
                        )
                    }
                }
        }
    }

    fun onOutputBaseNameChanged(value: String) {
        _uiState.update { it.copy(outputBaseName = value) }
    }

    fun onFormatChanged(format: PdfPageImageFormat) {
        _uiState.update { it.copy(imageFormat = format) }
    }

    fun onZipBundleOutputChanged(enabled: Boolean) {
        _uiState.update { it.copy(zipBundleOutput = enabled) }
    }

    fun onJpegQualityChanged(value: String) {
        _uiState.update { it.copy(jpegQualityInput = value) }
    }

    fun onScaleChanged(value: String) {
        _uiState.update { it.copy(scaleInput = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun exportPages() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        val quality = state.jpegQualityInput.toIntOrNull()
        val scale = state.scaleInput.toFloatOrNull()
        if (quality == null || scale == null) {
            _uiState.update { it.copy(errorMessage = "Enter numeric quality and scale values.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Exporting pages to images...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfPageImageExporter.exportPages(
                    inputUri = uri,
                    outputBaseName = state.outputBaseName,
                    format = state.imageFormat,
                    jpegQuality = quality,
                    scaleFactor = scale.coerceIn(0.25f, 2.0f),
                    zipBundle = state.zipBundleOutput
                )
            }.onSuccess { result ->
                val primaryOutput = result.bundleZipFile?.absolutePath
                    ?: result.outputFiles.firstOrNull()?.absolutePath
                    .orEmpty()
                val operation = if (state.zipBundleOutput) {
                    "PDF -> ${state.imageFormat.name} + ZIP"
                } else {
                    "PDF -> ${state.imageFormat.name}"
                }
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF pages (${result.pageCount})",
                        outputPath = primaryOutput,
                        operation = operation,
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = result.pageCount,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                val outputPaths = buildList {
                    result.bundleZipFile?.let { zip -> add(zip.absolutePath) }
                    addAll(result.outputFiles.map { file -> file.absolutePath })
                }
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPaths = outputPaths,
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = if (result.bundleZipFile != null) {
                            "Exported ${result.pageCount} image(s) + ZIP bundle"
                        } else {
                            "Exported ${result.pageCount} image(s)"
                        },
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "PDF page export failed"
                    )
                }
            }
        }
    }
}

class PdfPageImageViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val pdfPageImageExporter: PdfPageImageExporter,
    private val defaultJpegQuality: Int = 90
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfPageImageViewModel::class.java)) {
            return PdfPageImageViewModel(
                historyRepository = historyRepository,
                pdfPageImageExporter = pdfPageImageExporter,
                defaultJpegQuality = defaultJpegQuality
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
