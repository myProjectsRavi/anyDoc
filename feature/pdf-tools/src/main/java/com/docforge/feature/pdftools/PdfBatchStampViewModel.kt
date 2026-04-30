package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfBatchStampOptions
import com.docforge.core.pdf.PdfBatchStampTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfBatchStampViewModel(
    private val historyRepository: HistoryRepository,
    private val batchStampTool: PdfBatchStampTool
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfBatchStampUiState())
    val uiState: StateFlow<PdfBatchStampUiState> = _uiState.asStateFlow()

    fun onInputsSelected(uris: List<Uri>, labels: List<String>) {
        if (uris.isEmpty()) return
        _uiState.update {
            it.copy(
                selectedUris = uris,
                inputLabels = labels,
                statusMessage = "Selected ${uris.size} PDF file(s)",
                errorMessage = null,
                lastOutputPaths = emptyList(),
                lastOutputSizeBytes = null
            )
        }
    }

    fun onOutputBaseNameChanged(value: String) = _uiState.update { it.copy(outputBaseName = value) }
    fun onWatermarkTextChanged(value: String) = _uiState.update { it.copy(watermarkText = value) }
    fun onEnableWatermarkChanged(value: Boolean) = _uiState.update { it.copy(enableWatermark = value) }
    fun onEnableBatesChanged(value: Boolean) = _uiState.update { it.copy(enableBates = value) }
    fun onBatesPrefixChanged(value: String) = _uiState.update { it.copy(batesPrefix = value) }
    fun onBatesStartChanged(value: String) = _uiState.update { it.copy(batesStart = value) }
    fun onBatesPaddingChanged(value: Int) = _uiState.update { it.copy(batesPadding = value.coerceIn(1, 10)) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun applyStamping() {
        val state = _uiState.value
        if (state.selectedUris.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select one or more PDFs first.") }
            return
        }
        if (state.isProcessing) return

        val batesStart = state.batesStart.toIntOrNull()?.coerceAtLeast(1)
        if (batesStart == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid Bates start number.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Applying watermark/Bates stamps...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                batchStampTool.stampBatch(
                    inputUris = state.selectedUris,
                    outputBaseName = state.outputBaseName,
                    options = PdfBatchStampOptions(
                        watermarkText = state.watermarkText,
                        watermarkEnabled = state.enableWatermark,
                        batesEnabled = state.enableBates,
                        batesPrefix = state.batesPrefix,
                        batesStart = batesStart,
                        batesPadding = state.batesPadding
                    )
                )
            }.onSuccess { result ->
                result.outputs.forEach { output ->
                    historyRepository.insert(
                        ConversionRecord(
                            sourceLabel = "Batch stamp (${state.selectedUris.size} PDFs)",
                            outputPath = output.outputFile.absolutePath,
                            operation = "PDF Batch Stamp",
                            createdAtMillis = System.currentTimeMillis(),
                            inputCount = 1,
                            outputSizeBytes = output.outputSizeBytes
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Stamped ${result.outputs.size} PDF(s), ${result.totalPages} page(s)",
                        errorMessage = null,
                        lastOutputPaths = result.outputs.map { file -> file.outputFile.absolutePath },
                        lastOutputSizeBytes = result.totalOutputSizeBytes
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = error.message ?: "Batch stamping failed"
                    )
                }
            }
        }
    }
}

class PdfBatchStampViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val batchStampTool: PdfBatchStampTool
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfBatchStampViewModel::class.java)) {
            return PdfBatchStampViewModel(historyRepository, batchStampTool) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
