package com.voiceshield.ai.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceshield.ai.ui.theme.CrimsonCritical
import com.voiceshield.ai.ui.theme.CyanAccent
import com.voiceshield.ai.ui.theme.DarkSurface
import com.voiceshield.ai.ui.theme.TextPrimary
import com.voiceshield.ai.ui.theme.TextSecondary

@Composable
fun HighRiskAlertDialog(
    riskScore: Int,
    cloneProb: Float,
    reason: String,
    onDismiss: () -> Unit,
    onStopAndReport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        icon = {
            Icon(
                imageVector = Icons.Default.Dangerous,
                contentDescription = "High Risk Alert",
                tint = CrimsonCritical,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "HIGH-RISK VOICE DETECTED",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CrimsonCritical,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AI-Generated or synthetic speech probability exceeds safety thresholds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = CrimsonCritical.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "COMPOSITE RISK: $riskScore / 100",
                            fontWeight = FontWeight.Bold,
                            color = CrimsonCritical,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "SYNTHETIC PROBABILITY: ${(cloneProb * 100).toInt()}%",
                            fontWeight = FontWeight.SemiBold,
                            color = CrimsonCritical,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Advisory Guidance:",
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "1. Do NOT disclose credentials, MFA codes, or authorize transactions.\n" +
                           "2. Challenge caller with an unexpected out-of-band question.\n" +
                           "3. Terminate the call and verify through an independent channel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onStopAndReport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonCritical,
                    contentColor = Color.White
                )
            ) {
                Text("HALT & GENERATE REPORT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CONTINUE MONITORING", color = TextSecondary)
            }
        }
    )
}
