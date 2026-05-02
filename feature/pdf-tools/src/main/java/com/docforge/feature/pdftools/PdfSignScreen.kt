package com.docforge.feature.pdftools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.graphics.Path as AndroidPath
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PdfSignRoute(
    viewModel: PdfSignViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onInputSelected(uri, null)
        }
    }

    val points = remember { mutableStateListOf<Offset?>() }
    val canvasWidth = remember { mutableIntStateOf(0) }
    val canvasHeight = remember { mutableIntStateOf(0) }

    PdfSignScreen(
        state = state,
        paddingValues = paddingValues,
        points = points,
        canvasWidth = canvasWidth.intValue,
        canvasHeight = canvasHeight.intValue,
        onCanvasSizeChanged = { width, height ->
            canvasWidth.intValue = width
            canvasHeight.intValue = height
        },
        onClearSignature = { points.clear() },
        onPickPdf = { picker.launch("application/pdf") },
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onTargetPageChanged = viewModel::onTargetPageChanged,
        onXRatioChanged = viewModel::onXRatioChanged,
        onYRatioChanged = viewModel::onYRatioChanged,
        onWidthRatioChanged = viewModel::onWidthRatioChanged,
        onTemplateNameChanged = viewModel::onTemplateNameChanged,
        onSaveTemplate = viewModel::savePlacementTemplate,
        onLoadTemplate = viewModel::loadPlacementTemplate,
        onDeleteTemplate = viewModel::deletePlacementTemplate,
        onAddPlacement = viewModel::addPlacementFromInputs,
        onApplyPlacementToAll = viewModel::applyPlacementToAllPagesFromInputs,
        onRemovePlacement = viewModel::removePlacementAt,
        onLoadPlacement = viewModel::loadPlacementIntoInputs,
        onClearPlacements = viewModel::clearPlacements,
        onSaveSignatureSlot = { slot ->
            val signature = buildSignatureBitmap(
                points = points,
                width = canvasWidth.intValue,
                height = canvasHeight.intValue
            )
            if (signature == null) {
                viewModel.setError("Draw a signature before saving to a slot.")
            } else {
                viewModel.saveSignatureToSlot(slot, signature)
            }
        },
        onUseSignatureSlot = viewModel::signWithSavedSignature,
        onDeleteSignatureSlot = viewModel::deleteSignatureSlot,
        onSign = {
            val signature = buildSignatureBitmap(
                points = points,
                width = canvasWidth.intValue,
                height = canvasHeight.intValue
            )
            if (signature == null) {
                viewModel.setError("Draw a signature before signing.")
            } else {
                viewModel.signPdf(signature)
            }
        },
        onClearError = viewModel::clearError
    )
}

@Composable
fun PdfSignScreen(
    state: PdfSignUiState,
    paddingValues: PaddingValues,
    points: MutableList<Offset?>,
    canvasWidth: Int,
    canvasHeight: Int,
    onCanvasSizeChanged: (Int, Int) -> Unit,
    onClearSignature: () -> Unit,
    onPickPdf: () -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onTargetPageChanged: (String) -> Unit,
    onXRatioChanged: (String) -> Unit,
    onYRatioChanged: (String) -> Unit,
    onWidthRatioChanged: (String) -> Unit,
    onTemplateNameChanged: (String) -> Unit,
    onSaveTemplate: () -> Unit,
    onLoadTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onAddPlacement: () -> Unit,
    onApplyPlacementToAll: () -> Unit,
    onRemovePlacement: (Int) -> Unit,
    onLoadPlacement: (Int) -> Unit,
    onClearPlacements: () -> Unit,
    onSaveSignatureSlot: (Int) -> Unit,
    onUseSignatureSlot: (Int) -> Unit,
    onDeleteSignatureSlot: (Int) -> Unit,
    onSign: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val selectedPageOneBased = state.targetPageInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val xRatio = state.xRatioInput.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.72f
    val yRatio = state.yRatioInput.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.82f
    val widthRatio = state.widthRatioInput.toFloatOrNull()?.coerceIn(0.1f, 0.8f) ?: 0.24f
    val signatureAspectRatio = computeSignatureAspectRatio(points, canvasWidth, canvasHeight)

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }
    var draggingSignature by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedUri, selectedPageOneBased) {
        val selectedUri = state.selectedUri
        if (selectedUri == null) {
            previewBitmap?.recycle()
            previewBitmap = null
            previewError = null
            return@LaunchedEffect
        }

        val rendered = withContext(Dispatchers.IO) {
            renderPdfPreviewPage(
                context = context,
                pdfUri = selectedUri,
                pageOneBased = selectedPageOneBased
            )
        }
        val old = previewBitmap
        previewBitmap = rendered
        if (old != null && old !== rendered && !old.isRecycled) {
            old.recycle()
        }
        previewError = if (rendered == null) "Could not render selected PDF page preview." else null
    }

    DisposableEffect(Unit) {
        onDispose {
            previewBitmap?.recycle()
            previewBitmap = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Signature Image", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Draw your signature and place it on one or multiple PDF pages.")

        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "\u26A0\uFE0F This adds a visual signature image to the PDF. " +
                    "It is not a legally-binding digital (PKCS#7/PAdES) signature.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }

        Button(onClick = onPickPdf, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.selectedUri == null) "Select PDF" else "Replace PDF")
        }

        state.inputLabel?.let { label ->
            Text("Input: $label", style = MaterialTheme.typography.bodySmall)
        }
        state.pageCount?.let { count ->
            Text("Total pages: $count", style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
            value = state.outputName,
            onValueChange = onOutputNameChanged,
            label = { Text("Output File Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.targetPageInput,
                onValueChange = onTargetPageChanged,
                label = { Text("Page") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = state.widthRatioInput,
            onValueChange = onWidthRatioChanged,
            label = { Text("Width(0-1)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.xRatioInput,
                onValueChange = onXRatioChanged,
                label = { Text("X(0-1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = state.yRatioInput,
            onValueChange = onYRatioChanged,
            label = { Text("Y(0-1)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Placements", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAddPlacement,
                enabled = !state.isProcessing && state.selectedUri != null
            ) {
                Text("Add Placement")
            }
            Button(
                onClick = onApplyPlacementToAll,
                enabled = !state.isProcessing && state.selectedUri != null
            ) {
                Text("Apply to All Pages")
            }
            Button(
                onClick = onClearPlacements,
                enabled = !state.isProcessing && state.placements.isNotEmpty()
            ) {
                Text("Clear Placements")
            }
        }
        if (state.placements.isNotEmpty()) {
            state.placements.forEachIndexed { index, placement ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${index + 1}. P${placement.pageOneBased}  X${formatRatio(placement.xRatio)}  Y${formatRatio(placement.yRatio)}  W${formatRatio(placement.widthRatio)}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { onLoadPlacement(index) },
                        enabled = !state.isProcessing
                    ) {
                        Text("Load")
                    }
                    Button(
                        onClick = { onRemovePlacement(index) },
                        enabled = !state.isProcessing
                    ) {
                        Text("Remove")
                    }
                }
            }
        } else {
            Text(
                "No saved placements yet. Current page/X/Y/width will be used when signing.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text("Placement Templates", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = state.templateNameInput,
            onValueChange = onTemplateNameChanged,
            label = { Text("Template Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSaveTemplate,
                enabled = !state.isProcessing
            ) {
                Text("Save Template")
            }
            Button(
                onClick = { onLoadTemplate(state.templateNameInput.trim()) },
                enabled = !state.isProcessing && state.templateNameInput.isNotBlank()
            ) {
                Text("Load By Name")
            }
        }
        if (state.placementTemplates.isNotEmpty()) {
            state.placementTemplates.forEach { template ->
                val timestamp = DateFormat.getDateTimeInstance().format(Date(template.updatedAtMillis))
                Text(
                    text = "${template.name} (${template.placementCount}) • $timestamp",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onLoadTemplate(template.name) },
                        enabled = !state.isProcessing
                    ) {
                        Text("Load")
                    }
                    Button(
                        onClick = { onDeleteTemplate(template.name) },
                        enabled = !state.isProcessing
                    ) {
                        Text("Delete")
                    }
                }
            }
        } else {
            Text("No saved templates yet.", style = MaterialTheme.typography.bodySmall)
        }

        Text("Placement Preview", fontWeight = FontWeight.SemiBold)
        if (previewBitmap != null) {
            val bitmap = previewBitmap!!
            val pageAspectRatio = (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceAtLeast(0.2f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(pageAspectRatio)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .background(Color.White)
                    .pointerInput(
                        xRatio,
                        yRatio,
                        widthRatio,
                        signatureAspectRatio,
                        state.isProcessing
                    ) {
                        detectDragGestures(
                            onDragStart = { touch ->
                                val signatureRect = signatureRectForPreview(
                                    containerWidth = size.width.toFloat(),
                                    containerHeight = size.height.toFloat(),
                                    xRatio = xRatio,
                                    yRatio = yRatio,
                                    widthRatio = widthRatio,
                                    signatureAspectRatio = signatureAspectRatio
                                )
                                draggingSignature = touch.x in signatureRect.left..signatureRect.right &&
                                    touch.y in signatureRect.top..signatureRect.bottom
                            },
                            onDragCancel = { draggingSignature = false },
                            onDragEnd = { draggingSignature = false },
                            onDrag = { change, dragAmount ->
                                if (!draggingSignature || state.isProcessing) return@detectDragGestures

                                val signatureRect = signatureRectForPreview(
                                    containerWidth = size.width.toFloat(),
                                    containerHeight = size.height.toFloat(),
                                    xRatio = xRatio,
                                    yRatio = yRatio,
                                    widthRatio = widthRatio,
                                    signatureAspectRatio = signatureAspectRatio
                                )
                                val maxLeft = (size.width.toFloat() - signatureRect.width).coerceAtLeast(1f)
                                val maxTop = (size.height.toFloat() - signatureRect.height).coerceAtLeast(1f)

                                val nextLeft = (signatureRect.left + dragAmount.x).coerceIn(0f, maxLeft)
                                val nextTop = (signatureRect.top + dragAmount.y).coerceIn(0f, maxTop)

                                onXRatioChanged(formatRatio(nextLeft / maxLeft))
                                onYRatioChanged(formatRatio(nextTop / maxTop))
                                change.consume()
                            }
                        )
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF page placement preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val signatureRect = signatureRectForPreview(
                        containerWidth = size.width,
                        containerHeight = size.height,
                        xRatio = xRatio,
                        yRatio = yRatio,
                        widthRatio = widthRatio,
                        signatureAspectRatio = signatureAspectRatio
                    )
                    drawRect(
                        color = Color(0x330090FF),
                        topLeft = Offset(signatureRect.left, signatureRect.top),
                        size = Size(signatureRect.width, signatureRect.height)
                    )
                    drawRect(
                        color = if (draggingSignature) Color(0xFFFFA000) else Color(0xFF0080FF),
                        topLeft = Offset(signatureRect.left, signatureRect.top),
                        size = Size(signatureRect.width, signatureRect.height),
                        style = Stroke(width = 3f)
                    )
                }
            }
            Text(
                "Drag the highlighted box to position the signature on page $selectedPageOneBased.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text("Select a PDF to load a page preview.", style = MaterialTheme.typography.bodySmall)
        }

        previewError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Text("Signature", fontWeight = FontWeight.SemiBold)

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(Color.White)
                .onSizeChanged { size ->
                    onCanvasSizeChanged(size.width, size.height)
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> points.add(offset) },
                        onDrag = { change, _ ->
                            points.add(change.position)
                            change.consume()
                        },
                        onDragEnd = { points.add(null) },
                        onDragCancel = { points.add(null) }
                    )
                }
        ) {
            val path = Path()
            var segmentStarted = false
            points.forEach { p ->
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
            drawPath(
                path = path,
                color = Color.Black,
                style = Stroke(width = 4f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onClearSignature, enabled = !state.isProcessing) {
                Text("Clear Signature")
            }
            Button(onClick = onSign, enabled = !state.isProcessing) {
                if (state.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    val placementsLabel = if (state.placements.isEmpty()) "1" else state.placements.size.toString()
                    Text("Sign PDF ($placementsLabel)")
                }
            }
        }

        Text("Saved Signatures", fontWeight = FontWeight.SemiBold)
        state.savedSignatureSlots.forEach { slot ->
            val status = if (slot.exists) {
                val time = slot.updatedAtMillis?.let { millis ->
                    DateFormat.getDateTimeInstance().format(Date(millis))
                }.orEmpty()
                if (time.isBlank()) "Saved" else "Saved: $time"
            } else {
                "Empty"
            }

            Text("Slot ${slot.slot}: $status", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSaveSignatureSlot(slot.slot) },
                    enabled = !state.isProcessing
                ) {
                    Text("Save Slot ${slot.slot}")
                }
                Button(
                    onClick = { onUseSignatureSlot(slot.slot) },
                    enabled = !state.isProcessing && slot.exists
                ) {
                    Text("Use Slot ${slot.slot}")
                }
                Button(
                    onClick = { onDeleteSignatureSlot(slot.slot) },
                    enabled = !state.isProcessing && slot.exists
                ) {
                    Text("Delete")
                }
            }
        }

        state.statusMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.tertiary)
        }

        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onClearError) {
                Text("Dismiss Error")
            }
        }

        state.lastOutputPath?.let { path ->
            Text("Saved: $path", style = MaterialTheme.typography.bodySmall)
            Text("Size: ${state.lastOutputSizeBytes ?: 0} bytes", style = MaterialTheme.typography.bodySmall)
        }

        if (canvasWidth <= 0 || canvasHeight <= 0) {
            Text("Signature pad initializing...", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun buildSignatureBitmap(
    points: List<Offset?>,
    width: Int,
    height: Int
): Bitmap? {
    if (width <= 0 || height <= 0) return null
    if (points.none { it != null }) return null

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    val path = AndroidPath()
    var started = false
    points.forEach { p ->
        if (p == null) {
            started = false
        } else {
            if (!started) {
                path.moveTo(p.x, p.y)
                started = true
            } else {
                path.lineTo(p.x, p.y)
            }
        }
    }

    canvas.drawPath(path, paint)
    return bitmap
}

private fun computeSignatureAspectRatio(
    points: List<Offset?>,
    canvasWidth: Int,
    canvasHeight: Int
): Float {
    val nonNullPoints = points.filterNotNull()
    if (nonNullPoints.size >= 2) {
        val minX = nonNullPoints.minOf { it.x }
        val maxX = nonNullPoints.maxOf { it.x }
        val minY = nonNullPoints.minOf { it.y }
        val maxY = nonNullPoints.maxOf { it.y }
        val width = (maxX - minX).coerceAtLeast(8f)
        val height = (maxY - minY).coerceAtLeast(8f)
        return (width / height).coerceIn(1.2f, 8f)
    }
    if (canvasWidth > 0 && canvasHeight > 0) {
        return (canvasWidth.toFloat() / canvasHeight.toFloat()).coerceIn(1.2f, 8f)
    }
    return 3.2f
}

private data class SignaturePreviewRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

private fun signatureRectForPreview(
    containerWidth: Float,
    containerHeight: Float,
    xRatio: Float,
    yRatio: Float,
    widthRatio: Float,
    signatureAspectRatio: Float
): SignaturePreviewRect {
    val safeWidth = containerWidth.coerceAtLeast(1f)
    val safeHeight = containerHeight.coerceAtLeast(1f)
    val boxWidth = (safeWidth * widthRatio.coerceIn(0.1f, 0.8f)).coerceAtMost(safeWidth)
    val boxHeight = (boxWidth / signatureAspectRatio.coerceAtLeast(1f)).coerceAtLeast(10f)
    val clampedHeight = boxHeight.coerceAtMost(safeHeight)
    val maxLeft = (safeWidth - boxWidth).coerceAtLeast(0f)
    val maxTop = (safeHeight - clampedHeight).coerceAtLeast(0f)
    val left = maxLeft * xRatio.coerceIn(0f, 1f)
    val top = maxTop * yRatio.coerceIn(0f, 1f)
    return SignaturePreviewRect(
        left = left,
        top = top,
        width = boxWidth,
        height = clampedHeight
    )
}

private fun formatRatio(value: Float): String {
    return String.format(Locale.US, "%.3f", value.coerceIn(0f, 1f))
}

private fun renderPdfPreviewPage(
    context: Context,
    pdfUri: android.net.Uri,
    pageOneBased: Int,
    maxDimension: Int = 1400
): Bitmap? {
    return runCatching {
        context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount == 0) {
                    null
                } else {
                    val targetIndex = (pageOneBased - 1).coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(targetIndex).use { page ->
                        val baseWidth = page.width.coerceAtLeast(1)
                        val baseHeight = page.height.coerceAtLeast(1)
                        val largestSide = maxOf(baseWidth, baseHeight).coerceAtLeast(1)
                        val scale = (maxDimension.toFloat() / largestSide.toFloat()).coerceAtMost(1f)
                        val renderWidth = (baseWidth * scale).toInt().coerceAtLeast(1)
                        val renderHeight = (baseHeight * scale).toInt().coerceAtLeast(1)

                        Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.RGB_565).also { bitmap ->
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }
        }
    }.getOrNull()
}
