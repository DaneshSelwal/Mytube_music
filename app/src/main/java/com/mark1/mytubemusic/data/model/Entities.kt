package com.mark1.mytubemusic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val isFavorite: Boolean = false,
    val albumArtUri: String? = null
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlistId", "songUri"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songUri: String
)
