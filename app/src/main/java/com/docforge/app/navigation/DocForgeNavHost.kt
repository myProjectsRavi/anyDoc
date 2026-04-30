package com.docforge.app.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.docforge.app.AppDependencies
import com.docforge.app.batch.BatchQueueRoute
import com.docforge.app.batch.BatchQueueViewModel
import com.docforge.app.share.ShareLaunchRequest
import com.docforge.core.pdf.PdfCompressionLevel
import com.docforge.core.pdf.PdfPageSize
import com.docforge.feature.converter.AudioFormatRoute
import com.docforge.feature.converter.AudioFormatViewModel
import com.docforge.feature.converter.AudioFormatViewModelFactory
import com.docforge.feature.converter.ConverterRoute
import com.docforge.feature.converter.ConverterViewModel
import com.docforge.feature.converter.ConverterViewModelFactory
import com.docforge.feature.converter.DocumentPdfRoute
import com.docforge.feature.converter.DocumentPdfViewModel
import com.docforge.feature.converter.DocumentPdfViewModelFactory
import com.docforge.feature.converter.ImageFormatRoute
import com.docforge.feature.converter.ImageFormatViewModel
import com.docforge.feature.converter.ImageFormatViewModelFactory
import com.docforge.feature.converter.TextPdfRoute
import com.docforge.feature.converter.TextPdfViewModel
import com.docforge.feature.converter.TextPdfViewModelFactory
import com.docforge.feature.converter.VideoAudioRoute
import com.docforge.feature.converter.VideoAudioViewModel
import com.docforge.feature.converter.VideoAudioViewModelFactory
import com.docforge.feature.history.HistoryRoute
import com.docforge.feature.history.HistoryViewModel
import com.docforge.feature.history.HistoryViewModelFactory
import com.docforge.feature.pdftools.PdfCompressRoute
import com.docforge.feature.pdftools.PdfCompressViewModel
import com.docforge.feature.pdftools.PdfCompressViewModelFactory
import com.docforge.feature.pdftools.PdfAnnotateRoute
import com.docforge.feature.pdftools.PdfAnnotateViewModel
import com.docforge.feature.pdftools.PdfAnnotateViewModelFactory
import com.docforge.feature.pdftools.PdfBatchStampRoute
import com.docforge.feature.pdftools.PdfBatchStampViewModel
import com.docforge.feature.pdftools.PdfBatchStampViewModelFactory
import com.docforge.feature.pdftools.PdfFormRoute
import com.docforge.feature.pdftools.PdfFormViewModel
import com.docforge.feature.pdftools.PdfFormViewModelFactory
import com.docforge.feature.pdftools.PdfIdCardRoute
import com.docforge.feature.pdftools.PdfIdCardViewModel
import com.docforge.feature.pdftools.PdfIdCardViewModelFactory
import com.docforge.feature.pdftools.PdfMergeRoute
import com.docforge.feature.pdftools.PdfOcrRoute
import com.docforge.feature.pdftools.PdfOcrViewModel
import com.docforge.feature.pdftools.PdfOcrViewModelFactory
import com.docforge.feature.pdftools.PdfPasswordRoute
import com.docforge.feature.pdftools.PdfPasswordViewModel
import com.docforge.feature.pdftools.PdfPasswordViewModelFactory
import com.docforge.feature.pdftools.PdfPageImageRoute
import com.docforge.feature.pdftools.PdfPageImageViewModel
import com.docforge.feature.pdftools.PdfPageImageViewModelFactory
import com.docforge.feature.pdftools.PdfRedactRoute
import com.docforge.feature.pdftools.PdfRedactViewModel
import com.docforge.feature.pdftools.PdfRedactViewModelFactory
import com.docforge.feature.pdftools.PdfSignRoute
import com.docforge.feature.pdftools.PdfSignViewModel
import com.docforge.feature.pdftools.PdfSignViewModelFactory
import com.docforge.feature.pdftools.PdfSplitRoute
import com.docforge.feature.pdftools.PdfSplitViewModel
import com.docforge.feature.pdftools.PdfSplitViewModelFactory
import com.docforge.feature.pdftools.PdfTextExtractRoute
import com.docforge.feature.pdftools.PdfTextExtractViewModel
import com.docforge.feature.pdftools.PdfTextExtractViewModelFactory
import com.docforge.feature.pdftools.PdfTranslateRoute
import com.docforge.feature.pdftools.PdfTranslateViewModel
import com.docforge.feature.pdftools.PdfTranslateViewModelFactory
import com.docforge.feature.pdftools.PdfToolsViewModel
import com.docforge.feature.pdftools.PdfToolsViewModelFactory
import com.docforge.feature.scanner.ScannerRoute
import com.docforge.feature.scanner.ScannerViewModel
import com.docforge.feature.scanner.ScannerViewModelFactory

@Composable
fun DocForgeNavHost(
    dependencies: AppDependencies,
    startDestination: String = Routes.HOME,
    sharedLaunchRequest: ShareLaunchRequest? = null,
    onSharedLaunchHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    var activeSharedLaunch by remember { mutableStateOf<ShareLaunchRequest?>(null) }

    LaunchedEffect(sharedLaunchRequest) {
        val request = sharedLaunchRequest ?: return@LaunchedEffect
        activeSharedLaunch = request
        if (request.targetRoute != Routes.HOME) {
            navController.navigate(request.targetRoute) {
                launchSingleTop = true
            }
        }
        onSharedLaunchHandled()
    }

    Scaffold(
        bottomBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val hideBottomBar = currentRoute == Routes.ONBOARDING || currentRoute == Routes.SETTINGS
            if (!hideBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text("Home") },
                        icon = { Text("H") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SCANNER,
                        onClick = {
                            navController.navigate(Routes.SCANNER) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text("Scanner") },
                        icon = { Text("S") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.CONVERTER,
                        onClick = {
                            navController.navigate(Routes.CONVERTER) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text("Convert") },
                        icon = { Text("C") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PDF_MERGE,
                        onClick = {
                            navController.navigate(Routes.PDF_MERGE) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text("PDF") },
                        icon = { Text("P") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PDF_SPLIT,
                        onClick = {
                            navController.navigate(Routes.PDF_SPLIT) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text("Split") },
                        icon = { Text("T") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HISTORY,
                        onClick = {
                            navController.navigate(Routes.HISTORY) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text("History") },
                        icon = { Text("R") }
                    )
                }
            }
        }
    ) { paddingValues ->
        val sharedTarget = activeSharedLaunch?.targetRoute
        val sharedUris = activeSharedLaunch?.uris.orEmpty()

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onGetStarted = {
                        dependencies.settingsRepository.markOnboardingCompleted()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(
                        historyRepository = dependencies.historyRepository
                    )
                )
                HomeScreen(
                    state = homeViewModel.uiState.collectAsStateWithLifecycle().value,
                    paddingValues = paddingValues,
                    onOpenScanner = { navController.navigate(Routes.SCANNER) },
                    onOpenConverter = { navController.navigate(Routes.CONVERTER) },
                    onOpenImageFormatConverter = { navController.navigate(Routes.IMAGE_FORMAT) },
                    onOpenAudioFormatConverter = { navController.navigate(Routes.AUDIO_FORMAT) },
                    onOpenDocumentToPdf = { navController.navigate(Routes.DOC_TO_PDF) },
                    onOpenTextToPdf = { navController.navigate(Routes.TEXT_TO_PDF) },
                    onOpenVideoToAudio = { navController.navigate(Routes.VIDEO_TO_AUDIO) },
                    onOpenBatchQueue = { navController.navigate(Routes.BATCH_QUEUE) },
                    onOpenPdfMerge = { navController.navigate(Routes.PDF_MERGE) },
                    onOpenPdfSplit = { navController.navigate(Routes.PDF_SPLIT) },
                    onOpenPdfSign = { navController.navigate(Routes.PDF_SIGN) },
                    onOpenPdfAnnotate = { navController.navigate(Routes.PDF_ANNOTATE) },
                    onOpenPdfPassword = { navController.navigate(Routes.PDF_PASSWORD) },
                    onOpenPdfCompress = { navController.navigate(Routes.PDF_COMPRESS) },
                    onOpenPdfText = { navController.navigate(Routes.PDF_TEXT) },
                    onOpenPdfToImages = { navController.navigate(Routes.PDF_TO_IMAGES) },
                    onOpenPdfBatchStamp = { navController.navigate(Routes.PDF_BATCH_STAMP) },
                    onOpenPdfOcr = { navController.navigate(Routes.PDF_OCR) },
                    onOpenPdfForm = { navController.navigate(Routes.PDF_FORM) },
                    onOpenIdCard = { navController.navigate(Routes.ID_CARD) },
                    onOpenPdfTranslate = { navController.navigate(Routes.PDF_TRANSLATE) },
                    onOpenPdfRedact = { navController.navigate(Routes.PDF_REDACT) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(
                        settingsRepository = dependencies.settingsRepository
                    )
                )
                SettingsRoute(
                    viewModel = settingsViewModel,
                    paddingValues = paddingValues,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SCANNER) {
                val scannerViewModel: ScannerViewModel = viewModel(
                    factory = ScannerViewModelFactory(
                        pdfCreator = dependencies.pdfCreator,
                        scanImageExporter = dependencies.scanImageExporter,
                        historyRepository = dependencies.historyRepository,
                        defaultPageSize = dependencies.settingsRepository.currentSettings()
                            .defaultPdfPageSizeName
                            .let { name -> PdfPageSize.entries.firstOrNull { it.name == name } ?: PdfPageSize.A4 }
                    )
                )
                ScannerRoute(
                    viewModel = scannerViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.CONVERTER) {
                val converterViewModel: ConverterViewModel = viewModel(
                    factory = ConverterViewModelFactory(
                        pdfCreator = dependencies.pdfCreator,
                        historyRepository = dependencies.historyRepository,
                        defaultPageSize = dependencies.settingsRepository.currentSettings()
                            .defaultPdfPageSizeName
                            .let { name -> PdfPageSize.entries.firstOrNull { it.name == name } ?: PdfPageSize.A4 }
                    )
                )
                ConverterRoute(
                    viewModel = converterViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.IMAGE_FORMAT) {
                val imageFormatViewModel: ImageFormatViewModel = viewModel(
                    factory = ImageFormatViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        imageFormatConverter = dependencies.imageFormatConverter,
                        defaultQuality = dependencies.settingsRepository.currentSettings().defaultImageQuality
                    )
                )
                ImageFormatRoute(
                    viewModel = imageFormatViewModel,
                    paddingValues = paddingValues,
                    prefillUris = if (sharedTarget == Routes.IMAGE_FORMAT) sharedUris else emptyList(),
                    onPrefillConsumed = {
                        if (sharedTarget == Routes.IMAGE_FORMAT) {
                            activeSharedLaunch = null
                        }
                    }
                )
            }
            composable(Routes.AUDIO_FORMAT) {
                val audioFormatViewModel: AudioFormatViewModel = viewModel(
                    factory = AudioFormatViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        audioFormatConverter = dependencies.audioFormatConverter
                    )
                )
                AudioFormatRoute(
                    viewModel = audioFormatViewModel,
                    paddingValues = paddingValues,
                    prefillUris = if (sharedTarget == Routes.AUDIO_FORMAT) sharedUris else emptyList(),
                    onPrefillConsumed = {
                        if (sharedTarget == Routes.AUDIO_FORMAT) {
                            activeSharedLaunch = null
                        }
                    }
                )
            }
            composable(Routes.DOC_TO_PDF) {
                val documentPdfViewModel: DocumentPdfViewModel = viewModel(
                    factory = DocumentPdfViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        documentPdfConverter = dependencies.documentPdfConverter
                    )
                )
                DocumentPdfRoute(
                    viewModel = documentPdfViewModel,
                    paddingValues = paddingValues,
                    prefillUris = if (sharedTarget == Routes.DOC_TO_PDF) sharedUris else emptyList(),
                    onPrefillConsumed = {
                        if (sharedTarget == Routes.DOC_TO_PDF) {
                            activeSharedLaunch = null
                        }
                    }
                )
            }
            composable(Routes.TEXT_TO_PDF) {
                val textPdfViewModel: TextPdfViewModel = viewModel(
                    factory = TextPdfViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        textPdfConverter = dependencies.textPdfConverter,
                        defaultPageSize = dependencies.settingsRepository.currentSettings()
                            .defaultPdfPageSizeName
                            .let { name -> PdfPageSize.entries.firstOrNull { it.name == name } ?: PdfPageSize.A4 }
                    )
                )
                TextPdfRoute(
                    viewModel = textPdfViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.VIDEO_TO_AUDIO) {
                val videoAudioViewModel: VideoAudioViewModel = viewModel(
                    factory = VideoAudioViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        videoAudioExtractor = dependencies.videoAudioExtractor
                    )
                )
                VideoAudioRoute(
                    viewModel = videoAudioViewModel,
                    paddingValues = paddingValues,
                    prefillUris = if (sharedTarget == Routes.VIDEO_TO_AUDIO) sharedUris else emptyList(),
                    onPrefillConsumed = {
                        if (sharedTarget == Routes.VIDEO_TO_AUDIO) {
                            activeSharedLaunch = null
                        }
                    }
                )
            }
            composable(Routes.BATCH_QUEUE) {
                val batchQueueViewModel: BatchQueueViewModel = viewModel()
                BatchQueueRoute(
                    viewModel = batchQueueViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.HISTORY) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(
                        historyRepository = dependencies.historyRepository
                    )
                )
                HistoryRoute(
                    viewModel = historyViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_MERGE) {
                val pdfToolsViewModel: PdfToolsViewModel = viewModel(
                    factory = PdfToolsViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        pdfMerger = dependencies.pdfMerger
                    )
                )
                PdfMergeRoute(
                    viewModel = pdfToolsViewModel,
                    paddingValues = paddingValues,
                    prefillUris = if (sharedTarget == Routes.PDF_MERGE) sharedUris else emptyList(),
                    onPrefillConsumed = {
                        if (sharedTarget == Routes.PDF_MERGE) {
                            activeSharedLaunch = null
                        }
                    }
                )
            }
            composable(Routes.PDF_SPLIT) {
                val pdfSplitViewModel: PdfSplitViewModel = viewModel(
                    factory = PdfSplitViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        pdfSplitter = dependencies.pdfSplitter
                    )
                )
                PdfSplitRoute(
                    viewModel = pdfSplitViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_SIGN) {
                val pdfSignViewModel: PdfSignViewModel = viewModel(
                    factory = PdfSignViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        pdfSigner = dependencies.pdfSigner,
                        savedSignatureStore = dependencies.savedSignatureStore,
                        placementTemplateStore = dependencies.placementTemplateStore
                    )
                )
                PdfSignRoute(
                    viewModel = pdfSignViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_ANNOTATE) {
                val pdfAnnotateViewModel: PdfAnnotateViewModel = viewModel(
                    factory = PdfAnnotateViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        pdfAnnotator = dependencies.pdfAnnotator
                    )
                )
                PdfAnnotateRoute(
                    viewModel = pdfAnnotateViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_PASSWORD) {
                val pdfPasswordViewModel: PdfPasswordViewModel = viewModel(
                    factory = PdfPasswordViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        pdfPasswordTool = dependencies.pdfPasswordTool
                    )
                )
                PdfPasswordRoute(
                    viewModel = pdfPasswordViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_COMPRESS) {
                val pdfCompressViewModel: PdfCompressViewModel = viewModel(
                    factory = PdfCompressViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        pdfCompressor = dependencies.pdfCompressor,
                        defaultCompressionLevel = dependencies.settingsRepository.currentSettings()
                            .defaultPdfCompressionName
                            .let { name ->
                                PdfCompressionLevel.entries.firstOrNull { it.name == name } ?: PdfCompressionLevel.MEDIUM
                            }
                    )
                )
                PdfCompressRoute(
                    viewModel = pdfCompressViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_TEXT) {
                val pdfTextViewModel: PdfTextExtractViewModel = viewModel(
                    factory = PdfTextExtractViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        pdfTextExtractor = dependencies.pdfTextExtractor
                    )
                )
                PdfTextExtractRoute(
                    viewModel = pdfTextViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_TO_IMAGES) {
                val pdfPageImageViewModel: PdfPageImageViewModel = viewModel(
                    factory = PdfPageImageViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        pdfPageImageExporter = dependencies.pdfPageImageExporter,
                        defaultJpegQuality = dependencies.settingsRepository.currentSettings().defaultImageQuality
                    )
                )
                PdfPageImageRoute(
                    viewModel = pdfPageImageViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_BATCH_STAMP) {
                val batchStampViewModel: PdfBatchStampViewModel = viewModel(
                    factory = PdfBatchStampViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        batchStampTool = dependencies.pdfBatchStampTool
                    )
                )
                PdfBatchStampRoute(
                    viewModel = batchStampViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_OCR) {
                val ocrViewModel: PdfOcrViewModel = viewModel(
                    factory = PdfOcrViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        ocrTool = dependencies.pdfOcrTool
                    )
                )
                PdfOcrRoute(
                    viewModel = ocrViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_FORM) {
                val formViewModel: PdfFormViewModel = viewModel(
                    factory = PdfFormViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        formTool = dependencies.pdfFormTool
                    )
                )
                PdfFormRoute(
                    viewModel = formViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.ID_CARD) {
                val idCardViewModel: PdfIdCardViewModel = viewModel(
                    factory = PdfIdCardViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        idCardTool = dependencies.pdfIdCardTool
                    )
                )
                PdfIdCardRoute(
                    viewModel = idCardViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_TRANSLATE) {
                val translateViewModel: PdfTranslateViewModel = viewModel(
                    factory = PdfTranslateViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        translationTool = dependencies.pdfTranslationTool
                    )
                )
                PdfTranslateRoute(
                    viewModel = translateViewModel,
                    paddingValues = paddingValues
                )
            }
            composable(Routes.PDF_REDACT) {
                val redactViewModel: PdfRedactViewModel = viewModel(
                    factory = PdfRedactViewModelFactory(
                        historyRepository = dependencies.historyRepository,
                        redactionTool = dependencies.pdfRedactionTool
                    )
                )
                PdfRedactRoute(
                    viewModel = redactViewModel,
                    paddingValues = paddingValues
                )
            }
        }
    }
}
