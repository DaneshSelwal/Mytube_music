package com.mark1.mytubemusic.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mark1.mytubemusic.R
import com.mark1.mytubemusic.data.model.Song
import com.mark1.mytubemusic.ui.theme.MyTubeTypography
import com.mark1.mytubemusic.ui.theme.Tokens

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
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var artBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(song.uri) {
        artBitmap = null
        if (song.albumArtUri != null) return@LaunchedEffect
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = Tokens.accentPrimary.copy(alpha = 0.3f), spotColor = Tokens.accentPrimary.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(16.dp))
            .background(Tokens.bgElevated.copy(alpha = 0.95f))
            .border(1.dp, Tokens.strokeSubtle, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Tokens.bgSurface)
                    .sharedElement(
                        state = rememberSharedContentState(key = "cd_art"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> tween(400) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_foreground),
                        placeholder = painterResource(R.drawable.ic_launcher_foreground)
                    )
                } else if (artBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = artBitmap!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Tokens.textDisabled
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = song.title, 
                    style = MyTubeTypography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Tokens.textPrimary), 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist, 
                    style = MyTubeTypography.labelSmall.copy(color = Tokens.textSecondary), 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPrev()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, tint = Tokens.textSecondary, modifier = Modifier.size(24.dp), contentDescription = "Previous")
                }
                
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayPause()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        tint = Tokens.accentPrimary,
                        modifier = Modifier.size(32.dp),
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNext()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.SkipNext, tint = Tokens.textSecondary, modifier = Modifier.size(24.dp), contentDescription = "Next")
                }
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progressRatio)
                .height(4.dp)
                .background(Brush.horizontalGradient(listOf(Tokens.accentPrimary, Tokens.accentSecondary)))
        )
    }
}
