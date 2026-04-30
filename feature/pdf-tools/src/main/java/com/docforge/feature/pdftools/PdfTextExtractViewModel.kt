package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfTextExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfTextExtractViewModel(
    private val historyRepository: HistoryRepository,
    private val pdfTextExtractor: PdfTextExtractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfTextExtractUiState())
    val uiState: StateFlow<PdfTextExtractUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                pageCount = null,
                isProcessing = false,
                lastOutputPath = null,
                lastOutputSizeBytes = null,
                lastExtractedChars = null,
                statusMessage = "Loading PDF details...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching { pdfTextExtractor.getPageCount(uri) }
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun extractText() {
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
                    statusMessage = "Extracting text...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null,
                    lastExtractedChars = null
                )
            }

            runCatching {
                pdfTextExtractor.extractToTxt(
                    inputUri = uri,
                    outputName = state.outputName
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF text (${result.pageCount} pages)",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF -> TXT",
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
                        lastExtractedChars = result.extractedChars,
                        statusMessage = "Text extracted (${result.extractedChars} chars)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Text extraction failed"
                    )
                }
            }
        }
    }
}

class PdfTextExtractViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val pdfTextExtractor: PdfTextExtractor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfTextExtractViewModel::class.java)) {
            return PdfTextExtractViewModel(historyRepository, pdfTextExtractor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
