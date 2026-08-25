package com.mark1.mytubemusic.util

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

object ArtworkCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Use 1/8th of the available memory for this memory cache.
    private val cacheSize = if (maxMemory > 0) maxMemory / 8 else 1024 * 10 // 10MB fallback

    private val cache = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: ImageBitmap): Int {
            // The cache size will be measured in kilobytes rather than number of items.
            return (bitmap.width * bitmap.height * 4) / 1024
        }
    }

    fun get(key: String): ImageBitmap? {
        return cache.get(key)
    }

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }
}
