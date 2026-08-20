package com.mark1.mytubemusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mark1.mytubemusic.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Query("DELETE FROM songs WHERE uri NOT IN (:uris) AND downloadState = 'DOWNLOADED'")
    suspend fun deleteSongsNotIn(uris: List<String>)
    
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun updateFavorite(uri: String, isFavorite: Boolean)

    @Query("SELECT * FROM songs WHERE downloadState = 'PENDING'")
    suspend fun getPendingSongs(): List<Song>

    @Query("UPDATE songs SET downloadState = :state WHERE uri = :uri")
    suspend fun updateDownloadState(uri: String, state: String)

    @Query("UPDATE songs SET downloadState = 'DOWNLOADED', uri = :newUri WHERE uri = :oldUri")
    suspend fun completeDownload(oldUri: String, newUri: String)
}
