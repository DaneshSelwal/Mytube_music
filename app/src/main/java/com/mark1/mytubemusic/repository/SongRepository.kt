package com.mark1.mytubemusic.repository

import com.mark1.mytubemusic.data.db.SongDao
import com.mark1.mytubemusic.data.model.Song
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
        urisToDelete.chunked(900).forEach { chunk ->
            songDao.deleteSongs(chunk)
        }
    }
    
    suspend fun toggleFavorite(uri: String, currentStatus: Boolean) {
        songDao.updateFavorite(uri, !currentStatus)
    }
}
