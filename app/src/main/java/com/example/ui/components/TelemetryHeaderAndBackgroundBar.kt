package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenLensUiState
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberAmberGlow
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanGlow
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberEmeraldGlow
import com.example.ui.theme.CyberRose
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NormalSansFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TelemetryHeaderAndBackgroundBar(
    uiState: ScreenLensUiState,
    onToggleFloatingHud: () -> Unit,
    onStartOrStopLiveScreenCapture: () -> Unit,
    onEnterPipWindowNow: () -> Unit,
    onToggleInPlaceReplacement: () -> Unit,
    onToggleTestOfflineMode: () -> Unit,
    onTriggerOutboundProbe: () -> Unit,
    onToggleSimulatedOffScreen: () -> Unit,
    onToggleWorkerHz: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Brand Title + Prominent 100% LOCAL • ZERO CLOUD API Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CyberCyan.copy(alpha = 0.25f), CyberEmerald.copy(alpha = 0.25f))
                            )
                        )
                        .border(1.dp, CyberCyan, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "ScreenLens Optical Core",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "ScreenLens Background",
                        fontFamily = NormalSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "In-Place Screen Text Replacement • Background PiP",
                        fontFamily = NormalSansFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                color = CyberEmeraldGlow,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, CyberEmerald),
                modifier = Modifier.testTag("zero_cloud_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Zero Cloud API",
                        tint = CyberEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "100% LOCAL • ZERO CLOUD API",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = CyberEmerald
                    )
                }
            }
        }

        // Row 2: Real-Time Pipeline Latency Bar (Diff | OCR | Translate | Total) + Network Indicator
        Surface(
            color = CyberSurface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pipeline_telemetry_bar")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val diffStr = String.format(Locale.US, "%.2fms", uiState.diffLatencyMs)
                    val ocrStr = String.format(Locale.US, "%.2fms", uiState.ocrLatencyMs)
                    val transStr = String.format(Locale.US, "%.2fms", uiState.translateLatencyMs)
                    val totalStr = String.format(Locale.US, "%.2fms", uiState.totalLatencyMs)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Pipeline Latency",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        TelemetryMetricPill("Diff", diffStr, CyberCyan)
                        Text("|", color = TextMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        TelemetryMetricPill("OCR", ocrStr, CyberCyan)
                        Text("|", color = TextMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        TelemetryMetricPill("Translate", transStr, CyberEmerald)
                        Text("|", color = TextMuted, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        TelemetryMetricPill("Total", totalStr, CyberEmerald, highlight = true)
                    }
                }

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
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isDeviceOnline) CyberEmerald else CyberAmber)
                        )
                        Text(
                            text = if (uiState.isDeviceOnline) {
                                "navigator.onLine: TRUE (AIR-GAPPED)"
                            } else {
                                "navigator.onLine: FALSE (OFFLINE)"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.testOfflineModeEnabled) Icons.Outlined.WifiOff else Icons.Outlined.Shield,
                            contentDescription = "Test Offline Firewall",
                            tint = if (uiState.testOfflineModeEnabled) CyberEmerald else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Test Offline Mode",
                            fontFamily = NormalSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.testOfflineModeEnabled) CyberEmerald else TextSecondary
                        )
                        Switch(
                            checked = uiState.testOfflineModeEnabled,
                            onCheckedChange = { onToggleTestOfflineMode() },
                            modifier = Modifier
                                .height(22.dp)
                                .testTag("test_offline_mode_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBackground,
                                checkedTrackColor = CyberEmerald,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = CyberSurfaceElevated
                            )
                        )
                        Surface(
                            color = CyberSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { onTriggerOutboundProbe() }
                                .testTag("probe_firewall_button")
                        ) {
                            Text(
                                text = "Block Probe (${uiState.blockedRequests.size})",
                                fontFamily = NormalSansFontFamily,
                                fontSize = 10.sp,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Row 3: Unthrottled Off-Screen Background Engine Status Strip
        val isOffScreen = uiState.isEffectivelyOffScreen
        val bgStatusColor by animateColorAsState(
            targetValue = if (isOffScreen) CyberAmber else CyberEmerald,
            label = "bgStatusColor"
        )
        Surface(
            color = if (isOffScreen) CyberAmberGlow else CyberSurface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, bgStatusColor.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("background_engine_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = bgStatusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, bgStatusColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(bgStatusColor)
                        )
                        Text(
                            text = if (isOffScreen) {
                                "OFF-SCREEN ACTIVE (UNTHROTTLED)"
                            } else {
                                "TAB FOCUSED"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = bgStatusColor
                        )
                    }
                }

                // In-Place Normal Font Replacement Chip
                Surface(
                    color = if (uiState.inPlaceReplaceOnScreen) CyberEmerald.copy(alpha = 0.22f) else CyberSurfaceElevated,
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, if (uiState.inPlaceReplaceOnScreen) CyberEmerald else CyberBorder),
                    modifier = Modifier
                        .clickable { onToggleInPlaceReplacement() }
                        .testTag("header_inplace_replace_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = "In-Place Replace in Normal Font",
                            tint = if (uiState.inPlaceReplaceOnScreen) CyberEmerald else TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (uiState.inPlaceReplaceOnScreen) {
                                "In-Place Normal Font: ON"
                            } else {
                                "In-Place Normal Font: OFF"
                            },
                            fontFamily = NormalSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = if (uiState.inPlaceReplaceOnScreen) CyberEmerald else TextSecondary
                        )
                    }
                }

                Surface(
                    color = CyberSurfaceElevated,
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clickable { onToggleWorkerHz() }
                        .testTag("worker_hz_chip")
                ) {
                    Text(
                        text = "Worker: ${uiState.measuredWorkerHz}Hz",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = "Audio Keep-Alive",
                        tint = if (uiState.isAudioKeepAliveActive) CyberEmerald else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Away: ${uiState.offScreenElapsedSec}s | Off-Screen Lines: ${uiState.offScreenLinesTranslated}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = TextPrimary
                    )
                }

                Surface(
                    color = if (uiState.isSimulatedOffScreen) CyberAmber.copy(alpha = 0.25f) else CyberSurfaceElevated,
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, if (uiState.isSimulatedOffScreen) CyberAmber else CyberBorder),
                    modifier = Modifier
                        .clickable { onToggleSimulatedOffScreen() }
                        .testTag("simulate_offscreen_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Simulate Off-Screen",
                            tint = if (uiState.isSimulatedOffScreen) CyberAmber else TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (uiState.isSimulatedOffScreen) "Resume App Focus" else "Test App-Closed Mode",
                            fontFamily = NormalSansFontFamily,
                            fontSize = 10.sp,
                            color = if (uiState.isSimulatedOffScreen) CyberAmber else TextSecondary
                        )
                    }
                }
            }
        }

        // Row 4: Dual Action Row — (1) Live Background Screen Capture + Floating PiP when app not open, (2) Auto-Float HUD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartOrStopLiveScreenCapture,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isMediaProjectionActive) CyberRose else CyberCyan,
                    contentColor = if (uiState.isMediaProjectionActive) Color.White else Color(0xFF031926)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(48.dp)
                    .testTag("live_screen_capture_button")
            ) {
                Icon(
                    imageVector = if (uiState.isMediaProjectionActive) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                    contentDescription = "Capture Screen While App Closed",
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.isMediaProjectionActive) {
                        "Stop Background Screen Capture"
                    } else {
                        "Translate My Screen (App Closed)"
                    },
                    fontFamily = NormalSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onEnterPipWindowNow,
                border = BorderStroke(1.5.dp, CyberEmerald),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp)
                    .testTag("activate_hud_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PictureInPictureAlt,
                    contentDescription = "Float Window Outside App",
                    tint = CyberEmerald,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Float Over Other Apps (PiP)",
                    fontFamily = NormalSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = CyberEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricPill(
    label: String,
    value: String,
    accent: Color,
    highlight: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = if (highlight) {
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(CyberCyanGlow)
                .padding(horizontal = 5.dp, vertical = 2.dp)
        } else {
            Modifier
        }
    ) {
        Text(
            text = "$label:",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = accent
        )
    }
}
