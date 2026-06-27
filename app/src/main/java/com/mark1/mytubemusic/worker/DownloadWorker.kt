package com.mark1.mytubemusic.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.mark1.mytubemusic.repository.OnlineSongRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = id.hashCode()

    companion object {
        private const val CHANNEL_ID = "download_channel"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Song Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of downloading songs"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(progress: Int, title: String, artist: String): ForegroundInfo {
        createNotificationChannel()
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (progress < 100) "Downloading... $progress%" else "Download complete")
            .setSubText(artist)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(progress < 100)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun InputStream.copyToWithProgress(
        out: OutputStream,
        bufferSize: Int = 8192,
        totalBytes: Long,
        onProgress: (progress: Int) -> Unit
    ): Long {
        var bytesCopied: Long = 0
        val buffer = ByteArray(bufferSize)
        var bytes = read(buffer)
        var lastProgress = 0
        onProgress(0)
        while (bytes >= 0) {
            if (this@DownloadWorker.isStopped) {
                throw CancellationException("Download worker stopped.")
            }
            out.write(buffer, 0, bytes)
            bytesCopied += bytes
            if (totalBytes > 0) {
                val progress = ((bytesCopied * 100) / totalBytes).toInt()
                if (progress != lastProgress) {
                    onProgress(progress)
                    lastProgress = progress
                }
            }
            bytes = read(buffer)
        }
        return bytesCopied
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoId = inputData.getString("videoId") ?: return@withContext Result.failure()
        val title = inputData.getString("title")
        val artist = inputData.getString("artist")
        var albumArtUri = inputData.getString("albumArtUri")

        // Inform user we are preparing/resolving
        setForeground(createForegroundInfo(0, title ?: "Song Download", artist ?: "Resolving details..."))

        val repo = OnlineSongRepository()

        // Dynamic Metadata Resolution if not supplied
        var resolvedTitle = title
        var resolvedArtist = artist
        if (resolvedTitle == null || resolvedArtist == null) {
            val songDetails = repo.getSongMetadata(videoId)
            resolvedTitle = songDetails?.title ?: "YouTube Audio ($videoId)"
            resolvedArtist = songDetails?.artist ?: "Unknown Artist"
            if (albumArtUri == null) {
                albumArtUri = songDetails?.albumArtUri
            }
        }

        val finalTitle = resolvedTitle
        val finalArtist = resolvedArtist

        // Update notification with resolved title & artist
        setForeground(createForegroundInfo(0, finalTitle, finalArtist))

        val safeTitle = finalTitle.replace(Regex("[/:*?\"<>|]"), "")
        val safeArtist = finalArtist.replace(Regex("[/:*?\"<>|]"), "")
        val fileName = "$safeTitle - $safeArtist.m4a"

        val streamUrl = repo.getStreamUrl(videoId) ?: run {
            showFailedNotification(finalTitle, finalArtist)
            return@withContext Result.failure()
        }

        val client = OkHttpClient()
        val request = Request.Builder().url(streamUrl).build()
        var downloadUri: android.net.Uri? = null

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    showFailedNotification(finalTitle, finalArtist)
                    return@withContext Result.failure()
                }
                val body = response.body ?: run {
                    showFailedNotification(finalTitle, finalArtist)
                    return@withContext Result.failure()
                }
                val totalBytes = body.contentLength()

                val resolver = applicationContext.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.TITLE, finalTitle)
                    put(MediaStore.Audio.Media.ARTIST, finalArtist)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/MyTube")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                }
                
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: run {
                        showFailedNotification(finalTitle, finalArtist)
                        return@withContext Result.failure()
                    }
                downloadUri = uri

                // Copy stream chunk-by-chunk and update progress
                resolver.openOutputStream(uri)?.use { outputStream ->
                    body.byteStream().copyToWithProgress(outputStream, totalBytes = totalBytes) { progress ->
                        // Update notification directly via manager to bypass WorkManager IPC overhead
                        notificationManager.notify(notificationId, createForegroundInfo(progress, finalTitle, finalArtist).notification)
                    }
                } ?: run {
                    showFailedNotification(finalTitle, finalArtist)
                    return@withContext Result.failure()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            }
            
            // Handle Album Art download
            var artDownloaded = false
            if (!albumArtUri.isNullOrEmpty()) {
                try {
                    val artRequest = Request.Builder().url(albumArtUri).build()
                    client.newCall(artRequest).execute().use { artResponse ->
                        if (artResponse.isSuccessful) {
                            val artBody = artResponse.body
                            if (artBody != null) {
                                val cacheFile = File(applicationContext.cacheDir, "art_${(finalTitle + "_" + finalArtist).hashCode()}.jpg")
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
                try {
                    com.mark1.mytubemusic.util.ArtworkScraper.fetchAndSaveAlbumArt(
                        "$finalTitle $finalArtist",
                        applicationContext.cacheDir,
                        "art_${(finalTitle + "_" + finalArtist).hashCode()}"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Final notification update
            val successNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(finalTitle)
                .setContentText("Download complete")
                .setSubText(finalArtist)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(false)
                .build()
            notificationManager.notify(notificationId, successNotification)

            return@withContext Result.success()
        } catch (e: CancellationException) {
            downloadUri?.let { uri ->
                try {
                    applicationContext.contentResolver.delete(uri, null, null)
                } catch (deleteEx: Exception) {
                    deleteEx.printStackTrace()
                }
            }
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            downloadUri?.let { uri ->
                try {
                    applicationContext.contentResolver.delete(uri, null, null)
                } catch (deleteEx: Exception) {
                    deleteEx.printStackTrace()
                }
            }
            showFailedNotification(finalTitle, finalArtist)
            return@withContext Result.failure()
        }
    }

    private fun showFailedNotification(title: String, artist: String) {
        val failNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Download failed")
            .setSubText(artist)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setOngoing(false)
            .build()
        notificationManager.notify(notificationId, failNotification)
    }
}
