package com.mark1.mytubemusic.repository

import com.mark1.mytubemusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class OnlineSongRepository {
    private val client = OkHttpClient()
    // We use a public Piped instance. You can switch to another if this goes down.
    private val baseUrl = "https://pipedapi.kavin.rocks"

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?q=${query.replace(" ", "+")}&filter=music_songs"
        val request = Request.Builder().url(url).build()

        val songs = mutableListOf<Song>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val body = response.body?.string() ?: return@use
                val json = JSONObject(body)
                val items = json.optJSONArray("items") ?: return@use

                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i)
                    if (item != null) {
                        val videoId = item.optString("url").removePrefix("/watch?v=")
                        val title = item.optString("title")
                        val uploaderName = item.optString("uploaderName")
                        val duration = item.optLong("duration") * 1000 // Convert to ms
                        val thumbnail = item.optString("thumbnail")

                        // We store the videoId in the URI field temporarily prefixed with "online:"
                        songs.add(
                            Song(
                                uri = "online:$videoId",
                                title = title,
                                artist = uploaderName,
                                album = "YouTube Music",
                                duration = duration,
                                albumArtUri = thumbnail
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

    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val url = "$baseUrl/streams/$videoId"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null

                val body = response.body?.string() ?: return@use
                val json = JSONObject(body)
                val audioStreams = json.optJSONArray("audioStreams") ?: return@use

                var bestUrl: String? = null
                var bestBitrate = 0

                // Find the best quality m4a or webm audio stream
                for (i in 0 until audioStreams.length()) {
                    val stream = audioStreams.optJSONObject(i)
                    if (stream != null) {
                        val bitrate = stream.optInt("bitrate")
                        val streamUrl = stream.optString("url")
                        
                        if (bitrate > bestBitrate) {
                            bestBitrate = bitrate
                            bestUrl = streamUrl
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
}
