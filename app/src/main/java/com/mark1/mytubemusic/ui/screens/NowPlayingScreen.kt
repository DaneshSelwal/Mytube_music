package com.mark1.mytubemusic.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Speed
import androidx.media3.common.Player
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark1.mytubemusic.utils.PaletteExtractor
import com.mark1.mytubemusic.viewmodel.PlayerViewModel
import com.mark1.mytubemusic.ui.theme.MyTubeColors

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.NowPlayingScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToQueue: () -> Unit
) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val shuffleModeEnabled by playerViewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val sleepTimerText by playerViewModel.sleepTimerText.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val currentLyrics by playerViewModel.currentLyrics.collectAsState()
    val progress by playerViewModel.progress.collectAsState()
    val context = LocalContext.current

    var backgroundColor1 by remember { mutableStateOf(Color.DarkGray) }
    var backgroundColor2 by remember { mutableStateOf(Color.Black) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(currentSong?.uri) {
        currentSong?.uri?.let { uri ->
            PaletteExtractor.getColorsFromUri(context, uri) { c1, c2 ->
                backgroundColor1 = c1
                backgroundColor2 = c2
            }
        }
    }

    val animatedColor1 by animateColorAsState(targetValue = backgroundColor1, animationSpec = tween(1000))
    val animatedColor2 by animateColorAsState(targetValue = backgroundColor2, animationSpec = tween(1000))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedColor1, animatedColor2)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = onNavigateToQueue) {
                    Icon(Icons.Default.List, contentDescription = "Queue", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // CD Animation
            var artBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
            
            LaunchedEffect(currentSong?.uri) {
                currentSong?.uri?.let { uri ->
                    try {
                        val retriever = MediaMetadataRetriever()
                        context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                            val art = retriever.embeddedPicture
                            if (art != null) {
                                artBitmap = BitmapFactory.decodeByteArray(art, 0, art.size).asImageBitmap()
                            } else {
                                artBitmap = null
                            }
                        }
                        retriever.release()
                    } catch (e: Exception) {
                        artBitmap = null
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showLyrics,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SpinningCDAnimation(
                        animatedVisibilityScope = animatedVisibilityScope,
                        isPlaying = isPlaying,
                        artBitmap = artBitmap,
                        modifier = Modifier
                            .size(260.dp)
                            .sharedElement(
                                state = rememberSharedContentState(key = "cd_art"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> tween(400) }
                            )
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showLyrics,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LyricsView(
                        lyrics = currentLyrics,
                        currentProgress = progress
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Song Info
            Text(
                text = currentSong?.title ?: "No Song",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentSong?.artist ?: "Unknown Artist",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (currentLyrics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (showLyrics) "Hide Lyrics" else "Show Lyrics",
                    color = MyTubeColors.AccentSkyBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showLyrics = !showLyrics }.padding(8.dp).align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extra Controls (Sleep Timer & Speed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { showSleepTimerDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", tint = if (sleepTimerText != null) MyTubeColors.AccentSkyBlue else Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = sleepTimerText ?: "Timer", color = if (sleepTimerText != null) MyTubeColors.AccentSkyBlue else Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { showSpeedDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = if (playbackSpeed != 1.0f) MyTubeColors.AccentSkyBlue else Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${playbackSpeed}x", color = if (playbackSpeed != 1.0f) MyTubeColors.AccentSkyBlue else Color.White, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timeline / Seekbar
            val duration = currentSong?.duration?.takeIf { it > 0 } ?: 1L
            var sliderPosition by remember(progress) { mutableFloatStateOf(progress.toFloat()) }
            var isDragging by remember { mutableStateOf(false) }

            fun Long.toFormatTime(): String = String.format("%02d:%02d", this / 1000 / 60, (this / 1000) % 60)

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (isDragging) sliderPosition else progress.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = { 
                        sliderPosition = it
                        isDragging = true 
                    },
                    onValueChangeFinished = {
                        playerViewModel.seekTo(sliderPosition.toLong())
                        isDragging = false
                    },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MyTubeColors.AccentSkyBlue,
                        activeTrackColor = MyTubeColors.AccentSkyBlue,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = if (isDragging) sliderPosition.toLong().toFormatTime() else progress.toFormatTime(), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(text = duration.toFormatTime(), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Controls (Glassmorphism style)
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(100.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { playerViewModel.toggleShuffleMode() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle, 
                            contentDescription = "Shuffle", 
                            tint = if (shuffleModeEnabled) MyTubeColors.AccentSkyBlue else Color.White.copy(alpha = 0.5f), 
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { playerViewModel.skipToPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    IconButton(onClick = { playerViewModel.togglePlayPause() }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    IconButton(onClick = { playerViewModel.skipToNext() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    IconButton(onClick = { playerViewModel.cycleRepeatMode() }) {
                        val (icon, tint) = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne to MyTubeColors.AccentSkyBlue
                            Player.REPEAT_MODE_ALL -> Icons.Default.Repeat to MyTubeColors.AccentSkyBlue
                            else -> Icons.Default.Repeat to Color.White.copy(alpha = 0.5f)
                        }
                        Icon(
                            imageVector = icon, 
                            contentDescription = "Repeat", 
                            tint = tint, 
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column {
                    listOf(5, 10, 15, 30, 60).forEach { mins ->
                        TextButton(onClick = { 
                            playerViewModel.setSleepTimer(mins)
                            showSleepTimerDialog = false
                        }) { Text("$mins Minutes") }
                    }
                    TextButton(onClick = { 
                        playerViewModel.setSleepTimer(0)
                        showSleepTimerDialog = false
                    }) { Text("Turn Off") }
                }
            },
            confirmButton = {}
        )
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        TextButton(onClick = { 
                            playerViewModel.setPlaybackSpeed(speed)
                            showSpeedDialog = false
                        }) { Text("${speed}x") }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.SpinningCDAnimation(
    animatedVisibilityScope: AnimatedVisibilityScope,
    isPlaying: Boolean,
    artBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    var currentRotation by remember { mutableFloatStateOf(0f) }
    val rotation = remember { Animatable(currentRotation) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = currentRotation + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(7000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            ) {
                currentRotation = value % 360f
            }
        } else {
            rotation.animateTo(
                targetValue = currentRotation + 10f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            ) {
                currentRotation = value % 360f
            }
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation.value }
            .clip(CircleShape)
            .border(2.dp, MyTubeColors.CdChromeRim, CircleShape)
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(
                MyTubeColors.CdIridescent1,
                MyTubeColors.CdIridescent2,
                MyTubeColors.CdIridescent3,
                MyTubeColors.CdIridescent1
            )
            for (i in 48..110 step 4) {
                val color = colors[(i / 4) % colors.size]
                drawCircle(
                    color = color,
                    radius = i.dp.toPx(),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
            
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 210f,
                sweepAngle = 45f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF0D0D1A), Color(0xFF1C1C3A)))),
            contentAlignment = Alignment.Center
        ) {
            if (artBitmap != null) {
                Image(
                    bitmap = artBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MyTubeColors.AccentSkyBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun LyricsView(lyrics: List<com.mark1.mytubemusic.utils.LyricLine>, currentProgress: Long, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    var activeIndex by remember { mutableIntStateOf(-1) }
    
    LaunchedEffect(currentProgress) {
        val newIndex = lyrics.indexOfLast { it.startTimeMs <= currentProgress }
        if (newIndex != activeIndex && newIndex != -1) {
            activeIndex = newIndex
            listState.animateScrollToItem(if (newIndex > 2) newIndex - 2 else 0)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().height(260.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 100.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            Text(
                text = line.text,
                color = if (isActive) MyTubeColors.AccentSkyBlue else Color.White.copy(alpha = 0.5f),
                fontSize = if (isActive) 22.sp else 18.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

