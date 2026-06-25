package com.mark1.mytubemusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MyTubeColors.AccentSkyBlue,
    background = MyTubeColors.BackgroundDeep,
    surface = MyTubeColors.BackgroundSurface,
    onPrimary = MyTubeColors.TextPrimary,
    onBackground = MyTubeColors.TextPrimary,
    onSurface = MyTubeColors.TextPrimary
)

@Composable
fun MyTubeMusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MyTubeTypography,
        content = content
    )
}
