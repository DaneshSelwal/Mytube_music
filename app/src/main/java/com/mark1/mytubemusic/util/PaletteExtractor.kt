package com.mark1.mytubemusic.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteExtractor {

    private val colorCache = LruCache<String, Pair<Color, Color>>(50)

    suspend fun getColorsFromUri(context: Context, uri: String): Pair<Color, Color> = withContext(Dispatchers.IO) {
        colorCache.get(uri)?.let { return@withContext it }

        val defaultColors = Pair(Color(0xFF4A00E0), Color(0xFF8E2DE2))

        try {
            var result: Pair<Color, Color>? = null

            // ⚡ Bolt: Fast path - Reuse already decoded bitmap from ArtworkCache to avoid expensive disk I/O and MediaMetadataRetriever
            val cachedImageBitmap = ArtworkCache.get(uri)
            var targetBitmap: Bitmap? = cachedImageBitmap?.asAndroidBitmap()

            if (targetBitmap == null) {
                val retriever = MediaMetadataRetriever()
                context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                    val art = retriever.embeddedPicture
                    if (art != null) {
                        targetBitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    }
                }
                retriever.release()
            }

            if (targetBitmap != null) {
                // Generate palette synchronously on the IO thread
                val palette = Palette.from(targetBitmap!!).generate()

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
