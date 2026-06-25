package com.mark1.mytubemusic.ui.theme

import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object MyTubeColors {
    var isDarkTheme by mutableStateOf(true)

    val BackgroundDeep: Color get() = if (isDarkTheme) Color(0xFF070714) else Color(0xFFF0F2F5)
    val BackgroundSurface: Color get() = if (isDarkTheme) Color(0xFF0F0F23) else Color(0xFFFFFFFF)
    val AccentSkyBlue: Color get() = if (isDarkTheme) Color(0xFF42A5F5) else Color(0xFF1976D2)
    val AccentGlow: Color get() = if (isDarkTheme) Color(0x4D42A5F5) else Color(0x331976D2)
    val TextPrimary: Color get() = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF121212)
    val TextSecondary: Color get() = if (isDarkTheme) Color(0x99FFFFFF) else Color(0x99121212)
    val GlassSurface: Color get() = if (isDarkTheme) Color(0x14FFFFFF) else Color(0x0A000000)
    val GlassBorder: Color get() = if (isDarkTheme) Color(0x26FFFFFF) else Color(0x1A000000)
    val CdChromeRim: Color get() = if (isDarkTheme) Color(0x99C0C0C0) else Color(0x99808080)
    val CdIridescent1: Color get() = if (isDarkTheme) Color(0x1F7B1FA2) else Color(0x1F4A148C)
    val CdIridescent2: Color get() = if (isDarkTheme) Color(0x1A00ACC1) else Color(0x1A00838F)
    val CdIridescent3: Color get() = if (isDarkTheme) Color(0x14F9A825) else Color(0x14F57F17)
}
