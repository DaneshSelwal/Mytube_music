package com.mark1.mytubemusic.util

import android.media.MediaMetadataRetriever
import java.util.concurrent.ConcurrentLinkedQueue

object MediaMetadataRetrieverPool {
    private val pool = ConcurrentLinkedQueue<MediaMetadataRetriever>()
    private const val MAX_POOL_SIZE = 10
    private var currentSize = 0

    @Synchronized
    fun acquire(): MediaMetadataRetriever {
        val retriever = pool.poll()
        if (retriever != null) {
            currentSize--
            return retriever
        }
        return MediaMetadataRetriever()
    }

    @Synchronized
    fun release(retriever: MediaMetadataRetriever) {
        if (currentSize < MAX_POOL_SIZE) {
            pool.offer(retriever)
            currentSize++
        } else {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
