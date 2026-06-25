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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.ui.graphics.Brush

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
    
    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(300.dp)
            .shadow(32.dp, RoundedCornerShape(32.dp), ambientColor = Tokens.accentPrimary, spotColor = Tokens.accentPrimary.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(24.dp))
            .background(Tokens.bgElevated.copy(alpha = 0.95f))
            .border(1.dp, Tokens.strokeSubtle, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -40f) onClick()
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImage(
                model = song.uri,
                contentDescription = null,
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .border(1.dp, Tokens.strokeSubtle, CircleShape)
                    .sharedElement(
                        state = rememberSharedContentState(key = "cd_art"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> tween(400) }
                    )
                    .background(Tokens.bgSurface),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground),
                placeholder = painterResource(R.drawable.ic_launcher_foreground)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(song.title, style = MyTubeTypography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Tokens.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MyTubeTypography.labelSmall.copy(color = Tokens.textSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPrev()
                }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, tint = Tokens.textSecondary, modifier = Modifier.size(48.dp), contentDescription = "Previous")
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPause()
                }, modifier = Modifier.size(72.dp)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        tint = Tokens.accentPrimary,
                        modifier = Modifier.size(64.dp),
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNext()
                }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, tint = Tokens.textSecondary, modifier = Modifier.size(48.dp), contentDescription = "Next")
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progressRatio)
                .height(6.dp)
                .background(Brush.horizontalGradient(listOf(Tokens.accentPrimary, Tokens.accentSecondary)))
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Tokens.accentPrimary.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Now Playing",
                style = MyTubeTypography.labelSmall.copy(
                    color = Tokens.bgDeep,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }
    }
}
