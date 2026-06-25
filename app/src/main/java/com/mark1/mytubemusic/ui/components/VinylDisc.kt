package com.mark1.mytubemusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun VinylDisc(modifier: Modifier = Modifier, isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(32.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .rotate(if (isPlaying) rotation else 0f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            val radius = size.minDimension / 2
            
            // Draw vinyl grooves
            for (i in 1..8) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = radius - (i * 10.dp.toPx()),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            
            // Draw center label placeholder
            drawCircle(
                color = Color.DarkGray,
                radius = radius * 0.3f,
                center = center
            )
            
            // Draw spindle hole
            drawCircle(
                color = Color.Black,
                radius = radius * 0.05f,
                center = center
            )
        }
    }
}
