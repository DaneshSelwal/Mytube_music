package com.mark1.mytubemusic.util

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteExtractor {

    private val colorCache = LruCache<String, Pair<Color, Color>>(50)

    suspend fun getColorsFromUri(context: Context, uri: String): Pair<Color, Color> = withContext(Dispatchers.IO) {
        colorCache.get(uri)?.let { return@withContext it }

        val defaultColors = Pair(Color(0xFF4A00E0), Color(0xFF8E2DE2))

        try {
            val retriever = MediaMetadataRetriever()
            var result: Pair<Color, Color>? = null

            context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                val art = retriever.embeddedPicture
                if (art != null) {
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    // Generate palette synchronously on the IO thread
                    val palette = Palette.from(bitmap).generate()

                    val vibrant = palette.vibrantSwatch?.rgb
                        ?: palette.lightVibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: android.graphics.Color.parseColor("#4A00E0")
                    val secondary = palette.lightMutedSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                        ?: palette.darkVibrantSwatch?.rgb
                        ?: android.graphics.Color.parseColor("#8E2DE2")

                    result = Pair(Color(vibrant), Color(secondary))
                }
            }
            retriever.release()

            if (result != null) {
                colorCache.put(uri, result)
                return@withContext result!!
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext defaultColors
    }
}
