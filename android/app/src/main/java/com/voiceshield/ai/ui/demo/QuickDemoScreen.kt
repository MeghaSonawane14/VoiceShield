package com.voiceshield.ai.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceshield.ai.domain.model.DemoScenario
import com.voiceshield.ai.domain.model.RiskLevel
import com.voiceshield.ai.domain.repository.AnalysisRepository
import com.voiceshield.ai.services.DemoScenarioProvider
import com.voiceshield.ai.ui.components.CyberCard
import com.voiceshield.ai.ui.components.DemoBanner
import com.voiceshield.ai.ui.components.RiskBadge
import com.voiceshield.ai.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun QuickDemoScreen(
    demoScenarioProvider: DemoScenarioProvider,
    analysisRepository: AnalysisRepository,
    onLaunchScenario: (scenarioId: String) -> Unit
) {
    val scenarios = remember { demoScenarioProvider.getAllScenarios() }
    val coroutineScope = rememberCoroutineScope()
    var resetMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "QUICK DEMO LAB",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(onClick = {
                    coroutineScope.launch {
                        analysisRepository.clearDemoData()
                        resetMessage = "Demo sandbox data wiped."
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Demo Data",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DemoBanner()

            Spacer(modifier = Modifier.height(10.dp))

            if (resetMessage != null) {
                Surface(
                    color = EmeraldSafe.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resetMessage!!,
                        color = EmeraldSafe,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Deterministic, isolated sandbox testing for evaluators and training. All scenarios run locally and are tagged as demo sessions.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scenarios, key = { it.id }) { scenario ->
                    CyberCard(
                        borderColor = when (scenario.expectedRiskLevel) {
                            RiskLevel.HIGH_RISK -> CrimsonCritical
                            RiskLevel.SUSPICIOUS -> AmberWarning
                            RiskLevel.AUTHENTIC -> EmeraldSafe
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunchScenario(scenario.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = scenario.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                RiskBadge(riskLevel = scenario.expectedRiskLevel)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = scenario.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "EXPECTED ACTION: ",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = scenario.expectedAction,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SIMULATE SCENARIO",
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
