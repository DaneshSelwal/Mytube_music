package com.mark1.mytubemusic.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.mark1.mytubemusic.repository.OnlineSongRepository
import kotlinx.coroutines.runBlocking
import android.net.Uri


class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
            
                val onlineRepository = OnlineSongRepository()
        val defaultDataSourceFactory = DefaultDataSource.Factory(this)
        
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            defaultDataSourceFactory,
            object : ResolvingDataSource.Resolver {
                override fun resolveReportedUri(uri: Uri): Uri {
                    val uriString = uri.toString()
                    if (uriString.startsWith("online:")) {
                        val videoId = uriString.removePrefix("online:")
                        val streamUrl = runBlocking { onlineRepository.getStreamUrl(videoId) }
                        if (streamUrl != null) {
                            return Uri.parse(streamUrl)
                        }
                    }
                    return uri
                }

                override fun resolveDataSpec(dataSpec: androidx.media3.datasource.DataSpec): androidx.media3.datasource.DataSpec {
                    return dataSpec.buildUpon().setUri(resolveReportedUri(dataSpec.uri)).build()
                }
            }
        )

        val mediaSourceFactory = DefaultMediaSourceFactory(resolvingDataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    val updatedMediaItems = mediaItems.map { it.buildUpon().setUri(it.mediaId).build() }.toMutableList()
                    return Futures.immediateFuture(updatedMediaItems)
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            if (player.playWhenReady) {
                player.pause()
            }
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
