package com.voiceshield.ai.ui.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceshield.ai.domain.model.RiskLevel
import com.voiceshield.ai.domain.model.RiskTimelinePoint
import com.voiceshield.ai.ui.components.*
import com.voiceshield.ai.ui.theme.*

@Composable
fun LiveAnalysisScreen(
    viewModel: LiveAnalysisViewModel,
    inputMode: String = "MIC",
    scenarioId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateReport: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showChallengeModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startSession(inputMode, scenarioId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation & Session Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = uiState.sessionCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        val min = uiState.durationSec / 60
                        val sec = uiState.durationSec % 60
                        Text(
                            text = "%02d:%02d • %s".format(min, sec, if (uiState.isDemo) "SANDBOX DEMO" else inputMode),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                ConnectionPill(status = uiState.connectionStatus)
            }

            if (uiState.isDemo) {
                Spacer(modifier = Modifier.height(10.dp))
                DemoBanner()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Real-Time Risk Gauge
            RiskGauge(
                riskScore = uiState.riskScore,
                riskLevel = uiState.riskLevel,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Audio Waveform
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE AUDIO SPECTRAL WAVEFORM",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent
                        )
                        Text(
                            text = if (uiState.isPaused) "PAUSED" else "16 kHz STREAM",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (uiState.isPaused) AmberWarning else EmeraldSafe
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LiveWaveformView(
                        amplitudes = uiState.waveformAmplitudes,
                        isRecording = uiState.isRecording && !uiState.isPaused,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time Forensic Telemetry Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "CLONE PROBABILITY",
                    value = "${(uiState.cloneProbability * 100).toInt()}%",
                    subValue = if (uiState.cloneProbability > 0.6f) "High Probability" else "Normal",
                    accentColor = if (uiState.cloneProbability > 0.6f) CrimsonCritical else EmeraldSafe,
                    modifier = Modifier.weight(1f)
                )

                MetricTile(
                    title = "SPEAKER MATCH",
                    value = "${(uiState.speakerSimilarity * 100).toInt()}%",
                    subValue = uiState.detectedSpeaker,
                    accentColor = if (uiState.speakerSimilarity < 0.6f) AmberWarning else EmeraldSafe,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "CALLER INTENT",
                    value = uiState.intent,
                    subValue = "Semantic NLP",
                    accentColor = if (uiState.intent != "NORMAL") AmberWarning else CyanAccent,
                    modifier = Modifier.weight(1f)
                )

                MetricTile(
                    title = "SPECTRAL FLATNESS",
                    value = "%.3f".format(uiState.spectralFlatness),
                    subValue = "Acoustic Metric",
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Model Attribution & Transparency Note
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ACOUSTIC ATTRIBUTION & BIOMETRICS",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.modelAttribution,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enrolled Profile: ${uiState.enrolledSpeaker} • Model: ${if (uiState.isDemoModel) "Mock/Demo Provider" else "FastAPI Neural Forensic Pipeline"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-Time Scrolling Risk Timeline
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RISK TIMELINE HISTORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent
                        )
                        Text(
                            text = "${uiState.timeline.size} Samples",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TimelineChart(
                        timeline = uiState.timeline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Action Control Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pause / Resume Button
                OutlinedButton(
                    onClick = { viewModel.pauseResume() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (uiState.isPaused) EmeraldSafe else AmberWarning
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isPaused) "Resume" else "Pause")
                }

                // Voice Challenge Helper
                OutlinedButton(
                    onClick = { showChallengeModal = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyanAccent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Challenge")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stop & Generate Report CTA
            Button(
                onClick = {
                    viewModel.stopSession { reportId ->
                        onNavigateReport(reportId)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.riskScore >= 70) CrimsonCritical else CyanAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Summarize,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HALT SESSION & GENERATE REPORT",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // High Risk Pop-up Modal
        if (uiState.showHighRiskDialog) {
            HighRiskAlertDialog(
                riskScore = uiState.riskScore,
                cloneProb = uiState.cloneProbability,
                reason = uiState.lastAlertReason,
                onDismiss = { viewModel.dismissHighRiskDialog() },
                onStopAndReport = {
                    viewModel.dismissHighRiskDialog()
                    viewModel.stopSession { reportId ->
                        onNavigateReport(reportId)
                    }
                }
            )
        }

        // Challenge Modal
        if (showChallengeModal) {
            AlertDialog(
                onDismissRequest = { showChallengeModal = false },
                containerColor = DarkSurface,
                titleColor = CyanAccent,
                title = { Text("INTERACTIVE VOICE CHALLENGE") },
                text = {
                    Column {
                        Text(
                            text = "Ask the speaker to pronounce this tongue-twister or phrase to disrupt neural vocoder synthesis:",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "\"Flashy plastic phonemes fracture rapidly over fiber optics.\"",
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChallengeModal = false }) {
                        Text("OK", color = CyanAccent)
                    }
                }
            )
        }
    }
}

@Composable
fun TimelineChart(
    timeline: List<RiskTimelinePoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Background reference grid lines at 30%, 70%
        val line30Y = height * (1f - 0.30f)
        val line70Y = height * (1f - 0.70f)

        drawLine(
            color = AmberWarning.copy(alpha = 0.3f),
            start = Offset(0f, line30Y),
            end = Offset(width, line30Y),
            strokeWidth = 1f
        )
        drawLine(
            color = CrimsonCritical.copy(alpha = 0.3f),
            start = Offset(0f, line70Y),
            end = Offset(width, line70Y),
            strokeWidth = 1f
        )

        if (timeline.isEmpty()) {
            return@Canvas
        }

        val stepX = if (timeline.size > 1) width / (timeline.size - 1) else width
        val path = Path()

        timeline.forEachIndexed { index, pt ->
            val x = index * stepX
            val y = height * (1f - (pt.riskScore / 100f).coerceIn(0f, 1f))
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = CyanAccent,
            style = Stroke(width = 3f)
        )

        // Draw circles on points
        timeline.forEachIndexed { index, pt ->
            val x = index * stepX
            val y = height * (1f - (pt.riskScore / 100f).coerceIn(0f, 1f))
            val ptColor = when {
                pt.riskScore >= 70 -> CrimsonCritical
                pt.riskScore >= 40 -> AmberWarning
                else -> EmeraldSafe
            }
            drawCircle(
                color = ptColor,
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}
