package com.mark1.mytubemusic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

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

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songUri"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Song::class,
            parentColumns = ["uri"],
            childColumns = ["songUri"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songUri: String
)

@Entity(tableName = "online_playlists")
data class OnlinePlaylist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(tableName = "online_songs")
data class OnlinePlaylistSong(
    @PrimaryKey
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArtUri: String? = null
)

@Entity(
    tableName = "online_playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songUri"],
    foreignKeys = [
        ForeignKey(
            entity = OnlinePlaylist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OnlinePlaylistSong::class,
            parentColumns = ["uri"],
            childColumns = ["songUri"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OnlinePlaylistSongCrossRef(
    val playlistId: Long,
    val songUri: String
)

