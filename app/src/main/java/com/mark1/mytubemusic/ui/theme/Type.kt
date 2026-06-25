package com.mark1.mytubemusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.font.Font
import com.mark1.mytubemusic.R

val DmSerifDisplay = FontFamily(
    Font(R.font.dm_serif_display_italic, FontWeight.Normal, FontStyle.Italic)
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

val TitleLarge   = TextStyle(fontFamily = DmSerifDisplay, fontSize = 28.sp, fontStyle = FontStyle.Italic, lineHeight = 34.sp)
val TitleMedium  = TextStyle(fontFamily = DmSerifDisplay, fontSize = 20.sp, fontStyle = FontStyle.Italic)
val BodyMedium   = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Normal)
val BodySmall    = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Tokens.textSecondary)
val MonoSmall    = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp)

val MyTubeTypography = Typography(
    displayLarge  = TitleLarge,
    titleLarge    = TitleMedium,
    titleMedium   = TitleMedium.copy(fontSize = 16.sp),
    bodyMedium    = BodyMedium,
    labelSmall    = BodySmall
)
