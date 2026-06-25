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
        songDao.deleteSongsNotIn(uris)
    }
    
    suspend fun toggleFavorite(uri: String, currentStatus: Boolean) {
        songDao.updateFavorite(uri, !currentStatus)
    }
}
