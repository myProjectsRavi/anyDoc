package com.docforge.feature.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfPageSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TextPdfViewModel(
    private val historyRepository: HistoryRepository,
    private val textPdfConverter: TextPdfConverter,
    defaultPageSize: PdfPageSize = PdfPageSize.A4
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextPdfUiState(pageSize = defaultPageSize))
    val uiState: StateFlow<TextPdfUiState> = _uiState.asStateFlow()

    fun onTitleChanged(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onTextChanged(value: String) {
        _uiState.update { it.copy(text = value) }
    }

    fun onOutputNameChanged(value: String) {
        _uiState.update { it.copy(outputName = value) }
    }

    fun onPageSizeChanged(value: PdfPageSize) {
        _uiState.update { it.copy(pageSize = value) }
    }

    fun onFormatModeChanged(value: TextPdfFormatMode) {
        _uiState.update { it.copy(formatMode = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun convertToPdf() {
        val state = _uiState.value
        if (state.isProcessing) return

        if (state.text.trim().isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter text before creating a PDF.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Building PDF...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null,
                    lastPageCount = null,
                    lastParagraphCount = null,
                    lastCharacterCount = null
                )
            }

            runCatching {
                textPdfConverter.convert(
                    rawText = state.text,
                    outputName = state.outputName,
                    options = TextPdfConversionOptions(
                        pageSize = state.pageSize,
                        title = state.title,
                        formatMode = state.formatMode
                    )
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "Text (${result.characterCount} chars)",
                        outputPath = result.pdfResult.outputFile.absolutePath,
                        operation = "Text -> PDF",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.pdfResult.outputSizeBytes
                    )
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPath = result.pdfResult.outputFile.absolutePath,
                        lastOutputSizeBytes = result.pdfResult.outputSizeBytes,
                        lastPageCount = result.pdfResult.pageCount,
                        lastParagraphCount = result.paragraphCount,
                        lastCharacterCount = result.characterCount,
                        statusMessage = "Created PDF (${result.pdfResult.pageCount} page(s))",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Failed to convert text to PDF"
                    )
                }
            }
        }
    }
}

class TextPdfViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val textPdfConverter: TextPdfConverter,
    private val defaultPageSize: PdfPageSize = PdfPageSize.A4
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TextPdfViewModel::class.java)) {
            return TextPdfViewModel(
                historyRepository = historyRepository,
                textPdfConverter = textPdfConverter,
                defaultPageSize = defaultPageSize
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
