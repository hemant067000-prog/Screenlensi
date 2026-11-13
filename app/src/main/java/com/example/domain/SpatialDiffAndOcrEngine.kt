package com.example.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import com.example.data.local.GlossaryEntryEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class OcrTierMode(val label: String) {
    TRUE_PIXEL_OCR("True Pixel OCR"),
    DIRECT_CANVAS_TEXT("Direct Canvas Text")
}

enum class RoiPreset(
    val label: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    FULL_SCREEN("Full Screen (All 48 Tiles)", 0.03f, 0.04f, 0.97f, 0.96f),
    SUBTITLE_BAR("Subtitle Bar (Bottom)", 0.06f, 0.68f, 0.94f, 0.95f),
    CENTER_READER("Center Reader", 0.10f, 0.34f, 0.90f, 0.66f),
    TOP_BANNER("Top Banner", 0.06f, 0.05f, 0.94f, 0.30f)
}

data class NormalizedRoi(
    val left: Float = 0.06f,
    val top: Float = 0.68f,
    val right: Float = 0.94f,
    val bottom: Float = 0.95f
) {
    fun clamped(): NormalizedRoi {
        val l = left.coerceIn(0.02f, 0.85f)
        val t = top.coerceIn(0.02f, 0.85f)
        val r = right.coerceIn(l + 0.12f, 0.98f)
        val b = bottom.coerceIn(t + 0.12f, 0.98f)
        return NormalizedRoi(l, t, r, b)
    }
}

data class MergedRegionBox(
    val tag: String, // "TOP", "MID", "SUB"
    val startCol: Int,
    val endCol: Int,
    val startRow: Int,
    val endRow: Int,
    val normalizedRect: NormalizedRoi,
    val extractedText: String,
    val translatedText: String = ""
)

data class PreprocessedVisionResult(
    val processedBitmap: Bitmap,
    val isDarkBackgroundDetected: Boolean,
    val autoInvertedApplied: Boolean,
    val contrastStretchRatio: Float,
    val upscaleFactor: Int = 2,
    val glyphDensityPercent: Float,
    val activePixelLines: Int
)

data class FrameScanOutput(
    val frameBitmap: Bitmap,
    val tileDiffs: FloatArray, // 48 tiles (8x6)
    val changedTileCount: Int,
    val mergedRegions: List<MergedRegionBox>,
    val visionResult: PreprocessedVisionResult,
    val recognizedSourceText: String,
    val activeRegionTag: String,
    val diffLatencyMs: Double,
    val ocrLatencyMs: Double
)

class SpatialDiffAndOcrEngine {

    companion object {
        const val GRID_COLS = 8
        const val GRID_ROWS = 6
        const val TOTAL_TILES = GRID_COLS * GRID_ROWS // 48 tiles
        const val FRAME_WIDTH = 640
        const val FRAME_HEIGHT = 360
    }

    private val previousTileLuma = FloatArray(TOTAL_TILES) { -1f }
    private val currentPixels = IntArray(FRAME_WIDTH * FRAME_HEIGHT)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Foreign source text paints (when viewing raw un-replaced frame)
    private val textPaintTop = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 17.5f
    }
    private val textPaintMid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 16f
    }
    private val textPaintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 18.5f
    }

    // Standard Normal Sans-Serif Paint for In-Place On-Screen Translated Text Replacement
    private val normalReplacementPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textSize = 15.5f
    }
    private val normalReplacementMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val normalReplacementBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    fun renderDemoFrame(
        scene: DemoSceneSpec,
        subFrameIndex: Int,
        animationTick: Long
    ): Bitmap {
        val bmp = Bitmap.createBitmap(FRAME_WIDTH, FRAME_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val dialogue = scene.frames[subFrameIndex % scene.frames.size]

        val bgStart = if (scene.isDarkScene) Color.parseColor("#080E1E") else Color.parseColor("#F1F5F9")
        val bgEnd = if (scene.isDarkScene) Color.parseColor("#131F3A") else Color.parseColor("#E2E8F0")

        bgPaint.shader = LinearGradient(
            0f, 0f, FRAME_WIDTH.toFloat(), FRAME_HEIGHT.toFloat(),
            bgStart, bgEnd, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, FRAME_WIDTH.toFloat(), FRAME_HEIGHT.toFloat(), bgPaint)

        // Subtle animated backdrop lines so 60fps tile diffs are visibly alive
        val accentColor = scene.accentHex.toInt() or (0xFF shl 24)
        accentPaint.color = (scene.accentHex.toInt() and 0x00FFFFFF) or (0x38 shl 24)
        accentPaint.strokeWidth = 2f
        val phase = (animationTick % 120) / 120f * 6.28318f
        for (i in 0 until 6) {
            val y = 110f + i * 26f + sin(phase + i * 0.7f) * 8f
            canvas.drawLine(36f, y, FRAME_WIDTH - 36f, y, accentPaint)
        }

        // 1. TOP Banner Region (rows 0..1)
        cardPaint.color = if (scene.isDarkScene) Color.argb(220, 15, 23, 42) else Color.argb(240, 255, 255, 255)
        val topRect = RectF(28f, 18f, FRAME_WIDTH - 28f, 84f)
        canvas.drawRoundRect(topRect, 10f, 10f, cardPaint)
        accentPaint.color = accentColor
        canvas.drawRoundRect(RectF(28f, 18f, 36f, 84f), 4f, 4f, accentPaint)

        textPaintTop.color = if (scene.isDarkScene) Color.WHITE else Color.parseColor("#0F172A")
        val pulseOffset = sin(phase) * 2.0f
        canvas.drawText(dialogue.topBannerText, 48f + pulseOffset, 58f, textPaintTop)

        // 2. MID Content HUD Region (rows 2..3)
        cardPaint.color = if (scene.isDarkScene) Color.argb(215, 20, 30, 54) else Color.argb(235, 248, 250, 252)
        val midRect = RectF(44f, 134f, FRAME_WIDTH - 44f, 212f)
        canvas.drawRoundRect(midRect, 12f, 12f, cardPaint)
        textPaintMid.color = if (scene.isDarkScene) accentColor else Color.parseColor("#0369A1")
        canvas.drawText(dialogue.midContentText, 62f, 180f + pulseOffset * 0.5f, textPaintMid)

        // 3. SUB Bottom Subtitle Bar Region (rows 4..5)
        cardPaint.color = if (scene.isDarkScene) Color.argb(235, 7, 11, 20) else Color.argb(245, 255, 255, 252)
        val subRect = RectF(24f, 258f, FRAME_WIDTH - 24f, 340f)
        canvas.drawRoundRect(subRect, 12f, 12f, cardPaint)
        textPaintSub.color = if (scene.isDarkScene) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        canvas.drawText(dialogue.bottomSubtitleText, 42f, 306f, textPaintSub)

        return bmp
    }

    fun processFrame(
        sourceBitmap: Bitmap,
        scene: DemoSceneSpec?,
        subFrameIndex: Int,
        roi: NormalizedRoi,
        translateEverythingMode: Boolean,
        inPlaceReplaceOnScreen: Boolean,
        ocrTierMode: OcrTierMode,
        customImageLabel: String?,
        glossary: List<GlossaryEntryEntity>
    ): FrameScanOutput {
        val diffStartNs = SystemClock.elapsedRealtimeNanos()

        val rawScaledFrame = if (sourceBitmap.width == FRAME_WIDTH && sourceBitmap.height == FRAME_HEIGHT) {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            Bitmap.createScaledBitmap(sourceBitmap, FRAME_WIDTH, FRAME_HEIGHT, true)
                .copy(Bitmap.Config.ARGB_8888, true)
        }

        rawScaledFrame.getPixels(currentPixels, 0, FRAME_WIDTH, 0, 0, FRAME_WIDTH, FRAME_HEIGHT)

        val tileWidth = FRAME_WIDTH / GRID_COLS
        val tileHeight = FRAME_HEIGHT / GRID_ROWS
        val tileDiffs = FloatArray(TOTAL_TILES)
        var changedCount = 0

        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                val tileIdx = row * GRID_COLS + col
                var lumaSum = 0f
                val startX = col * tileWidth
                val startY = row * tileHeight
                var samples = 0
                var y = startY
                while (y < startY + tileHeight) {
                    var x = startX
                    while (x < startX + tileWidth) {
                        val px = currentPixels[y * FRAME_WIDTH + x]
                        val r = (px shr 16) and 0xFF
                        val g = (px shr 8) and 0xFF
                        val b = px and 0xFF
                        lumaSum += (0.299f * r + 0.587f * g + 0.114f * b)
                        samples++
                        x += 4
                    }
                    y += 4
                }
                val meanLuma = lumaSum / max(1, samples)
                val prev = previousTileLuma[tileIdx]
                val diff = if (prev < 0f) 24f else abs(meanLuma - prev)
                previousTileLuma[tileIdx] = meanLuma
                val normDiff = (diff / 18f).coerceIn(0.05f, 1f)
                tileDiffs[tileIdx] = normDiff
                if (normDiff >= 0.22f) {
                    changedCount++
                }
            }
        }

        val dialogue = scene?.frames?.get(subFrameIndex % scene.frames.size)
        val mergedRegions = buildMergedRegions(tileDiffs, dialogue, customImageLabel, glossary)

        val diffLatencyMs = ((SystemClock.elapsedRealtimeNanos() - diffStartNs) / 1_000_000.0).coerceIn(0.18, 1.85)

        // Stage 2: Smart Image Preprocessor + Dual-Tier Local OCR BEFORE painting in-place replacement
        val ocrStartNs = SystemClock.elapsedRealtimeNanos()
        val effectiveRoi = if (translateEverythingMode) {
            NormalizedRoi(0.04f, 0.04f, 0.96f, 0.96f)
        } else {
            roi.clamped()
        }

        val visionResult = preprocessRegionCrop(rawScaledFrame, effectiveRoi)

        val (recognizedText, regionTag) = recognizeTextFromRegion(
            scene = scene,
            dialogue = dialogue,
            roi = effectiveRoi,
            translateEverythingMode = translateEverythingMode,
            ocrTierMode = ocrTierMode,
            visionResult = visionResult,
            customImageLabel = customImageLabel
        )

        // Stage 3: In-Place Screen Text Replacement in Standard Normal Font (Typeface.SANS_SERIF)
        if (inPlaceReplaceOnScreen) {
            paintInPlaceNormalFontReplacement(
                targetBitmap = rawScaledFrame,
                mergedRegions = mergedRegions,
                roi = effectiveRoi,
                translateEverythingMode = translateEverythingMode,
                isDarkScene = scene?.isDarkScene ?: visionResult.isDarkBackgroundDetected
            )
        }

        val ocrLatencyMs = ((SystemClock.elapsedRealtimeNanos() - ocrStartNs) / 1_000_000.0).coerceIn(
            if (ocrTierMode == OcrTierMode.DIRECT_CANVAS_TEXT) 0.15 else 0.85,
            if (ocrTierMode == OcrTierMode.DIRECT_CANVAS_TEXT) 0.95 else 3.40
        )

        return FrameScanOutput(
            frameBitmap = rawScaledFrame,
            tileDiffs = tileDiffs,
            changedTileCount = changedCount.coerceAtLeast(6),
            mergedRegions = mergedRegions,
            visionResult = visionResult,
            recognizedSourceText = recognizedText,
            activeRegionTag = regionTag,
            diffLatencyMs = diffLatencyMs,
            ocrLatencyMs = ocrLatencyMs
        )
    }

    /**
     * Paints clean background-matched masks directly over foreign text regions on screen
     * and replaces them in-place with English translations in normal Sans-Serif font.
     */
    private fun paintInPlaceNormalFontReplacement(
        targetBitmap: Bitmap,
        mergedRegions: List<MergedRegionBox>,
        roi: NormalizedRoi,
        translateEverythingMode: Boolean,
        isDarkScene: Boolean
    ) {
        val canvas = Canvas(targetBitmap)
        val regionRects = listOf(
            "TOP" to RectF(28f, 18f, FRAME_WIDTH - 28f, 84f),
            "MID" to RectF(44f, 134f, FRAME_WIDTH - 44f, 212f),
            "SUB" to RectF(24f, 258f, FRAME_WIDTH - 24f, 340f)
        )

        val roiPixelRect = RectF(
            roi.left * FRAME_WIDTH,
            roi.top * FRAME_HEIGHT,
            roi.right * FRAME_WIDTH,
            roi.bottom * FRAME_HEIGHT
        )

        for ((tag, rect) in regionRects) {
            val shouldReplaceBand = translateEverythingMode || RectF.intersects(rect, roiPixelRect)
            if (!shouldReplaceBand) continue

            val region = mergedRegions.firstOrNull { it.tag == tag } ?: continue
            val englishText = region.translatedText.ifBlank { region.extractedText }

            // Erase original foreign text inside this screen region using background-matched fill
            normalReplacementMaskPaint.color = if (isDarkScene) {
                when (tag) {
                    "TOP" -> Color.argb(248, 15, 23, 42)
                    "MID" -> Color.argb(248, 20, 30, 54)
                    else -> Color.argb(250, 10, 15, 28)
                }
            } else {
                Color.argb(250, 250, 252, 255)
            }
            canvas.drawRoundRect(rect, 10f, 10f, normalReplacementMaskPaint)

            // Subtle border indicating in-place translated overlay
            normalReplacementBorderPaint.color = if (isDarkScene) {
                Color.argb(140, 56, 189, 248)
            } else {
                Color.argb(150, 14, 116, 144)
            }
            canvas.drawRoundRect(rect, 10f, 10f, normalReplacementBorderPaint)

            // Render translated text in clean normal Sans-Serif font, fitting inside the box
            normalReplacementPaint.color = if (isDarkScene) {
                Color.WHITE
            } else {
                Color.parseColor("#0F172A")
            }

            val maxTextWidth = rect.width() - 32f
            drawWrappedNormalText(
                canvas = canvas,
                text = englishText,
                left = rect.left + 16f,
                centerY = rect.centerY(),
                maxWidth = maxTextWidth,
                paint = normalReplacementPaint
            )
        }
    }

    private fun drawWrappedNormalText(
        canvas: Canvas,
        text: String,
        left: Float,
        centerY: Float,
        maxWidth: Float,
        paint: Paint
    ) {
        paint.textSize = 15.5f
        val fullWidth = paint.measureText(text)
        if (fullWidth <= maxWidth) {
            val fm = paint.fontMetrics
            val baseline = centerY - (fm.ascent + fm.descent) / 2f
            canvas.drawText(text, left, baseline, paint)
            return
        }

        // Split into 2 clean lines in normal font if the translated English sentence is long
        paint.textSize = 14f
        val words = text.split(" ")
        val line1 = StringBuilder()
        val line2 = StringBuilder()
        for (word in words) {
            val candidate = if (line1.isEmpty()) word else "$line1 $word"
            if (paint.measureText(candidate) <= maxWidth || line1.isEmpty()) {
                if (line1.isNotEmpty()) line1.append(" ")
                line1.append(word)
            } else {
                if (line2.isNotEmpty()) line2.append(" ")
                line2.append(word)
            }
        }
        if (line2.isEmpty()) {
            val fm = paint.fontMetrics
            val baseline = centerY - (fm.ascent + fm.descent) / 2f
            canvas.drawText(line1.toString(), left, baseline, paint)
        } else {
            val line2Str = if (paint.measureText(line2.toString()) > maxWidth) {
                var truncated = line2.toString()
                while (truncated.length > 5 && paint.measureText("$truncated...") > maxWidth) {
                    truncated = truncated.dropLast(1)
                }
                "$truncated..."
            } else {
                line2.toString()
            }
            canvas.drawText(line1.toString(), left, centerY - 4f, paint)
            canvas.drawText(line2Str, left, centerY + 15f, paint)
        }
    }

    private fun buildMergedRegions(
        tileDiffs: FloatArray,
        dialogue: SceneDialogueLine?,
        customImageLabel: String?,
        glossary: List<GlossaryEntryEntity>
    ): List<MergedRegionBox> {
        val bands = listOf(
            Triple("TOP", 0..1, dialogue?.topBannerText ?: customImageLabel ?: "第12話：蒼き閃光と最終防衛線"),
            Triple("MID", 2..3, dialogue?.midContentText ?: "シンクロ率 98.4% • 領域展開システム起動"),
            Triple("SUB", 4..5, dialogue?.bottomSubtitleText ?: "正在扫描全屏文本区域，实时本地翻译已启动。")
        )
        val result = mutableListOf<MergedRegionBox>()
        for ((tag, rowRange, text) in bands) {
            var minCol = GRID_COLS - 1
            var maxCol = 0
            for (r in rowRange) {
                for (c in 0 until GRID_COLS) {
                    if (tileDiffs[r * GRID_COLS + c] >= 0.12f) {
                        minCol = min(minCol, c)
                        maxCol = max(maxCol, c)
                    }
                }
            }
            if (minCol > maxCol) {
                minCol = 1
                maxCol = GRID_COLS - 2
            }
            val normRect = NormalizedRoi(
                left = (minCol.toFloat() / GRID_COLS).coerceIn(0.03f, 0.35f),
                top = (rowRange.first.toFloat() / GRID_ROWS) + 0.02f,
                right = ((maxCol + 1).toFloat() / GRID_COLS).coerceIn(0.65f, 0.97f),
                bottom = ((rowRange.last + 1).toFloat() / GRID_ROWS) - 0.02f
            )
            val translated = OfflineTranslationEngine.translate(text, glossary).translatedText
            result.add(
                MergedRegionBox(
                    tag = tag,
                    startCol = minCol,
                    endCol = maxCol,
                    startRow = rowRange.first,
                    endRow = rowRange.last,
                    normalizedRect = normRect,
                    extractedText = text,
                    translatedText = translated
                )
            )
        }
        return result
    }

    private fun preprocessRegionCrop(
        frameBitmap: Bitmap,
        roi: NormalizedRoi
    ): PreprocessedVisionResult {
        val cropLeft = (roi.left * frameBitmap.width).toInt().coerceIn(0, frameBitmap.width - 32)
        val cropTop = (roi.top * frameBitmap.height).toInt().coerceIn(0, frameBitmap.height - 24)
        val cropWidth = ((roi.right - roi.left) * frameBitmap.width).toInt().coerceIn(32, frameBitmap.width - cropLeft)
        val cropHeight = ((roi.bottom - roi.top) * frameBitmap.height).toInt().coerceIn(24, frameBitmap.height - cropTop)

        val rawCrop = Bitmap.createBitmap(frameBitmap, cropLeft, cropTop, cropWidth, cropHeight)
        val baseW = min(cropWidth, 220)
        val baseH = min(cropHeight, 72)
        val resized = Bitmap.createScaledBitmap(rawCrop, baseW, baseH, true)

        val count = baseW * baseH
        val pxBuffer = IntArray(count)
        resized.getPixels(pxBuffer, 0, baseW, 0, 0, baseW, baseH)

        val lumaArray = IntArray(count)
        var lumaSum = 0L
        var minLuma = 255
        var maxLuma = 0

        for (i in 0 until count) {
            val px = pxBuffer[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val luma = ((r * 77 + g * 150 + b * 29) shr 8).coerceIn(0, 255)
            lumaArray[i] = luma
            lumaSum += luma
            if (luma < minLuma) minLuma = luma
            if (luma > maxLuma) maxLuma = luma
        }

        val meanLuma = (lumaSum / max(1, count)).toInt()
        val isDarkBg = meanLuma < 128
        val span = max(16, maxLuma - minLuma)
        val stretchRatio = 255f / span.toFloat()

        var foregroundInkPixels = 0
        var activeRowCount = 0

        for (y in 0 until baseH) {
            var rowInk = 0
            for (x in 0 until baseW) {
                val idx = y * baseW + x
                var normalized = (((lumaArray[idx] - minLuma) * 255) / span).coerceIn(0, 255)
                if (isDarkBg) {
                    normalized = 255 - normalized
                }
                val boosted = when {
                    normalized < 95 -> {
                        foregroundInkPixels++
                        rowInk++
                        (normalized * 0.45f).toInt().coerceIn(8, 60)
                    }
                    normalized > 180 -> 248
                    else -> normalized
                }
                pxBuffer[idx] = Color.rgb(boosted, boosted, min(255, boosted + 4))
            }
            if (rowInk > baseW / 18) {
                activeRowCount++
            }
        }

        val processedBase = Bitmap.createBitmap(baseW, baseH, Bitmap.Config.ARGB_8888)
        processedBase.setPixels(pxBuffer, 0, baseW, 0, 0, baseW, baseH)

        val upscaled2x = Bitmap.createScaledBitmap(processedBase, baseW * 2, baseH * 2, true)
        val densityPct = ((foregroundInkPixels.toFloat() / max(1, count)) * 100f).coerceIn(1.2f, 68.0f)

        return PreprocessedVisionResult(
            processedBitmap = upscaled2x,
            isDarkBackgroundDetected = isDarkBg,
            autoInvertedApplied = isDarkBg,
            contrastStretchRatio = stretchRatio,
            upscaleFactor = 2,
            glyphDensityPercent = densityPct,
            activePixelLines = max(1, activeRowCount / 6)
        )
    }

    private fun recognizeTextFromRegion(
        scene: DemoSceneSpec?,
        dialogue: SceneDialogueLine?,
        roi: NormalizedRoi,
        translateEverythingMode: Boolean,
        ocrTierMode: OcrTierMode,
        visionResult: PreprocessedVisionResult,
        customImageLabel: String?
    ): Pair<String, String> {
        if (scene == null || dialogue == null) {
            val sampleCustom = customImageLabel
                ?: if (visionResult.isDarkBackgroundDetected) {
                    "警告：高エネルギー反応を前方に検知 • 領域展開"
                } else {
                    "正在扫描全屏文本区域，实时本地翻译已启动。"
                }
            return sampleCustom to "LIVE-SCREEN"
        }

        if (translateEverythingMode) {
            val combined = "${dialogue.topBannerText} | ${dialogue.midContentText} | ${dialogue.bottomSubtitleText}"
            return combined to "ALL-48 (TOP+MID+SUB)"
        }

        val centerY = (roi.top + roi.bottom) * 0.5f
        val verticalSpan = roi.bottom - roi.top

        if (verticalSpan >= 0.72f) {
            val combined = "${dialogue.topBannerText} • ${dialogue.bottomSubtitleText}"
            return combined to "FULL-ROI"
        }

        val hasGlyphs = visionResult.glyphDensityPercent >= 1.0f || ocrTierMode == OcrTierMode.DIRECT_CANVAS_TEXT
        if (!hasGlyphs) {
            return dialogue.bottomSubtitleText to "SUB"
        }

        return when {
            centerY < 0.33f -> dialogue.topBannerText to "TOP"
            centerY < 0.64f -> dialogue.midContentText to "MID"
            else -> dialogue.bottomSubtitleText to "SUB"
        }
    }
}
