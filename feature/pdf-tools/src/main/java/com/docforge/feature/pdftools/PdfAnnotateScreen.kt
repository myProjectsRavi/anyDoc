package com.docforge.feature.pdftools

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.core.pdf.PdfAnnotationType
import com.docforge.core.pdf.PdfFreehandPoint

@Composable
fun PdfAnnotateRoute(
    viewModel: PdfAnnotateViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val label = readLabel(context, uri)
            viewModel.onInputSelected(uri, label)
        }
    }

    val freehandPoints = remember { mutableStateListOf<Offset?>() }
    val freehandCanvasWidth = remember { mutableIntStateOf(0) }
    val freehandCanvasHeight = remember { mutableIntStateOf(0) }

    PdfAnnotateScreen(
        state = state,
        paddingValues = paddingValues,
        freehandPoints = freehandPoints,
        freehandCanvasWidth = freehandCanvasWidth.intValue,
        freehandCanvasHeight = freehandCanvasHeight.intValue,
        onFreehandCanvasSizeChanged = { width, height ->
            freehandCanvasWidth.intValue = width
            freehandCanvasHeight.intValue = height
        },
        onClearFreehand = { freehandPoints.clear() },
        onPickPdf = { picker.launch("application/pdf") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onTypeChanged = viewModel::onTypeChanged,
        onPageChanged = viewModel::onPageChanged,
        onXRatioChanged = viewModel::onXRatioChanged,
        onYRatioChanged = viewModel::onYRatioChanged,
        onWidthRatioChanged = viewModel::onWidthRatioChanged,
        onHeightRatioChanged = viewModel::onHeightRatioChanged,
        onTextChanged = viewModel::onTextChanged,
        onAddAnnotation = viewModel::addAnnotation,
        onAddFreehandAnnotation = {
            val points = buildFreehandPoints(
                points = freehandPoints,
                width = freehandCanvasWidth.intValue,
                height = freehandCanvasHeight.intValue
            )
            viewModel.addFreehandAnnotation(points)
            freehandPoints.clear()
        },
        onRemoveAnnotation = viewModel::removeAnnotation,
        onClearAnnotations = viewModel::clearAnnotations,
        onExport = viewModel::exportAnnotatedPdf,
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfAnnotateScreen(
    state: PdfAnnotateUiState,
    paddingValues: PaddingValues,
    freehandPoints: MutableList<Offset?>,
    freehandCanvasWidth: Int,
    freehandCanvasHeight: Int,
    onFreehandCanvasSizeChanged: (Int, Int) -> Unit,
    onClearFreehand: () -> Unit,
    onPickPdf: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onTypeChanged: (PdfAnnotationType) -> Unit,
    onPageChanged: (String) -> Unit,
    onXRatioChanged: (String) -> Unit,
    onYRatioChanged: (String) -> Unit,
    onWidthRatioChanged: (String) -> Unit,
    onHeightRatioChanged: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onAddAnnotation: () -> Unit,
    onAddFreehandAnnotation: () -> Unit,
    onRemoveAnnotation: (Int) -> Unit,
    onClearAnnotations: () -> Unit,
    onExport: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("PDF Annotate", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Add highlight, text, sticky-note, and freehand annotations fully offline. Exported PDF is flattened.")

        Button(onClick = onPickPdf, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select PDF" else "Replace PDF")
        }

        state.inputLabel?.let { Text("Input: $it", style = MaterialTheme.typography.bodySmall) }
        state.pageCount?.let { Text("Pages: $it", style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Annotation Type", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PdfAnnotationType.entries.forEach { type ->
                Button(
                    onClick = { onTypeChanged(type) },
                    enabled = !state.isProcessing
                ) {
                    val selected = if (state.selectedType == type) "*" else ""
                    Text("${type.name}$selected")
                }
            }
        }

        OutlinedTextField(
            value = state.pageInput,
            onValueChange = onPageChanged,
            label = { Text("Page (1-based)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.selectedType == PdfAnnotationType.FREEHAND) {
            Text("Freehand Canvas", fontWeight = FontWeight.SemiBold)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .background(Color.White)
                    .onSizeChanged { size ->
                        onFreehandCanvasSizeChanged(size.width, size.height)
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> freehandPoints.add(offset) },
                            onDrag = { change, _ ->
                                freehandPoints.add(change.position)
                                change.consume()
                            },
                            onDragEnd = { freehandPoints.add(null) },
                            onDragCancel = { freehandPoints.add(null) }
                        )
                    }
            ) {
                val path = Path()
                var segmentStarted = false
                freehandPoints.forEach { p ->
                    if (p == null) {
                        segmentStarted = false
                    } else {
                        if (!segmentStarted) {
                            path.moveTo(p.x, p.y)
                            segmentStarted = true
                        } else {
                            path.lineTo(p.x, p.y)
                        }
                    }
                }
                drawPath(path = path, color = Color.Black, style = Stroke(width = 4f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClearFreehand, enabled = !state.isProcessing) {
                    Text("Clear Strokes")
                }
                Button(onClick = onAddFreehandAnnotation, enabled = !state.isProcessing) {
                    Text("Add Freehand")
                }
            }

            if (freehandCanvasWidth <= 0 || freehandCanvasHeight <= 0) {
                Text("Freehand pad initializing...", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            OutlinedTextField(
                value = state.xRatioInput,
                onValueChange = onXRatioChanged,
                label = { Text("X Ratio (0-1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.yRatioInput,
                onValueChange = onYRatioChanged,
                label = { Text("Y Ratio (0-1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.widthRatioInput,
                onValueChange = onWidthRatioChanged,
                label = { Text("Width Ratio") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.heightRatioInput,
                onValueChange = onHeightRatioChanged,
                label = { Text("Height Ratio") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.selectedType != PdfAnnotationType.HIGHLIGHT) {
                OutlinedTextField(
                    value = state.textInput,
                    onValueChange = onTextChanged,
                    label = { Text("Annotation Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(onClick = onAddAnnotation, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
                Text("Add Annotation")
            }
        }

        if (state.pendingAnnotations.isNotEmpty()) {
            Text("Pending (${state.pendingAnnotations.size})", fontWeight = FontWeight.SemiBold)
            state.pendingAnnotations.forEachIndexed { index, command ->
                Text(
                    text = "${index + 1}. P${command.pageOneBased} ${command.type.name} @ (${formatRatio(command.xRatio)}, ${formatRatio(command.yRatio)})",
                    style = MaterialTheme.typography.bodySmall
                )
                if (command.type == PdfAnnotationType.FREEHAND) {
                    val pointsCount = command.freehandPoints.count { !it.isBreak }
                    Text("Freehand points: $pointsCount", style = MaterialTheme.typography.bodySmall)
                }
                if (command.text.isNotBlank()) {
                    Text("Text: ${command.text}", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { onRemoveAnnotation(index) }, enabled = !state.isProcessing) {
                    Text("Remove ${index + 1}")
                }
            }
            Button(onClick = onClearAnnotations, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
                Text("Clear Pending")
            }
        }

        Button(onClick = onExport, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Export Annotated PDF")
            }
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) { Text("Dismiss Error") }
        }

        state.lastOutputPath?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
        state.lastOutputSizeBytes?.let { Text("Output size: $it bytes", style = MaterialTheme.typography.bodySmall) }
    }
}

private fun readLabel(context: Context, uri: Uri): String? {
    return DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
}

private fun formatRatio(value: Float): String {
    return "%.2f".format(value)
}

private fun buildFreehandPoints(
    points: List<Offset?>,
    width: Int,
    height: Int
): List<PdfFreehandPoint> {
    if (width <= 0 || height <= 0) return emptyList()
    if (points.none { it != null }) return emptyList()

    return points.map { point ->
        if (point == null) {
            PdfFreehandPoint(xRatio = 0f, yRatio = 0f, isBreak = true)
        } else {
            PdfFreehandPoint(
                xRatio = (point.x / width.toFloat()).coerceIn(0f, 1f),
                yRatio = (point.y / height.toFloat()).coerceIn(0f, 1f),
                isBreak = false
            )
        }
    }
}
