package com.mark1.mytubemusic.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mark1.mytubemusic.ui.components.MiniPlayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mark1.mytubemusic.ui.theme.Tokens
import com.mark1.mytubemusic.ui.theme.MyTubeTypography
import com.mark1.mytubemusic.viewmodel.LibraryViewModel
import com.mark1.mytubemusic.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DetailScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit
) {
    val title by libraryViewModel.selectedDetailTitle.collectAsState()
    val songs by libraryViewModel.selectedDetailSongs.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val progress by playerViewModel.progress.collectAsState()
    val duration by playerViewModel.duration.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var artBitmap by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(songs.firstOrNull()?.uri) {
        val song = songs.firstOrNull() ?: return@LaunchedEffect
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
                        }
                    }
                    retriever.release()
                } catch (e: Exception) {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Tokens.bgDeep)) {
        // Hero Background
        if (artBitmap != null) {
            Image(
                bitmap = artBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(400.dp).blur(40.dp)
            )
        }
        
        // Gradient Scrim
        Box(
            modifier = Modifier.fillMaxWidth().height(400.dp).background(
                Brush.verticalGradient(
                    colors = listOf(Tokens.bgDeep.copy(alpha = 0.4f), Tokens.bgDeep),
                    startY = 0f,
                    endY = 1000f
                )
            )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(title, style = MyTubeTypography.titleLarge, color = Tokens.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Tokens.textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (currentSong != null) 180.dp else 16.dp)
                ) {
                itemsIndexed(songs) { index, song ->
                    val isCurrent = currentSong?.uri == song.uri
                    SongItem(song = song, isCurrent = isCurrent, onClick = {
                        playerViewModel.playQueue(songs, index)
                    })
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
}

}