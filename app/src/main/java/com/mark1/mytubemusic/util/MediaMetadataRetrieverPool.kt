package com.mark1.mytubemusic.util

import android.media.MediaMetadataRetriever

object MediaMetadataRetrieverPool {
    private const val MAX_POOL_SIZE = 4
    private val pool = ArrayDeque<MediaMetadataRetriever>(MAX_POOL_SIZE)

    @Synchronized
    fun acquire(): MediaMetadataRetriever {
        return if (pool.isNotEmpty()) {
            pool.removeFirst()
        } else {
            MediaMetadataRetriever()
        }
    }

    @Synchronized
    fun release(retriever: MediaMetadataRetriever) {
        if (pool.size < MAX_POOL_SIZE) {
            try {
                // Clear any data sources by resetting it (calling release on resource isn't strictly needed if we reuse)
                // However, there is no reset() method. We simply re-add it to the pool.
                // The caller is expected to always call setDataSource() before usage.
                pool.addLast(retriever)
            } catch (e: Exception) {
                retriever.release() // Safety net
            }
        } else {
            retriever.release()
        }
    }
}
