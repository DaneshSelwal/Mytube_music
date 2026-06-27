package com.mark1.mytubemusic.repository

import com.mark1.mytubemusic.data.db.OnlinePlaylistDao
import com.mark1.mytubemusic.data.model.OnlinePlaylist
import com.mark1.mytubemusic.data.model.OnlinePlaylistSong
import com.mark1.mytubemusic.data.model.OnlinePlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

class OnlinePlaylistRepository(private val onlinePlaylistDao: OnlinePlaylistDao) {

    val allPlaylists: Flow<List<OnlinePlaylist>> = onlinePlaylistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long {
        return onlinePlaylistDao.insertPlaylist(OnlinePlaylist(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        onlinePlaylistDao.deletePlaylist(playlistId)
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) {
        onlinePlaylistDao.renamePlaylist(playlistId, name)
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: OnlinePlaylistSong) {
        onlinePlaylistDao.insertSong(song)
        onlinePlaylistDao.insertCrossRef(OnlinePlaylistSongCrossRef(playlistId, song.uri))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songUri: String) {
        onlinePlaylistDao.deleteCrossRef(playlistId, songUri)
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<OnlinePlaylistSong>> {
        return onlinePlaylistDao.getSongsInPlaylist(playlistId)
    }
}
