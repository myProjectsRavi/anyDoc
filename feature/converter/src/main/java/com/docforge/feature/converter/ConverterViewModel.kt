package com.docforge.feature.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfCreationOptions
import com.docforge.core.pdf.PdfCreator
import com.docforge.core.pdf.PdfPageSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConverterViewModel(
    private val pdfCreator: PdfCreator,
    private val historyRepository: HistoryRepository,
    defaultPageSize: PdfPageSize = PdfPageSize.A4
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState(pageSize = defaultPageSize))
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    fun onImagesSelected(uris: List<android.net.Uri>) {
        _uiState.update {
            it.copy(
                selectedUris = uris,
                statusMessage = if (uris.isNotEmpty()) "${uris.size} file(s) selected" else null,
                errorMessage = null
            )
        }
    }

    fun onOutputNameChanged(name: String) {
        _uiState.update { it.copy(outputName = name) }
    }

    fun onPageSizeChanged(pageSize: PdfPageSize) {
        _uiState.update { it.copy(pageSize = pageSize) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun convertImagesToPdf() {
        val state = uiState.value
        if (state.selectedUris.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select at least one image first.") }
            return
        }
        if (state.isConverting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isConverting = true, errorMessage = null, statusMessage = "Converting...") }
            runCatching {
                pdfCreator.createPdfFromImages(
                    imageUris = state.selectedUris,
                    outputName = state.outputName,
                    options = PdfCreationOptions(pageSize = state.pageSize)
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "Images (${state.selectedUris.size})",
                        outputPath = result.outputFile.absolutePath,
                        operation = "Images -> PDF",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = state.selectedUris.size,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        lastOutputPath = result.outputFile.absolutePath,
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "PDF created (${result.pageCount} pages)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        errorMessage = err.message ?: "Conversion failed",
                        statusMessage = null
                    )
                }
            }
        }
    }
}

class ConverterViewModelFactory(
    private val pdfCreator: PdfCreator,
    private val historyRepository: HistoryRepository,
    private val defaultPageSize: PdfPageSize = PdfPageSize.A4
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConverterViewModel::class.java)) {
            return ConverterViewModel(
                pdfCreator = pdfCreator,
                historyRepository = historyRepository,
                defaultPageSize = defaultPageSize
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
