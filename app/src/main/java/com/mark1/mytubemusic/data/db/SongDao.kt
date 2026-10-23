package com.mark1.mytubemusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mark1.mytubemusic.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Query("SELECT uri FROM songs")
    suspend fun getAllSongUris(): List<String>

    @Query("DELETE FROM songs WHERE uri IN (:uris)")
    suspend fun deleteSongs(uris: List<String>)
    
    @Transaction
    suspend fun deleteSongsInChunks(uris: List<String>) {
        uris.chunked(900).forEach { chunk ->
            deleteSongs(chunk)
        }
    }

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun updateFavorite(uri: String, isFavorite: Boolean)
}
