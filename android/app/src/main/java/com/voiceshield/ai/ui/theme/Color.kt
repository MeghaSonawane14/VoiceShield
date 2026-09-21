package com.voiceshield.ai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Primary Cyber Aesthetic Accents
val CyanPrimary = Color(0xFF06B6D4)
val CyanAccent = Color(0xFF06B6D4)
val CyanLight = Color(0xFF38BDF8)
val PurpleAccent = Color(0xFFA855F7)

// Risk & Security Classification Accents
val EmeraldSafe = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val CrimsonCritical = Color(0xFFEF4444)

val RiskLow = EmeraldSafe
val RiskSuspicious = AmberWarning
val RiskHigh = CrimsonCritical

// Background & Surface Palettes
val CyberDarkBackground = Color(0xFF080C15)
val DarkBackground = Color(0xFF080C15)
val DarkSurface = Color(0xFF0E1526)
val SurfaceDark = Color(0xFF0E1526)
val DarkSurfaceVariant = Color(0xFF131B2E)
val DarkSurfaceLight = Color(0xFF1E293B)
val CardBackground = Color(0xFF131B2E)
val CardBorder = Color(0xFF1E293B)

// Typography Palettes
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color.Black,
    secondary = CyanLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)
