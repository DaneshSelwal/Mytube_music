package com.mark1.mytubemusic.viewmodel

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.content.ContentUris
import android.database.Cursor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mark1.mytubemusic.data.model.Song
import com.mark1.mytubemusic.repository.SongRepository
import com.mark1.mytubemusic.repository.OnlineSongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(private val repository: SongRepository) : ViewModel() {

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val albums: StateFlow<Map<String, List<Song>>> = allSongs.map { songs ->
        songs.groupBy { it.album }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val artists: StateFlow<Map<String, List<Song>>> = allSongs.map { songs ->
        songs.groupBy { it.artist }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    
    private val onlineRepository = OnlineSongRepository()

    private val _onlineSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val onlineSearchResults: StateFlow<List<Song>> = _onlineSearchResults.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.length >= 3) {
            searchJob = viewModelScope.launch(Dispatchers.IO) {
                kotlinx.coroutines.delay(500) // Debounce
                _onlineSearchResults.value = onlineRepository.searchSongs(query)
            }
        } else {
            _onlineSearchResults.value = emptyList()
        }
    }

    val filteredSongs: StateFlow<List<Song>> = combine(allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) songs else songs.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedDetailTitle = MutableStateFlow("")
    val selectedDetailTitle: StateFlow<String> = _selectedDetailTitle.asStateFlow()

    private val _selectedDetailSongs = MutableStateFlow<List<Song>>(emptyList())
    val selectedDetailSongs: StateFlow<List<Song>> = _selectedDetailSongs.asStateFlow()

    fun selectDetail(title: String, songs: List<Song>) {
        _selectedDetailTitle.value = title
        _selectedDetailSongs.value = songs
    }


    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow("")
    val scanProgress: StateFlow<String> = _scanProgress.asStateFlow()

    fun scanDevice(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _scanProgress.value = "Scanning device for music..."
            
            val songs = mutableListOf<Song>()
            
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )
            
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    var title = cursor.getString(titleColumn) ?: "Unknown Title"
                    var artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)
                    val data = cursor.getString(dataColumn)
                    
                    if (data != null && (data.endsWith(".mp3", ignoreCase = true) || data.endsWith(".m4a", ignoreCase = true))) {
                        val fileName = java.io.File(data).nameWithoutExtension
                        
                        // User requested to figure out singer name from the last part of filename
                        if (fileName.contains(" - ")) {
                            val parts = fileName.split(" - ")
                            if (parts.size >= 2) {
                                artist = parts.last().trim()
                                title = parts.dropLast(1).joinToString(" - ").trim()
                                if (title.startsWith("._")) title = title.removePrefix("._")
                            }
                        } else if (fileName.startsWith("._")) {
                            title = fileName.removePrefix("._")
                        }

                        if (artist == "<unknown>" || artist.isBlank()) {
                            artist = "Unknown Artist"
                        }
                        
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )
                        
                        songs.add(
                            Song(
                                uri = contentUri.toString(),
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration
                            )
                        )
                    }
                }
            }
            
            if (songs.isNotEmpty()) {
                _scanProgress.value = "Saving ${songs.size} songs to library..."
                repository.insertSongs(songs)
                repository.deleteSongsNotIn(songs.map { it.uri })
            }
            
            withContext(Dispatchers.Main) {
                _isScanning.value = false
                onComplete()
            }
        }
    }
}
