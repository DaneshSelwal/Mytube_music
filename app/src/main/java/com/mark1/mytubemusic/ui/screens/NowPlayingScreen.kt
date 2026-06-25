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
import androidx.compose.ui.draw.shadow
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
import com.mark1.mytubemusic.util.PaletteExtractor
import com.mark1.mytubemusic.viewmodel.PlayerViewModel
import com.mark1.mytubemusic.ui.theme.MyTubeColors
import com.mark1.mytubemusic.data.model.Song
import com.mark1.mytubemusic.ui.theme.Tokens
import com.mark1.mytubemusic.ui.theme.MyTubeTypography
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

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

    var backgroundColor1 by remember { mutableStateOf(Tokens.bgDeep) }
    var backgroundColor2 by remember { mutableStateOf(Tokens.bgDeep) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

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

    val pulseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f + (0.05f * ((progress / 400) % 2L).toFloat()) else 1.0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing)
    )

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.bgDeep)
    ) {
        // Glowing animated background based on palette
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
            rotationZ = rotation
        }) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedColor1.copy(alpha = 0.15f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.2f),
                    radius = size.width * 1.2f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedColor2.copy(alpha = 0.15f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.8f),
                    radius = size.width * 1.2f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Tokens.accentPrimary.copy(alpha = 0.08f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * 0.8f
                )
            )
        }
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val haptic = LocalHapticFeedback.current

        val topBar = @Composable {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = if (isLandscape) 0.dp else 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back", tint = Tokens.textPrimary, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { showQueueSheet = true }) {
                    Icon(Icons.Default.List, contentDescription = "Queue", tint = Tokens.textPrimary, modifier = Modifier.size(32.dp))
                }
            }
        }

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

        val cdOrLyrics = @Composable {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 220.dp else 260.dp),
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
                            .fillMaxWidth(if (isLandscape) 0.7f else 0.85f)
                            .aspectRatio(1f)
                            .widthIn(max = 450.dp)
                            .shadow(32.dp, CircleShape, spotColor = MyTubeColors.AccentSkyBlue.copy(alpha = 0.4f), ambientColor = MyTubeColors.AccentSkyBlue)
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
                    LyricsView(lyrics = currentLyrics, currentProgress = progress)
                }
            }
        }

        val songInfo = @Composable {
            Column(horizontalAlignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentSong?.title ?: "No Song",
                    style = MyTubeTypography.displayLarge.copy(color = Tokens.textPrimary),
                    fontSize = if (isLandscape) 28.sp else 32.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentSong?.artist ?: "Unknown Artist",
                    style = MyTubeTypography.titleLarge.copy(color = Tokens.textSecondary),
                    fontSize = if (isLandscape) 18.sp else 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentLyrics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Text(
                            text = if (showLyrics) "Hide Lyrics" else "Show Lyrics",
                            style = MyTubeTypography.labelSmall.copy(color = Tokens.accentPrimary),
                            modifier = Modifier.clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showLyrics = !showLyrics 
                            }.padding(8.dp).background(Tokens.glassTint, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sleep Timer",
                            style = MyTubeTypography.labelSmall.copy(color = Tokens.textSecondary),
                            modifier = Modifier.clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showSleepTimerDialog = true 
                            }.padding(8.dp).background(Tokens.glassTint, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sleep Timer",
                        style = MyTubeTypography.labelSmall.copy(color = Tokens.textSecondary),
                        modifier = Modifier.clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSleepTimerDialog = true 
                        }.padding(8.dp).background(Tokens.glassTint, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        val extraControls = @Composable {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isLandscape) Arrangement.Start else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MyTubeColors.TextPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { showSleepTimerDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", tint = if (sleepTimerText != null) MyTubeColors.AccentSkyBlue else MyTubeColors.TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = sleepTimerText ?: "Timer", color = if (sleepTimerText != null) MyTubeColors.AccentSkyBlue else MyTubeColors.TextPrimary, style = MyTubeTypography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Surface(
                    color = MyTubeColors.TextPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { showSpeedDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = if (playbackSpeed != 1.0f) MyTubeColors.AccentSkyBlue else MyTubeColors.TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${playbackSpeed}x", color = if (playbackSpeed != 1.0f) MyTubeColors.AccentSkyBlue else MyTubeColors.TextPrimary, style = MyTubeTypography.labelSmall)
                    }
                }
            }
        }

        val duration = currentSong?.duration?.takeIf { it > 0 } ?: 1L
        var sliderPosition by remember(progress) { mutableFloatStateOf(progress.toFloat()) }
        var isDragging by remember { mutableStateOf(false) }
        fun Long.toFormatTime(): String = String.format("%02d:%02d", this / 1000 / 60, (this / 1000) % 60)

        val timeline = @Composable {
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
                        thumbColor = Tokens.accentSecondary,
                        activeTrackColor = Tokens.accentPrimary,
                        inactiveTrackColor = Tokens.strokeSubtle
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = if (isDragging) sliderPosition.toLong().toFormatTime() else progress.toFormatTime(), style = MyTubeTypography.labelSmall.copy(color = Tokens.textDisabled))
                    Text(text = duration.toFormatTime(), style = MyTubeTypography.labelSmall.copy(color = Tokens.textDisabled))
                }
            }
        }

        val mainControls = @Composable {
            Surface(
                color = Tokens.bgSurface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(if (isLandscape) 80.dp else 100.dp).border(1.dp, Tokens.strokeSubtle, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerViewModel.toggleShuffleMode() 
                    }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (shuffleModeEnabled) Tokens.accentPrimary else Tokens.textDisabled, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playerViewModel.skipToPrevious() 
                    }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Tokens.textPrimary, modifier = Modifier.size(42.dp))
                    }
                    // Play/Pause glowing button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(12.dp, CircleShape, spotColor = Tokens.accentPrimary)
                            .clip(CircleShape)
                            .background(Tokens.accentPrimary)
                            .clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                playerViewModel.togglePlayPause() 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Tokens.bgDeep, modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playerViewModel.skipToNext() 
                    }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Tokens.textPrimary, modifier = Modifier.size(42.dp))
                    }
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerViewModel.cycleRepeatMode() 
                    }) {
                        val (icon, tint) = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne to Tokens.accentPrimary
                            Player.REPEAT_MODE_ALL -> Icons.Default.Repeat to Tokens.accentPrimary
                            else -> Icons.Default.Repeat to Tokens.textDisabled
                        }
                        Icon(icon, contentDescription = "Repeat", tint = tint, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    topBar()
                    Spacer(modifier = Modifier.weight(1f))
                    cdOrLyrics()
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.width(32.dp))
                Column(
                    modifier = Modifier.weight(1.2f).fillMaxHeight(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    songInfo()
                    Spacer(modifier = Modifier.height(12.dp))
                    extraControls()
                    Spacer(modifier = Modifier.height(16.dp))
                    timeline()
                    Spacer(modifier = Modifier.height(16.dp))
                    mainControls()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                topBar()
                Spacer(modifier = Modifier.weight(1f))
                cdOrLyrics()
                Spacer(modifier = Modifier.weight(1f))
                songInfo()
                Spacer(modifier = Modifier.height(16.dp))
                extraControls()
                Spacer(modifier = Modifier.height(24.dp))
                timeline()
                Spacer(modifier = Modifier.height(32.dp))
                mainControls()
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", style = MyTubeTypography.titleLarge.copy(color = Tokens.textPrimary)) },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        Text(
                            text = "$minutes Minutes",
                            style = MyTubeTypography.bodyMedium.copy(color = Tokens.textSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerViewModel.setSleepTimer(minutes)
                                    showSleepTimerDialog = false
                                }
                                .padding(16.dp)
                        )
                    }
                    Text(
                        text = "Turn Off",
                        style = MyTubeTypography.bodyMedium.copy(color = Tokens.accentPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerViewModel.setSleepTimer(0)
                                showSleepTimerDialog = false
                            }
                            .padding(16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel", style = MyTubeTypography.bodyMedium.copy(color = Tokens.accentPrimary))
                }
            },
            containerColor = Tokens.bgElevated
        )
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", style = MyTubeTypography.titleLarge.copy(color = Tokens.textPrimary)) },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Text(
                            text = "${speed}x",
                            style = MyTubeTypography.bodyMedium.copy(color = Tokens.textSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerViewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Cancel", style = MyTubeTypography.bodyMedium.copy(color = Tokens.accentPrimary))
                }
            },
            containerColor = Tokens.bgElevated
        )
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            playerViewModel = playerViewModel,
            onDismissRequest = { showQueueSheet = false }
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
            .border(1.dp, Tokens.strokeSubtle, CircleShape)
            .background(Tokens.bgDeep),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2
            val minRadius = maxRadius * 0.35f
            val step = (maxRadius - minRadius) / 12
            
            // Vinyl grooves
            for (i in 0..11) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = minRadius + (i * step),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
            }
            
            // Subtle vinyl shine
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 210f,
                sweepAngle = 30f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx())
            )
        }

        // Center label (album art + record center hole)
        Box(
            modifier = Modifier
                .fillMaxSize(0.4f)
                .clip(CircleShape)
                .border(2.dp, Tokens.bgDeep, CircleShape)
                .background(Tokens.bgSurface),
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
                    tint = Tokens.textDisabled,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // The hole in the middle
            Box(
                modifier = Modifier
                    .fillMaxSize(0.12f)
                    .clip(CircleShape)
                    .background(Tokens.bgDeep)
            )
        }
    }
}

@Composable
fun LyricsView(lyrics: List<com.mark1.mytubemusic.util.LyricLine>, currentProgress: Long, modifier: Modifier = Modifier) {
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
                style = if (isActive) MyTubeTypography.titleLarge else MyTubeTypography.bodyMedium,
                color = if (isActive) Tokens.accentPrimary else Tokens.textSecondary,
                modifier = Modifier.padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(playerViewModel: PlayerViewModel, onDismissRequest: () -> Unit) {
    val queue by playerViewModel.queue.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = com.mark1.mytubemusic.ui.theme.Tokens.bgElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Playing Queue",
                style = com.mark1.mytubemusic.ui.theme.MyTubeTypography.titleLarge.copy(color = com.mark1.mytubemusic.ui.theme.Tokens.textPrimary),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(queue) { index, song ->
                    val isPlaying = currentSong?.uri == song.uri
                    QueueItem(
                        song = song,
                        isPlaying = isPlaying,
                        onClick = { playerViewModel.skipToIndex(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItem(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(if (isPlaying) com.mark1.mytubemusic.ui.theme.Tokens.glassTint else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = com.mark1.mytubemusic.ui.theme.MyTubeTypography.bodyMedium.copy(
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPlaying) com.mark1.mytubemusic.ui.theme.Tokens.accentPrimary else com.mark1.mytubemusic.ui.theme.Tokens.textPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = com.mark1.mytubemusic.ui.theme.MyTubeTypography.labelSmall.copy(color = com.mark1.mytubemusic.ui.theme.Tokens.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

