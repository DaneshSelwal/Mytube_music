package com.mark1.mytubemusic.util

import android.media.MediaMetadataRetriever

/**
 * ⚡ Bolt: A small pool for MediaMetadataRetriever instances.
 * Instantiating MediaMetadataRetriever is a heavy operation that involves native allocation.
 * By pooling instances, we significantly reduce CPU spikes and memory churn when scrolling lists.
 */
object RetrieverPool {
    private val lock = Any()
    private val pool = ArrayList<MediaMetadataRetriever>(4)

    fun acquire(): MediaMetadataRetriever {
        synchronized(lock) {
            if (pool.isNotEmpty()) {
                return pool.removeLast()
            }
        }
        return MediaMetadataRetriever()
    }

    fun release(retriever: MediaMetadataRetriever) {
        synchronized(lock) {
            if (pool.size < 4) {
                pool.add(retriever)
                return
            }
        }
        try {
            retriever.release()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
