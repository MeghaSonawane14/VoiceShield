package com.voiceshield.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceshield.ai.domain.model.ConnectionStatus
import com.voiceshield.ai.domain.model.RiskLevel
import com.voiceshield.ai.ui.theme.*

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CardBorder,
    backgroundColor: Color = CardBackground,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun RiskBadge(
    level: RiskLevel? = null,
    riskLevel: RiskLevel? = level,
    modifier: Modifier = Modifier
) {
    val effectiveLevel = riskLevel ?: level ?: RiskLevel.AUTHENTIC
    val (color, text) = when (effectiveLevel) {
        RiskLevel.AUTHENTIC, RiskLevel.LOW -> EmeraldSafe to "AUTHENTIC"
        RiskLevel.SUSPICIOUS -> AmberWarning to "SUSPICIOUS"
        RiskLevel.HIGH, RiskLevel.HIGH_RISK -> CrimsonCritical to "HIGH RISK"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ConnectionPill(status: ConnectionStatus, modifier: Modifier = Modifier) {
    val (color, text) = when (status) {
        ConnectionStatus.CONNECTED -> EmeraldSafe to "CONNECTED"
        ConnectionStatus.CONNECTING -> AmberWarning to "CONNECTING"
        ConnectionStatus.DISCONNECTED -> CrimsonCritical to "OFFLINE"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun DemoBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1B4B))
            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFFA5B4FC),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "DEMO MODE — SAMPLE DATA",
            color = Color(0xFFA5B4FC),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun RiskGauge(
    score: Float = 0f,
    riskScore: Int = (score * 100).toInt(),
    riskLevel: RiskLevel = RiskLevel.AUTHENTIC,
    modifier: Modifier = Modifier,
    sizeDp: Int = 140
) {
    val normalizedTarget = when {
        score > 0f -> score.coerceIn(0.0f, 1.0f)
        riskScore > 0 -> (riskScore / 100f).coerceIn(0.0f, 1.0f)
        else -> 0.0f
    }

    val targetColor = when (riskLevel) {
        RiskLevel.AUTHENTIC, RiskLevel.LOW -> EmeraldSafe
        RiskLevel.SUSPICIOUS -> AmberWarning
        RiskLevel.HIGH, RiskLevel.HIGH_RISK -> CrimsonCritical
    }

    val animatedScore by animateFloatAsState(
        targetValue = normalizedTarget,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "RiskGaugeScore"
    )

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background Arc (240 degrees)
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Progress Arc
            drawArc(
                color = targetColor,
                startAngle = 150f,
                sweepAngle = 240f * animatedScore,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(animatedScore * 100).toInt()}%",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "OVERALL RISK",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun LiveWaveformView(
    amplitude: Float = 0.05f,
    amplitudes: List<Float> = emptyList(),
    isRecording: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveformPhase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B1220))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bars = 32
            val barWidth = size.width / (bars * 1.6f)
            val maxHeight = size.height * 0.8f

            for (i in 0 until bars) {
                val barAmp = if (amplitudes.isNotEmpty()) {
                    val index = ((i.toFloat() / bars) * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
                    amplitudes[index]
                } else {
                    val waveOffset = kotlin.math.sin(Math.toRadians((phase + i * 20).toDouble())).toFloat()
                    val baseAmp = if (isRecording) maxOf(0.15f, amplitude) else 0.05f
                    baseAmp * (0.6f + 0.4f * waveOffset)
                }

                val barHeight = (maxHeight * barAmp.coerceIn(0.05f, 1.0f)).coerceAtLeast(4f)
                val x = i * (barWidth * 1.6f) + barWidth * 0.3f
                val y = (size.height - barHeight) / 2

                drawRoundRect(
                    color = if (isRecording) CyanAccent else TextMuted,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        }
    }
}

@Composable
fun MetricTile(
    title: String? = null,
    label: String = title ?: "",
    value: String,
    subValue: String? = null,
    subtext: String = subValue ?: "",
    accentColor: Color? = null,
    valueColor: Color = accentColor ?: CyanAccent,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        color = CardBackground
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtext,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
