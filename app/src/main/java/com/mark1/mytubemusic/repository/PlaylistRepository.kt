package com.mark1.mytubemusic.repository

import com.mark1.mytubemusic.data.db.PlaylistDao
import com.mark1.mytubemusic.data.model.Playlist
import com.mark1.mytubemusic.data.model.PlaylistSongCrossRef
import com.mark1.mytubemusic.data.model.Song
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val playlistDao: PlaylistDao) {

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) {
        playlistDao.renamePlaylist(playlistId, name)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songUri: String) {
        playlistDao.insertCrossRef(PlaylistSongCrossRef(playlistId, songUri))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songUri: String) {
        playlistDao.deleteCrossRef(playlistId, songUri)
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsInPlaylist(playlistId)
    }
}
