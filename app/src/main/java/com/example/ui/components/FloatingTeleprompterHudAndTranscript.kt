package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SubtitleRecordEntity
import com.example.ui.HudFontSize
import com.example.ui.ScreenLensUiState
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRose
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NormalSansFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Rendered when the user leaves the app and ScreenLens floats over other apps in Android Picture-in-Picture (PiP) mode.
 * Displays the live screen with in-place replaced normal-font English text.
 */
@Composable
fun PictureInPictureFloatingOverlay(
    uiState: ScreenLensUiState,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        val boxW = maxWidth
        val boxH = maxHeight

        uiState.currentFrameBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "PiP Live In-Place Replaced Screen",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        // High-DPI In-Place Normal Font Replacement overlays inside the floating PiP window
        val isDarkScene = uiState.currentScene.isDarkScene
        val bands = listOf(
            Triple("TOP", 0.04f to 0.04f, 0.92f to 0.20f),
            Triple("MID", 0.06f to 0.36f, 0.88f to 0.22f),
            Triple("SUB", 0.03f to 0.68f, 0.94f to 0.26f)
        )
        bands.forEach { (tag, posPair, sizePair) ->
            val region = uiState.mergedRegions.firstOrNull { it.tag == tag } ?: return@forEach
            if (region.translatedText.isNotBlank()) {
                Surface(
                    color = if (isDarkScene) Color(0xF50F172A) else Color(0xF8FFFFFF),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(
                        0.8.dp,
                        if (isDarkScene) CyberCyan.copy(alpha = 0.5f) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .offset(x = boxW * posPair.first, y = boxH * posPair.second)
                        .width(boxW * sizePair.first)
                        .height(boxH * sizePair.second)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = region.translatedText,
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            color = if (isDarkScene) Color.White else Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DraggableFloatingTeleprompterHud(
    uiState: ScreenLensUiState,
    onSelectFontSize: (HudFontSize) -> Unit,
    onToggleOpacity: () -> Unit,
    onToggleSourceLine: () -> Unit,
    onTogglePause: () -> Unit,
    onCloseHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val hudAlpha = if (uiState.hudHighOpacity) 0.95f else 0.74f

    Surface(
        color = CyberBackground.copy(alpha = hudAlpha),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, CyberCyan),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(-240f, 240f)
                    offsetY = (offsetY + dragAmount.y).coerceIn(-520f, 120f)
                }
            }
            .testTag("floating_teleprompter_hud")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag Teleprompter HUD",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Surface(
                        color = CyberEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, CyberEmerald)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isHudPaused) CyberAmber else CyberEmerald)
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%s -> EN • %.1fms • NORMAL FONT",
                                    uiState.liveDetectedLangCode,
                                    uiState.totalLatencyMs
                                ),
                                fontFamily = NormalSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                color = CyberEmerald
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HudFontSize.entries.forEach { sizeOption ->
                        val active = uiState.hudFontSize == sizeOption
                        Surface(
                            color = if (active) CyberCyan else CyberSurfaceElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .clickable { onSelectFontSize(sizeOption) }
                                .testTag("hud_font_${sizeOption.label.lowercase()}")
                        ) {
                            Text(
                                text = sizeOption.label,
                                fontFamily = NormalSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (active) Color(0xFF031926) else TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        color = CyberSurfaceElevated,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier
                            .clickable { onToggleOpacity() }
                            .testTag("hud_opacity_toggle")
                    ) {
                        Text(
                            text = if (uiState.hudHighOpacity) "95%" else "74%",
                            fontFamily = NormalSansFontFamily,
                            fontSize = 10.sp,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = if (uiState.hudShowSourceLine) CyberCyan.copy(alpha = 0.22f) else CyberSurfaceElevated,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (uiState.hudShowSourceLine) CyberCyan else CyberBorder),
                        modifier = Modifier
                            .clickable { onToggleSourceLine() }
                            .testTag("hud_source_toggle")
                    ) {
                        Text(
                            text = "SRC",
                            fontFamily = NormalSansFontFamily,
                            fontSize = 10.sp,
                            color = if (uiState.hudShowSourceLine) CyberCyan else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = if (uiState.isHudPaused) CyberAmber.copy(alpha = 0.25f) else CyberSurfaceElevated,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (uiState.isHudPaused) CyberAmber else CyberBorder),
                        modifier = Modifier
                            .clickable { onTogglePause() }
                            .testTag("hud_pause_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isHudPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause or Resume HUD",
                                tint = if (uiState.isHudPaused) CyberAmber else CyberEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onCloseHud,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("hud_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Minimize Floating HUD",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Primary Live English Subtitle Line rendered in clean Normal Sans-Serif Font
            Text(
                text = uiState.liveTranslatedText,
                fontFamily = NormalSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = uiState.hudFontSize.scaleSp.sp,
                lineHeight = (uiState.hudFontSize.scaleSp + 6).sp,
                color = TextPrimary,
                modifier = Modifier.testTag("hud_english_subtitle")
            )

            if (uiState.hudShowSourceLine) {
                Text(
                    text = uiState.liveSourceText,
                    fontFamily = NormalSansFontFamily,
                    fontSize = (uiState.hudFontSize.scaleSp - 3).coerceAtLeast(11).sp,
                    color = CyberCyan.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LiveSubtitleTranscriptSection(
    subtitles: List<SubtitleRecordEntity>,
    onCopyAllToClipboard: () -> Unit,
    onCopySingleSubtitle: (SubtitleRecordEntity) -> Unit,
    onExportTxt: () -> Unit,
    onExportSrt: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                            text = "DEDUPLICATED LIVE SUBTITLE TRANSCRIPT (${subtitles.size})",
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "All translated lines rendered in normal font • Copy, .TXT, and .SRT export",
                            fontFamily = NormalSansFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_transcript_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Subtitle Transcript",
                            tint = CyberRose
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCopyAllToClipboard,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color(0xFF031926)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("copy_transcript_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Transcript",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy All",
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onExportTxt,
                        border = BorderStroke(1.dp, CyberEmerald),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("export_txt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Export .TXT",
                            tint = CyberEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export .TXT",
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberEmerald
                        )
                    }

                    OutlinedButton(
                        onClick = onExportSrt,
                        border = BorderStroke(1.dp, CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("export_srt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Export .SRT",
                            tint = CyberCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export .SRT",
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                    }
                }

                if (subtitles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Scanning screen regions... New deduplicated subtitles will appear here.",
                            fontFamily = NormalSansFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        subtitles.take(40).forEach { item ->
                            SubtitleFeedCard(
                                item = item,
                                onCopy = { onCopySingleSubtitle(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleFeedCard(
    item: SubtitleRecordEntity,
    onCopy: () -> Unit
) {
    val totalSec = (item.startTimeMs / 1000L).coerceAtLeast(0L)
    val mins = (totalSec / 60L) % 60L
    val secs = totalSec % 60L
    val timeBadge = String.format(Locale.US, "%02d:%02d", mins, secs)

    Surface(
        color = CyberBackground,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (item.wasOffScreen) CyberAmber.copy(alpha = 0.6f) else CyberBorder
        ),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${item.sequenceNumber} [$timeBadge]",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Surface(
                        color = CyberCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${item.detectedLangCode} • ${item.regionTag}",
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    if (item.wasOffScreen) {
                        Surface(
                            color = CyberAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "OFF-SCREEN",
                                fontFamily = NormalSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = CyberAmber,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fms", item.latencyMs),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = CyberEmerald
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Subtitle Line",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onCopy() }
                    )
                }
            }

            Text(
                text = item.translatedText,
                fontFamily = NormalSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = TextPrimary
            )
            Text(
                text = item.sourceText,
                fontFamily = NormalSansFontFamily,
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun TranscriptExportDialog(
    title: String,
    content: String,
    onCopyAndShare: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = title,
                fontFamily = NormalSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CyberCyan
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBackground)
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = content,
                    fontFamily = NormalSansFontFamily,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCopyAndShare,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberEmerald,
                    contentColor = Color(0xFF022C22)
                )
            ) {
                Text(
                    text = "Copy & Share File",
                    fontFamily = NormalSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    fontFamily = NormalSansFontFamily,
                    color = TextSecondary
                )
            }
        }
    )
}
