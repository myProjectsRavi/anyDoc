package com.docforge.feature.converter

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DocumentPdfViewModel(
    private val historyRepository: HistoryRepository,
    private val documentPdfConverter: DocumentPdfConverter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentPdfUiState())
    val uiState: StateFlow<DocumentPdfUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?) {
        val detectedType = documentPdfConverter.detectInputType(uri, labelHint)
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                inputType = detectedType,
                isProcessing = false,
                lastOutputPath = null,
                lastOutputSizeBytes = null,
                lastPageCount = null,
                lastLineCount = null,
                statusMessage = "Ready to convert ${detectedType.name} to PDF",
                errorMessage = null
            )
        }
    }

    fun onOutputNameChanged(value: String) {
        _uiState.update { it.copy(outputName = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun convertToPdf() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one document first.") }
            return
        }
        if (state.isProcessing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Converting document to PDF...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null,
                    lastPageCount = null,
                    lastLineCount = null
                )
            }

            runCatching {
                documentPdfConverter.convertToPdf(
                    inputUri = uri,
                    inputNameHint = state.inputLabel,
                    outputName = state.outputName
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = state.inputLabel ?: result.inputType.name,
                        outputPath = result.pdfResult.outputFile.absolutePath,
                        operation = "${result.inputType.name} -> PDF",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.pdfResult.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        inputType = result.inputType,
                        lastOutputPath = result.pdfResult.outputFile.absolutePath,
                        lastOutputSizeBytes = result.pdfResult.outputSizeBytes,
                        lastPageCount = result.pdfResult.pageCount,
                        lastLineCount = result.lineCount,
                        statusMessage = "Created PDF (${result.pdfResult.pageCount} page(s))",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Document conversion failed"
                    )
                }
            }
        }
    }
}

class DocumentPdfViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val documentPdfConverter: DocumentPdfConverter
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentPdfViewModel::class.java)) {
            return DocumentPdfViewModel(historyRepository, documentPdfConverter) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
