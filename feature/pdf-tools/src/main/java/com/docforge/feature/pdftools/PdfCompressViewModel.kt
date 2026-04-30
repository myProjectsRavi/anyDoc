package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfCompressionLevel
import com.docforge.core.pdf.PdfCompressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfCompressViewModel(
    private val historyRepository: HistoryRepository,
    private val pdfCompressor: PdfCompressor,
    defaultCompressionLevel: PdfCompressionLevel = PdfCompressionLevel.MEDIUM
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfCompressUiState(compressionLevel = defaultCompressionLevel))
    val uiState: StateFlow<PdfCompressUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?, sizeBytes: Long?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                inputSizeBytes = sizeBytes,
                pageCount = null,
                lastOutputPath = null,
                lastOutputSizeBytes = null,
                statusMessage = "Loading PDF details...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching { pdfCompressor.getPageCount(uri) }
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

    fun onOutputNameChanged(value: String) {
        _uiState.update { it.copy(outputName = value) }
    }

    fun onLevelChanged(level: PdfCompressionLevel) {
        _uiState.update { it.copy(compressionLevel = level) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun compress() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Compressing PDF...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfCompressor.compress(
                    inputUri = uri,
                    outputName = state.outputName,
                    level = state.compressionLevel
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF ${state.compressionLevel.name}",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Compress",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPath = result.outputFile.absolutePath,
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Compression complete (${result.pageCount} pages)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Compression failed"
                    )
                }
            }
        }
    }
}

class PdfCompressViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val pdfCompressor: PdfCompressor,
    private val defaultCompressionLevel: PdfCompressionLevel = PdfCompressionLevel.MEDIUM
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfCompressViewModel::class.java)) {
            return PdfCompressViewModel(
                historyRepository = historyRepository,
                pdfCompressor = pdfCompressor,
                defaultCompressionLevel = defaultCompressionLevel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
