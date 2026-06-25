package com.mark1.mytubemusic.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark1.mytubemusic.data.model.Song
import com.mark1.mytubemusic.viewmodel.LibraryViewModel
import com.mark1.mytubemusic.viewmodel.PlayerViewModel
import com.mark1.mytubemusic.ui.theme.MyTubeColors
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
        colors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)),
        start = Offset(xOffset, 0f),
        end = Offset(xOffset + 400f, 400f)
    )
    Box(modifier = modifier.background(brush))
}

@Composable
fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Box(modifier = Modifier.width(4.dp).height(20.dp).background(MyTubeColors.AccentSkyBlue, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.HomeScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToNowPlaying: () -> Unit
) {
    val songs by libraryViewModel.allSongs.collectAsState()
    val isScanning by libraryViewModel.isScanning.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val progress by playerViewModel.progress.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Songs", "Albums", "Artists")
    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    val filteredSongs by libraryViewModel.filteredSongs.collectAsState()
    val albums by libraryViewModel.albums.collectAsState()
    val artists by libraryViewModel.artists.collectAsState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(MyTubeColors.BackgroundDeep, MyTubeColors.BackgroundSurface)
    )

    val listState = rememberLazyListState()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(backgroundBrush)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Text(
                        "My Library",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = MyTubeColors.TextPrimary,
                        modifier = Modifier.padding(start = 24.dp, top = 48.dp).align(Alignment.CenterStart)
                    )
                    IconButton(
                        onClick = { MyTubeColors.isDarkTheme = !MyTubeColors.isDarkTheme },
                        modifier = Modifier.padding(end = 16.dp, top = 48.dp).align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = if (MyTubeColors.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MyTubeColors.TextPrimary
                        )
                    }
                }

                if (isScanning) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(8) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                ShimmerEffect(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                } else if (songs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No songs found. Try rescanning your device.", color = Color.White.copy(alpha = 0.5f))
                    }
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { libraryViewModel.updateSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search songs or artists...", color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MyTubeColors.AccentSkyBlue,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = MyTubeColors.AccentSkyBlue,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MyTubeColors.AccentSkyBlue
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title, color = if (selectedTabIndex == index) MyTubeColors.AccentSkyBlue else Color.White.copy(alpha = 0.5f)) }
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = if (currentSong != null) 180.dp else 16.dp)
                    ) {
                        when (selectedTabIndex) {
                            0 -> { // Songs
                                item { SectionHeader("ALL SONGS (${filteredSongs.size})") }
                                itemsIndexed(filteredSongs) { index, song ->
                                    val isCurrent = currentSong?.uri == song.uri
                                    SongItem(song = song, isCurrent = isCurrent, onClick = {
                                        playerViewModel.playQueue(filteredSongs, index)
                                    })
                                }
                            }
                            1 -> { // Albums
                                item { SectionHeader("ALBUMS (${albums.size})") }
                                albums.forEach { (album, albumSongs) ->
                                    item {
                                        Text(
                                            text = album,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                    itemsIndexed(albumSongs) { index, song ->
                                        val isCurrent = currentSong?.uri == song.uri
                                        SongItem(song = song, isCurrent = isCurrent, onClick = {
                                            playerViewModel.playQueue(albumSongs, index)
                                        })
                                    }
                                }
                            }
                            2 -> { // Artists
                                item { SectionHeader("ARTISTS (${artists.size})") }
                                artists.forEach { (artist, artistSongs) ->
                                    item {
                                        Text(
                                            text = artist,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                    itemsIndexed(artistSongs) { index, song ->
                                        val isCurrent = currentSong?.uri == song.uri
                                        SongItem(song = song, isCurrent = isCurrent, onClick = {
                                            playerViewModel.playQueue(artistSongs, index)
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (currentSong != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 24.dp, end = 24.dp)
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
fun SongItem(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
    val cardColor = if (isCurrent) MyTubeColors.AccentGlow else getSongColorHash(song.title)
    val context = androidx.compose.ui.platform.LocalContext.current
    var artBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    LaunchedEffect(song.uri) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fileName = "art_${song.title.hashCode()}"
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
                        } else {
                            val success = com.mark1.mytubemusic.util.ArtworkScraper.fetchAndSaveAlbumArt(
                                "${song.title} ${song.artist}", 
                                context.cacheDir, 
                                fileName
                            )
                            if (success) {
                                artBitmap = android.graphics.BitmapFactory.decodeFile(cachedFile.absolutePath)?.asImageBitmap()
                            }
                        }
                    }
                    retriever.release()
                } catch (e: Exception) {}
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(cardColor, MyTubeColors.BackgroundSurface.copy(alpha=0.5f))))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isCurrent) MyTubeColors.AccentSkyBlue else MyTubeColors.GlassSurface),
            contentAlignment = Alignment.Center
        ) {
            if (artBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = artBitmap!!,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) MyTubeColors.TextPrimary else MyTubeColors.TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) MyTubeColors.AccentSkyBlue else MyTubeColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                fontSize = 14.sp,
                color = MyTubeColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MiniPlayer(
    animatedVisibilityScope: AnimatedVisibilityScope,
    song: Song, 
    isPlaying: Boolean, 
    progress: Float,
    duration: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClick: () -> Unit
) {
    val progressRatio = if (duration > 0L) progress / duration.toFloat() else 0f
    
    Box(
        modifier = Modifier
            .size(150.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = MyTubeColors.AccentSkyBlue, spotColor = MyTubeColors.AccentSkyBlue.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        // Glass background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))))
                .run {
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        this.graphicsLayer {
                            renderEffect = androidx.compose.ui.graphics.BlurEffect(24f, 24f)
                        }
                    } else this
                }
        )
        
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Shared CD Art
            SpinningCDAnimation(
                animatedVisibilityScope = animatedVisibilityScope,
                isPlaying = isPlaying,
                artBitmap = null,
                modifier = Modifier
                    .size(56.dp)
                    .sharedElement(
                        state = rememberSharedContentState(key = "cd_art"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> tween(400) }
                    )
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MyTubeColors.AccentSkyBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
        
        // Progress Bar
        LinearProgressIndicator(
            progress = { progressRatio },
            modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter),
            color = MyTubeColors.AccentSkyBlue,
            trackColor = Color.Transparent
        )
    }
}
