package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfPasswordTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfPasswordViewModel(
    private val historyRepository: HistoryRepository,
    private val pdfPasswordTool: PdfPasswordTool
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfPasswordUiState())
    val uiState: StateFlow<PdfPasswordUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                isEncrypted = null,
                isProcessing = false,
                lastOutputPath = null,
                lastOutputSizeBytes = null,
                statusMessage = "Inspecting PDF security...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching { pdfPasswordTool.isEncrypted(uri) }
                .onSuccess { encrypted ->
                    _uiState.update {
                        it.copy(
                            isEncrypted = encrypted,
                            statusMessage = if (encrypted) "PDF is password protected" else "PDF is not password protected",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isEncrypted = null,
                            statusMessage = null,
                            errorMessage = err.message ?: "Failed to inspect PDF"
                        )
                    }
                }
        }
    }

    fun onModeChanged(mode: PdfPasswordMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun onOutputNameChanged(value: String) {
        _uiState.update { it.copy(outputName = value) }
    }

    fun onUserPasswordChanged(value: String) {
        _uiState.update { it.copy(userPasswordInput = value) }
    }

    fun onOwnerPasswordChanged(value: String) {
        _uiState.update { it.copy(ownerPasswordInput = value) }
    }

    fun onUnlockPasswordChanged(value: String) {
        _uiState.update { it.copy(unlockPasswordInput = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun applyPasswordAction() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        when (state.mode) {
            PdfPasswordMode.PROTECT -> {
                if (state.userPasswordInput.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "User password is required.") }
                    return
                }
            }
            PdfPasswordMode.UNLOCK -> {
                if (state.unlockPasswordInput.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Enter the current PDF password.") }
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = if (state.mode == PdfPasswordMode.PROTECT) "Applying password protection..." else "Removing password protection...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null
                )
            }

            val result = runCatching {
                when (state.mode) {
                    PdfPasswordMode.PROTECT -> pdfPasswordTool.protect(
                        inputUri = uri,
                        outputName = state.outputName,
                        userPassword = state.userPasswordInput,
                        ownerPassword = state.ownerPasswordInput.ifBlank { state.userPasswordInput }
                    )
                    PdfPasswordMode.UNLOCK -> pdfPasswordTool.removePassword(
                        inputUri = uri,
                        outputName = state.outputName,
                        password = state.unlockPasswordInput
                    )
                }
            }

            result.onSuccess { output ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF password",
                        outputPath = output.outputFile.absolutePath,
                        operation = if (state.mode == PdfPasswordMode.PROTECT) "PDF Protect" else "PDF Unlock",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = output.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPath = output.outputFile.absolutePath,
                        lastOutputSizeBytes = output.outputSizeBytes,
                        statusMessage = if (state.mode == PdfPasswordMode.PROTECT) {
                            "Password-protected PDF saved"
                        } else {
                            "Unlocked PDF saved"
                        },
                        errorMessage = null
                    )
                }

                runCatching { pdfPasswordTool.isEncrypted(uri) }
                    .onSuccess { encrypted ->
                        _uiState.update { it.copy(isEncrypted = encrypted) }
                    }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "PDF password operation failed"
                    )
                }
            }
        }
    }
}

class PdfPasswordViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val pdfPasswordTool: PdfPasswordTool
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfPasswordViewModel::class.java)) {
            return PdfPasswordViewModel(historyRepository, pdfPasswordTool) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
