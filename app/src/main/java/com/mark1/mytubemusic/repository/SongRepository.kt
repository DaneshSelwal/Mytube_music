package com.mark1.mytubemusic.repository

import com.mark1.mytubemusic.data.db.SongDao
import com.mark1.mytubemusic.data.model.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {
    
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    
    suspend fun insertSongs(songs: List<Song>) {
        songDao.insertSongs(songs)
    }
    
    suspend fun deleteSongsNotIn(uris: List<String>) {
        val existingUris = songDao.getAllSongUris()
        val validUriSet = uris.toSet()
        val urisToDelete = existingUris.filterNot { it in validUriSet }

        // Chunk deletes to avoid SQLite's 999 parameter limit in IN clause
        // Execute chunk deletions concurrently to prevent I/O bottlenecks.
        // Room safely handles parallel queries using its internal connection pool.
        coroutineScope {
            urisToDelete.chunked(900).map { chunk ->
                async { songDao.deleteSongs(chunk) }
            }.awaitAll()
        }
    }
    
    suspend fun toggleFavorite(uri: String, currentStatus: Boolean) {
        songDao.updateFavorite(uri, !currentStatus)
    }
}
