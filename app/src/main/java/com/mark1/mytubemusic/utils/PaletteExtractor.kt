package com.mark1.mytubemusic.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

object PaletteExtractor {

    fun getColorsFromUri(context: Context, uri: String, onResult: (Color, Color) -> Unit) {
        try {
            val retriever = MediaMetadataRetriever()
            context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                val art = retriever.embeddedPicture
                if (art != null) {
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    Palette.from(bitmap).generate { palette ->
                        val vibrant = palette?.vibrantSwatch?.rgb ?: android.graphics.Color.DKGRAY
                        val darkVibrant = palette?.darkVibrantSwatch?.rgb ?: android.graphics.Color.BLACK
                        onResult(Color(vibrant), Color(darkVibrant))
                    }
                } else {
                    onResult(Color.DarkGray, Color.Black)
                }
            }
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(Color.DarkGray, Color.Black)
        }
    }
}
