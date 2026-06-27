package com.mark1.mytubemusic.repository

import com.mark1.mytubemusic.data.model.Song
import com.mark1.mytubemusic.util.YouTubeExtractor

class OnlineSongRepository {
    private val youtubeExtractor = YouTubeExtractor()

    suspend fun searchSongs(query: String): List<Song> = youtubeExtractor.searchSongs(query)

    suspend fun getStreamUrl(videoId: String): String? = youtubeExtractor.getStreamUrl(videoId)

    suspend fun getSongMetadata(videoId: String): Song? = youtubeExtractor.getSongMetadata(videoId)
}
