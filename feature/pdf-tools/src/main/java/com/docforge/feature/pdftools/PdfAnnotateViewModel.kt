package com.docforge.feature.pdftools

import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfAnnotationCommand
import com.docforge.core.pdf.PdfAnnotationType
import com.docforge.core.pdf.PdfAnnotator
import com.docforge.core.pdf.PdfFreehandPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfAnnotateViewModel(
    private val historyRepository: HistoryRepository,
    private val pdfAnnotator: PdfAnnotator
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfAnnotateUiState())
    val uiState: StateFlow<PdfAnnotateUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                pageCount = null,
                isProcessing = false,
                lastOutputPath = null,
                lastOutputSizeBytes = null,
                pendingAnnotations = emptyList(),
                statusMessage = "Loading PDF details...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching { pdfAnnotator.getPageCount(uri) }
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            pageCount = count,
                            pageInput = if (count > 0) "1" else it.pageInput,
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

    fun onTypeChanged(type: PdfAnnotationType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun onPageChanged(value: String) {
        _uiState.update { it.copy(pageInput = value) }
    }

    fun onXRatioChanged(value: String) {
        _uiState.update { it.copy(xRatioInput = value) }
    }

    fun onYRatioChanged(value: String) {
        _uiState.update { it.copy(yRatioInput = value) }
    }

    fun onWidthRatioChanged(value: String) {
        _uiState.update { it.copy(widthRatioInput = value) }
    }

    fun onHeightRatioChanged(value: String) {
        _uiState.update { it.copy(heightRatioInput = value) }
    }

    fun onTextChanged(value: String) {
        _uiState.update { it.copy(textInput = value) }
    }

    fun addAnnotation() {
        val state = _uiState.value
        if (state.selectedType == PdfAnnotationType.FREEHAND) {
            _uiState.update { it.copy(errorMessage = "Use the freehand canvas controls for FREEHAND annotations.") }
            return
        }
        val pageCount = state.pageCount ?: run {
            _uiState.update { it.copy(errorMessage = "Select a PDF first.") }
            return
        }

        val page = state.pageInput.toIntOrNull()
        val x = state.xRatioInput.toFloatOrNull()
        val y = state.yRatioInput.toFloatOrNull()
        val width = state.widthRatioInput.toFloatOrNull()
        val height = state.heightRatioInput.toFloatOrNull()

        if (page == null || x == null || y == null || width == null || height == null) {
            _uiState.update { it.copy(errorMessage = "Enter valid numeric page/position/size values.") }
            return
        }
        if (page !in 1..pageCount) {
            _uiState.update { it.copy(errorMessage = "Page must be between 1 and $pageCount.") }
            return
        }

        val text = when (state.selectedType) {
            PdfAnnotationType.HIGHLIGHT -> ""
            PdfAnnotationType.TEXT,
            PdfAnnotationType.STICKY_NOTE -> state.textInput.ifBlank { "Note" }
            PdfAnnotationType.FREEHAND -> ""
        }

        val color = when (state.selectedType) {
            PdfAnnotationType.HIGHLIGHT -> Color.YELLOW
            PdfAnnotationType.TEXT -> Color.rgb(255, 249, 196)
            PdfAnnotationType.STICKY_NOTE -> Color.rgb(255, 235, 59)
            PdfAnnotationType.FREEHAND -> Color.BLACK
        }

        val command = PdfAnnotationCommand(
            pageOneBased = page,
            type = state.selectedType,
            xRatio = x,
            yRatio = y,
            widthRatio = width,
            heightRatio = height,
            text = text,
            colorArgb = color
        )

        _uiState.update {
            it.copy(
                pendingAnnotations = it.pendingAnnotations + command,
                statusMessage = "Added ${state.selectedType.name} annotation on page $page.",
                errorMessage = null,
                textInput = if (state.selectedType == PdfAnnotationType.HIGHLIGHT) it.textInput else ""
            )
        }
    }

    fun addFreehandAnnotation(points: List<PdfFreehandPoint>) {
        val state = _uiState.value
        val pageCount = state.pageCount ?: run {
            _uiState.update { it.copy(errorMessage = "Select a PDF first.") }
            return
        }
        if (state.selectedType != PdfAnnotationType.FREEHAND) {
            _uiState.update { it.copy(errorMessage = "Switch annotation type to FREEHAND first.") }
            return
        }

        val page = state.pageInput.toIntOrNull()
        if (page == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid page number.") }
            return
        }
        if (page !in 1..pageCount) {
            _uiState.update { it.copy(errorMessage = "Page must be between 1 and $pageCount.") }
            return
        }
        if (points.none { !it.isBreak }) {
            _uiState.update { it.copy(errorMessage = "Draw freehand strokes before adding.") }
            return
        }

        val command = PdfAnnotationCommand(
            pageOneBased = page,
            type = PdfAnnotationType.FREEHAND,
            xRatio = 0f,
            yRatio = 0f,
            widthRatio = 1f,
            heightRatio = 1f,
            text = "",
            colorArgb = Color.BLACK,
            freehandPoints = points
        )

        _uiState.update {
            it.copy(
                pendingAnnotations = it.pendingAnnotations + command,
                statusMessage = "Added FREEHAND annotation on page $page.",
                errorMessage = null
            )
        }
    }

    fun removeAnnotation(index: Int) {
        val current = _uiState.value.pendingAnnotations
        if (index !in current.indices) return
        _uiState.update {
            it.copy(
                pendingAnnotations = current.toMutableList().also { list -> list.removeAt(index) },
                statusMessage = "Removed annotation ${index + 1}",
                errorMessage = null
            )
        }
    }

    fun clearAnnotations() {
        _uiState.update {
            it.copy(
                pendingAnnotations = emptyList(),
                statusMessage = "Cleared pending annotations",
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun exportAnnotatedPdf() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.pendingAnnotations.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one annotation first.") }
            return
        }
        if (state.isProcessing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Applying annotations...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                pdfAnnotator.annotate(
                    inputUri = uri,
                    outputName = state.outputName,
                    annotations = state.pendingAnnotations
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "PDF annotate (${state.pendingAnnotations.size} items)",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Annotate",
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
                        statusMessage = "Annotated PDF saved (${result.pageCount} pages)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "PDF annotation failed"
                    )
                }
            }
        }
    }
}

class PdfAnnotateViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val pdfAnnotator: PdfAnnotator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfAnnotateViewModel::class.java)) {
            return PdfAnnotateViewModel(historyRepository, pdfAnnotator) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
