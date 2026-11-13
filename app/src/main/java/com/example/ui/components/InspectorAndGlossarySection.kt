package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GlossaryEntryEntity
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

@Composable
fun OcrVisionInspectorCard(
    uiState: ScreenLensUiState,
    onTriggerOutboundProbe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vision = uiState.preprocessedVision
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = CyberSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ocr_vision_inspector_card")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "OCR Vision Inspector",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "OCR VISION INSPECTOR (SMART PREPROCESSOR)",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = CyberCyan
                            )
                            Text(
                                text = "Polarity Auto-Invert • Contrast Stretch • 2× Bilinear Upscale",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        color = CyberEmerald.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CyberEmerald)
                    ) {
                        Text(
                            text = "${uiState.ocrTierMode.label}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = CyberEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Live 2x Upscaled & Polarity-Normalized Crop Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (vision != null) {
                        Image(
                            bitmap = vision.processedBitmap.asImageBitmap(),
                            contentDescription = "Preprocessed 2x OCR Crop",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp)
                        )
                    } else {
                        Text(
                            text = "Initializing Smart Preprocessor Crop...",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Preprocessor Telemetry Chips
                if (vision != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InspectorMetricBadge(
                            label = "BG Polarity",
                            value = if (vision.isDarkBackgroundDetected) "DARK (Auto-Inverted)" else "LIGHT (Direct)",
                            color = if (vision.autoInvertedApplied) CyberAmber else CyberEmerald
                        )
                        InspectorMetricBadge(
                            label = "Upscale",
                            value = "${vision.upscaleFactor}× (${vision.processedBitmap.width}×${vision.processedBitmap.height}px)",
                            color = CyberCyan
                        )
                        InspectorMetricBadge(
                            label = "Contrast Stretch",
                            value = String.format(Locale.US, "%.2f× Gain", vision.contrastStretchRatio),
                            color = CyberCyan
                        )
                        InspectorMetricBadge(
                            label = "Glyph Ink Density",
                            value = String.format(Locale.US, "%.1f%% (%d lines)", vision.glyphDensityPercent, vision.activePixelLines),
                            color = CyberEmerald
                        )
                    }
                }
            }
        }

        // Card 2: Offline Firewall Verification & Blocked Outbound Log
        Surface(
            color = CyberSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            imageVector = Icons.Default.Security,
                            contentDescription = "Offline Mode Firewall",
                            tint = CyberEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "ZERO-CLOUD NETWORK INTERCEPTOR PROOF",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = CyberEmerald
                            )
                            Text(
                                text = "Intercepts outbound fetch / HTTP calls to prove 100% local execution",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onTriggerOutboundProbe,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color(0xFF031926)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("inspector_test_probe_button")
                    ) {
                        Text(
                            text = "Simulate Cloud Call (Block)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                uiState.blockedRequests.take(5).forEach { ev ->
                    Surface(
                        color = CyberBackground,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "[${ev.timestampStr}] ${ev.protocol}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = CyberAmber
                                )
                                Text(
                                    text = ev.statusNote,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = CyberEmerald
                                )
                            }
                            Text(
                                text = "Blocked Target: ${ev.endpoint}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomGlossaryAndSandboxSection(
    uiState: ScreenLensUiState,
    glossaryEntries: List<GlossaryEntryEntity>,
    onSandboxInputChange: (String) -> Unit,
    onAddGlossaryOverride: (String, String, String) -> Unit,
    onDeleteGlossaryOverride: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var sourceInput by remember { mutableStateOf("") }
    var englishInput by remember { mutableStateOf("") }
    var langCodeInput by remember { mutableStateOf("AUTO") }

    val sampleLanguageChips = listOf(
        "JA" to "諦めないで！領域展開システム起動",
        "KO" to "성문을 열어라! 어둠의 군주가 깨어나기 전에 봉인해야 한다!",
        "ZH" to "所有机密图像均在本地设备完成识别，绝不上传网络。",
        "HI" to "देशभर में डिजिटल तकनीक और ऑफलाइन अनुवाद का तेजी से विस्तार हो रहा है।",
        "MR" to "महाराष्ट्र वृत्तवाहिनी • थेट प्रक्षेपण",
        "ES" to "¡Increíble jugada del equipo campeón para asegurar la victoria en el último segundo!",
        "FR" to "Les souvenirs que nous gardons en silence finissent toujours par éclairer notre chemin.",
        "DE" to "Unsere neue optische Architektur übersetzt Bildschirminhalte vollkommen ohne Internetverbindung.",
        "RU" to "Экипаж успешно завершил стыковку исследовательского модуля в автоматическом режиме.",
        "AR" to "تعتمد الأنظمة الحديثة على الترجمة الفورية بدون اتصال بالإنترنت لحماية خصوصية المستخدم.",
        "PT" to "Tradução em tempo real totalmente offline ativada no dispositivo.",
        "IT" to "Traduzione visiva istantanea senza connessione cloud attiva.",
        "TR" to "Çevrimdışı ekran çevirisi yüksek hızda çalışıyor.",
        "VI" to "Hệ thống dịch màn hình ngoại tuyến đang hoạt động ở tốc độ cao.",
        "ID" to "Penerjemah layar offline berjalan otomatis tanpa koneksi internet."
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: 15+ Language Live Offline Sandbox
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "15+ Language Offline Translator Sandbox",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "15+ LANGUAGE EMBEDDED OFFLINE SANDBOX (< 3ms)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "Supports JA, KO, ZH, HI, MR, ES, FR, DE, RU, AR, PT, IT, TR, VI, ID -> English",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Quick-fill 15 Language Sample Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleLanguageChips.forEach { (code, phrase) ->
                        Surface(
                            color = CyberSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { onSandboxInputChange(phrase) }
                                .testTag("sandbox_sample_$code")
                        ) {
                            Text(
                                text = "Test $code",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.sandboxInput,
                    onValueChange = onSandboxInputChange,
                    label = { Text("Enter any foreign phrase or test glossary override") },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandbox_input_field")
                )

                uiState.sandboxResult?.let { res ->
                    Surface(
                        color = CyberBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyberEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "DETECTED: ${res.detectedLangCode} (${res.detectedLangName})",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = CyberCyan
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "LATENCY: %.2fms %s",
                                        res.latencyMs,
                                        if (res.usedGlossaryOverride) "• [0.1ms GLOSSARY]" else ""
                                    ),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = CyberEmerald
                                )
                            }
                            Text(
                                text = res.translatedText,
                                fontFamily = NormalSansFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                color = TextPrimary
                            )
                            if (res.grammarReordered) {
                                Text(
                                    text = "Grammar Engine: Applied SOV -> SVO syntactic clause reordering",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Persisted Custom Offline Glossary (0.1ms Priority Overrides)
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
                Text(
                    text = "CUSTOM OFFLINE GLOSSARY (0.1MS PRIORITY OVERRIDES)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyberEmerald
                )
                Text(
                    text = "Persisted locally in Room SQLite. Matches override dictionary translation in 0.1ms.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sourceInput,
                        onValueChange = { sourceInput = it },
                        label = { Text("Source Phrase") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("glossary_source_input")
                    )
                    OutlinedTextField(
                        value = englishInput,
                        onValueChange = { englishInput = it },
                        label = { Text("English Override") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberEmerald,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("glossary_english_input")
                    )
                }

                Button(
                    onClick = {
                        if (sourceInput.isNotBlank() && englishInput.isNotBlank()) {
                            onAddGlossaryOverride(sourceInput, englishInput, langCodeInput)
                            sourceInput = ""
                            englishInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberEmerald,
                        contentColor = Color(0xFF022C22)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_glossary_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Glossary Override",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add 0.1ms Priority Glossary Rule",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Active Glossary List
                glossaryEntries.forEach { entry ->
                    Surface(
                        color = CyberBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "[${entry.languageCode}]",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = entry.sourcePhrase,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "-> ${entry.englishTranslation}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = CyberEmerald
                                )
                            }

                            IconButton(
                                onClick = { onDeleteGlossaryOverride(entry.id) },
                                modifier = Modifier.testTag("delete_glossary_${entry.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Glossary Entry",
                                    tint = CyberRose
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorMetricBadge(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = CyberBackground,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = color
            )
        }
    }
}
