package com.voiceshield.ai.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceshield.ai.domain.model.RiskLevel
import com.voiceshield.ai.ui.components.CyberCard
import com.voiceshield.ai.ui.components.MetricTile
import com.voiceshield.ai.ui.components.RiskBadge
import com.voiceshield.ai.ui.theme.*

@Composable
fun SessionDetailScreen(
    sessionId: String,
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadSessionDetail(sessionId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberDarkBackground
    ) {
        val s = uiState.selectedSessionDetail
        if (s == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanAccent)
            }
        } else {
            val riskLvl = when {
                s.overallRisk >= 70 -> RiskLevel.HIGH_RISK
                s.overallRisk >= 40 -> RiskLevel.SUSPICIOUS
                else -> RiskLevel.AUTHENTIC
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
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
                        Text(
                            text = "SESSION AUDIT",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }

                    IconButton(onClick = {
                        viewModel.deleteSession(s.sessionId)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = CrimsonCritical
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CyberCard(
                    borderColor = when (riskLvl) {
                        RiskLevel.HIGH_RISK -> CrimsonCritical
                        RiskLevel.SUSPICIOUS -> AmberWarning
                        RiskLevel.AUTHENTIC -> EmeraldSafe
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
                                    text = s.sessionCode,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${s.startTime} • ${s.inputMode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            RiskBadge(riskLevel = riskLvl)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("COMPOSITE RISK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${s.overallRisk} / 100",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (riskLvl) {
                                        RiskLevel.HIGH_RISK -> CrimsonCritical
                                        RiskLevel.SUSPICIOUS -> AmberWarning
                                        RiskLevel.AUTHENTIC -> EmeraldSafe
                                    }
                                )
                            }

                            Column {
                                Text("DURATION", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${s.durationSec.toInt()} sec",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = "PEAK CLONE PROB",
                        value = "${(s.maxCloneProb * 100).toInt()}%",
                        subValue = if (s.maxCloneProb > 0.6f) "High Probability" else "Normal",
                        accentColor = if (s.maxCloneProb > 0.6f) CrimsonCritical else EmeraldSafe,
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        title = "SPEAKER SIMILARITY",
                        value = "${(s.speakerSimilarity * 100).toInt()}%",
                        subValue = s.likelySpeaker,
                        accentColor = if (s.speakerSimilarity < 0.6f) AmberWarning else EmeraldSafe,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "FORENSIC NOTES",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Session archived in encrypted local database. Isolated to current authenticated analyst context.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
