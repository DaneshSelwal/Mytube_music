package com.mark1.mytubemusic.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.net.URLEncoder

object ArtworkScraper {
    private val client = OkHttpClient()

    suspend fun fetchAndSaveAlbumArt(query: String, cacheDir: File, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cachedFile = File(cacheDir, "$fileName.jpg")
                if (cachedFile.exists()) return@withContext true

                val encodedQuery = URLEncoder.encode("$query album art", "UTF-8")
                val url = "https://www.google.com/search?q=$encodedQuery&tbm=isch"

                // ⚡ Optimization: Replaced synchronous Jsoup.connect().get() with an asynchronous
                // OkHttp request using the custom await() extension. This prevents blocking a thread
                // in the Dispatchers.IO pool.
                val htmlRequest = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .build()

                val htmlResponse = client.newCall(htmlRequest).await()
                val htmlBody = htmlResponse.body?.string() ?: return@withContext false

                val doc = Jsoup.parse(htmlBody)

                // Find first image URL
                val imgElements = doc.select("img")
                var imageUrl: String? = null

                for (img in imgElements) {
                    val src = img.attr("data-src")
                    if (src.isNotEmpty() && src.startsWith("http")) {
                        imageUrl = src
                        break
                    }
                }

                if (imageUrl == null) return@withContext false

                val request = Request.Builder().url(imageUrl).build()

                // ⚡ Optimization: Replaced synchronous client.newCall(request).execute() with an
                // asynchronous OkHttp request using the custom await() extension.
                val response = client.newCall(request).await()

                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input ->
                        cachedFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
