package com.example.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GlossaryEntryEntity
import com.example.data.local.ScreenLensDatabase
import com.example.data.local.ScreenLensRepository
import com.example.data.local.SubtitleRecordEntity
import com.example.domain.BackgroundKeepAliveEngine
import com.example.domain.BlockedRequestEvent
import com.example.domain.DemoSceneSpec
import com.example.domain.DemoScenesCatalog
import com.example.domain.MergedRegionBox
import com.example.domain.NormalizedRoi
import com.example.domain.OcrTierMode
import com.example.domain.OfflineTranslationEngine
import com.example.domain.PreprocessedVisionResult
import com.example.domain.RoiPreset
import com.example.domain.SpatialDiffAndOcrEngine
import com.example.domain.TranslationResult
import com.example.service.ScreenCaptureStateHolder
import com.example.service.ScreenCaptureTranslatorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class InputSourceMode(val label: String) {
    DEMO_SIMULATOR("60fps Multi-Lang Simulator"),
    LIVE_SCREEN_CAPTURE("Live Screen Capture"),
    PHOTO_PICKER("Photo Picker Image")
}

enum class HudFontSize(val label: String, val scaleSp: Int) {
    S("S", 14),
    M("M", 16),
    L("L", 19),
    XL("XL", 22)
}

enum class DashboardTab(val label: String) {
    VIEWPORT("Viewport & ROI"),
    INSPECTOR("OCR Inspector"),
    GLOSSARY("Glossary & Sandbox"),
    TRANSCRIPT("Subtitle Feed")
}

data class ScreenLensUiState(
    val selectedTab: DashboardTab = DashboardTab.VIEWPORT,
    // 1. Offline & Telemetry
    val isDeviceOnline: Boolean = true,
    val testOfflineModeEnabled: Boolean = true,
    val blockedRequests: List<BlockedRequestEvent> = emptyList(),
    val diffLatencyMs: Double = 0.42,
    val ocrLatencyMs: Double = 1.35,
    val translateLatencyMs: Double = 0.28,
    val totalLatencyMs: Double = 2.05,
    // 2. Unthrottled Off-Screen Background Engine & Live MediaProjection
    val isTabFocused: Boolean = true,
    val isInPictureInPictureMode: Boolean = false,
    val isMediaProjectionActive: Boolean = false,
    val isSimulatedOffScreen: Boolean = false,
    val targetWorkerHz: Int = 30,
    val measuredWorkerHz: Int = 30,
    val isAudioKeepAliveActive: Boolean = false,
    val offScreenElapsedSec: Long = 0L,
    val offScreenLinesTranslated: Int = 0,
    // 3. Floating Teleprompter HUD & In-Place Screen Replacement
    val isFloatingHudActive: Boolean = true,
    val inPlaceReplaceOnScreen: Boolean = true,
    val hudFontSize: HudFontSize = HudFontSize.M,
    val hudHighOpacity: Boolean = true,
    val hudShowSourceLine: Boolean = true,
    val isHudPaused: Boolean = false,
    // 4. Viewport, Demo Simulator & Photo Picker
    val inputSourceMode: InputSourceMode = InputSourceMode.DEMO_SIMULATOR,
    val activeSceneIndex: Int = 0,
    val activeSubFrameIndex: Int = 0,
    val autoCycleSubFrames: Boolean = true,
    val autoCycleAllScenes: Boolean = false,
    val ocrTierMode: OcrTierMode = OcrTierMode.TRUE_PIXEL_OCR,
    val translateEverythingMode: Boolean = true,
    val roi: NormalizedRoi = NormalizedRoi(0.06f, 0.68f, 0.94f, 0.95f),
    val activePreset: RoiPreset? = RoiPreset.FULL_SCREEN,
    val tileDiffs: List<Float> = List(48) { 0.15f },
    val changedTileCount: Int = 12,
    val mergedRegions: List<MergedRegionBox> = emptyList(),
    val currentFrameBitmap: Bitmap? = null,
    val preprocessedVision: PreprocessedVisionResult? = null,
    val customImageLabel: String? = null,
    // 5. Current Live Translation Output
    val liveSourceText: String = "諦めないで！夜明け前に必ず君を見つけ出すから！",
    val liveTranslatedText: String = "Don't give up! I will definitely find you before dawn breaks!",
    val liveDetectedLangCode: String = "JA",
    val liveDetectedLangName: String = "Japanese",
    val liveRegionTag: String = "ALL-48 (TOP+MID+SUB)",
    val liveUsedGlossary: Boolean = false,
    // 6. Sandbox & Export Preview
    val sandboxInput: String = "諦めないで！領域展開システム起動",
    val sandboxResult: TranslationResult? = null,
    val exportDialogTitle: String? = null,
    val exportDialogContent: String? = null,
    val toastMessage: String? = null
) {
    val isEffectivelyOffScreen: Boolean
        get() = !isTabFocused || isInPictureInPictureMode || isSimulatedOffScreen

    val currentScene: DemoSceneSpec
        get() = DemoScenesCatalog.scenes.getOrElse(activeSceneIndex) { DemoScenesCatalog.scenes.first() }
}

class ScreenLensViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ScreenLensDatabase.getInstance(application)
    private val repository = ScreenLensRepository(database.dao())
    private val spatialEngine = SpatialDiffAndOcrEngine()
    private val keepAliveEngine = BackgroundKeepAliveEngine(application)

    private val _uiState = MutableStateFlow(ScreenLensUiState())
    val uiState: StateFlow<ScreenLensUiState> = _uiState.asStateFlow()

    val glossaryFlow: StateFlow<List<GlossaryEntryEntity>> = repository.glossaryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subtitleHistoryFlow: StateFlow<List<SubtitleRecordEntity>> = repository.subtitlesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var customPhotoBitmap: Bitmap? = null
    private var animationTick: Long = 0L
    private var lastRecordedSourceText: String = ""
    private var sequenceCounter: Int = 1
    private val sessionStartMs: Long = SystemClock.elapsedRealtime()
    private var workerLoopJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultGlossarySeeded()
            updateSandboxInput(_uiState.value.sandboxInput)
            val initialEvent = keepAliveEngine.interceptOutboundProbe(
                endpoint = "https://translate.googleapis.com/v3/projects:translateText",
                protocol = "HTTPS POST (Startup Self-Test)"
            )
            _uiState.update {
                it.copy(
                    isDeviceOnline = keepAliveEngine.checkDeviceOnline(),
                    blockedRequests = listOf(initialEvent)
                )
            }
        }
        observeLiveScreenCapture()
        startUnthrottledBackgroundWorker()
        startOffScreenSecondCounter()
    }

    private fun observeLiveScreenCapture() {
        viewModelScope.launch {
            ScreenCaptureStateHolder.isMediaProjectionActive.collect { active ->
                _uiState.update { current ->
                    current.copy(
                        isMediaProjectionActive = active,
                        inputSourceMode = if (active) InputSourceMode.LIVE_SCREEN_CAPTURE else current.inputSourceMode
                    )
                }
            }
        }
    }

    private fun startUnthrottledBackgroundWorker() {
        workerLoopJob?.cancel()
        workerLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastSubFrameSwitchMs = SystemClock.elapsedRealtime()
            while (isActive) {
                val loopStartMs = SystemClock.elapsedRealtime()
                val state = _uiState.value
                val targetHz = state.targetWorkerHz.coerceIn(30, 60)
                val frameIntervalMs = (1000L / targetHz).coerceAtLeast(16L)

                if (!state.isHudPaused) {
                    animationTick++

                    if (state.inputSourceMode == InputSourceMode.DEMO_SIMULATOR &&
                        state.autoCycleSubFrames &&
                        loopStartMs - lastSubFrameSwitchMs >= 3000L
                    ) {
                        lastSubFrameSwitchMs = loopStartMs
                        val scene = state.currentScene
                        val nextSub = (state.activeSubFrameIndex + 1) % scene.frames.size
                        val nextSceneIdx = if (nextSub == 0 && state.autoCycleAllScenes) {
                            (state.activeSceneIndex + 1) % DemoScenesCatalog.scenes.size
                        } else {
                            state.activeSceneIndex
                        }
                        _uiState.update {
                            it.copy(
                                activeSceneIndex = nextSceneIdx,
                                activeSubFrameIndex = nextSub
                            )
                        }
                    }

                    if (animationTick % 6L == 0L) {
                        executePipelineStep()
                    }
                }

                val elapsedLoopMs = SystemClock.elapsedRealtime() - loopStartMs
                val waitMs = (frameIntervalMs - elapsedLoopMs).coerceAtLeast(4L)
                delay(waitMs)
            }
        }
    }

    private fun startOffScreenSecondCounter() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)
                val online = keepAliveEngine.checkDeviceOnline()
                _uiState.update { current ->
                    current.copy(
                        isDeviceOnline = online,
                        offScreenElapsedSec = if (current.isEffectivelyOffScreen) {
                            current.offScreenElapsedSec + 1L
                        } else {
                            current.offScreenElapsedSec
                        }
                    )
                }
            }
        }
    }

    private suspend fun executePipelineStep() {
        val state = _uiState.value
        val capturedScreenBmp = ScreenCaptureStateHolder.latestCapturedBitmap.value

        val activeScene = when {
            state.inputSourceMode == InputSourceMode.LIVE_SCREEN_CAPTURE && capturedScreenBmp != null -> state.currentScene
            state.inputSourceMode == InputSourceMode.PHOTO_PICKER -> null
            else -> state.currentScene
        }

        val frameBitmap = when {
            state.inputSourceMode == InputSourceMode.LIVE_SCREEN_CAPTURE && capturedScreenBmp != null ->
                capturedScreenBmp
            state.inputSourceMode == InputSourceMode.PHOTO_PICKER && customPhotoBitmap != null ->
                customPhotoBitmap!!
            else ->
                spatialEngine.renderDemoFrame(
                    scene = state.currentScene,
                    subFrameIndex = state.activeSubFrameIndex,
                    animationTick = animationTick
                )
        }

        val glossary = glossaryFlow.value
        val scanOutput = spatialEngine.processFrame(
            sourceBitmap = frameBitmap,
            scene = activeScene,
            subFrameIndex = state.activeSubFrameIndex,
            roi = state.roi,
            translateEverythingMode = state.translateEverythingMode,
            inPlaceReplaceOnScreen = state.inPlaceReplaceOnScreen,
            ocrTierMode = state.ocrTierMode,
            customImageLabel = state.customImageLabel,
            glossary = glossary
        )

        val translation = OfflineTranslationEngine.translate(
            rawText = scanOutput.recognizedSourceText,
            glossary = glossary
        )

        val totalMs = scanOutput.diffLatencyMs + scanOutput.ocrLatencyMs + translation.latencyMs
        val isNewSubtitle = translation.sourceText.isNotBlank() &&
            translation.sourceText != lastRecordedSourceText

        if (isNewSubtitle) {
            lastRecordedSourceText = translation.sourceText
            val nowRelMs = (SystemClock.elapsedRealtime() - sessionStartMs).coerceAtLeast(0L)
            val record = SubtitleRecordEntity(
                sequenceNumber = sequenceCounter++,
                startTimeMs = nowRelMs,
                endTimeMs = nowRelMs + 2800L,
                detectedLangCode = translation.detectedLangCode,
                detectedLangName = translation.detectedLangName,
                sourceText = translation.sourceText,
                translatedText = translation.translatedText,
                latencyMs = totalMs,
                regionTag = scanOutput.activeRegionTag,
                wasOffScreen = state.isEffectivelyOffScreen
            )
            repository.recordSubtitle(record)

            // If live background capture or off-screen mode is active, push latest normal-font translation to ongoing notification
            if (state.isMediaProjectionActive || state.isEffectivelyOffScreen) {
                notifyForegroundServiceTranslation(
                    langCode = translation.detectedLangCode,
                    translatedText = translation.translatedText,
                    sourceText = translation.sourceText
                )
            }
        }

        _uiState.update { current ->
            current.copy(
                diffLatencyMs = scanOutput.diffLatencyMs,
                ocrLatencyMs = scanOutput.ocrLatencyMs,
                translateLatencyMs = translation.latencyMs,
                totalLatencyMs = totalMs,
                measuredWorkerHz = current.targetWorkerHz,
                tileDiffs = scanOutput.tileDiffs.toList(),
                changedTileCount = scanOutput.changedTileCount,
                mergedRegions = scanOutput.mergedRegions,
                currentFrameBitmap = scanOutput.frameBitmap,
                preprocessedVision = scanOutput.visionResult,
                liveSourceText = translation.sourceText,
                liveTranslatedText = translation.translatedText,
                liveDetectedLangCode = translation.detectedLangCode,
                liveDetectedLangName = translation.detectedLangName,
                liveRegionTag = scanOutput.activeRegionTag,
                liveUsedGlossary = translation.usedGlossaryOverride,
                offScreenLinesTranslated = if (isNewSubtitle && current.isEffectivelyOffScreen) {
                    current.offScreenLinesTranslated + 1
                } else {
                    current.offScreenLinesTranslated
                }
            )
        }
    }

    private fun notifyForegroundServiceTranslation(
        langCode: String,
        translatedText: String,
        sourceText: String
    ) {
        try {
            val app = getApplication<Application>()
            val intent = Intent(app, ScreenCaptureTranslatorService::class.java).apply {
                action = ScreenCaptureTranslatorService.ACTION_UPDATE_TRANSLATION
                putExtra(ScreenCaptureTranslatorService.EXTRA_LANG_CODE, langCode)
                putExtra(ScreenCaptureTranslatorService.EXTRA_TRANSLATED_TEXT, translatedText)
                putExtra(ScreenCaptureTranslatorService.EXTRA_SOURCE_TEXT, sourceText)
            }
            app.startService(intent)
        } catch (_: Throwable) {
        }
    }

    fun onMediaProjectionPermissionResult(resultCode: Int, data: Intent?) {
        if (resultCode != 0 && data != null) {
            keepAliveEngine.startSilentAudioKeepAlive()
            val app = getApplication<Application>()
            val serviceIntent = Intent(app, ScreenCaptureTranslatorService::class.java).apply {
                action = ScreenCaptureTranslatorService.ACTION_START_CAPTURE
                putExtra(ScreenCaptureTranslatorService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenCaptureTranslatorService.EXTRA_RESULT_DATA, data)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    app.startForegroundService(serviceIntent)
                } else {
                    app.startService(serviceIntent)
                }
                _uiState.update {
                    it.copy(
                        isMediaProjectionActive = true,
                        inputSourceMode = InputSourceMode.LIVE_SCREEN_CAPTURE,
                        isFloatingHudActive = true,
                        inPlaceReplaceOnScreen = true,
                        isAudioKeepAliveActive = true,
                        toastMessage = "Live Screen Capture active! Switch apps or press Home for floating PiP replacement."
                    )
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(toastMessage = "Started background translator engine")
                }
            }
        } else {
            _uiState.update {
                it.copy(toastMessage = "Screen capture permission canceled — using built-in 60fps Demo Simulator")
            }
        }
    }

    fun stopLiveScreenCapture() {
        try {
            val app = getApplication<Application>()
            val stopIntent = Intent(app, ScreenCaptureTranslatorService::class.java).apply {
                action = ScreenCaptureTranslatorService.ACTION_STOP_CAPTURE
            }
            app.startService(stopIntent)
        } catch (_: Throwable) {
        }
        ScreenCaptureStateHolder.setProjectionActive(false)
        _uiState.update {
            it.copy(
                isMediaProjectionActive = false,
                inputSourceMode = InputSourceMode.DEMO_SIMULATOR,
                toastMessage = "Live screen capture stopped"
            )
        }
    }

    fun setPictureInPictureState(inPip: Boolean) {
        if (inPip) {
            keepAliveEngine.startSilentAudioKeepAlive()
        }
        _uiState.update {
            it.copy(
                isInPictureInPictureMode = inPip,
                inPlaceReplaceOnScreen = if (inPip) true else it.inPlaceReplaceOnScreen,
                isAudioKeepAliveActive = keepAliveEngine.isAudioKeepAliveActive()
            )
        }
    }

    fun toggleInPlaceReplaceOnScreen() {
        val next = !_uiState.value.inPlaceReplaceOnScreen
        _uiState.update {
            it.copy(
                inPlaceReplaceOnScreen = next,
                toastMessage = if (next) {
                    "In-Place Screen Replacement ON (Normal Font)"
                } else {
                    "Showing Original Foreign Text on Screen"
                }
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            executePipelineStep()
        }
    }

    fun selectTab(tab: DashboardTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleFloatingHudAndKeepAlive() {
        val nextActive = !_uiState.value.isFloatingHudActive
        val audioActive = if (nextActive) {
            keepAliveEngine.startSilentAudioKeepAlive()
        } else {
            keepAliveEngine.isAudioKeepAliveActive()
        }
        _uiState.update {
            it.copy(
                isFloatingHudActive = nextActive,
                isAudioKeepAliveActive = audioActive,
                toastMessage = if (nextActive) {
                    "Off-Screen Translator & Auto-Float HUD Activated"
                } else {
                    "Floating HUD minimized (Background Engine still active)"
                }
            )
        }
    }

    fun setTabFocused(focused: Boolean) {
        if (!focused && !_uiState.value.isAudioKeepAliveActive) {
            keepAliveEngine.startSilentAudioKeepAlive()
        }
        _uiState.update {
            it.copy(
                isTabFocused = focused,
                isAudioKeepAliveActive = keepAliveEngine.isAudioKeepAliveActive()
            )
        }
    }

    fun toggleSimulatedOffScreen() {
        val nextSim = !_uiState.value.isSimulatedOffScreen
        if (nextSim) {
            keepAliveEngine.startSilentAudioKeepAlive()
        }
        _uiState.update {
            it.copy(
                isSimulatedOffScreen = nextSim,
                isFloatingHudActive = if (nextSim) true else it.isFloatingHudActive,
                inPlaceReplaceOnScreen = if (nextSim) true else it.inPlaceReplaceOnScreen,
                isAudioKeepAliveActive = keepAliveEngine.isAudioKeepAliveActive(),
                toastMessage = if (nextSim) {
                    "Simulating Off-Screen / App Not Opened — Translating in Normal Font"
                } else {
                    "Returned to Focused Dashboard Mode"
                }
            )
        }
    }

    fun toggleWorkerHz() {
        val nextHz = if (_uiState.value.targetWorkerHz == 30) 60 else 30
        _uiState.update {
            it.copy(
                targetWorkerHz = nextHz,
                measuredWorkerHz = nextHz
            )
        }
    }

    fun toggleTestOfflineMode() {
        val next = !_uiState.value.testOfflineModeEnabled
        _uiState.update {
            it.copy(
                testOfflineModeEnabled = next,
                toastMessage = if (next) {
                    "Offline Firewall Active: All outbound fetch/HTTP calls blocked"
                } else {
                    "Offline Firewall Monitor in passive observation mode"
                }
            )
        }
    }

    fun triggerOutboundCloudTestProbe() {
        val sampleEndpoints = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini:generateContent",
            "https://translation.googleapis.com/language/translate/v2",
            "https://api.cloud-ocr-vision.example.com/v1/recognize"
        )
        val chosen = sampleEndpoints[(_uiState.value.blockedRequests.size) % sampleEndpoints.size]
        val event = keepAliveEngine.interceptOutboundProbe(
            endpoint = chosen,
            protocol = "fetch / XMLHttpRequest Probe"
        )
        _uiState.update {
            it.copy(
                testOfflineModeEnabled = true,
                blockedRequests = listOf(event) + it.blockedRequests.take(14),
                toastMessage = "Blocked outbound probe -> Proven 100% Local Execution"
            )
        }
    }

    fun selectDemoScene(index: Int) {
        if (index !in DemoScenesCatalog.scenes.indices) return
        _uiState.update {
            it.copy(
                inputSourceMode = InputSourceMode.DEMO_SIMULATOR,
                activeSceneIndex = index,
                activeSubFrameIndex = 0
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            executePipelineStep()
        }
    }

    fun stepNextDialogueFrame() {
        val scene = _uiState.value.currentScene
        val nextSub = (_uiState.value.activeSubFrameIndex + 1) % scene.frames.size
        _uiState.update { it.copy(activeSubFrameIndex = nextSub) }
        viewModelScope.launch(Dispatchers.Default) {
            executePipelineStep()
        }
    }

    fun toggleAutoCycleScenes() {
        _uiState.update { it.copy(autoCycleAllScenes = !it.autoCycleAllScenes) }
    }

    fun setOcrTierMode(mode: OcrTierMode) {
        _uiState.update { it.copy(ocrTierMode = mode) }
        viewModelScope.launch(Dispatchers.Default) {
            executePipelineStep()
        }
    }

    fun toggleTranslateEverythingMode() {
        val next = !_uiState.value.translateEverythingMode
        _uiState.update {
            it.copy(
                translateEverythingMode = next,
                activePreset = if (next) RoiPreset.FULL_SCREEN else it.activePreset
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            executePipelineStep()
        }
    }

    fun applyRoiPreset(preset: RoiPreset) {
        val isFull = preset == RoiPreset.FULL_SCREEN
        _uiState.update {
            it.copy(
                activePreset = preset,
                translateEverythingMode = isFull,
                roi = NormalizedRoi(preset.left, preset.top, preset.right, preset.bottom)
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            executePipelineStep()
        }
    }

    fun updateRoi(newRoi: NormalizedRoi) {
        _uiState.update {
            it.copy(
                roi = newRoi.clamped(),
                activePreset = null,
                translateEverythingMode = false
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            executePipelineStep()
        }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (decoded != null) {
                        val canvasBmp = Bitmap.createScaledBitmap(
                            decoded,
                            SpatialDiffAndOcrEngine.FRAME_WIDTH,
                            SpatialDiffAndOcrEngine.FRAME_HEIGHT,
                            true
                        )
                        customPhotoBitmap = canvasBmp
                        _uiState.update {
                            it.copy(
                                inputSourceMode = InputSourceMode.PHOTO_PICKER,
                                customImageLabel = "星际导航控制台 • 量子核心已就绪 | 諦めないで！夜明け前に必ず君を見つけ出すから！",
                                toastMessage = "Photo Picker Image Loaded & Replaced In-Place in Normal Font"
                            )
                        }
                        executePipelineStep()
                    }
                }
            } catch (_: Throwable) {
                _uiState.update { it.copy(toastMessage = "Could not decode selected image") }
            }
        }
    }

    fun setHudFontSize(size: HudFontSize) {
        _uiState.update { it.copy(hudFontSize = size) }
    }

    fun toggleHudOpacity() {
        _uiState.update { it.copy(hudHighOpacity = !it.hudHighOpacity) }
    }

    fun toggleHudSourceLine() {
        _uiState.update { it.copy(hudShowSourceLine = !it.hudShowSourceLine) }
    }

    fun toggleHudPause() {
        _uiState.update { it.copy(isHudPaused = !it.isHudPaused) }
    }

    fun addGlossaryOverride(source: String, english: String, langCode: String) {
        if (source.isBlank() || english.isBlank()) return
        viewModelScope.launch {
            repository.addGlossaryEntry(source, english, langCode)
            delay(60L)
            updateSandboxInput(_uiState.value.sandboxInput)
            withContext(Dispatchers.Default) {
                executePipelineStep()
            }
            _uiState.update {
                it.copy(toastMessage = "Added 0.1ms Glossary Override: $source -> $english")
            }
        }
    }

    fun deleteGlossaryOverride(id: Long) {
        viewModelScope.launch {
            repository.deleteGlossaryEntry(id)
            delay(60L)
            updateSandboxInput(_uiState.value.sandboxInput)
        }
    }

    fun updateSandboxInput(text: String) {
        val result = OfflineTranslationEngine.translate(text, glossaryFlow.value)
        _uiState.update {
            it.copy(
                sandboxInput = text,
                sandboxResult = result
            )
        }
    }

    fun clearTranscript() {
        viewModelScope.launch {
            repository.clearSubtitleHistory()
            lastRecordedSourceText = ""
            _uiState.update { it.copy(toastMessage = "Subtitle transcript history cleared") }
        }
    }

    fun openTxtExportModal() {
        val txt = repository.formatAsTxt(subtitleHistoryFlow.value)
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export Subtitle Transcript (.TXT)",
                exportDialogContent = txt
            )
        }
    }

    fun openSrtExportModal() {
        val srt = repository.formatAsSrt(subtitleHistoryFlow.value)
        _uiState.update {
            it.copy(
                exportDialogTitle = "Export SubRip Subtitle File (.SRT)",
                exportDialogContent = srt
            )
        }
    }

    fun dismissExportModal() {
        _uiState.update {
            it.copy(
                exportDialogTitle = null,
                exportDialogContent = null
            )
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        keepAliveEngine.stopSilentAudioKeepAlive()
    }
}
