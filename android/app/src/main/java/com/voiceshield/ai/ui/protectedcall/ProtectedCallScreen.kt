package com.voiceshield.ai.ui.protectedcall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceshield.ai.domain.model.RiskLevel
import com.voiceshield.ai.ui.components.CyberCard
import com.voiceshield.ai.ui.components.RiskBadge
import com.voiceshield.ai.ui.theme.*
import kotlinx.coroutines.delay

enum class CallState {
    IDLE,
    INCOMING,
    ACTIVE,
    ENDED
}

@Composable
fun ProtectedCallScreen(
    onNavigateBack: () -> Unit,
    onLaunchDeepAnalysis: () -> Unit
) {
    var callState by remember { mutableStateOf(CallState.INCOMING) }
    var callerName by remember { mutableStateOf("David (Internal IT Desk)") }
    var callerNumber by remember { mutableStateOf("+1 (555) 019-4821") }
    var callDurationSec by remember { mutableStateOf(0) }
    
    var currentRiskScore by remember { mutableStateOf(84) }
    var cloneProbability by remember { mutableStateOf(0.89f) }
    var speakerSimilarity by remember { mutableStateOf(0.31f) }
    var isMismatched by remember { mutableStateOf(true) }
    var showChallengeModal by remember { mutableStateOf(false) }

    LaunchedEffect(callState) {
        if (callState == CallState.ACTIVE) {
            while (callState == CallState.ACTIVE) {
                delay(1000)
                callDurationSec++
            }
        }
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
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanAccent
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PROTECTED CALL MONITOR",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Caller Identity Card
            CyberCard(
                borderColor = when (callState) {
                    CallState.ACTIVE -> if (currentRiskScore > 70) CrimsonCritical else CyanAccent
                    CallState.INCOMING -> AmberWarning
                    else -> DarkSurfaceLight
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Caller",
                            tint = if (currentRiskScore > 70) CrimsonCritical else CyanAccent,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = callerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = callerNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val statusLabel = when (callState) {
                        CallState.INCOMING -> "Incoming Protected Audio Stream..."
                        CallState.ACTIVE -> "Active Call: %02d:%02d".format(callDurationSec / 60, callDurationSec % 60)
                        CallState.ENDED -> "Call Terminated"
                        CallState.IDLE -> "Monitor Ready"
                    }

                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = if (callState == CallState.ACTIVE) CyanAccent else AmberWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Call Surveillance HUD
            AnimatedVisibility(visible = callState == CallState.ACTIVE) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Impersonation Advisory Alert
                    if (isMismatched) {
                        Surface(
                            color = CrimsonCritical.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonCritical),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = CrimsonCritical,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "POTENTIAL IMPERSONATION DETECTED",
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonCritical,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "Caller's vocal biometric pattern significantly deviates from the enrolled trusted voice profile.",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Real-Time Risk Overlay HUD
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE AUDIO RISK METRICS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyanAccent
                                )
                                RiskBadge(riskLevel = RiskLevel.HIGH_RISK)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("OVERALL RISK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        text = "$currentRiskScore / 100",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonCritical
                                    )
                                }

                                Column {
                                    Text("SYNTHETIC CLONE PROB.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        text = "${(cloneProbability * 100).toInt()}%",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonCritical
                                    )
                                }

                                Column {
                                    Text("VOICE SIMILARITY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        text = "${(speakerSimilarity * 100).toInt()}%",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { cloneProbability },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CrimsonCritical,
                                trackColor = DarkSurfaceLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // In-Call Action Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showChallengeModal = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = CyanAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Challenge", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = onLaunchDeepAnalysis,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deep Analysis", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Call Controls: Answer, Decline, End Call
            when (callState) {
                CallState.INCOMING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Decline Button
                        FloatingActionButton(
                            onClick = { callState = CallState.ENDED },
                            containerColor = CrimsonCritical,
                            contentColor = Color.White,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline Call", modifier = Modifier.size(32.dp))
                        }

                        // Answer Button
                        FloatingActionButton(
                            onClick = { callState = CallState.ACTIVE },
                            containerColor = EmeraldSafe,
                            contentColor = Color.White,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Answer Call", modifier = Modifier.size(32.dp))
                        }
                    }
                }
                CallState.ACTIVE -> {
                    FloatingActionButton(
                        onClick = { callState = CallState.ENDED },
                        containerColor = CrimsonCritical,
                        contentColor = Color.White,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Hang Up", modifier = Modifier.size(32.dp))
                    }
                }
                CallState.ENDED, CallState.IDLE -> {
                    Button(
                        onClick = {
                            callState = CallState.INCOMING
                            callDurationSec = 0
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceVariant,
                            contentColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simulate New Incoming Call")
                    }
                }
            }
        }

        // Voice Challenge Questions Dialog
        if (showChallengeModal) {
            AlertDialog(
                onDismissRequest = { showChallengeModal = false },
                containerColor = DarkSurface,
                titleContentColor = CyanAccent,
                title = {
                    Text(
                        text = "SECURITY VOICE CHALLENGES",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "To expose AI voice cloning latency or conversational model gaps, ask the caller one of these verification questions:",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val questions = listOf(
                            "\"Can you repeat the phrase: 'Blue acoustic zebras jump over velvet mountains'?\"",
                            "\"What was the exact project code we finalized last Tuesday?\"",
                            "\"Please pause for 3 seconds, then count backwards from five.\""
                        )

                        questions.forEach { q ->
                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = q,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChallengeModal = false }) {
                        Text("CLOSE", color = CyanAccent, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
