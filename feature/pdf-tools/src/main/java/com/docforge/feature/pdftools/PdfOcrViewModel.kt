package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfOcrTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfOcrViewModel(
    private val historyRepository: HistoryRepository,
    private val ocrTool: PdfOcrTool
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfOcrUiState())
    val uiState: StateFlow<PdfOcrUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                statusMessage = "Input selected",
                errorMessage = null,
                progressStage = null,
                progressCurrent = 0,
                progressTotal = 0,
                textOutputPath = null,
                searchablePdfPath = null,
                extractedTextPreview = null,
                extractedChars = null,
                lineCount = null
            )
        }
    }

    fun onOutputNameChanged(value: String) = _uiState.update { it.copy(outputName = value) }
    fun onCreateSearchablePdfChanged(enabled: Boolean) = _uiState.update { it.copy(createSearchablePdf = enabled) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun runOcr() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select a PDF or image first.") }
            return
        }
        if (state.isProcessing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Running offline OCR...",
                    errorMessage = null,
                    progressStage = "Running offline OCR...",
                    progressCurrent = 0,
                    progressTotal = 0,
                    textOutputPath = null,
                    searchablePdfPath = null,
                    extractedTextPreview = null,
                    extractedChars = null,
                    lineCount = null
                )
            }

            runCatching {
                ocrTool.process(
                    inputUri = uri,
                    outputName = state.outputName,
                    createSearchablePdf = state.createSearchablePdf,
                    onProgress = { progress ->
                        _uiState.update {
                            it.copy(
                                progressStage = progress.stage,
                                progressCurrent = progress.current,
                                progressTotal = progress.total,
                                statusMessage = if (progress.total > 0) {
                                    "${progress.stage}: ${progress.current}/${progress.total}"
                                } else {
                                    progress.stage
                                }
                            )
                        }
                    }
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = state.inputLabel ?: "OCR input",
                        outputPath = result.searchablePdfFile?.absolutePath ?: result.textOutputFile.absolutePath,
                        operation = "PDF OCR",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "OCR complete: ${result.lineCount} line(s)",
                        errorMessage = null,
                        progressStage = null,
                        progressCurrent = 0,
                        progressTotal = 0,
                        textOutputPath = result.textOutputFile.absolutePath,
                        searchablePdfPath = result.searchablePdfFile?.absolutePath,
                        extractedTextPreview = result.extractedText.take(8000),
                        extractedChars = result.extractedChars,
                        lineCount = result.lineCount
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = error.message ?: "OCR failed",
                        progressStage = null
                    )
                }
            }
        }
    }
}

class PdfOcrViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val ocrTool: PdfOcrTool
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfOcrViewModel::class.java)) {
            return PdfOcrViewModel(historyRepository, ocrTool) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
