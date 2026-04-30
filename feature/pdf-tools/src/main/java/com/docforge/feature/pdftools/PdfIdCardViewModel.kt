package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfIdCardTool
import com.docforge.core.pdf.PdfPageSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfIdCardViewModel(
    private val historyRepository: HistoryRepository,
    private val idCardTool: PdfIdCardTool
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfIdCardUiState())
    val uiState: StateFlow<PdfIdCardUiState> = _uiState.asStateFlow()

    fun onFrontSelected(uri: Uri, label: String?) {
        _uiState.update {
            it.copy(frontUri = uri, frontLabel = label ?: uri.lastPathSegment ?: uri.toString(), errorMessage = null)
        }
    }

    fun onBackSelected(uri: Uri, label: String?) {
        _uiState.update {
            it.copy(backUri = uri, backLabel = label ?: uri.lastPathSegment ?: uri.toString(), errorMessage = null)
        }
    }

    fun onOutputNameChanged(value: String) = _uiState.update { it.copy(outputName = value) }
    fun onPageSizeChanged(size: PdfPageSize) = _uiState.update { it.copy(pageSize = size) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun exportIdSheet() {
        val state = _uiState.value
        val front = state.frontUri
        val back = state.backUri
        if (front == null || back == null) {
            _uiState.update { it.copy(errorMessage = "Select both front and back images.") }
            return
        }
        if (state.isProcessing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Building ID card sheet...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                idCardTool.createFrontBackSheet(
                    frontImageUri = front,
                    backImageUri = back,
                    outputName = state.outputName,
                    pageSize = state.pageSize
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "ID Card sheet",
                        outputPath = result.outputFile.absolutePath,
                        operation = "ID Card -> PDF",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 2,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "ID sheet exported",
                        errorMessage = null,
                        lastOutputPath = result.outputFile.absolutePath,
                        lastOutputSizeBytes = result.outputSizeBytes
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = error.message ?: "ID sheet export failed"
                    )
                }
            }
        }
    }
}

class PdfIdCardViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val idCardTool: PdfIdCardTool
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfIdCardViewModel::class.java)) {
            return PdfIdCardViewModel(historyRepository, idCardTool) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
