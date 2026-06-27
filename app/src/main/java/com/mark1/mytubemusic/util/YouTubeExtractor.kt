package com.mark1.mytubemusic.util

import android.util.Log
import com.mark1.mytubemusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NpRequest
import org.schabi.newpipe.extractor.downloader.Response as NpResponse
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamExtractor

private const val TAG = "YouTubeExtractor"

/**
 * OkHttp-based Downloader bridge required by NewPipe Extractor.
 * NewPipe needs a Downloader to make HTTP requests — we plug in OkHttp 4.x.
 */
class OkHttpDownloader private constructor(private val client: OkHttpClient) : Downloader() {

    companion object {
        @Volatile private var instance: OkHttpDownloader? = null
        fun getInstance(): OkHttpDownloader = instance ?: synchronized(this) {
            instance ?: OkHttpDownloader(
                OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
            ).also { instance = it }
        }
    }

    override fun execute(request: NpRequest): NpResponse {
        val requestBuilder = Request.Builder().url(request.url())

        request.headers().forEach { (key, values) ->
            values.forEach { value -> requestBuilder.addHeader(key, value) }
        }

        val httpRequest = when (request.httpMethod()) {
            "POST" -> {
                val bodyBytes = request.dataToSend() ?: ByteArray(0)
                val body = bodyBytes.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                requestBuilder.post(body).build()
            }
            else -> requestBuilder.get().build()
        }

        val response: Response = client.newCall(httpRequest).execute()

        // OkHttp 4.x uses Kotlin property syntax instead of Java-style method calls
        val responseHeaders = mutableMapOf<String, MutableList<String>>()
        response.headers.names().forEach { name ->
            responseHeaders[name] = response.headers(name).toMutableList()
        }

        val body = response.body?.string() ?: ""
        return NpResponse(
            response.code,
            response.message,
            responseHeaders,
            body,
            response.request.url.toString()
        )
    }
}

/**
 * YouTube Music extractor backed by NewPipe Extractor library.
 * Handles all YouTube signature decryption, throttle bypass, and format
 * selection automatically. No manual InnerTube reverse-engineering needed.
 */
class YouTubeExtractor {

    init {
        try {
            NewPipe.init(OkHttpDownloader.getInstance())
        } catch (e: Exception) {
            Log.w(TAG, "NewPipe init: ${e.message}")
        }
    }

    /**
     * Search for songs on YouTube Music.
     * Uses the MUSIC_SONGS filter for music-specific results.
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        try {
            val ytService = ServiceList.YouTube
            val searchHandler = ytService.searchQHFactory
                .fromQuery(query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), "")
            val extractor: SearchExtractor = ytService.getSearchExtractor(searchHandler)
            extractor.fetchPage()

            for (item in extractor.initialPage.items) {
                try {
                    val stream = item as? org.schabi.newpipe.extractor.stream.StreamInfoItem ?: continue
                    val videoId = extractVideoId(stream.url) ?: continue
                    val thumbnailUrl = stream.thumbnails.lastOrNull()?.url
                        ?.let { getHighResThumbnail(it) }

                    songs.add(
                        Song(
                            uri = "online:$videoId",
                            title = stream.name,
                            artist = stream.uploaderName ?: "Unknown Artist",
                            album = "YouTube Music",
                            duration = stream.duration * 1000L,
                            albumArtUri = thumbnailUrl
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping item: ${e.message}")
                }
            }
            Log.d(TAG, "Search '$query' → ${songs.size} results")
        } catch (e: Exception) {
            Log.e(TAG, "Search failed: ${e.message}", e)
        }
        songs
    }

    /**
     * Get the best direct audio stream URL for a given YouTube video ID.
     * NewPipe handles all signature decryption automatically.
     */
    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val ytService = ServiceList.YouTube
            val linkHandler = ytService.streamLHFactory
                .fromUrl("https://www.youtube.com/watch?v=$videoId")
            val extractor: StreamExtractor = ytService.getStreamExtractor(linkHandler)
            extractor.fetchPage()

            val audioStreams: List<AudioStream> = extractor.audioStreams
            Log.d(TAG, "videoId=$videoId → ${audioStreams.size} audio streams")

            if (audioStreams.isEmpty()) {
                Log.w(TAG, "No audio streams for $videoId")
                return@withContext null
            }

            // Prefer M4A/AAC for best Android ExoPlayer compatibility
            val m4aStream = audioStreams
                .filter { it.format?.mimeType?.contains("mp4") == true && it.content.isNotEmpty() }
                .maxByOrNull { it.averageBitrate }

            val best = m4aStream
                ?: audioStreams.filter { it.content.isNotEmpty() }.maxByOrNull { it.averageBitrate }

            if (best == null) {
                Log.w(TAG, "All streams empty for $videoId")
                return@withContext null
            }

            Log.d(TAG, "Selected: ${best.format?.mimeType} @ ${best.averageBitrate}kbps")
            best.content
        } catch (e: Exception) {
            Log.e(TAG, "getStreamUrl failed for $videoId: ${e.message}", e)
            null
        }
    }

    private fun extractVideoId(url: String): String? = try {
        val uri = android.net.Uri.parse(url)
        uri.getQueryParameter("v") ?: uri.lastPathSegment?.takeIf { it.length == 11 }
    } catch (e: Exception) { null }

    private fun getHighResThumbnail(url: String?): String? {
        if (url == null) return null
        val index = url.indexOf("=")
        return if (index != -1) url.substring(0, index) + "=w512-h512" else url
    }
}
