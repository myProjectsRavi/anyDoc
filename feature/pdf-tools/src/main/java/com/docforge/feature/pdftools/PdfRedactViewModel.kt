package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfRedactionOptions
import com.docforge.core.pdf.PdfRedactionTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfRedactViewModel(
    private val historyRepository: HistoryRepository,
    private val redactionTool: PdfRedactionTool
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfRedactUiState())
    val uiState: StateFlow<PdfRedactUiState> = _uiState.asStateFlow()

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
                removedTextOperatorCount = null,
                clearedFormFieldCount = null
            )
        }
    }

    fun onOutputNameChanged(value: String) = _uiState.update { it.copy(outputName = value) }
    fun onTermsTextChanged(value: String) = _uiState.update { it.copy(termsText = value) }
    fun onCaseSensitiveChanged(value: Boolean) = _uiState.update { it.copy(caseSensitive = value) }
    fun onScrubMetadataChanged(value: Boolean) = _uiState.update { it.copy(scrubMetadata = value) }
    fun onScrubFormValuesChanged(value: Boolean) = _uiState.update { it.copy(scrubFormValues = value) }
    fun onVerifyIrreversibleChanged(value: Boolean) = _uiState.update { it.copy(verifyIrreversible = value) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun redact() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select a PDF first.") }
            return
        }
        if (state.isProcessing) return

        val terms = state.termsText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (terms.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter one redaction term per line.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Preparing irreversible redaction...",
                    errorMessage = null,
                    progressStage = "Preparing irreversible redaction",
                    progressCurrent = 0,
                    progressTotal = 0,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null,
                    removedTextOperatorCount = null,
                    clearedFormFieldCount = null
                )
            }

            runCatching {
                redactionTool.redact(
                    inputUri = uri,
                    outputName = state.outputName,
                    options = PdfRedactionOptions(
                        terms = terms,
                        caseSensitive = state.caseSensitive,
                        scrubMetadata = state.scrubMetadata,
                        scrubFormValues = state.scrubFormValues,
                        verifyIrreversible = state.verifyIrreversible
                    ),
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
                        sourceLabel = state.inputLabel ?: "PDF redaction",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Redact",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Redaction complete and verified.",
                        errorMessage = null,
                        progressStage = null,
                        progressCurrent = 0,
                        progressTotal = 0,
                        lastOutputPath = result.outputFile.absolutePath,
                        lastOutputSizeBytes = result.outputSizeBytes,
                        removedTextOperatorCount = result.removedTextOperatorCount,
                        clearedFormFieldCount = result.clearedFormFieldCount
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = error.message ?: "Redaction failed",
                        progressStage = null
                    )
                }
            }
        }
    }
}

class PdfRedactViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val redactionTool: PdfRedactionTool
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfRedactViewModel::class.java)) {
            return PdfRedactViewModel(historyRepository, redactionTool) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
