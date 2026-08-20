package com.mark1.mytubemusic.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.mark1.mytubemusic.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.room.Room

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Initialize Python if not already initialized
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(applicationContext))
            }

            val py = Python.getInstance()
            val sys = py.getModule("sys")
            val ytDlp = py.getModule("yt_dlp")

            val database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "mytube_music_db"
            ).fallbackToDestructiveMigration().build()
            
            val songDao = database.songDao()
            val pendingSongs = songDao.getPendingSongs().take(10) // Limit to 10 per run
            
            if (pendingSongs.isEmpty()) {
                return@withContext Result.success()
            }

            val musicDir = File(applicationContext.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), "MyTubeDownloads")
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            val downloaderModule = py.getModule("downloader")
            
            for (song in pendingSongs) {
                songDao.updateDownloadState(song.uri, "DOWNLOADING")
                
                try {
                    val query = "${song.title} ${song.artist}"
                    // Calls the python download_song function
                    val success = downloaderModule.callAttr("download_song", query, musicDir.absolutePath).toBoolean()
                    
                    if (success) {
                        // Find the downloaded file
                        val downloadedFile = musicDir.listFiles()?.find { it.name.contains(song.title) && it.extension == "mp3" }
                        if (downloadedFile != null) {
                            songDao.completeDownload(song.uri, downloadedFile.absolutePath)
                        } else {
                            // Couldn't find the file, revert to pending
                            songDao.updateDownloadState(song.uri, "PENDING")
                        }
                    } else {
                        songDao.updateDownloadState(song.uri, "PENDING")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    songDao.updateDownloadState(song.uri, "PENDING")
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
