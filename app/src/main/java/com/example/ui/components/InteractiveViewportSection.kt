package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DemoScenesCatalog
import com.example.domain.NormalizedRoi
import com.example.domain.OcrTierMode
import com.example.domain.RoiPreset
import com.example.domain.SpatialDiffAndOcrEngine
import com.example.ui.InputSourceMode
import com.example.ui.ScreenLensUiState
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NormalSansFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

private enum class ReticleDragTarget {
    NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

@Composable
fun InteractiveViewportSection(
    uiState: ScreenLensUiState,
    onSelectDemoScene: (Int) -> Unit,
    onStepNextFrame: () -> Unit,
    onToggleAutoCycleScenes: () -> Unit,
    onLaunchPhotoPicker: () -> Unit,
    onSetOcrTierMode: (OcrTierMode) -> Unit,
    onToggleTranslateEverything: () -> Unit,
    onToggleInPlaceReplacement: () -> Unit,
    onApplyRoiPreset: (RoiPreset) -> Unit,
    onUpdateRoi: (NormalizedRoi) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Input Source & Dual-Tier Local OCR Selector + 10-Scene Strip
        Surface(
            color = CyberSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIVE SCREEN CAPTURE & 60FPS MULTI-LANGUAGE SIMULATOR",
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = when (uiState.inputSourceMode) {
                                InputSourceMode.LIVE_SCREEN_CAPTURE ->
                                    "Active Source: Live Android Screen Capture (Translating in Background)"
                                InputSourceMode.PHOTO_PICKER ->
                                    "Active Source: Imported Photo Picker Screenshot (In-Place Replaced)"
                                InputSourceMode.DEMO_SIMULATOR ->
                                    "Active Scene: ${uiState.currentScene.categoryTitle} (${uiState.currentScene.languageCode})"
                            },
                            fontFamily = NormalSansFontFamily,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = onLaunchPhotoPicker,
                        border = BorderStroke(1.dp, CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("photo_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Import Screenshot via Photo Picker",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Photo Picker",
                            fontFamily = NormalSansFontFamily,
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                    }
                }

                // 10-Scene Horizontal Selector (JA, KO, ZH, HI, MR, ES, FR, DE, RU, AR)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DemoScenesCatalog.scenes.forEach { scene ->
                        val selected = uiState.inputSourceMode == InputSourceMode.DEMO_SIMULATOR &&
                            uiState.activeSceneIndex == scene.index
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectDemoScene(scene.index) },
                            label = {
                                Text(
                                    text = "${scene.languageCode} • ${scene.categoryTitle}",
                                    fontFamily = NormalSansFontFamily,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan,
                                containerColor = CyberSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = CyberBorder,
                                selectedBorderColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("scene_chip_${scene.languageCode.lowercase()}")
                        )
                    }
                }

                // Toggle between True Pixel OCR and Direct Canvas Text + Frame Step Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OcrTierMode.entries.forEach { mode ->
                            val isSelected = uiState.ocrTierMode == mode
                            Surface(
                                color = if (isSelected) CyberEmerald.copy(alpha = 0.22f) else CyberSurfaceElevated,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isSelected) CyberEmerald else CyberBorder),
                                modifier = Modifier
                                    .clickable { onSetOcrTierMode(mode) }
                                    .testTag("ocr_mode_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.label,
                                    fontFamily = NormalSansFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isSelected) CyberEmerald else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CyberSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier
                                .clickable { onStepNextFrame() }
                                .testTag("next_dialogue_frame_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Dialogue Frame",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Step Line",
                                    fontFamily = NormalSansFontFamily,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        Surface(
                            color = if (uiState.autoCycleAllScenes) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (uiState.autoCycleAllScenes) CyberCyan else CyberBorder),
                            modifier = Modifier
                                .clickable { onToggleAutoCycleScenes() }
                                .testTag("auto_cycle_scenes_button")
                        ) {
                            Text(
                                text = if (uiState.autoCycleAllScenes) "Cycle 10 Scenes: ON" else "Cycle 10 Scenes: OFF",
                                fontFamily = NormalSansFontFamily,
                                fontSize = 11.sp,
                                color = if (uiState.autoCycleAllScenes) CyberCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Card 2: 16:9 Interactive Viewport with In-Place Normal Font Replacement + 48-Tile Diff Grid + ROI Reticle
        Surface(
            color = CyberSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Toggle 1: Replace Translated Text On Screen with Normal Font (In-Place Replacement)
                Surface(
                    color = if (uiState.inPlaceReplaceOnScreen) CyberEmerald.copy(alpha = 0.14f) else CyberSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (uiState.inPlaceReplaceOnScreen) CyberEmerald else CyberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FindReplace,
                                contentDescription = "Replace Text In-Place On Screen",
                                tint = CyberEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Replace Translated Text On Screen (Normal Font)",
                                    fontFamily = NormalSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (uiState.inPlaceReplaceOnScreen) {
                                        "Active: Foreign text on screen is replaced in-place with normal Sans-Serif English"
                                    } else {
                                        "Inactive: Showing original foreign text on screen canvas"
                                    },
                                    fontFamily = NormalSansFontFamily,
                                    fontSize = 11.sp,
                                    color = if (uiState.inPlaceReplaceOnScreen) CyberEmerald else TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = uiState.inPlaceReplaceOnScreen,
                            onCheckedChange = { onToggleInPlaceReplacement() },
                            modifier = Modifier.testTag("in_place_replace_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBackground,
                                checkedTrackColor = CyberEmerald,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = CyberSurfaceElevated
                            )
                        )
                    }
                }

                // Toggle 2: Translate Everything On Screen (48-Tile Grid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "48-Tile Spatial Diff Scanner",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Translate Everything On Screen (8×6 = 48 Tiles)",
                                fontFamily = NormalSansFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Changed Tiles: ${uiState.changedTileCount}/48 • Regions: TOP, MID, SUB",
                                fontFamily = NormalSansFontFamily,
                                fontSize = 11.sp,
                                color = CyberCyan
                            )
                        }
                    }

                    Switch(
                        checked = uiState.translateEverythingMode,
                        onCheckedChange = { onToggleTranslateEverything() },
                        modifier = Modifier.testTag("translate_everything_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CyberSurfaceElevated
                        )
                    )
                }

                // 16:9 Live Preview Canvas + High-DPI Normal-Font In-Place Replacement Overlays + ROI Reticle
                var canvasSize by remember { mutableStateOf(IntSize.Zero) }
                val currentRoi by rememberUpdatedState(uiState.roi)
                val currentOnUpdateRoi by rememberUpdatedState(onUpdateRoi)
                var activeDragTarget by remember { mutableStateOf(ReticleDragTarget.NONE) }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBackground)
                        .border(1.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { touchOffset ->
                                    if (canvasSize.width <= 0 || canvasSize.height <= 0) return@detectDragGestures
                                    val w = canvasSize.width.toFloat()
                                    val h = canvasSize.height.toFloat()
                                    val normX = (touchOffset.x / w).coerceIn(0f, 1f)
                                    val normY = (touchOffset.y / h).coerceIn(0f, 1f)
                                    val r = currentRoi
                                    val handleRadius = 0.08f

                                    activeDragTarget = when {
                                        abs(normX - r.left) < handleRadius && abs(normY - r.top) < handleRadius ->
                                            ReticleDragTarget.TOP_LEFT
                                        abs(normX - r.right) < handleRadius && abs(normY - r.top) < handleRadius ->
                                            ReticleDragTarget.TOP_RIGHT
                                        abs(normX - r.left) < handleRadius && abs(normY - r.bottom) < handleRadius ->
                                            ReticleDragTarget.BOTTOM_LEFT
                                        abs(normX - r.right) < handleRadius && abs(normY - r.bottom) < handleRadius ->
                                            ReticleDragTarget.BOTTOM_RIGHT
                                        normX in r.left..r.right && normY in r.top..r.bottom ->
                                            ReticleDragTarget.MOVE
                                        else -> ReticleDragTarget.MOVE
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (canvasSize.width <= 0 || canvasSize.height <= 0) return@detectDragGestures
                                    val dx = dragAmount.x / canvasSize.width.toFloat()
                                    val dy = dragAmount.y / canvasSize.height.toFloat()
                                    val r = currentRoi
                                    val updated = when (activeDragTarget) {
                                        ReticleDragTarget.TOP_LEFT ->
                                            r.copy(left = r.left + dx, top = r.top + dy)
                                        ReticleDragTarget.TOP_RIGHT ->
                                            r.copy(right = r.right + dx, top = r.top + dy)
                                        ReticleDragTarget.BOTTOM_LEFT ->
                                            r.copy(left = r.left + dx, bottom = r.bottom + dy)
                                        ReticleDragTarget.BOTTOM_RIGHT ->
                                            r.copy(right = r.right + dx, bottom = r.bottom + dy)
                                        else -> {
                                            val widthSpan = r.right - r.left
                                            val heightSpan = r.bottom - r.top
                                            val newLeft = (r.left + dx).coerceIn(0.02f, 0.98f - widthSpan)
                                            val newTop = (r.top + dy).coerceIn(0.02f, 0.98f - heightSpan)
                                            NormalizedRoi(
                                                left = newLeft,
                                                top = newTop,
                                                right = newLeft + widthSpan,
                                                bottom = newTop + heightSpan
                                            )
                                        }
                                    }
                                    currentOnUpdateRoi(updated)
                                },
                                onDragEnd = { activeDragTarget = ReticleDragTarget.NONE }
                            )
                        }
                        .testTag("viewport_canvas")
                ) {
                    val boxW = maxWidth
                    val boxH = maxHeight

                    uiState.currentFrameBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Live Screen Frame with In-Place Replaced Normal Font Text",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // High-DPI Native Compose In-Place Normal Font Replacement Boxes over active screen regions
                    if (uiState.inPlaceReplaceOnScreen && uiState.mergedRegions.isNotEmpty()) {
                        val isDarkScene = uiState.currentScene.isDarkScene
                        val bands = listOf(
                            Triple("TOP", 0.044f to 0.05f, 0.912f to 0.185f),
                            Triple("MID", 0.068f to 0.37f, 0.864f to 0.215f),
                            Triple("SUB", 0.038f to 0.715f, 0.924f to 0.228f)
                        )

                        bands.forEach { (tag, posPair, sizePair) ->
                            val region = uiState.mergedRegions.firstOrNull { it.tag == tag } ?: return@forEach
                            val bandTop = posPair.second
                            val bandBottom = bandTop + sizePair.second
                            val intersectsRoi = uiState.translateEverythingMode ||
                                (uiState.roi.bottom >= bandTop && uiState.roi.top <= bandBottom)

                            if (intersectsRoi && region.translatedText.isNotBlank()) {
                                Surface(
                                    color = if (isDarkScene) Color(0xF50F172A) else Color(0xF8FFFFFF),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDarkScene) CyberCyan.copy(alpha = 0.45f) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier
                                        .offset(
                                            x = boxW * posPair.first,
                                            y = boxH * posPair.second
                                        )
                                        .width(boxW * sizePair.first)
                                        .height(boxH * sizePair.second)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = region.translatedText,
                                            fontFamily = NormalSansFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 13.sp,
                                            lineHeight = 16.sp,
                                            color = if (isDarkScene) Color.White else Color(0xFF0F172A),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Overlay 48-Tile (8x6) Spatial Diff Grid and 4-Corner ROI Reticle
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cols = SpatialDiffAndOcrEngine.GRID_COLS
                        val rows = SpatialDiffAndOcrEngine.GRID_ROWS
                        val tileW = size.width / cols
                        val tileH = size.height / rows

                        for (row in 0 until rows) {
                            for (col in 0 until cols) {
                                val idx = row * cols + col
                                val diffVal = uiState.tileDiffs.getOrElse(idx) { 0.1f }
                                val left = col * tileW
                                val top = row * tileH

                                if (diffVal >= 0.22f && !uiState.inPlaceReplaceOnScreen) {
                                    drawRect(
                                        color = CyberEmerald.copy(alpha = (diffVal * 0.18f).coerceIn(0.04f, 0.18f)),
                                        topLeft = Offset(left, top),
                                        size = Size(tileW, tileH)
                                    )
                                }
                                drawRect(
                                    color = CyberCyan.copy(alpha = 0.10f),
                                    topLeft = Offset(left, top),
                                    size = Size(tileW, tileH),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }

                        if (!uiState.translateEverythingMode) {
                            val roi = uiState.roi
                            val rLeft = roi.left * size.width
                            val rTop = roi.top * size.height
                            val rRight = roi.right * size.width
                            val rBottom = roi.bottom * size.height
                            val rW = (rRight - rLeft).coerceAtLeast(20f)
                            val rH = (rBottom - rTop).coerceAtLeast(20f)

                            drawRect(
                                color = CyberCyan,
                                topLeft = Offset(rLeft, rTop),
                                size = Size(rW, rH),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))
                                )
                            )

                            val handleLen = 16.dp.toPx()
                            val strokeW = 3.5.dp.toPx()
                            val corners = listOf(
                                Offset(rLeft, rTop) to Pair(1f, 1f),
                                Offset(rRight, rTop) to Pair(-1f, 1f),
                                Offset(rLeft, rBottom) to Pair(1f, -1f),
                                Offset(rRight, rBottom) to Pair(-1f, -1f)
                            )
                            corners.forEach { (pt, dir) ->
                                drawLine(
                                    color = CyberCyan,
                                    start = pt,
                                    end = Offset(pt.x + dir.first * handleLen, pt.y),
                                    strokeWidth = strokeW
                                )
                                drawLine(
                                    color = CyberCyan,
                                    start = pt,
                                    end = Offset(pt.x, pt.y + dir.second * handleLen),
                                    strokeWidth = strokeW
                                )
                                drawCircle(
                                    color = CyberEmerald,
                                    radius = 5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }

                    // Overlay mode badge at top-right of canvas
                    Surface(
                        color = CyberBackground.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        val badgeText = if (uiState.inPlaceReplaceOnScreen) {
                            "IN-PLACE NORMAL FONT REPLACEMENT"
                        } else {
                            "ORIGINAL SCREEN TEXT"
                        }
                        Text(
                            text = badgeText,
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            color = if (uiState.inPlaceReplaceOnScreen) CyberEmerald else CyberAmber,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // One-Click ROI Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "ROI Presets",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    RoiPreset.entries.forEach { preset ->
                        val isSelected = uiState.activePreset == preset
                        Surface(
                            color = if (isSelected) CyberCyan.copy(alpha = 0.22f) else CyberSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isSelected) CyberCyan else CyberBorder),
                            modifier = Modifier
                                .clickable { onApplyRoiPreset(preset) }
                                .testTag("roi_preset_${preset.name.lowercase()}")
                        ) {
                            Text(
                                text = preset.label,
                                fontFamily = NormalSansFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (isSelected) CyberCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Live Recognized & Translated Preview Banner under Canvas (Rendered in clean Normal Sans-Serif Font)
                Surface(
                    color = CyberBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRANSLATED TEXT (NORMAL FONT) • [${uiState.liveDetectedLangCode} -> English]",
                                fontFamily = NormalSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = CyberEmerald
                            )
                            if (uiState.liveUsedGlossary) {
                                Text(
                                    text = "0.1ms GLOSSARY HIT",
                                    fontFamily = NormalSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = CyberCyan
                                )
                            }
                        }
                        Text(
                            text = uiState.liveTranslatedText,
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Original: ${uiState.liveSourceText}",
                            fontFamily = NormalSansFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
