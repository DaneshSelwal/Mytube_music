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
     * Fetch direct streaming URLs using the InnerTube /player API.
     * Tries multiple clients — YouTube frequently blocks specific clients from
     * returning direct (non-ciphered) stream URLs.
     */
    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        // Ordered by current reliability for returning direct (non-ciphered) URLs
        data class ClientConfig(val name: String, val version: String, val userAgent: String, val clientId: String)
        val clients = listOf(
            ClientConfig("IOS", "19.29.1",
                "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X; en_US)", "5"),
            ClientConfig("ANDROID", "19.29.34",
                "com.google.android.youtube/19.29.34 (Linux; U; Android 14; en_US) gzip", "3"),
            ClientConfig("ANDROID_MUSIC", "7.27.52",
                "com.google.android.youtube.music/7.27.52 (Linux; U; Android 14; en_US) gzip", "21")
        )

        for (cfg in clients) {
            val result = tryFetchStreamUrl(videoId, cfg.name, cfg.version, cfg.userAgent, cfg.clientId)
            if (result != null) {
                android.util.Log.d("YouTubeExtractor", "[${cfg.name}] Resolved stream for $videoId")
                return@withContext result
            }
            android.util.Log.w("YouTubeExtractor", "[${cfg.name}] No direct URL, trying next client")
        }
        android.util.Log.e("YouTubeExtractor", "All clients failed for videoId=$videoId")
        null
    }

    private fun tryFetchStreamUrl(
        videoId: String, clientName: String, clientVersion: String,
        userAgent: String, clientId: String
    ): String? {
        val url = "https://www.youtube.com/youtubei/v1/player"
        val payload = JSONObject().apply {
            put("videoId", videoId)
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", clientName)
                    put("clientVersion", clientVersion)
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("playbackContext", JSONObject().apply {
                put("contentPlaybackContext", JSONObject().apply {
                    put("signatureTimestamp", 19950)
                })
            })
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }
        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("User-Agent", userAgent)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Youtube-Client-Name", clientId)
            .addHeader("X-Youtube-Client-Version", clientVersion)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.w("YouTubeExtractor", "HTTP ${response.code} from $clientName")
                    return null
                }
                val body = response.body?.string() ?: return null
                val root = JSONObject(body)

                // Check playability
                val status = root.optJSONObject("playabilityStatus")?.optString("status") ?: ""
                if (status == "UNPLAYABLE" || status == "LOGIN_REQUIRED" || status == "ERROR") {
                    android.util.Log.w("YouTubeExtractor", "$clientName: status=$status")
                    return null
                }

                val streamingData = root.optJSONObject("streamingData") ?: return null
                var bestUrl: String? = null
                var bestBitrate = 0

                // Check both adaptiveFormats (audio-only) and formats (muxed)
                listOfNotNull(
                    streamingData.optJSONArray("adaptiveFormats"),
                    streamingData.optJSONArray("formats")
                ).forEach { arr ->
                    for (i in 0 until arr.length()) {
                        val fmt = arr.optJSONObject(i) ?: continue
                        val mime = fmt.optString("mimeType", "")
                        if (!mime.startsWith("audio/")) continue

                        val directUrl = fmt.optString("url", "")
                        if (directUrl.isEmpty()) continue  // skip signature-ciphered

                        val bitrate = fmt.optInt("bitrate", 0)
                        // Boost AAC/mp4a priority — best Android ExoPlayer compat
                        val adjusted = if (mime.contains("mp4a")) bitrate + 1_000_000 else bitrate
                        if (adjusted > bestBitrate) {
                            bestBitrate = adjusted
                            bestUrl = directUrl
                        }
                    }
                }
                bestUrl
            }
        } catch (e: Exception) {
            android.util.Log.e("YouTubeExtractor", "$clientName exception: ${e.message}")
            null
        }
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

