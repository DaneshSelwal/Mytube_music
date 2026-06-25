package com.mark1.mytubemusic.ui.theme

import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object Tokens {
    // Backgrounds — warm-tinted, NOT pure black
    val bgDeep      = Color(0xFF0D0B0F)   // main scaffold background
    val bgSurface   = Color(0xFF161218)   // cards, sheets
    val bgElevated  = Color(0xFF1E1A22)   // dialogs, bottom sheets

    // Accent — a single muted violet-gold that reads premium
    val accentPrimary   = Color(0xFFB08DFF)   // playback controls, active states
    val accentSecondary = Color(0xFFFFD98E)   // timestamps, badges (used sparingly)

    // Text
    val textPrimary   = Color(0xFFF2EEF8)
    val textSecondary = Color(0xFF9A8FAA)
    val textDisabled  = Color(0xFF4A3F55)

    // Separator / stroke
    val strokeSubtle = Color(0xFF2A2432)

    // Glassmorphism tint
    val glassTint = Color(0x22B08DFF)   // 13% alpha violet for glass surfaces
    
    // Light Mode Equivalents (fallback/toggle)
    val lightBgDeep = Color(0xFFF7F5FA)
    val lightBgSurface = Color(0xFFFFFFFF)
    val lightBgElevated = Color(0xFFF0EDF5)
    val lightTextPrimary = Color(0xFF161218)
    val lightTextSecondary = Color(0xFF5E5468)
    val lightTextDisabled = Color(0xFF9A8FAA)
}

object MyTubeColors {
    var isDarkTheme by mutableStateOf(true)

    val BackgroundDeep: Color get() = if (isDarkTheme) Tokens.bgDeep else Tokens.lightBgDeep
    val BackgroundSurface: Color get() = if (isDarkTheme) Tokens.bgSurface else Tokens.lightBgSurface
    val AccentSkyBlue: Color get() = Tokens.accentPrimary // Keeping old name to not break existing usage, mapping to new accent
    val AccentGlow: Color get() = if (isDarkTheme) Tokens.glassTint else Tokens.accentPrimary.copy(alpha = 0.1f)
    val TextPrimary: Color get() = if (isDarkTheme) Tokens.textPrimary else Tokens.lightTextPrimary
    val TextSecondary: Color get() = if (isDarkTheme) Tokens.textSecondary else Tokens.lightTextSecondary
    val GlassSurface: Color get() = if (isDarkTheme) Tokens.glassTint else Tokens.accentPrimary.copy(alpha = 0.05f)
    val GlassBorder: Color get() = if (isDarkTheme) Tokens.strokeSubtle else Color(0x1A000000)
    
    val CdChromeRim: Color get() = if (isDarkTheme) Color(0x99C0C0C0) else Color(0x99808080)
    val CdIridescent1: Color get() = if (isDarkTheme) Color(0x1F7B1FA2) else Color(0x1F4A148C)
    val CdIridescent2: Color get() = if (isDarkTheme) Color(0x1A00ACC1) else Color(0x1A00838F)
    val CdIridescent3: Color get() = if (isDarkTheme) Color(0x14F9A825) else Color(0x14F57F17)
}
