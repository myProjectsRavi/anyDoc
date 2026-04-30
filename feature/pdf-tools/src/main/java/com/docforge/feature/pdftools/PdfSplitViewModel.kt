package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfSplitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfSplitViewModel(
    private val historyRepository: HistoryRepository,
    private val pdfSplitter: PdfSplitter
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfSplitUiState())
    val uiState: StateFlow<PdfSplitUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                pageCount = null,
                lastOutputPaths = emptyList(),
                lastOutputSizeBytes = null,
                statusMessage = "Loading PDF details...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching { pdfSplitter.getPageCount(uri) }
                .onSuccess { count ->
                    _uiState.update {
                        val endInput = if (count > 0) count.toString() else it.endPageInput
                        it.copy(
                            pageCount = count,
                            endPageInput = endInput,
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

    fun onOutputBaseNameChanged(name: String) {
        _uiState.update { it.copy(outputBaseName = name) }
    }

    fun onStartPageChanged(value: String) {
        _uiState.update { it.copy(startPageInput = value) }
    }

    fun onEndPageChanged(value: String) {
        _uiState.update { it.copy(endPageInput = value) }
    }

    fun onSplitEveryNChanged(value: String) {
        _uiState.update { it.copy(splitEveryNInput = value) }
    }

    fun onExtractPagesChanged(value: String) {
        _uiState.update { it.copy(extractPagesInput = value) }
    }

    fun onReorderPagesChanged(value: String) {
        _uiState.update { it.copy(reorderPagesInput = value) }
    }

    fun onDeletePagesChanged(value: String) {
        _uiState.update { it.copy(deletePagesInput = value) }
    }

    fun onRotatePagesChanged(value: String) {
        _uiState.update { it.copy(rotatePagesInput = value) }
    }

    fun onRotateDegreesChanged(value: Int) {
        val normalized = when (value) {
            90, 180, 270 -> value
            else -> 90
        }
        _uiState.update { it.copy(rotateDegrees = normalized) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun splitRange() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        val start = state.startPageInput.toIntOrNull()
        val end = state.endPageInput.toIntOrNull()
        if (start == null || end == null) {
            _uiState.update { it.copy(errorMessage = "Enter valid numeric start/end pages.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Splitting by range...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfSplitter.splitByRange(
                    inputUri = uri,
                    outputName = state.outputBaseName,
                    startPageOneBased = start,
                    endPageOneBased = end
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF range $start-$end",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Split Range",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPaths = listOf(result.outputFile.absolutePath),
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Split complete (${result.pageCount} pages)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Split failed"
                    )
                }
            }
        }
    }

    fun extractPages() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        val pages = parsePageExpression(
            raw = state.extractPagesInput,
            preserveOrder = false,
            unique = true
        )
        if (pages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter pages like 1,3,5-7") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Extracting pages...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfSplitter.extractPages(
                    inputUri = uri,
                    outputBaseName = state.outputBaseName,
                    pagesOneBased = pages
                )
            }.onSuccess { result ->
                val firstOutput = result.outputFiles.firstOrNull()?.absolutePath.orEmpty()
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF pages ${pages.joinToString(",")}",
                        outputPath = firstOutput,
                        operation = "PDF Extract Pages",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = pages.size,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPaths = result.outputFiles.map { file -> file.absolutePath },
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Extracted ${result.totalPagesExported} page file(s)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Extraction failed"
                    )
                }
            }
        }
    }

    fun splitEveryNPages() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        val pagesPerChunk = state.splitEveryNInput.toIntOrNull()
        if (pagesPerChunk == null || pagesPerChunk <= 0) {
            _uiState.update { it.copy(errorMessage = "Enter a valid positive number for pages per split.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Splitting every $pagesPerChunk pages...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfSplitter.splitEveryNPages(
                    inputUri = uri,
                    outputBaseName = state.outputBaseName,
                    pagesPerChunk = pagesPerChunk
                )
            }.onSuccess { result ->
                val firstOutput = result.outputFiles.firstOrNull()?.absolutePath.orEmpty()
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF every $pagesPerChunk pages",
                        outputPath = firstOutput,
                        operation = "PDF Split Every N",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = result.outputFiles.size,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPaths = result.outputFiles.map { file -> file.absolutePath },
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Created ${result.outputFiles.size} split file(s)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Split-every-N failed"
                    )
                }
            }
        }
    }

    fun splitByBookmarks() {
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
                    statusMessage = "Splitting by top-level bookmarks...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfSplitter.splitByBookmarks(
                    inputUri = uri,
                    outputBaseName = state.outputBaseName
                )
            }.onSuccess { result ->
                val firstOutput = result.outputFiles.firstOrNull()?.absolutePath.orEmpty()
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF bookmarks split",
                        outputPath = firstOutput,
                        operation = "PDF Split Bookmarks",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = result.outputFiles.size,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPaths = result.outputFiles.map { file -> file.absolutePath },
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Created ${result.outputFiles.size} bookmark split file(s)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Split-by-bookmarks failed"
                    )
                }
            }
        }
    }

    fun reorderPages() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        val orderedPages = parsePageExpression(
            raw = state.reorderPagesInput,
            preserveOrder = true,
            unique = false
        )
        if (orderedPages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter page order like 3,1,2 or 10-1") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Reordering pages...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfSplitter.reorderPages(
                    inputUri = uri,
                    outputName = state.outputBaseName,
                    orderedPagesOneBased = orderedPages
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF reorder ${state.reorderPagesInput.trim()}",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Reorder Pages",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = orderedPages.size,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPaths = listOf(result.outputFile.absolutePath),
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Reordered PDF created (${result.pageCount} pages)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Page reorder failed"
                    )
                }
            }
        }
    }

    fun deletePages() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        val pagesToDelete = parsePageExpression(
            raw = state.deletePagesInput,
            preserveOrder = false,
            unique = true
        )
        if (pagesToDelete.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter pages to delete like 2,4,8-10") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Deleting selected pages...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfSplitter.deletePages(
                    inputUri = uri,
                    outputName = state.outputBaseName,
                    pagesToDeleteOneBased = pagesToDelete
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF delete ${state.deletePagesInput.trim()}",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Delete Pages",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = pagesToDelete.size,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPaths = listOf(result.outputFile.absolutePath),
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Deleted pages and created PDF (${result.pageCount} pages)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Page deletion failed"
                    )
                }
            }
        }
    }

    fun rotatePages() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        val pagesToRotate = parsePageExpression(
            raw = state.rotatePagesInput,
            preserveOrder = false,
            unique = true
        )
        if (pagesToRotate.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter pages to rotate like 1,3,5-7") }
            return
        }
        if (state.rotateDegrees !in setOf(90, 180, 270)) {
            _uiState.update { it.copy(errorMessage = "Rotation must be 90, 180, or 270.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Rotating selected pages...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfSplitter.rotatePages(
                    inputUri = uri,
                    outputName = state.outputBaseName,
                    pagesToRotateOneBased = pagesToRotate,
                    degreesClockwise = state.rotateDegrees
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF rotate ${state.rotatePagesInput.trim()} by ${state.rotateDegrees}",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Rotate Pages",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = pagesToRotate.size,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastOutputPaths = listOf(result.outputFile.absolutePath),
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Rotated pages and created PDF (${result.pageCount} pages)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Page rotation failed"
                    )
                }
            }
        }
    }

    private fun parsePageExpression(
        raw: String,
        preserveOrder: Boolean,
        unique: Boolean
    ): List<Int> {
        if (raw.isBlank()) return emptyList()

        val orderedPages = mutableListOf<Int>()
        val uniquePages = linkedSetOf<Int>()
        val tokens = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        for (token in tokens) {
            if ('-' in token) {
                val pair = token.split('-', limit = 2)
                if (pair.size != 2) return emptyList()
                val start = pair[0].trim().toIntOrNull() ?: return emptyList()
                val end = pair[1].trim().toIntOrNull() ?: return emptyList()
                if (start <= 0 || end <= 0) return emptyList()

                val range = if (start <= end) {
                    start..end
                } else {
                    end..start
                }

                if (preserveOrder && start > end) {
                    for (page in range.reversed()) {
                        if (unique) uniquePages += page else orderedPages += page
                    }
                } else {
                    for (page in range) {
                        if (unique) uniquePages += page else orderedPages += page
                    }
                }
            } else {
                val number = token.toIntOrNull() ?: return emptyList()
                if (number <= 0) return emptyList()
                if (unique) uniquePages += number else orderedPages += number
            }
        }

        return when {
            preserveOrder && !unique -> orderedPages
            preserveOrder && unique -> uniquePages.toList()
            !preserveOrder && unique -> uniquePages.toList().sorted()
            else -> orderedPages.sorted()
        }
    }
}

class PdfSplitViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val pdfSplitter: PdfSplitter
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfSplitViewModel::class.java)) {
            return PdfSplitViewModel(historyRepository, pdfSplitter) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
