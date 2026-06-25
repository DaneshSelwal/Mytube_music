package com.mark1.mytubemusic.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteExtractor {
    suspend fun extractColors(bitmap: Bitmap): PaletteColors {
        return withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap).generate()
            PaletteColors(
                dominant = palette.dominantSwatch?.rgb?.let { Color(it) },
                vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) },
                muted = palette.mutedSwatch?.rgb?.let { Color(it) }
            )
        }
    }
}

data class PaletteColors(
    val dominant: Color?,
    val vibrant: Color?,
    val muted: Color?
)
