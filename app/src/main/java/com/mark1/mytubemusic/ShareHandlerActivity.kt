package com.mark1.mytubemusic

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mark1.mytubemusic.util.YouTubeUrlParser
import com.mark1.mytubemusic.worker.DownloadWorker

class ShareHandlerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish() // Terminate immediately to return control back to Brave Browser
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        val url = YouTubeUrlParser.extractUrl(sharedText)
        
        if (url == null || !YouTubeUrlParser.isYouTubeUrl(url)) {
            Toast.makeText(this, "Invalid YouTube link", Toast.LENGTH_SHORT).show()
            return
        }

        val videoId = YouTubeUrlParser.extractVideoId(url)
        if (videoId == null) {
            Toast.makeText(this, "Could not extract Video ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Configure network constraint for the download task
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workData = Data.Builder()
            .putString("videoId", videoId)
            // Leave title and artist null so that DownloadWorker resolves them dynamically
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workData)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(downloadRequest)
        Toast.makeText(this, "Download started in background...", Toast.LENGTH_SHORT).show()
    }
}
