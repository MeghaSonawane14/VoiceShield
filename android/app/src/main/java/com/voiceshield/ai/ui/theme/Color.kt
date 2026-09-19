package com.voiceshield.ai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val CyanPrimary = Color(0xFF06B6D4)
val CyanLight = Color(0xFF38BDF8)
val DarkBackground = Color(0xFF080C15)
val SurfaceDark = Color(0xFF0E1526)
val CardBackground = Color(0xFF131B2E)
val CardBorder = Color(0xFF1E293B)

val RiskLow = Color(0xFF10B981)
val RiskSuspicious = Color(0xFFF59E0B)
val RiskHigh = Color(0xFFEF4444)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.Black,
    secondary = CyanLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)
