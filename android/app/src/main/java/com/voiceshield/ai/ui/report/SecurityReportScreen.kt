package com.voiceshield.ai.ui.report

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceshield.ai.domain.model.RiskLevel
import com.voiceshield.ai.domain.model.SecurityReport
import com.voiceshield.ai.domain.repository.AnalysisRepository
import com.voiceshield.ai.domain.repository.AuthRepository
import com.voiceshield.ai.services.ReportExporter
import com.voiceshield.ai.ui.components.CyberCard
import com.voiceshield.ai.ui.components.DemoBanner
import com.voiceshield.ai.ui.components.MetricTile
import com.voiceshield.ai.ui.components.RiskBadge
import com.voiceshield.ai.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SecurityReportScreen(
    reportId: String,
    analysisRepository: AnalysisRepository,
    authRepository: AuthRepository,
    onNavigateHome: () -> Unit,
    onReanalyze: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var report by remember { mutableStateOf<SecurityReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(reportId) {
        val user = authRepository.currentUser.value
        val userId = user?.uid ?: "guest"
        report = analysisRepository.getReport(reportId, userId)
        isLoading = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberDarkBackground
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanAccent)
            }
        } else if (report == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Report Not Found", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateHome) {
                    Text("Return Home")
                }
            }
        } else {
            val r = report!!
            val riskLevelEnum = when (r.overallRiskLevel) {
                "HIGH_RISK" -> RiskLevel.HIGH_RISK
                "SUSPICIOUS" -> RiskLevel.SUSPICIOUS
                else -> RiskLevel.AUTHENTIC
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateHome) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Home",
                                tint = CyanAccent
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SECURITY FORENSIC REPORT",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }

                    IconButton(onClick = {
                        val file = ReportExporter.exportReportToTxt(context, r)
                        ReportExporter.shareReport(context, file)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Report",
                            tint = CyanAccent
                        )
                    }
                }

                if (r.isDemo) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DemoBanner()
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Card
                CyberCard(
                    borderColor = when (riskLevelEnum) {
                        RiskLevel.HIGH_RISK, RiskLevel.HIGH -> CrimsonCritical
                        RiskLevel.SUSPICIOUS -> AmberWarning
                        RiskLevel.AUTHENTIC, RiskLevel.LOW -> EmeraldSafe
                        else -> EmeraldSafe
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = r.sessionCode,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${r.timestamp} • ${r.inputMode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            RiskBadge(riskLevel = riskLevelEnum)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("COMPOSITE RISK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${r.overallRiskScore} / 100",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (riskLevelEnum) {
                                        RiskLevel.HIGH_RISK, RiskLevel.HIGH -> CrimsonCritical
                                        RiskLevel.SUSPICIOUS -> AmberWarning
                                        RiskLevel.AUTHENTIC, RiskLevel.LOW -> EmeraldSafe
                                        else -> EmeraldSafe
                                    }
                                )
                            }

                            Column {
                                Text("SESSION DURATION", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${r.durationSec.toInt()}s",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Biometrics Breakdown
                Text(
                    text = "BIOMETRIC & ACOUSTIC ANALYSIS",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = "CLONE PROBABILITY",
                        value = "${(r.maxCloneProbability * 100).toInt()}%",
                        subValue = "Peak Synthetic Cue",
                        accentColor = if (r.maxCloneProbability > 0.6f) CrimsonCritical else EmeraldSafe,
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        title = "SPEAKER SIMILARITY",
                        value = "${(r.meanSpeakerSimilarity * 100).toInt()}%",
                        subValue = r.detectedSpeaker,
                        accentColor = if (r.meanSpeakerSimilarity < 0.6f) AmberWarning else EmeraldSafe,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Acoustic Model Attribution
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "MODEL ATTRIBUTION & SIGNATURE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = r.modelAttribution,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enrolled Profile: ${r.enrolledSpeaker} • Model: ${if (r.isDemoModel) "Mock/Demo Provider" else "Neural Spectral Classifier"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Forensic Advisory Recommendations
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "FORENSIC RECOMMENDATIONS",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        r.advisoryRecommendations.forEach { rec ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text("• ", color = CyanAccent, fontWeight = FontWeight.Bold)
                                Text(
                                    text = rec,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Probabilistic Disclaimer Banner
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "DISCLAIMER: AI voice-clone detection provides probabilistic estimations based on spectral & acoustic artifacts. It does not constitute definitive proof. Always corroborate with independent verification channels.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Export, Re-analyze, Home
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val file = ReportExporter.exportReportToTxt(context, r)
                            ReportExporter.shareReport(context, file)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export TXT")
                    }

                    Button(
                        onClick = onReanalyze,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze More", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
