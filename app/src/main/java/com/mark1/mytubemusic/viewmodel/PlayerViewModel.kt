package com.mark1.mytubemusic.viewmodel

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mark1.mytubemusic.data.model.Song
import com.mark1.mytubemusic.service.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import com.mark1.mytubemusic.util.LyricLine
import com.mark1.mytubemusic.util.LrcParser

class PlayerViewModel : ViewModel() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var player: MediaController? = null
        private set

    private var appContext: Context? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _sleepTimerText = MutableStateFlow<String?>(null)
    val sleepTimerText: StateFlow<String?> = _sleepTimerText.asStateFlow()
    
    private var sleepTimerJob: Job? = null

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentLyrics: StateFlow<List<LyricLine>> = _currentLyrics.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value) {
                    player?.let {
                        _progress.value = it.currentPosition
                        _duration.value = it.duration.takeIf { d -> d > 0 } ?: 0L
                    }
                }
                delay(1000L)
            }
        }
    }

    fun initialize(context: Context) {
        if (controllerFuture != null) return
        
        appContext = context.applicationContext
        
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture?.addListener(
            {
                player = controllerFuture?.get()
                setupPlayerListener()
                updateQueue()
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun setupPlayerListener() {
        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentSong(mediaItem)
                player?.let {
                    _progress.value = it.currentPosition
                    _duration.value = it.duration.takeIf { d -> d > 0 } ?: 0L
                }
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                updateQueue()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleModeEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    private fun updateQueue() {
        val p = player ?: return
        val currentQueue = mutableListOf<Song>()
        for (i in 0 until p.mediaItemCount) {
            val item = p.getMediaItemAt(i)
            val metadata = item.mediaMetadata
            currentQueue.add(
                Song(
                    uri = item.mediaId,
                    title = metadata.title?.toString() ?: "Unknown",
                    artist = metadata.artist?.toString() ?: "Unknown",
                    album = metadata.albumTitle?.toString() ?: "Unknown",
                    duration = metadata.extras?.getLong("duration") ?: 0L
                )
            )
        }
        _queue.value = currentQueue
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        if (mediaItem == null) {
            _currentSong.value = null
            _currentLyrics.value = emptyList()
            return
        }
        val metadata = mediaItem.mediaMetadata
        val trueDuration = player?.duration?.takeIf { it > 0 } ?: metadata.extras?.getLong("duration") ?: 0L
        _currentSong.value = Song(
            uri = mediaItem.mediaId,
            title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown",
            album = metadata.albumTitle?.toString() ?: "Unknown",
            duration = trueDuration
        )
        loadLyricsForUri(mediaItem.mediaId)
    }

    private fun loadLyricsForUri(uriString: String) {
        val context = appContext ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(uriString)
                var dataPath: String? = null
                
                val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                        dataPath = cursor.getString(dataIndex)
                    }
                }
                
                if (dataPath != null) {
                    val lrcPath = dataPath!!.replace(Regex("(?i)\\.mp3$"), ".lrc")
                    _currentLyrics.value = LrcParser.getLyricsFromFile(lrcPath)
                } else {
                    _currentLyrics.value = emptyList()
                }
            } catch (e: Exception) {
                _currentLyrics.value = emptyList()
            }
        }
    }

    fun playSong(song: Song) {
        player?.let {
            val extras = android.os.Bundle().apply { putLong("duration", song.duration) }
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setExtras(extras)
                .build()
                
            val mediaItem = MediaItem.Builder()
                .setMediaId(song.uri)
                .setMediaMetadata(metadata)
                .build()
                
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }
    }
    
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        player?.let { p ->
            val mediaItems = songs.map { song ->
                val extras = android.os.Bundle().apply { putLong("duration", song.duration) }
                val metadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setExtras(extras)
                    .build()
                    
                MediaItem.Builder()
                    .setMediaId(song.uri)
                    .setMediaMetadata(metadata)
                    .build()
            }
            p.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            p.prepare()
            p.play()
        }
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun skipToNext() {
        player?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        player?.seekToPreviousMediaItem()
    }

    fun skipToIndex(index: Int) {
        player?.seekToDefaultPosition(index)
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun toggleShuffleMode() {
        player?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerText.value = null
            return
        }
        sleepTimerJob = viewModelScope.launch {
            var remainingSeconds = minutes * 60
            while (remainingSeconds > 0) {
                val m = remainingSeconds / 60
                val s = remainingSeconds % 60
                _sleepTimerText.value = String.format("%02d:%02d", m, s)
                delay(1000)
                remainingSeconds--
            }
            _sleepTimerText.value = null
            player?.pause()
        }
    }

    fun cycleRepeatMode() {
        player?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.let {
            it.setPlaybackParameters(androidx.media3.common.PlaybackParameters(speed))
            _playbackSpeed.value = speed
        }
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
