package com.mark1.mytubemusic.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mark1.mytubemusic.data.model.Playlist
import com.mark1.mytubemusic.data.model.PlaylistSongCrossRef
import com.mark1.mytubemusic.data.model.Song
import com.mark1.mytubemusic.data.model.OnlinePlaylist
import com.mark1.mytubemusic.data.model.OnlinePlaylistSong
import com.mark1.mytubemusic.data.model.OnlinePlaylistSongCrossRef

@Database(
    entities = [
        Song::class, Playlist::class, PlaylistSongCrossRef::class,
        OnlinePlaylist::class, OnlinePlaylistSong::class, OnlinePlaylistSongCrossRef::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun onlinePlaylistDao(): OnlinePlaylistDao
    abstract fun playlistDao(): PlaylistDao
}
