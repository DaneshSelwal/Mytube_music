package com.mark1.mytubemusic.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.DataSpec
import com.mark1.mytubemusic.repository.OnlineSongRepository
import kotlinx.coroutines.runBlocking



class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Standard HTTP data source with dynamic on-demand stream resolution
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val onlineRepository = OnlineSongRepository()
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            dataSourceFactory,
            object : ResolvingDataSource.Resolver {
                override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                    val uri = dataSpec.uri
                    if (uri.scheme == "online") {
                        val videoId = uri.schemeSpecificPart
                        val streamUrl = runBlocking {
                            onlineRepository.getStreamUrl(videoId)
                        }
                        if (streamUrl != null) {
                            return dataSpec.buildUpon()
                                .setUri(android.net.Uri.parse(streamUrl))
                                .build()
                        }
                    }
                    return dataSpec
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
                    // URIs are resolved dynamically on-demand — pass items through unchanged
                    return Futures.immediateFuture(mediaItems)
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady) {
            stopSelf()
        }
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
