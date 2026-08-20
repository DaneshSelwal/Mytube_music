package com.mark1.mytubemusic.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mark1.mytubemusic.data.model.Playlist
import com.mark1.mytubemusic.data.model.PlaylistSongCrossRef
import com.mark1.mytubemusic.data.model.Song

@Database(entities = [Song::class, Playlist::class, PlaylistSongCrossRef::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
