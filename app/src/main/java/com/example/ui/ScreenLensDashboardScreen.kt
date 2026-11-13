package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CustomGlossaryAndSandboxSection
import com.example.ui.components.DraggableFloatingTeleprompterHud
import com.example.ui.components.InteractiveViewportSection
import com.example.ui.components.LiveSubtitleTranscriptSection
import com.example.ui.components.OcrVisionInspectorCard
import com.example.ui.components.PictureInPictureFloatingOverlay
import com.example.ui.components.TelemetryHeaderAndBackgroundBar
import com.example.ui.components.TranscriptExportDialog
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.NormalSansFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ScreenLensDashboardScreen(
    viewModel: ScreenLensViewModel,
    onRequestEnterPictureInPicture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val glossaryEntries by viewModel.glossaryFlow.collectAsStateWithLifecycle()
    val subtitleHistory by viewModel.subtitleHistoryFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // If the app is currently floating in Android Picture-in-Picture (PiP) mode over other apps,
    // render the full-bleed In-Place Normal-Font Replaced Screen view!
    if (uiState.isInPictureInPictureMode) {
        PictureInPictureFloatingOverlay(
            uiState = uiState,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // BackHandler for secondary tabs to return to VIEWPORT tab
    BackHandler(enabled = uiState.selectedTab != DashboardTab.VIEWPORT) {
        viewModel.selectTab(DashboardTab.VIEWPORT)
    }

    // Track real app focus vs background state
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setTabFocused(true)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> viewModel.setTabFocused(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Runtime permission launcher for POST_NOTIFICATIONS (Android 13+) when starting background translator service
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // MediaProjection Screen Capture launcher for translating screen while app is not opened
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onMediaProjectionPermissionResult(result.resultCode, result.data)
    }

    val startOrStopLiveCapture = {
        if (uiState.isMediaProjectionActive) {
            viewModel.stopLiveScreenCapture()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            val captureIntent = mpManager?.createScreenCaptureIntent()
            if (captureIntent != null) {
                mediaProjectionLauncher.launch(captureIntent)
            } else {
                viewModel.showToast("Screen capture service unavailable on this device")
            }
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoPicked(uri)
        }
    }

    // Toast / Snackbar feedback
    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            CyberNavigationBar(
                selectedTab = uiState.selectedTab,
                subtitleCount = subtitleHistory.size,
                glossaryCount = glossaryEntries.size,
                onSelectTab = viewModel::selectTab
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Sticky Cyber-Telemetry Top Header & Unthrottled Background Engine Bar
                TelemetryHeaderAndBackgroundBar(
                    uiState = uiState,
                    onToggleFloatingHud = viewModel::toggleFloatingHudAndKeepAlive,
                    onStartOrStopLiveScreenCapture = startOrStopLiveCapture,
                    onEnterPipWindowNow = onRequestEnterPictureInPicture,
                    onToggleInPlaceReplacement = viewModel::toggleInPlaceReplaceOnScreen,
                    onToggleTestOfflineMode = viewModel::toggleTestOfflineMode,
                    onTriggerOutboundProbe = viewModel::triggerOutboundCloudTestProbe,
                    onToggleSimulatedOffScreen = viewModel::toggleSimulatedOffScreen,
                    onToggleWorkerHz = viewModel::toggleWorkerHz
                )

                // Adaptive Main Content Area
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val isExpandedScreen = maxWidth >= 760.dp
                    val scrollState = rememberScrollState()

                    if (isExpandedScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1.15f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InteractiveViewportSection(
                                    uiState = uiState,
                                    onSelectDemoScene = viewModel::selectDemoScene,
                                    onStepNextFrame = viewModel::stepNextDialogueFrame,
                                    onToggleAutoCycleScenes = viewModel::toggleAutoCycleScenes,
                                    onLaunchPhotoPicker = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onSetOcrTierMode = viewModel::setOcrTierMode,
                                    onToggleTranslateEverything = viewModel::toggleTranslateEverythingMode,
                                    onToggleInPlaceReplacement = viewModel::toggleInPlaceReplaceOnScreen,
                                    onApplyRoiPreset = viewModel::applyRoiPreset,
                                    onUpdateRoi = viewModel::updateRoi
                                )
                                OcrVisionInspectorCard(
                                    uiState = uiState,
                                    onTriggerOutboundProbe = viewModel::triggerOutboundCloudTestProbe
                                )
                            }

                            Column(
                                modifier = Modifier.weight(0.85f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CustomGlossaryAndSandboxSection(
                                    uiState = uiState,
                                    glossaryEntries = glossaryEntries,
                                    onSandboxInputChange = viewModel::updateSandboxInput,
                                    onAddGlossaryOverride = viewModel::addGlossaryOverride,
                                    onDeleteGlossaryOverride = viewModel::deleteGlossaryOverride
                                )
                                LiveSubtitleTranscriptSection(
                                    subtitles = subtitleHistory,
                                    onCopyAllToClipboard = {
                                        val fullTxt = subtitleHistory.joinToString("\n") {
                                            "[${it.detectedLangCode}] ${it.translatedText} (${it.sourceText})"
                                        }
                                        copyToClipboard(context, "ScreenLens Transcript", fullTxt)
                                        viewModel.showToast("Copied ${subtitleHistory.size} subtitles to clipboard")
                                    },
                                    onCopySingleSubtitle = { item ->
                                        copyToClipboard(
                                            context,
                                            "ScreenLens Line",
                                            "${item.translatedText}\n${item.sourceText}"
                                        )
                                        viewModel.showToast("Copied subtitle #${item.sequenceNumber}")
                                    },
                                    onExportTxt = viewModel::openTxtExportModal,
                                    onExportSrt = viewModel::openSrtExportModal,
                                    onClearHistory = viewModel::clearTranscript
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (uiState.selectedTab) {
                                DashboardTab.VIEWPORT -> {
                                    InteractiveViewportSection(
                                        uiState = uiState,
                                        onSelectDemoScene = viewModel::selectDemoScene,
                                        onStepNextFrame = viewModel::stepNextDialogueFrame,
                                        onToggleAutoCycleScenes = viewModel::toggleAutoCycleScenes,
                                        onLaunchPhotoPicker = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        onSetOcrTierMode = viewModel::setOcrTierMode,
                                        onToggleTranslateEverything = viewModel::toggleTranslateEverythingMode,
                                        onToggleInPlaceReplacement = viewModel::toggleInPlaceReplaceOnScreen,
                                        onApplyRoiPreset = viewModel::applyRoiPreset,
                                        onUpdateRoi = viewModel::updateRoi
                                    )
                                    OcrVisionInspectorCard(
                                        uiState = uiState,
                                        onTriggerOutboundProbe = viewModel::triggerOutboundCloudTestProbe
                                    )
                                    LiveSubtitleTranscriptSection(
                                        subtitles = subtitleHistory,
                                        onCopyAllToClipboard = {
                                            val fullTxt = subtitleHistory.joinToString("\n") {
                                                "[${it.detectedLangCode}] ${it.translatedText} (${it.sourceText})"
                                            }
                                            copyToClipboard(context, "ScreenLens Transcript", fullTxt)
                                            viewModel.showToast("Copied transcript to clipboard")
                                        },
                                        onCopySingleSubtitle = { item ->
                                            copyToClipboard(
                                                context,
                                                "ScreenLens Line",
                                                "${item.translatedText}\n${item.sourceText}"
                                            )
                                            viewModel.showToast("Copied subtitle #${item.sequenceNumber}")
                                        },
                                        onExportTxt = viewModel::openTxtExportModal,
                                        onExportSrt = viewModel::openSrtExportModal,
                                        onClearHistory = viewModel::clearTranscript
                                    )
                                }

                                DashboardTab.INSPECTOR -> {
                                    OcrVisionInspectorCard(
                                        uiState = uiState,
                                        onTriggerOutboundProbe = viewModel::triggerOutboundCloudTestProbe
                                    )
                                }

                                DashboardTab.GLOSSARY -> {
                                    CustomGlossaryAndSandboxSection(
                                        uiState = uiState,
                                        glossaryEntries = glossaryEntries,
                                        onSandboxInputChange = viewModel::updateSandboxInput,
                                        onAddGlossaryOverride = viewModel::addGlossaryOverride,
                                        onDeleteGlossaryOverride = viewModel::deleteGlossaryOverride
                                    )
                                }

                                DashboardTab.TRANSCRIPT -> {
                                    LiveSubtitleTranscriptSection(
                                        subtitles = subtitleHistory,
                                        onCopyAllToClipboard = {
                                            val fullTxt = subtitleHistory.joinToString("\n") {
                                                "[${it.detectedLangCode}] ${it.translatedText} (${it.sourceText})"
                                            }
                                            copyToClipboard(context, "ScreenLens Transcript", fullTxt)
                                            viewModel.showToast("Copied transcript to clipboard")
                                        },
                                        onCopySingleSubtitle = { item ->
                                            copyToClipboard(
                                                context,
                                                "ScreenLens Line",
                                                "${item.translatedText}\n${item.sourceText}"
                                            )
                                            viewModel.showToast("Copied subtitle #${item.sequenceNumber}")
                                        },
                                        onExportTxt = viewModel::openTxtExportModal,
                                        onExportSrt = viewModel::openSrtExportModal,
                                        onClearHistory = viewModel::clearTranscript
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(110.dp))
                        }
                    }
                }
            }

            // Always-On-Top In-App Draggable Floating Teleprompter HUD Overlay
            AnimatedVisibility(
                visible = uiState.isFloatingHudActive,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                DraggableFloatingTeleprompterHud(
                    uiState = uiState,
                    onSelectFontSize = viewModel::setHudFontSize,
                    onToggleOpacity = viewModel::toggleHudOpacity,
                    onToggleSourceLine = viewModel::toggleHudSourceLine,
                    onTogglePause = viewModel::toggleHudPause,
                    onCloseHud = viewModel::toggleFloatingHudAndKeepAlive
                )
            }
        }
    }

    val modalTitle = uiState.exportDialogTitle
    val modalContent = uiState.exportDialogContent
    if (modalTitle != null && modalContent != null) {
        TranscriptExportDialog(
            title = modalTitle,
            content = modalContent,
            onCopyAndShare = {
                copyToClipboard(context, modalTitle, modalContent)
                shareTextFile(context, modalTitle, modalContent)
                viewModel.dismissExportModal()
                viewModel.showToast("Copied exported file to clipboard")
            },
            onDismiss = viewModel::dismissExportModal
        )
    }
}

@Composable
private fun CyberNavigationBar(
    selectedTab: DashboardTab,
    subtitleCount: Int,
    glossaryCount: Int,
    onSelectTab: (DashboardTab) -> Unit
) {
    Surface(
        color = CyberSurface,
        border = BorderStroke(1.dp, CyberBorder),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items: List<Triple<DashboardTab, ImageVector, String>> = listOf(
                Triple(DashboardTab.VIEWPORT, Icons.Default.Tv, "Viewport & ROI"),
                Triple(DashboardTab.INSPECTOR, Icons.Default.AutoFixHigh, "OCR Inspector"),
                Triple(DashboardTab.GLOSSARY, Icons.Default.MenuBook, "Glossary ($glossaryCount)"),
                Triple(DashboardTab.TRANSCRIPT, Icons.Default.Subtitles, "Subtitles ($subtitleCount)")
            )

            items.forEach { (tab, icon, label) ->
                val active = selectedTab == tab
                Surface(
                    color = if (active) CyberCyan.copy(alpha = 0.18f) else CyberSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (active) CyberCyan else CyberBorder
                    ),
                    modifier = Modifier
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 2.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (active) CyberCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            fontFamily = NormalSansFontFamily,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            color = if (active) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    } catch (_: Throwable) {
    }
}

private fun shareTextFile(context: Context, title: String, content: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Throwable) {
    }
}
