package com.mark1.mytubemusic.ui.screens

import com.mark1.mytubemusic.ui.components.MiniPlayer

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Data
import androidx.work.WorkManager
import com.mark1.mytubemusic.worker.DownloadWorker
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark1.mytubemusic.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import com.mark1.mytubemusic.data.model.Song
import com.mark1.mytubemusic.viewmodel.LibraryViewModel
import com.mark1.mytubemusic.viewmodel.PlayerViewModel
import com.mark1.mytubemusic.ui.theme.MyTubeColors
import com.mark1.mytubemusic.ui.theme.Tokens
import com.mark1.mytubemusic.ui.theme.MyTubeTypography
import kotlin.math.abs

fun getSongColorHash(title: String): Color {
    val hash = title.hashCode()
    val r = abs((hash and 0xFF0000) shr 16)
    val g = abs((hash and 0x00FF00) shr 8)
    val b = abs(hash and 0x0000FF)
    return Color(r / 255f * 0.3f + 0.1f, g / 255f * 0.3f + 0.1f, b / 255f * 0.3f + 0.1f, 1f)
}

@Composable
fun ShimmerEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val brush = Brush.linearGradient(
        colors = listOf(Tokens.accentPrimary.copy(alpha = 0.05f), Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)),
        start = Offset(xOffset, 0f),
        end = Offset(xOffset + 400f, 400f)
    )
    Box(modifier = modifier.background(brush))
}

@Composable
fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Box(modifier = Modifier.width(4.dp).height(20.dp).background(Tokens.accentPrimary, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = Tokens.textPrimary, style = MyTubeTypography.titleMedium, letterSpacing = 1.sp)
    }
}

@Composable
fun ShimmerSongItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(Tokens.bgElevated, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            ShimmerEffect(Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp))) {
                ShimmerEffect(Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp))) {
                ShimmerEffect(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun EmptyLibrary() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Tokens.textDisabled,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your library is empty",
            style = MyTubeTypography.titleLarge.copy(color = Tokens.textSecondary)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try rescanning your device to find music.",
            style = MyTubeTypography.bodyMedium.copy(color = Tokens.textDisabled)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.HomeScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToDetail: () -> Unit
) {
    val songs by libraryViewModel.allSongs.collectAsState()
    val isScanning by libraryViewModel.isScanning.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val progress by playerViewModel.progress.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Songs", "Albums", "Artists", "Playlists", "Online", "Browser")
    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    val filteredSongs by libraryViewModel.filteredSongs.collectAsState()
    val onlineSearchResults by libraryViewModel.onlineSearchResults.collectAsState()
    val isOnlineSearching by libraryViewModel.isOnlineSearching.collectAsState()
    val albums by libraryViewModel.albums.collectAsState()
    val artists by libraryViewModel.artists.collectAsState()
    val localPlaylists by libraryViewModel.localPlaylists.collectAsState()
    val onlinePlaylists by libraryViewModel.onlinePlaylists.collectAsState()

    var onlineSearchText by remember { mutableStateOf("") }

    var showCreateLocalPlaylistDialog by remember { mutableStateOf(false) }
    var showCreateOnlinePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    
    var songToAddToLocalPlaylist by remember { mutableStateOf<Song?>(null) }
    var songToAddToOnlinePlaylist by remember { mutableStateOf<Song?>(null) }
    
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(MyTubeColors.BackgroundDeep, MyTubeColors.BackgroundSurface)
    )

    val listState = rememberLazyListState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(backgroundBrush).nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MyTube", style = MyTubeTypography.titleLarge, color = Tokens.accentPrimary)
                        Text("Music", style = MyTubeTypography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { MyTubeColors.isDarkTheme = !MyTubeColors.isDarkTheme }) {
                        AnimatedContent(targetState = MyTubeColors.isDarkTheme, label = "ThemeToggle") { dark ->
                            Icon(
                                imageVector = if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = Tokens.accentPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Tokens.bgSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {

                if (isScanning) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(8) {
                            ShimmerSongItem()
                        }
                    }
                } else {
                    // Local search only shown outside Online tab
                    if (selectedTabIndex < 4) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { libraryViewModel.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search songs or artists...", color = Tokens.textSecondary) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Tokens.bgSurface,
                                unfocusedContainerColor = Tokens.bgSurface,
                                focusedTextColor = Tokens.textPrimary,
                                unfocusedTextColor = Tokens.textPrimary,
                                cursorColor = MyTubeColors.AccentSkyBlue,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                    }

                    PillTabRow(
                        selectedIndex = selectedTabIndex,
                        tabs = tabs,
                        onTabSelect = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedTabIndex = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedTabIndex != 5) {
                            when (selectedTabIndex) {
                        0 -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = if (currentSong != null) 96.dp else 16.dp)
                            ) {
                                if (songs.isEmpty() && onlineSearchResults.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillParentMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            EmptyLibrary()
                                        }
                                    }
                                } else {
                                    if (songs.isNotEmpty() && (filteredSongs.isNotEmpty() || searchQuery.isBlank())) {
                                        item { SectionHeader("LOCAL SONGS (${filteredSongs.size})") }
                                        itemsIndexed(filteredSongs) { index, song ->
                                            val isCurrent = currentSong?.uri == song.uri
                                            SongItem(
                                                song = song, 
                                                isCurrent = isCurrent, 
                                                modifier = Modifier.animateItem(),
                                                onAddClick = {
                                                    songToAddToLocalPlaylist = song
                                                },
                                                onClick = {
                                                    playerViewModel.playQueue(filteredSongs, index)
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                if (onlineSearchResults.isNotEmpty()) {
                                    item { SectionHeader("ONLINE SEARCH RESULTS") }
                                    itemsIndexed(onlineSearchResults) { index, song ->
                                        val isCurrent = currentSong?.uri == song.uri
                                        SongItem(
                                            song = song, 
                                            isCurrent = isCurrent, 
                                            modifier = Modifier.animateItem(),
                                            onAddClick = {
                                                songToAddToOnlinePlaylist = song
                                            },
                                            onClick = {
                                                playerViewModel.playQueue(onlineSearchResults, index)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> { // Albums
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = if (currentSong != null) 96.dp else 16.dp, start = 8.dp, end = 8.dp, top = 8.dp)
                            ) {
                                items(albums.entries.toList()) { (album, albumSongs) ->
                                    val artist = albumSongs.firstOrNull()?.artist ?: "Unknown Artist"
                                    AlbumArtistCard(modifier = Modifier.animateItem(), title = album, subtitle = artist, song = albumSongs.firstOrNull()) {
                                        libraryViewModel.selectDetail(album, albumSongs)
                                        onNavigateToDetail()
                                    }
                                }
                            }
                        }
                        2 -> { // Artists
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = if (currentSong != null) 96.dp else 16.dp, start = 8.dp, end = 8.dp, top = 8.dp)
                            ) {
                                items(artists.entries.toList()) { (artist, artistSongs) ->
                                    val albumCount = artistSongs.map { it.album }.distinct().size
                                    AlbumArtistCard(modifier = Modifier.animateItem(), title = artist, subtitle = "$albumCount Album(s)", song = artistSongs.firstOrNull(), badge = "Artist") {
                                        libraryViewModel.selectDetail(artist, artistSongs)
                                        onNavigateToDetail()
                                    }
                                }
                            }
                        }
                        3 -> { // Playlists
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = if (currentSong != null) 96.dp else 16.dp, start = 16.dp, end = 16.dp, top = 8.dp)
                            ) {
                                // Local Playlists Section
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SectionHeader("Local Playlists")
                                        IconButton(onClick = {
                                            newPlaylistName = ""
                                            showCreateLocalPlaylistDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Create Local Playlist",
                                                tint = Tokens.accentPrimary
                                            )
                                        }
                                    }
                                }

                                if (localPlaylists.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No local playlists available.",
                                            color = Tokens.textDisabled,
                                            style = MyTubeTypography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                        )
                                    }
                                } else {
                                    items(localPlaylists) { playlist ->
                                        PlaylistItem(
                                            name = playlist.name,
                                            type = "Local",
                                            onClick = {
                                                scope.launch {
                                                    libraryViewModel.playlistRepository.getSongsInPlaylist(playlist.id)
                                                        .first().let { songsInPlaylist ->
                                                            libraryViewModel.selectPlaylistDetail(
                                                                id = playlist.id,
                                                                type = "local",
                                                                name = playlist.name,
                                                                songs = songsInPlaylist
                                                            )
                                                            onNavigateToDetail()
                                                        }
                                                }
                                            }
                                        )
                                    }
                                }

                                // Online Playlists Section
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SectionHeader("Online Playlists")
                                        IconButton(onClick = {
                                            newPlaylistName = ""
                                            showCreateOnlinePlaylistDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Create Online Playlist",
                                                tint = Tokens.accentPrimary
                                            )
                                        }
                                    }
                                }

                                if (onlinePlaylists.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No online playlists available.",
                                            color = Tokens.textDisabled,
                                            style = MyTubeTypography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                        )
                                    }
                                } else {
                                    items(onlinePlaylists) { playlist ->
                                        PlaylistItem(
                                            name = playlist.name,
                                            type = "Online",
                                            onClick = {
                                                scope.launch {
                                                    libraryViewModel.onlinePlaylistRepository.getSongsInPlaylist(playlist.id)
                                                        .first().let { onlineSongs ->
                                                            val mappedSongs = onlineSongs.map { onlineSong ->
                                                                Song(
                                                                    uri = onlineSong.uri,
                                                                    title = onlineSong.title,
                                                                    artist = onlineSong.artist,
                                                                    album = onlineSong.album,
                                                                    duration = onlineSong.duration,
                                                                    isFavorite = false,
                                                                    albumArtUri = onlineSong.albumArtUri
                                                                )
                                                            }
                                                            libraryViewModel.selectPlaylistDetail(
                                                                id = playlist.id,
                                                                type = "online",
                                                                name = playlist.name,
                                                                songs = mappedSongs
                                                            )
                                                            onNavigateToDetail()
                                                        }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        4 -> { // Online streaming tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Online search bar
                                OutlinedTextField(
                                    value = onlineSearchText,
                                    onValueChange = {
                                        onlineSearchText = it
                                        libraryViewModel.updateSearchQuery(it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    placeholder = { Text("Search YouTube Music...", color = Tokens.textSecondary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Tokens.accentPrimary
                                        )
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Tokens.bgSurface,
                                        unfocusedContainerColor = Tokens.bgSurface,
                                        focusedTextColor = Tokens.textPrimary,
                                        unfocusedTextColor = Tokens.textPrimary,
                                        cursorColor = MyTubeColors.AccentSkyBlue,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )

                                when {
                                    isOnlineSearching -> {
                                        // Loading state
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(
                                                    color = Tokens.accentPrimary,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(Modifier.height(16.dp))
                                                Text(
                                                    "Searching YouTube Music...",
                                                    style = MyTubeTypography.bodyMedium.copy(color = Tokens.textSecondary)
                                                )
                                            }
                                        }
                                    }
                                    onlineSearchResults.isNotEmpty() -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(bottom = if (currentSong != null) 96.dp else 16.dp)
                                        ) {
                                            item { SectionHeader("RESULTS (${onlineSearchResults.size})") }
                                            itemsIndexed(onlineSearchResults) { index, song ->
                                                val isCurrent = currentSong?.uri == song.uri
                                                SongItem(
                                                    song = song,
                                                    isCurrent = isCurrent,
                                                    modifier = Modifier.animateItem(),
                                                    onAddClick = { songToAddToOnlinePlaylist = song },
                                                    onClick = {
                                                        playerViewModel.playQueue(onlineSearchResults, index)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    onlineSearchText.isNotBlank() -> {
                                        // No results
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = Tokens.textDisabled,
                                                    modifier = Modifier.size(64.dp)
                                                )
                                                Spacer(Modifier.height(16.dp))
                                                Text(
                                                    "No results found",
                                                    style = MyTubeTypography.titleLarge.copy(color = Tokens.textSecondary)
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        // Empty / prompt state
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = Tokens.accentPrimary.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(72.dp)
                                                )
                                                Spacer(Modifier.height(16.dp))
                                                Text(
                                                    "Stream ad-free music",
                                                    style = MyTubeTypography.titleLarge.copy(color = Tokens.textPrimary)
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    "Search for any song, artist or album above",
                                                    style = MyTubeTypography.bodyMedium.copy(color = Tokens.textSecondary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                            }
                        }

                        // Persistent WebView Browser Screen (shifted offscreen when not active)
                        BrowserScreen(
                            isActive = selectedTabIndex == 5,
                            playerViewModel = playerViewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (selectedTabIndex == 5) Modifier 
                                    else Modifier.offset(x = 10000.dp) // Offscreen to avoid destruction
                                )
                        )
                    }

                    if (showCreateLocalPlaylistDialog) {
                        AlertDialog(
                            onDismissRequest = { showCreateLocalPlaylistDialog = false },
                            title = { Text("Create Local Playlist", color = Tokens.textPrimary) },
                            text = {
                                OutlinedTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    label = { Text("Playlist Name") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newPlaylistName.isNotBlank()) {
                                            libraryViewModel.createLocalPlaylist(newPlaylistName)
                                        }
                                        showCreateLocalPlaylistDialog = false
                                    }
                                ) {
                                    Text("Create", color = Tokens.accentPrimary)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateLocalPlaylistDialog = false }) {
                                    Text("Cancel", color = Tokens.textSecondary)
                                }
                            }
                        )
                    }

                    if (showCreateOnlinePlaylistDialog) {
                        AlertDialog(
                            onDismissRequest = { showCreateOnlinePlaylistDialog = false },
                            title = { Text("Create Online Playlist", color = Tokens.textPrimary) },
                            text = {
                                OutlinedTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    label = { Text("Playlist Name") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newPlaylistName.isNotBlank()) {
                                            libraryViewModel.createOnlinePlaylist(newPlaylistName)
                                        }
                                        showCreateOnlinePlaylistDialog = false
                                    }
                                ) {
                                    Text("Create", color = Tokens.accentPrimary)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateOnlinePlaylistDialog = false }) {
                                    Text("Cancel", color = Tokens.textSecondary)
                                }
                            }
                        )
                    }

                    // Dialog for adding local song to local playlists
                    if (songToAddToLocalPlaylist != null) {
                        val song = songToAddToLocalPlaylist!!
                        AlertDialog(
                            onDismissRequest = { songToAddToLocalPlaylist = null },
                            title = { Text("Add to Local Playlist", color = Tokens.textPrimary) },
                            text = {
                                if (localPlaylists.isEmpty()) {
                                    Text("No local playlists found. Please create one first.", color = Tokens.textSecondary)
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                        items(localPlaylists) { playlist ->
                                            Text(
                                                text = playlist.name,
                                                color = Tokens.textPrimary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        libraryViewModel.addSongToLocalPlaylist(playlist.id, song.uri)
                                                        songToAddToLocalPlaylist = null
                                                    }
                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                                style = MyTubeTypography.bodyLarge
                                            )
                                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Tokens.textDisabled.copy(alpha = 0.2f)))
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { songToAddToLocalPlaylist = null }) {
                                    Text("Cancel", color = Tokens.textSecondary)
                                }
                            }
                        )
                    }

                    // Dialog for adding online song to online playlists
                    if (songToAddToOnlinePlaylist != null) {
                        val song = songToAddToOnlinePlaylist!!
                        AlertDialog(
                            onDismissRequest = { songToAddToOnlinePlaylist = null },
                            title = { Text("Add to Online Playlist", color = Tokens.textPrimary) },
                            text = {
                                if (onlinePlaylists.isEmpty()) {
                                    Text("No online playlists found. Please create one first.", color = Tokens.textSecondary)
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                        items(onlinePlaylists) { playlist ->
                                            Text(
                                                text = playlist.name,
                                                color = Tokens.textPrimary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        libraryViewModel.addSongToOnlinePlaylist(playlist.id, song)
                                                        songToAddToOnlinePlaylist = null
                                                    }
                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                                style = MyTubeTypography.bodyLarge
                                            )
                                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Tokens.textDisabled.copy(alpha = 0.2f)))
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { songToAddToOnlinePlaylist = null }) {
                                    Text("Cancel", color = Tokens.textSecondary)
                                }
                            }
                        )
                    }
                }
            }

            if (currentSong != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    MiniPlayer(
                        animatedVisibilityScope = animatedVisibilityScope,
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        progress = progress.toFloat(),
                        duration = duration,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.skipToNext() },
                        onPrev = { playerViewModel.skipToPrevious() },
                        onClick = onNavigateToNowPlaying
                    )
                }
            }
        }
    }
}

@Composable
fun SongItem(
    modifier: Modifier = Modifier, 
    song: Song, 
    isCurrent: Boolean, 
    onAddClick: (() -> Unit)? = null,
    onRemoveClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isCurrent) Tokens.bgElevated else Color.Transparent,
        animationSpec = tween(300)
    )
    val context = androidx.compose.ui.platform.LocalContext.current
    var artBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    LaunchedEffect(song.uri) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fileName = "art_${(song.title + "_" + song.artist).hashCode()}"
            val cachedFile = java.io.File(context.cacheDir, "$fileName.jpg")
            if (cachedFile.exists()) {
                artBitmap = android.graphics.BitmapFactory.decodeFile(cachedFile.absolutePath)?.asImageBitmap()
            } else {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    context.contentResolver.openFileDescriptor(android.net.Uri.parse(song.uri), "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        val art = retriever.embeddedPicture
                        if (art != null) {
                            artBitmap = android.graphics.BitmapFactory.decodeByteArray(art, 0, art.size).asImageBitmap()
                        }
                    }
                    retriever.release()
                } catch (e: Exception) {}
            }
        }
    }
    
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar — only visible when playing
        AnimatedVisibility(visible = isCurrent) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Tokens.accentPrimary, Tokens.accentSecondary)
                        )
                    )
            )
            Spacer(Modifier.width(8.dp))
        }

        
        // Album art thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Tokens.bgElevated),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                coil.compose.AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (artBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = artBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Tokens.textDisabled
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MyTubeTypography.bodyMedium.copy(
                    color = if (isCurrent) Tokens.accentPrimary else Tokens.textPrimary,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MyTubeTypography.labelSmall.copy(color = Tokens.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (onAddClick != null) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to playlist",
                        tint = Tokens.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            if (onRemoveClick != null) {
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from playlist",
                        tint = Tokens.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            
            fun Long.toFormatTime(): String = String.format("%02d:%02d", this / 1000 / 60, (this / 1000) % 60)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    song.duration.toFormatTime(),
                    style = MyTubeTypography.labelSmall.copy(color = Tokens.textDisabled, fontSize = 11.sp)
                )
                if (song.uri.startsWith("online:")) {
                    Spacer(Modifier.height(4.dp))
                    IconButton(
                        onClick = {
                            val videoId = song.uri.removePrefix("online:")
                            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                                .setInputData(
                                    Data.Builder()
                                        .putString("videoId", videoId)
                                        .putString("title", song.title)
                                        .putString("artist", song.artist)
                                        .putString("albumArtUri", song.albumArtUri)
                                        .build()
                                )
                                .build()
                            WorkManager.getInstance(context).enqueue(workRequest)
                            android.widget.Toast.makeText(context, "Downloading...", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Tokens.accentPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistItem(
    name: String,
    type: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Tokens.bgElevated, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Tokens.accentPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Tokens.accentPrimary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Tokens.textPrimary,
                style = MyTubeTypography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$type Playlist",
                color = Tokens.textSecondary,
                style = MyTubeTypography.labelSmall
            )
        }
    }
}


@Composable
fun AlbumArtistCard(modifier: Modifier = Modifier, title: String, subtitle: String, song: Song?, badge: String? = null, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var artBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    LaunchedEffect(song?.uri) {
        if (song == null) return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fileName = "art_${(song.title + "_" + song.artist).hashCode()}"
            val cachedFile = java.io.File(context.cacheDir, "$fileName.jpg")
            if (cachedFile.exists()) {
                artBitmap = android.graphics.BitmapFactory.decodeFile(cachedFile.absolutePath)?.asImageBitmap()
            } else {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    context.contentResolver.openFileDescriptor(android.net.Uri.parse(song.uri), "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        val art = retriever.embeddedPicture
                        if (art != null) {
                            artBitmap = android.graphics.BitmapFactory.decodeByteArray(art, 0, art.size).asImageBitmap()
                        }
                    }
                    retriever.release()
                } catch (e: Exception) {}
            }
        }
    }
    
    Box(
        modifier = modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        if (artBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = artBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Tokens.bgElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Tokens.textDisabled, modifier = Modifier.size(48.dp))
            }
        }

        // Bottom gradient scrim for text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Tokens.bgDeep.copy(alpha = 0.9f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(title, style = MyTubeTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Tokens.textPrimary), maxLines = 1)
            Text(subtitle, style = MyTubeTypography.labelSmall.copy(color = Tokens.textSecondary), maxLines = 1)
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Tokens.accentPrimary.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    style = MyTubeTypography.labelSmall.copy(
                        color = Tokens.bgDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

@Composable
fun PillTabRow(selectedIndex: Int, tabs: List<String>, onTabSelect: (Int) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Tokens.bgSurface)
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
            label = "tab_indicator"
        )
        Box(
            modifier = Modifier
                .width(tabWidth)
                .fillMaxHeight()
                .offset(x = indicatorOffset)
                .clip(RoundedCornerShape(20.dp))
                .background(Tokens.glassTint)
                .border(1.dp, Tokens.accentPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MyTubeTypography.bodyMedium.copy(
                            color = if (i == selectedIndex) Tokens.accentPrimary else Tokens.textSecondary,
                            fontWeight = if (i == selectedIndex) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}
