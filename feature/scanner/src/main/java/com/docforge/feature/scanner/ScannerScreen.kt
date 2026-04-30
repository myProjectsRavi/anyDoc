package com.docforge.feature.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.core.opencv.DetectedDocument
import com.docforge.core.opencv.DocumentEdgeDetector
import com.docforge.core.pdf.decodeBitmapConstrained
import com.docforge.core.pdf.PdfPageSize
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun ScannerRoute(
    viewModel: ScannerViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val detector = remember { DocumentEdgeDetector() }
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val normalizedAndFiltered = withContext(Dispatchers.Default) {
                uris.map { uri ->
                    val normalized = normalizeCapturedImage(context, uri, detector) ?: uri
                    applyFilterToCapturedImage(context, normalized, state.selectedFilterMode) ?: normalized
                }
            }
            viewModel.onImagesImported(normalizedAndFiltered)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        PermissionMissingScreen(
            paddingValues = paddingValues,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    ScannerScreen(
        state = state,
        detector = detector,
        paddingValues = paddingValues,
        onCaptured = viewModel::onImageCaptured,
        onImportGallery = { importLauncher.launch("image/*") },
        onRemovePage = viewModel::removePage,
        onReplacePage = viewModel::replacePage,
        onOutputNameChanged = viewModel::onOutputNameChanged,
        onFilterModeChanged = viewModel::onFilterModeChanged,
        onExportFormatChanged = viewModel::onExportFormatChanged,
        onZipImageOutputChanged = viewModel::onZipImageOutputChanged,
        onPageSizeChanged = viewModel::onPageSizeChanged,
        onExport = viewModel::exportScan,
        onClearError = viewModel::clearError
    )
}

@Composable
private fun PermissionMissingScreen(
    paddingValues: PaddingValues,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Camera permission required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("DocForge scanner stays fully offline, but needs camera access to capture document pages.")
        Button(onClick = onRequestPermission) {
            Text("Grant Camera Permission")
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ScannerScreen(
    state: ScannerUiState,
    detector: DocumentEdgeDetector,
    paddingValues: PaddingValues,
    onCaptured: (Uri) -> Unit,
    onImportGallery: () -> Unit,
    onRemovePage: (Int) -> Unit,
    onReplacePage: (Int, Uri, String) -> Unit,
    onOutputNameChanged: (String) -> Unit,
    onFilterModeChanged: (ScanFilterMode) -> Unit,
    onExportFormatChanged: (ScannerExportFormat) -> Unit,
    onZipImageOutputChanged: (Boolean) -> Unit,
    onPageSizeChanged: (PdfPageSize) -> Unit,
    onExport: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Document Scanner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Capture pages, import from gallery, then export as PDF or high-res JPG/PNG.")

        CameraCapturePanel(
            detector = detector,
            onCaptured = onCaptured,
            selectedFilterMode = state.selectedFilterMode,
            enabled = !state.isExporting
        )

        Text("Scan Filter", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScanFilterMode.entries.forEach { mode ->
                Button(
                    onClick = { onFilterModeChanged(mode) },
                    enabled = !state.isExporting
                ) {
                    val selected = if (mode == state.selectedFilterMode) "*" else ""
                    Text("${mode.name}$selected")
                }
            }
        }

        Text("Export Format", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScannerExportFormat.entries.forEach { format ->
                Button(
                    onClick = { onExportFormatChanged(format) },
                    enabled = !state.isExporting
                ) {
                    val selected = if (format == state.exportFormat) "*" else ""
                    Text("${format.name}$selected")
                }
            }
        }
        if (state.exportFormat != ScannerExportFormat.PDF) {
            Button(
                onClick = { onZipImageOutputChanged(!state.zipImageOutput) },
                enabled = !state.isExporting
            ) {
                Text(if (state.zipImageOutput) "ZIP Bundle: ON" else "ZIP Bundle: OFF")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onImportGallery, enabled = !state.isExporting) {
                Text("Import Gallery")
            }
            Button(onClick = onExport, enabled = !state.isExporting) {
                if (state.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        when (state.exportFormat) {
                            ScannerExportFormat.PDF -> "Export PDF"
                            ScannerExportFormat.JPG -> "Export JPG"
                            ScannerExportFormat.PNG -> "Export PNG"
                        }
                    )
                }
            }
        }

        Text("Pages queued: ${state.capturedUris.size}", fontWeight = FontWeight.SemiBold)

        if (state.capturedUris.isNotEmpty()) {
            OutlinedTextField(
                value = state.outputName,
                onValueChange = onOutputNameChanged,
                label = { Text("Output File Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (state.exportFormat == ScannerExportFormat.PDF) {
                Text("Page Size", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PdfPageSize.entries.forEach { size ->
                        Button(
                            onClick = { onPageSizeChanged(size) },
                            enabled = !state.isExporting
                        ) {
                            val selected = if (size == state.pageSize) "*" else ""
                            Text("${size.name}$selected")
                        }
                    }
                }
            } else {
                Text(
                    "Image export keeps full processed page resolution.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(state.capturedUris) { index, uri ->
                    CapturedPageItem(
                        index = index,
                        uri = uri,
                        filterMode = state.selectedFilterMode,
                        enabled = !state.isExporting,
                        onApplySelectedFilter = {
                            scope.launch {
                                val filtered = withContext(Dispatchers.Default) {
                                    applyFilterToCapturedImage(context, uri, state.selectedFilterMode)
                                }
                                if (filtered != null) {
                                    onReplacePage(index, filtered, "Applied ${state.selectedFilterMode.name}")
                                }
                            }
                        },
                        onRemove = { onRemovePage(index) }
                    )
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
            if (state.lastOutputCount > 1) {
                Text("Files: ${state.lastOutputCount}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Size: ${state.lastOutputSizeBytes ?: 0} bytes", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CapturedPageItem(
    index: Int,
    uri: Uri,
    filterMode: ScanFilterMode,
    enabled: Boolean,
    onApplySelectedFilter: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val label = remember(uri) {
        DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment ?: uri.toString()
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Page ${index + 1}", fontWeight = FontWeight.SemiBold)
                Text(label, style = MaterialTheme.typography.bodySmall)
                Text("Selected filter: ${filterMode.name}", style = MaterialTheme.typography.bodySmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onApplySelectedFilter, enabled = enabled) {
                    Text("Apply Filter")
                }
                Button(onClick = onRemove, enabled = enabled) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun CameraCapturePanel(
    detector: DocumentEdgeDetector,
    onCaptured: (Uri) -> Unit,
    selectedFilterMode: ScanFilterMode,
    enabled: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val overlayColor = MaterialTheme.colorScheme.tertiary

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var detectedOverlay by remember { mutableStateOf<DetectedOverlay?>(null) }
    var manualCornerRatios by remember { mutableStateOf<List<android.graphics.PointF>?>(null) }
    var autoCaptureEnabled by remember { mutableStateOf(false) }
    var autoCaptureInFlight by remember { mutableStateOf(false) }
    var lastStableCornerRatios by remember { mutableStateOf<List<android.graphics.PointF>?>(null) }
    var stableSinceAtMillis by remember { mutableStateOf(0L) }
    var lastAutoCaptureAtMillis by remember { mutableStateOf(0L) }
    var draggedCornerIndex by remember { mutableStateOf(-1) }

    fun triggerCapture(cornerRatios: List<android.graphics.PointF>?, fromAutoCapture: Boolean) {
        val capture = imageCapture ?: run {
            captureError = "Camera not ready yet."
            if (fromAutoCapture) {
                autoCaptureInFlight = false
            }
            return
        }
        takePictureToCache(
            context = context,
            imageCapture = capture,
            onSaved = { rawUri ->
                scope.launch {
                    val corrected = withContext(Dispatchers.Default) {
                        normalizeCapturedImage(
                            context = context,
                            inputUri = rawUri,
                            detector = detector,
                            normalizedCorners = cornerRatios
                        ) ?: rawUri
                    }
                    val filtered = withContext(Dispatchers.Default) {
                        applyFilterToCapturedImage(context, corrected, selectedFilterMode) ?: corrected
                    }
                    captureError = null
                    onCaptured(filtered)
                    if (fromAutoCapture) {
                        autoCaptureInFlight = false
                        lastAutoCaptureAtMillis = System.currentTimeMillis()
                    }
                }
            },
            onError = {
                captureError = it
                if (fromAutoCapture) {
                    autoCaptureInFlight = false
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PreviewView(viewContext).apply {
                        scaleType = PreviewView.ScaleType.FIT_CENTER
                        bindCameraUseCases(
                            context = viewContext,
                            lifecycleOwner = lifecycleOwner,
                            previewView = this,
                            detector = detector,
                            onImageCaptureReady = { imageCapture = it },
                            onDocumentDetected = { detectedOverlay = it },
                            onBindError = { captureError = it }
                        )
                    }
                }
            )

            val overlay = detectedOverlay
            if (overlay != null && overlay.corners.size == 4 && overlay.frameWidth > 0 && overlay.frameHeight > 0) {
                val effectiveRatios = manualCornerRatios ?: overlay.corners.toNormalizedRatios(
                    frameWidth = overlay.frameWidth,
                    frameHeight = overlay.frameHeight
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(overlay, effectiveRatios, enabled) {
                            detectDragGestures(
                                onDragStart = { touch ->
                                    val mapping = OverlayMapping(
                                        frameWidth = overlay.frameWidth,
                                        frameHeight = overlay.frameHeight,
                                        canvasWidth = size.width.toFloat(),
                                        canvasHeight = size.height.toFloat()
                                    )
                                    val mappedCorners = effectiveRatios.map { ratio ->
                                        mapping.normalizedRatioToCanvasOffset(ratio)
                                    }
                                    val nearest = mappedCorners
                                        .mapIndexed { index, corner -> index to corner.distanceTo(touch) }
                                        .minByOrNull { it.second }
                                        ?.takeIf { it.second <= 52f }
                                        ?.first

                                    draggedCornerIndex = nearest ?: -1
                                    if (nearest != null && manualCornerRatios == null) {
                                        manualCornerRatios = effectiveRatios
                                    }
                                },
                                onDragCancel = { draggedCornerIndex = -1 },
                                onDragEnd = { draggedCornerIndex = -1 },
                                onDrag = { change, _ ->
                                    val targetIndex = draggedCornerIndex
                                    if (!enabled || targetIndex !in 0..3) return@detectDragGestures
                                    val mapping = OverlayMapping(
                                        frameWidth = overlay.frameWidth,
                                        frameHeight = overlay.frameHeight,
                                        canvasWidth = size.width.toFloat(),
                                        canvasHeight = size.height.toFloat()
                                    )
                                    val updated = (manualCornerRatios ?: effectiveRatios).toMutableList()
                                    updated[targetIndex] = mapping.canvasOffsetToNormalizedRatio(change.position)
                                    manualCornerRatios = updated
                                    change.consume()
                                }
                            )
                        }
                ) {
                    val mapping = OverlayMapping(
                        frameWidth = overlay.frameWidth,
                        frameHeight = overlay.frameHeight,
                        canvasWidth = size.width,
                        canvasHeight = size.height
                    )
                    val mapped = (manualCornerRatios ?: effectiveRatios).map { ratio ->
                        mapping.normalizedRatioToCanvasOffset(ratio)
                    }
                    if (mapped.size != 4) return@Canvas

                    val path = Path().apply {
                        moveTo(mapped[0].x, mapped[0].y)
                        lineTo(mapped[1].x, mapped[1].y)
                        lineTo(mapped[2].x, mapped[2].y)
                        lineTo(mapped[3].x, mapped[3].y)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = overlayColor,
                        style = Stroke(width = 5f)
                    )
                    mapped.forEachIndexed { index, corner ->
                        val dragged = index == draggedCornerIndex
                        drawCircle(
                            color = if (dragged) Color.Yellow else overlayColor,
                            radius = if (dragged) 12f else 9f,
                            center = corner
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    autoCaptureEnabled = !autoCaptureEnabled
                    stableSinceAtMillis = 0L
                    lastStableCornerRatios = null
                },
                enabled = enabled
            ) {
                Text(if (autoCaptureEnabled) "Auto Capture: ON" else "Auto Capture: OFF")
            }
            Button(
                onClick = { manualCornerRatios = null },
                enabled = enabled
            ) {
                Text("Reset Corners")
            }
        }

        Button(
            onClick = {
                val overlay = detectedOverlay
                val ratios = manualCornerRatios ?: overlay?.corners?.toNormalizedRatios(
                    frameWidth = overlay.frameWidth,
                    frameHeight = overlay.frameHeight
                )
                triggerCapture(ratios, fromAutoCapture = false)
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Capture Page")
        }

        if (!detector.isAvailable()) {
            Text(
                "OpenCV not available on this device build. Using fallback scan bounds.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                "Live edge confidence: ${((detectedOverlay?.confidence ?: 0f) * 100f).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Tip: drag corner handles to fine-tune perspective before capture.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        captureError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        LaunchedEffect(autoCaptureEnabled, detectedOverlay, enabled, imageCapture, manualCornerRatios, autoCaptureInFlight) {
            if (!enabled || !autoCaptureEnabled || autoCaptureInFlight) return@LaunchedEffect
            val overlay = detectedOverlay ?: run {
                stableSinceAtMillis = 0L
                lastStableCornerRatios = null
                return@LaunchedEffect
            }
            if (overlay.confidence < 0.70f) {
                stableSinceAtMillis = 0L
                lastStableCornerRatios = null
                return@LaunchedEffect
            }

            val effectiveRatios = manualCornerRatios ?: overlay.corners.toNormalizedRatios(
                frameWidth = overlay.frameWidth,
                frameHeight = overlay.frameHeight
            )

            val now = System.currentTimeMillis()
            val movement = effectiveRatios.averageCornerDistance(lastStableCornerRatios)
            if (lastStableCornerRatios == null || movement > 0.02f) {
                stableSinceAtMillis = now
                lastStableCornerRatios = effectiveRatios
                return@LaunchedEffect
            }

            if (now - stableSinceAtMillis < 850L) return@LaunchedEffect
            if (now - lastAutoCaptureAtMillis < 1700L) return@LaunchedEffect

            autoCaptureInFlight = true
            delay(120L)
            triggerCapture(
                cornerRatios = effectiveRatios,
                fromAutoCapture = true
            )
        }
    }
}

private fun bindCameraUseCases(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    detector: DocumentEdgeDetector,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onDocumentDetected: (DetectedOverlay?) -> Unit,
    onBindError: (String) -> Unit
) {
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener(
        {
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val analysisExecutor = Executors.newSingleThreadExecutor()
                var lastAnalyzedAt = 0L

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        val now = System.currentTimeMillis()
                        if (now - lastAnalyzedAt < 120L) return@setAnalyzer
                        lastAnalyzedAt = now

                        val lumaBytes = extractLumaPlane(imageProxy)
                        val rotation = normalizeRotation(imageProxy.imageInfo.rotationDegrees)
                        val frameWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
                        val frameHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

                        val detected = detector.detectDocumentBounds(
                            lumaBytes = lumaBytes,
                            frameWidth = imageProxy.width,
                            frameHeight = imageProxy.height,
                            rotationDegrees = rotation
                        )

                        val overlay = detected?.let {
                            DetectedOverlay(
                                corners = it.corners,
                                confidence = it.confidence,
                                frameWidth = frameWidth,
                                frameHeight = frameHeight
                            )
                        }
                        ContextCompat.getMainExecutor(context).execute {
                            onDocumentDetected(overlay)
                        }
                    } catch (_: Throwable) {
                        ContextCompat.getMainExecutor(context).execute {
                            onDocumentDetected(null)
                        }
                    } finally {
                        imageProxy.close()
                    }
                }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
                onImageCaptureReady(imageCapture)
            }.onFailure {
                onBindError(it.message ?: "Failed to start camera")
            }
        },
        ContextCompat.getMainExecutor(context)
    )
}

private fun takePictureToCache(
    context: Context,
    imageCapture: ImageCapture,
    onSaved: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val outputFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSaved(outputFileResults.savedUri ?: Uri.fromFile(outputFile))
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Capture failed")
            }
        }
    )
}

private fun extractLumaPlane(imageProxy: ImageProxy): ByteArray {
    val plane = imageProxy.planes.firstOrNull() ?: return ByteArray(0)
    val rowStride = plane.rowStride
    val width = imageProxy.width
    val height = imageProxy.height
    val buffer = plane.buffer

    if (rowStride == width) {
        return ByteArray(width * height).also { out ->
            buffer.rewind()
            buffer.get(out, 0, min(out.size, buffer.remaining()))
        }
    }

    val output = ByteArray(width * height)
    val row = ByteArray(rowStride)
    buffer.rewind()
    var outOffset = 0
    for (y in 0 until height) {
        val read = min(rowStride, buffer.remaining())
        if (read <= 0) break
        buffer.get(row, 0, read)
        val copy = min(width, read)
        System.arraycopy(row, 0, output, outOffset, copy)
        outOffset += width
    }
    return output
}

private fun normalizeCapturedImage(
    context: Context,
    inputUri: Uri,
    detector: DocumentEdgeDetector,
    normalizedCorners: List<android.graphics.PointF>? = null
): Uri? {
    if (!detector.isAvailable()) return null
    val sourceBitmap = decodeBitmapFromUri(context, inputUri) ?: return null

    val corrected = if (normalizedCorners != null && normalizedCorners.size == 4) {
        val mappedCorners = normalizedCorners.map { ratio ->
            android.graphics.PointF(
                ratio.x.coerceIn(0f, 1f) * (sourceBitmap.width - 1).coerceAtLeast(1),
                ratio.y.coerceIn(0f, 1f) * (sourceBitmap.height - 1).coerceAtLeast(1)
            )
        }
        detector.perspectiveCorrect(sourceBitmap, mappedCorners) ?: detector.autoCorrect(sourceBitmap)
    } else {
        detector.autoCorrect(sourceBitmap)
    } ?: return null

    return writeBitmapToCache(context, corrected)
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        decodeBitmapConstrained(context, uri, maxLongEdge = 2200)
    }.getOrNull()
}

private fun applyFilterToCapturedImage(
    context: Context,
    inputUri: Uri,
    filterMode: ScanFilterMode
): Uri? {
    if (filterMode == ScanFilterMode.COLOR) {
        return inputUri
    }

    val source = decodeBitmapFromUri(context, inputUri) ?: return null
    val working = source.copy(Bitmap.Config.ARGB_8888, true) ?: return null
    if (working !== source) {
        source.recycle()
    }

    val width = working.width
    val height = working.height
    val pixels = IntArray(width * height)
    working.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
        val color = pixels[i]
        val alpha = (color ushr 24) and 0xFF
        val red = (color ushr 16) and 0xFF
        val green = (color ushr 8) and 0xFF
        val blue = color and 0xFF
        val gray = (0.299f * red + 0.587f * green + 0.114f * blue).toInt().coerceIn(0, 255)

        pixels[i] = when (filterMode) {
            ScanFilterMode.GRAYSCALE -> {
                android.graphics.Color.argb(alpha, gray, gray, gray)
            }

            ScanFilterMode.BW -> {
                val threshold = if (gray >= 150) 255 else 0
                android.graphics.Color.argb(alpha, threshold, threshold, threshold)
            }

            ScanFilterMode.ENHANCED -> {
                val contrasted = (((gray - 128f) * 1.45f) + 138f).toInt().coerceIn(0, 255)
                android.graphics.Color.argb(alpha, contrasted, contrasted, contrasted)
            }

            ScanFilterMode.COLOR -> color
        }
    }

    working.setPixels(pixels, 0, width, 0, 0, width, height)
    val outputUri = writeBitmapToCache(context, working)
    working.recycle()
    return outputUri
}

private fun writeBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    return runCatching {
        val output = File(context.cacheDir, "scan_warp_${System.currentTimeMillis()}.jpg")
        FileOutputStream(output).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 96, stream)
        }
        Uri.fromFile(output)
    }.getOrNull()
}

private fun normalizeRotation(rotation: Int): Int {
    val normalized = ((rotation % 360) + 360) % 360
    return when (normalized) {
        90, 180, 270 -> normalized
        else -> 0
    }
}

private data class OverlayMapping(
    val frameWidth: Int,
    val frameHeight: Int,
    val canvasWidth: Float,
    val canvasHeight: Float
) {
    private val safeFrameWidth = frameWidth.coerceAtLeast(1).toFloat()
    private val safeFrameHeight = frameHeight.coerceAtLeast(1).toFloat()
    private val safeCanvasWidth = canvasWidth.coerceAtLeast(1f)
    private val safeCanvasHeight = canvasHeight.coerceAtLeast(1f)
    private val scale = min(safeCanvasWidth / safeFrameWidth, safeCanvasHeight / safeFrameHeight)
    private val offsetX = (safeCanvasWidth - safeFrameWidth * scale) / 2f
    private val offsetY = (safeCanvasHeight - safeFrameHeight * scale) / 2f

    fun normalizedRatioToCanvasOffset(ratio: android.graphics.PointF): Offset {
        val xFrame = ratio.x.coerceIn(0f, 1f) * safeFrameWidth
        val yFrame = ratio.y.coerceIn(0f, 1f) * safeFrameHeight
        return Offset(
            x = xFrame * scale + offsetX,
            y = yFrame * scale + offsetY
        )
    }

    fun canvasOffsetToNormalizedRatio(offset: Offset): android.graphics.PointF {
        val frameX = ((offset.x - offsetX) / scale).coerceIn(0f, safeFrameWidth)
        val frameY = ((offset.y - offsetY) / scale).coerceIn(0f, safeFrameHeight)
        return android.graphics.PointF(
            (frameX / safeFrameWidth).coerceIn(0f, 1f),
            (frameY / safeFrameHeight).coerceIn(0f, 1f)
        )
    }
}

private fun List<android.graphics.PointF>.toNormalizedRatios(frameWidth: Int, frameHeight: Int): List<android.graphics.PointF> {
    val safeFrameWidth = frameWidth.coerceAtLeast(1).toFloat()
    val safeFrameHeight = frameHeight.coerceAtLeast(1).toFloat()
    return map { corner ->
        android.graphics.PointF(
            (corner.x / safeFrameWidth).coerceIn(0f, 1f),
            (corner.y / safeFrameHeight).coerceIn(0f, 1f)
        )
    }
}

private fun Offset.distanceTo(other: Offset): Float {
    return hypot(x - other.x, y - other.y)
}

private fun List<android.graphics.PointF>.averageCornerDistance(previous: List<android.graphics.PointF>?): Float {
    if (previous == null || previous.size != size || size != 4) return Float.MAX_VALUE
    var total = 0.0
    for (index in indices) {
        total += hypot(
            (this[index].x - previous[index].x).toDouble(),
            (this[index].y - previous[index].y).toDouble()
        )
    }
    return (total / size).toFloat()
}

private data class DetectedOverlay(
    val corners: List<android.graphics.PointF>,
    val confidence: Float,
    val frameWidth: Int,
    val frameHeight: Int
)
