package com.mark1.mytubemusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark1.mytubemusic.data.model.Song
import com.mark1.mytubemusic.viewmodel.PlayerViewModel

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
