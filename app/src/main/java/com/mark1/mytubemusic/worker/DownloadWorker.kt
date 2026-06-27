package com.mark1.mytubemusic.worker

import android.content.Context
import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mark1.mytubemusic.repository.OnlineSongRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoId = inputData.getString("videoId") ?: return@withContext Result.failure()
        val title = inputData.getString("title") ?: "Unknown Title"
        val artist = inputData.getString("artist") ?: "Unknown Artist"
        
        // Sanitize filenames
        val safeTitle = title.replace(Regex("[/:*?\"<>|]"), "")
        val safeArtist = artist.replace(Regex("[/:*?\"<>|]"), "")
        val fileName = "$safeTitle - $safeArtist.m4a"

        val repo = OnlineSongRepository()
        val streamUrl = repo.getStreamUrl(videoId) ?: return@withContext Result.failure()

        val client = OkHttpClient()
        val request = Request.Builder().url(streamUrl).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure()
                
                val body = response.body ?: return@withContext Result.failure()
                
                val resolver = applicationContext.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.TITLE, title)
                    put(MediaStore.Audio.Media.ARTIST, artist)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/MyTube")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                }
                
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure()
                
                resolver.openOutputStream(uri)?.use { outputStream ->
                    body.byteStream().copyTo(outputStream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            }

            val albumArtUri = inputData.getString("albumArtUri")
            var artDownloaded = false

            if (!albumArtUri.isNullOrEmpty()) {
                try {
                    val artRequest = Request.Builder().url(albumArtUri).build()
                    client.newCall(artRequest).execute().use { artResponse ->
                        if (artResponse.isSuccessful) {
                            val artBody = artResponse.body
                            if (artBody != null) {
                                val cacheFile = File(applicationContext.cacheDir, "art_${title.hashCode()}.jpg")
                                cacheFile.outputStream().use { outputStream ->
                                    artBody.byteStream().copyTo(outputStream)
                                }
                                artDownloaded = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (!artDownloaded) {
                com.mark1.mytubemusic.util.ArtworkScraper.fetchAndSaveAlbumArt(
                    "$title $artist",
                    applicationContext.cacheDir,
                    "art_${title.hashCode()}"
                )
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure()
        }
    }
}
