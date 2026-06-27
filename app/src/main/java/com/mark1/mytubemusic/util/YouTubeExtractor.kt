package com.mark1.mytubemusic.util

import com.mark1.mytubemusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class YouTubeExtractor {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Standard public InnerTube API Key used by YouTube Music Web client
    private val innerTubeApiKey = "AIzaSyAo2cYncd-Kj4Qh0xJ491o5L9Nn4wHwY7I"

    /**
     * Search songs on YouTube Music directly using the InnerTube API.
     * Uses WEB_REMIX clientName and specific params to filter results to Songs only.
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://music.youtube.com/youtubei/v1/search?key=$innerTubeApiKey"
        
        // Base64-encoded protobuf filter parameters for "Songs" category
        val songsFilterParams = "Eg-KAQwIARAAGAAgACgAMABqChAEEAMQCRAFEAo="

        val payload = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240101.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("query", query)
            put("params", songsFilterParams)
        }

        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("Referer", "https://music.youtube.com/")
            .addHeader("Origin", "https://music.youtube.com")
            .build()

        val songs = mutableListOf<Song>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Search request failed: $response")

                val responseBody = response.body?.string() ?: return@use
                val root = JSONObject(responseBody)
                val contentsNode = root.optJSONObject("contents") ?: return@use
                
                var sectionContents: JSONArray? = null
                if (contentsNode.has("tabbedSearchResultsRenderer")) {
                    val tabs = contentsNode.getJSONObject("tabbedSearchResultsRenderer").optJSONArray("tabs")
                    if (tabs != null && tabs.length() > 0) {
                        val tabRenderer = tabs.getJSONObject(0).optJSONObject("tabRenderer")
                        val content = tabRenderer?.optJSONObject("content")
                        val sectionListRenderer = content?.optJSONObject("sectionListRenderer")
                        sectionContents = sectionListRenderer?.optJSONArray("contents")
                    }
                } else if (contentsNode.has("sectionListRenderer")) {
                    sectionContents = contentsNode.getJSONObject("sectionListRenderer").optJSONArray("contents")
                }

                if (sectionContents == null) return@use

                // Scan list of sections to find the musicShelfRenderer
                for (i in 0 until sectionContents.length()) {
                    val section = sectionContents.optJSONObject(i) ?: continue
                    val musicShelfRenderer = section.optJSONObject("musicShelfRenderer") ?: continue
                    val shelfItems = musicShelfRenderer.optJSONArray("contents") ?: continue

                    for (j in 0 until shelfItems.length()) {
                        val itemWrapper = shelfItems.optJSONObject(j) ?: continue
                        val item = itemWrapper.optJSONObject("musicResponsiveListItemRenderer") ?: continue

                        // 1. Video ID
                        var videoId: String? = null
                        val playlistItemData = item.optJSONObject("playlistItemData")
                        if (playlistItemData != null) {
                            videoId = playlistItemData.optString("videoId")
                        }
                        
                        if (videoId.isNullOrEmpty()) {
                            val overlay = item.optJSONObject("overlay")
                            val thumbnailOverlay = overlay?.optJSONObject("musicItemThumbnailOverlayRenderer")
                            val playButton = thumbnailOverlay?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                            val watchEndpoint = playButton?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")
                            videoId = watchEndpoint?.optString("videoId")
                        }

                        if (videoId.isNullOrEmpty()) continue

                        // 2. Flex columns containing title and runs
                        val flexColumns = item.optJSONArray("flexColumns") ?: continue
                        if (flexColumns.length() < 2) continue

                        // Title is in the first column
                        val col1Text = flexColumns.optJSONObject(0)
                            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.optJSONObject("text")
                        val title = col1Text?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "Unknown Title"

                        // Metadata runs (Artist, Album, Duration) are in the second column
                        val col2Text = flexColumns.optJSONObject(1)
                            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.optJSONObject("text")
                        val col2Runs = col2Text?.optJSONArray("runs")

                        var artist = "Unknown Artist"
                        var album = "YouTube Music"
                        var durationMs = 0L

                        if (col2Runs != null) {
                            val runTexts = mutableListOf<String>()
                            for (k in 0 until col2Runs.length()) {
                                val text = col2Runs.optJSONObject(k)?.optString("text")?.trim() ?: ""
                                if (text.isNotEmpty() && text != "•" && text != "·") {
                                    runTexts.add(text)
                                }
                            }

                            if (runTexts.isNotEmpty()) {
                                val lastText = runTexts.last()
                                // Check if duration matches standard pattern: mm:ss or hh:mm:ss
                                if (lastText.matches(Regex("\\d+:\\d+(:\\d+)?"))) {
                                    durationMs = parseDuration(lastText)
                                    if (runTexts.size > 1) {
                                        artist = runTexts[0]
                                    }
                                    if (runTexts.size > 2) {
                                        album = runTexts[1]
                                    }
                                } else {
                                    artist = runTexts[0]
                                    if (runTexts.size > 1) {
                                        album = runTexts[1]
                                    }
                                }
                            }
                        }

                        // 3. Album Art URL (extract high resolution)
                        val thumbnails = item.optJSONObject("thumbnail")
                            ?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail")
                            ?.optJSONArray("thumbnails")
                        var thumbnailUri: String? = null
                        if (thumbnails != null && thumbnails.length() > 0) {
                            val rawUrl = thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
                            thumbnailUri = getHighResThumbnail(rawUrl)
                        }

                        songs.add(
                            Song(
                                uri = "online:$videoId",
                                title = title,
                                artist = artist,
                                album = album,
                                duration = durationMs,
                                albumArtUri = thumbnailUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext songs
    }

    /**
     * Fetch direct unencrypted streaming URLs using the InnerTube /player API.
     * Setting the client to ANDROID_MUSIC or ANDROID avoids signature encryption ciphers,
     * returning the stream URL directly.
     */
    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val url = "https://www.youtube.com/youtubei/v1/player?key=$innerTubeApiKey"

        val payload = JSONObject().apply {
            put("videoId", videoId)
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", "6.05.51")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            // signatureTimestamp is sometimes helpful to match client signature cycles
            put("playbackContext", JSONObject().apply {
                put("contentPlaybackContext", JSONObject().apply {
                    put("signatureTimestamp", 19500)
                })
            })
        }

        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("User-Agent", "com.google.android.youtube.music/6.05.51 (Linux; U; Android 14; en_US)")
            .addHeader("X-Goog-Api-Format-Version", "2")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null

                val responseBody = response.body?.string() ?: return@use null
                val root = JSONObject(responseBody)
                val streamingData = root.optJSONObject("streamingData") ?: return@use null
                val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: return@use null

                var bestUrl: String? = null
                var bestBitrate = 0

                // Scan through adaptive streams (which contain isolated audio formats like M4A or WebM)
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.optJSONObject(i) ?: continue
                    val mimeType = format.optString("mimeType") ?: ""
                    
                    // We only want audio streams (preferably MP4/M4A, which are natively supported)
                    if (mimeType.startsWith("audio/")) {
                        val streamUrl = format.optString("url")
                        if (!streamUrl.isNullOrEmpty()) {
                            val bitrate = format.optInt("bitrate")
                            if (bitrate > bestBitrate) {
                                bestBitrate = bitrate
                                bestUrl = streamUrl
                            }
                        }
                    }
                }
                return@withContext bestUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun parseDuration(durationStr: String): Long {
        val parts = durationStr.split(":")
        var seconds = 0L
        for (part in parts) {
            seconds = seconds * 60 + (part.toLongOrNull() ?: 0L)
        }
        return seconds * 1000
    }

    private fun getHighResThumbnail(url: String?): String? {
        if (url == null) return null
        val index = url.indexOf("=")
        return if (index != -1) {
            url.substring(0, index) + "=w512-h512"
        } else {
            url
        }
    }
}
