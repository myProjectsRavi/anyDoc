package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfMergeOptions
import com.docforge.core.pdf.PdfMergePageSizeMode
import com.docforge.core.pdf.PdfMerger
import com.docforge.core.ui.model.toStableUriRefList
import com.docforge.core.ui.model.toUriList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfToolsViewModel(
    private val historyRepository: HistoryRepository,
    private val pdfMerger: PdfMerger
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfToolsUiState())
    val uiState: StateFlow<PdfToolsUiState> = _uiState.asStateFlow()

    fun onFilesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val stableUris = uris.toStableUriRefList()
        _uiState.update {
            it.copy(
                selectedUris = stableUris,
                statusMessage = "${uris.size} source file(s) selected",
                errorMessage = null
            )
        }
    }

    fun remove(index: Int) {
        _uiState.update { state ->
            if (index !in state.selectedUris.indices) return@update state
            val updated = state.selectedUris.toMutableList().apply { removeAt(index) }.toPersistentList()
            state.copy(selectedUris = updated, statusMessage = "Removed item ${index + 1}")
        }
    }

    fun moveUp(index: Int) {
        _uiState.update { state ->
            if (index <= 0 || index !in state.selectedUris.indices) return@update state
            val updated = state.selectedUris.toMutableList()
            val tmp = updated[index - 1]
            updated[index - 1] = updated[index]
            updated[index] = tmp
            state.copy(selectedUris = updated.toPersistentList())
        }
    }

    fun moveDown(index: Int) {
        _uiState.update { state ->
            if (index < 0 || index >= state.selectedUris.lastIndex) return@update state
            val updated = state.selectedUris.toMutableList()
            val tmp = updated[index + 1]
            updated[index + 1] = updated[index]
            updated[index] = tmp
            state.copy(selectedUris = updated.toPersistentList())
        }
    }

    fun onOutputNameChanged(name: String) {
        _uiState.update { it.copy(outputName = name) }
    }

    fun onMergeTitleChanged(value: String) {
        _uiState.update { it.copy(mergeTitle = value) }
    }

    fun onMergeAuthorChanged(value: String) {
        _uiState.update { it.copy(mergeAuthor = value) }
    }

    fun onMergeSubjectChanged(value: String) {
        _uiState.update { it.copy(mergeSubject = value) }
    }

    fun onAddBookmarksChanged(enabled: Boolean) {
        _uiState.update { it.copy(addBookmarks = enabled) }
    }

    fun onMergePageSizeModeChanged(mode: PdfMergePageSizeMode) {
        _uiState.update { it.copy(mergePageSizeMode = mode) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun mergeSelectedPdfs() {
        val state = _uiState.value
        if (state.selectedUris.size < 2) {
            _uiState.update { it.copy(errorMessage = "Select at least 2 PDF/image files to merge.") }
            return
        }
        if (state.isMerging) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isMerging = true, statusMessage = "Merging sources...", errorMessage = null)
            }

            runCatching {
                pdfMerger.merge(
                    inputUris = state.selectedUris.toUriList(),
                    outputName = state.outputName,
                    options = PdfMergeOptions(
                        title = state.mergeTitle.trim(),
                        author = state.mergeAuthor.trim(),
                        subject = state.mergeSubject.trim(),
                        addSourceBookmarks = state.addBookmarks,
                        pageSizeMode = state.mergePageSizeMode
                    )
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "Merged sources (${state.selectedUris.size})",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Merge",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = state.selectedUris.size,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )
                _uiState.update {
                    it.copy(
                        isMerging = false,
                        lastOutputPath = result.outputFile.absolutePath,
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Merge complete (${result.pageCount} pages)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMerging = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Merge failed"
                    )
                }
            }
        }
    }
}

class PdfToolsViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val pdfMerger: PdfMerger
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfToolsViewModel::class.java)) {
            return PdfToolsViewModel(historyRepository, pdfMerger) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
