package com.mark1.mytubemusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mark1.mytubemusic.data.model.OnlinePlaylist
import com.mark1.mytubemusic.data.model.OnlinePlaylistSong
import com.mark1.mytubemusic.data.model.OnlinePlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface OnlinePlaylistDao {

    @Query("SELECT * FROM online_playlists")
    fun getAllPlaylists(): Flow<List<OnlinePlaylist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: OnlinePlaylist): Long

    @Query("DELETE FROM online_playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("UPDATE online_playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: OnlinePlaylistSong)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: OnlinePlaylistSongCrossRef)

    @Query("DELETE FROM online_playlist_song_cross_ref WHERE playlistId = :playlistId AND songUri = :songUri")
    suspend fun deleteCrossRef(playlistId: Long, songUri: String)

    @Query("""
        SELECT s.* FROM online_songs s
        INNER JOIN online_playlist_song_cross_ref ref ON s.uri = ref.songUri
        WHERE ref.playlistId = :playlistId
    """)
    fun getSongsInPlaylist(playlistId: Long): Flow<List<OnlinePlaylistSong>>
}
