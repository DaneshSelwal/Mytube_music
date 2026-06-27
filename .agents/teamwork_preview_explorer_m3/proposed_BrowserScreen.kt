package com.mark1.mytubemusic.ui.screens

import android.content.Context
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mark1.mytubemusic.repository.OnlineSongRepository
import com.mark1.mytubemusic.ui.theme.Tokens
import com.mark1.mytubemusic.util.YouTubeUrlParser
import com.mark1.mytubemusic.viewmodel.PlayerViewModel
import com.mark1.mytubemusic.worker.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Custom WebView subclass that overrides window visibility changes.
 * This prevents the WebView from automatically pausing its HTML5 video/audio players
 * when the screen is turned off or the app is minimized.
 */
class PlaybackWebView(context: Context) : WebView(context) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        // Force the WebView to think it is always visible.
        // This keeps JS and audio playback active in the background.
        if (visibility == View.VISIBLE) {
            super.onWindowVisibilityChanged(visibility)
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == View.VISIBLE) {
            super.onVisibilityChanged(changedView)
        }
    }
}

@Composable
fun BrowserScreen(
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUrl by remember { mutableStateOf("https://music.youtube.com") }
    var isResolving by remember { mutableStateOf(false) }

    // Parse the video ID from the current URL
    val videoId = remember(currentUrl) {
        YouTubeUrlParser.extractVideoId(currentUrl)
    }
    val isDownloadable = videoId != null

    // Pause the app's player when we navigate to or load the Browser screen.
    LaunchedEffect(Unit) {
        playerViewModel.player?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlaybackWebView(ctx).apply {
                    // Enable cookies
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(this@apply, true)
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        
                        // User Agent configuration:
                        // Use a tablet user agent (e.g. iPad) to get a responsive, touch-friendly mobile layout
                        // that does not force the "Open in app" mobile redirect/banners.
                        userAgentString = "Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            url?.let {
                                currentUrl = it
                            }
                        }
                    }

                    webChromeClient = WebChromeClient()
                    loadUrl(currentUrl)
                }
            },
            update = { /* WebView handles internally */ },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Action Download Button
        AnimatedVisibility(
            visible = isDownloadable,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 24.dp) // Adjusted offset to clear the bottom MiniPlayer
        ) {
            FloatingActionButton(
                onClick = {
                    if (videoId != null && !isResolving) {
                        isResolving = true
                        Toast.makeText(context, "Resolving metadata...", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            try {
                                val repository = OnlineSongRepository()
                                // Resolve metadata from YouTube Music API dynamically before download
                                val songMetadata = withContext(Dispatchers.IO) {
                                    repository.getSongMetadata(videoId)
                                }

                                val title = songMetadata?.title ?: "YouTube Audio ($videoId)"
                                val artist = songMetadata?.artist ?: "Unknown Artist"
                                val albumArt = songMetadata?.albumArtUri

                                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                                    .setInputData(
                                        Data.Builder()
                                            .putString("videoId", videoId)
                                            .putString("title", title)
                                            .putString("artist", artist)
                                            .putString("albumArtUri", albumArt)
                                            .build()
                                    )
                                    .build()

                                WorkManager.getInstance(context).enqueue(workRequest)
                                Toast.makeText(context, "Enqueued download: $title", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Download initiation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isResolving = false
                            }
                        }
                    }
                },
                containerColor = Tokens.accentPrimary,
                contentColor = Tokens.bgDeep
            ) {
                if (isResolving) {
                    CircularProgressIndicator(
                        color = Tokens.bgDeep,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Video as M4A Audio"
                    )
                }
            }
        }
    }
}
