package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfTranslationTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfTranslateViewModel(
    private val historyRepository: HistoryRepository,
    private val translationTool: PdfTranslationTool
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfTranslateUiState())
    val uiState: StateFlow<PdfTranslateUiState> = _uiState.asStateFlow()

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
                lastOutputPath = null,
                lastOutputSizeBytes = null,
                translatedLineCount = null
            )
        }
    }

    fun onOutputNameChanged(value: String) = _uiState.update { it.copy(outputName = value) }
    fun onSourceLanguageChanged(value: String) = _uiState.update { it.copy(sourceLanguageTag = value) }
    fun onTargetLanguageChanged(value: String) = _uiState.update { it.copy(targetLanguageTag = value) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun translate() {
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
                    statusMessage = "Translating with layout preservation...",
                    errorMessage = null,
                    progressStage = "Translating with layout preservation...",
                    progressCurrent = 0,
                    progressTotal = 0,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null,
                    translatedLineCount = null
                )
            }

            runCatching {
                translationTool.translatePdfWithLayout(
                    inputUri = uri,
                    outputName = state.outputName,
                    sourceLanguageTag = state.sourceLanguageTag,
                    targetLanguageTag = state.targetLanguageTag,
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
                        sourceLabel = state.inputLabel ?: "PDF translation",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Translate",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Translation complete (${result.translatedLineCount} lines)",
                        errorMessage = null,
                        progressStage = null,
                        progressCurrent = 0,
                        progressTotal = 0,
                        lastOutputPath = result.outputFile.absolutePath,
                        lastOutputSizeBytes = result.outputSizeBytes,
                        translatedLineCount = result.translatedLineCount
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = error.message ?: "Translation failed",
                        progressStage = null
                    )
                }
            }
        }
    }
}

class PdfTranslateViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val translationTool: PdfTranslationTool
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfTranslateViewModel::class.java)) {
            return PdfTranslateViewModel(historyRepository, translationTool) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
