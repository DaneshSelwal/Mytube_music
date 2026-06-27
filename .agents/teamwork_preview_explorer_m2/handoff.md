# Handoff Report: Milestone 2 — Share Sheet / Download Workflow Analysis

## Observation

1. **MainActivity Configuration**:
   In `app/src/main/AndroidManifest.xml` (lines 23-31), only `MainActivity` is defined under the `<application>` tag, and it only handles the standard `MAIN` action:
   ```xml
   <activity
       android:name=".MainActivity"
       android:exported="true"
       android:theme="@style/Theme.MyTubeMusic">
       <intent-filter>
           <action android:name="android.intent.action.MAIN" />
           <category android:name="android.intent.category.LAUNCHER" />
       </intent-filter>
   </activity>
   ```
   No intent filter for `android.intent.action.SEND` or mimeType `"text/plain"` exists.

2. **DownloadWorker Implementation**:
   In `app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt` (lines 18-109):
   - It retrieves inputs via `inputData.getString("videoId")`, `"title"`, and `"artist"` (lines 24-26).
   - The file stream is copied to `MediaStore` via `body.byteStream().copyTo(outputStream)` (lines 60-62). This method runs in a single-shot fashion and lacks progress tracking.
   - It does not contain any calls to `setForeground()`, `setForegroundInfo()`, or `NotificationCompat` builders, meaning it runs as a pure background worker without user-facing progress notifications.

3. **YouTube Extraction**:
   In `app/src/main/java/com/mark1/mytubemusic/util/YouTubeExtractor.kt`:
   - It uses `org.schabi.newpipe.extractor.stream.StreamExtractor` (lines 191-192) to fetch video pages and get stream metadata.
   - The method `extractVideoId` is private (lines 223-237).
   - No public method exists to query video/song metadata (Title, Artist, Duration, Album Art) for a single ID without requesting the stream URL.

4. **SDK and Build Targets**:
   In `app/build.gradle.kts` (lines 9-14):
   ```kotlin
   compileSdk = 34
   defaultConfig {
       minSdk = 26
       targetSdk = 34
   }
   ```
   Targeting API 34 (Android 14) means running foreground services or foreground workers requires explicit permission declarations (like `FOREGROUND_SERVICE_DATA_SYNC`) and explicit service type declarations in the manifest.

5. **WorkManager Triggering**:
   In `app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt` (lines 947-967), the app enqueues the worker using:
   ```kotlin
   val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
       .setInputData(
           Data.Builder()
               .putString("videoId", videoId)
               .putString("title", song.title)
               .putString("artist", song.artist)
               .putString("albumArtUri", song.albumArtUri)
               .build()
       )
       .build()
   WorkManager.getInstance(context).enqueue(workRequest)
   ```

---

## Logic Chain

1. **Share Intent Receiver Choice**:
   - **Targeting MainActivity**: If we add the `SEND` intent filter to `MainActivity`, processing the shared URL will force the entire Compose UI hierarchy, NavHost, ExoPlayer, and repositories to initialize. If the app is closed, it pops up on the screen, pulling the user away from their web browser.
   - **Targeting a Dedicated Activity (`ShareHandlerActivity`)**: We can register a lightweight translucent activity that handles the intent, extracts and validates the URL, enqueues the download in WorkManager, shows a Toast, and finishes immediately. This allows a seamless, non-disruptive experience where the user remains in the Brave Browser, and the download runs in the background.
   - *Therefore*, a dedicated `ShareHandlerActivity` with `Theme.Translucent.NoTitleBar` is the superior implementation.

2. **URL Extraction and Parsing**:
   - Shared text from browser share sheets can contain arbitrary text alongside the URL (e.g. `"Check out this song: https://music.youtube.com/watch?v=dQw4w9WgXcQ"`).
   - We must first extract the URL using a standard `http(s)` pattern, verify it points to a YouTube domain (e.g., `youtube.com`, `youtu.be`, `music.youtube.com`), and then extract the 11-character video ID using query string or path segment matching.

3. **Metadata Resolution**:
   - NewPipe's `StreamExtractor` already populates fields such as `name`, `uploaderName`, `length`, and `thumbnails` during `fetchPage()`.
   - We can expose a public `getSongMetadata(videoId: String): Song?` in `YouTubeExtractor` and `OnlineSongRepository` that queries the video page and returns a `Song` data class populated with these fields.
   - To make the share workflow extremely fast, we can enqueue `DownloadWorker` with only the `videoId` from `ShareHandlerActivity`. The worker itself can then resolve the metadata in the background via `getSongMetadata` before initiating the download stream. This decouples network operations from the activity lifecycle.

4. **Background Progress Notifications (API 34 Foreground Compliance)**:
   - Since `targetSdk = 34`, running `setForeground()` inside a worker requires:
     1. Declaring `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />` in the manifest.
     2. Modifying WorkManager's `SystemForegroundService` registration in the manifest to attach `android:foregroundServiceType="dataSync"`.
   - To calculate progress, we cannot use `InputStream.copyTo(OutputStream)`. We must implement a custom copy loop that reads the stream in chunks (e.g. 4KB-8KB), writes to the destination, and calls `NotificationManager.notify()` with updated percentages calculated from `response.body.contentLength()`.

---

## Caveats

- **Network Environment**: Since we are in `CODE_ONLY` network mode, we cannot verify live URL extractions against YouTube's production servers. However, mock tests and regex pattern verifications are fully applicable.
- **POST_NOTIFICATIONS Permission**: Starting in Android 13, apps must request runtime permission for notifications (`Manifest.permission.POST_NOTIFICATIONS`). If not granted, the progress notification will not appear, although the worker will still run. The app must prompt for this permission.
- **Duplicate Downloads**: If a user shares the same URL multiple times, we should check if the file already exists or use `ExistingWorkPolicy.KEEP` / `ExistingWorkPolicy.REPLACE` to prevent redundant background tasks.

---

## Conclusion & Proposed Implementation

We recommend the following structured plan to implement the Share Sheet and Download workflow.

### A. Manifest and Permissions Changes (`AndroidManifest.xml`)

Add the required permissions and register the dedicated `ShareHandlerActivity` and WorkManager foreground service type:

```xml
<!-- Required for Android 14+ Foreground Worker -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<application ...>

    <!-- Dedicated translucent Share Sheet handler activity -->
    <activity
        android:name=".ShareHandlerActivity"
        android:exported="true"
        android:theme="@android:style/Theme.Translucent.NoTitleBar">
        <intent-filter>
            <action android:name="android.intent.action.SEND" />
            <category android:name="android.intent.category.DEFAULT" />
            <data android:mimeType="text/plain" />
        </intent-filter>
    </activity>

    <!-- Configure WorkManager's foreground service with dataSync type -->
    <service
        android:name="androidx.work.impl.foreground.SystemForegroundService"
        android:foregroundServiceType="dataSync"
        tools:node="merge" />

</application>
```

### B. YouTube URL Parsing Utility (`YouTubeUrlParser.kt`)

Create a robust parsing utility under `util/` to validate URLs and extract video IDs:

```kotlin
package com.mark1.mytubemusic.util

object YouTubeUrlParser {
    private val urlPattern = Regex("""https?://[^\s]+""")
    private val youtubeHostPattern = Regex("""https?://(?:www\.|m\.|music\.)?(?:youtube\.com|youtu\.be)/.*""")

    fun extractUrl(text: String): String? {
        return urlPattern.find(text)?.value
    }

    fun isYouTubeUrl(url: String): Boolean {
        return youtubeHostPattern.matches(url)
    }

    fun extractVideoId(url: String): String? {
        // 1. Short links (youtu.be/ID)
        if (url.contains("youtu.be/")) {
            val path = url.substringAfter("youtu.be/").substringBefore("?").substringBefore("#")
            if (path.length == 11) return path
        }
        
        // 2. Query param (v=ID)
        val queryRegex = Regex("[?&]v=([a-zA-Z0-9_-]{11})")
        val matchQuery = queryRegex.find(url)
        if (matchQuery != null) {
            return matchQuery.groupValues[1]
        }
        
        // 3. Path patterns (/embed/ID, /shorts/ID)
        val pathRegex = Regex("/(?:embed|shorts|v)/([a-zA-Z0-9_-]{11})")
        val matchPath = pathRegex.find(url)
        if (matchPath != null) {
            return matchPath.groupValues[1]
        }
        
        return null
    }
}
```

### C. Expose Metadata Resolution (`YouTubeExtractor.kt` & `OnlineSongRepository.kt`)

Add `getSongMetadata` to `YouTubeExtractor.kt`:

```kotlin
    suspend fun getSongMetadata(videoId: String): Song? = withContext(Dispatchers.IO) {
        try {
            val ytService = ServiceList.YouTube
            val linkHandler = ytService.streamLHFactory
                .fromUrl("https://www.youtube.com/watch?v=$videoId")
            val extractor: StreamExtractor = ytService.getStreamExtractor(linkHandler)
            extractor.fetchPage()

            val thumbnailUrl = extractor.thumbnails.lastOrNull()?.url
                ?.let { getHighResThumbnail(it) }

            Song(
                uri = "online:$videoId",
                title = extractor.name ?: "Unknown Title",
                artist = extractor.uploaderName ?: "Unknown Artist",
                album = "YouTube Music",
                duration = extractor.length * 1000L,
                albumArtUri = thumbnailUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "getSongMetadata failed for $videoId: ${e.message}", e)
            null
        }
    }
```
*And expose it in `OnlineSongRepository.kt`*:
```kotlin
    suspend fun getSongMetadata(videoId: String): Song? = youtubeExtractor.getSongMetadata(videoId)
```

### D. Dedicated Share Handler Activity (`ShareHandlerActivity.kt`)

```kotlin
package com.mark1.mytubemusic

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
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
```

### E. Download Progress Notifications (`DownloadWorker.kt`)

Update `DownloadWorker` to support progress tracking, notification updates, metadata resolution, and API 34 foreground execution:

1. **Buffered Copying with Progress Callback**:
   ```kotlin
   private fun java.io.InputStream.copyToWithProgress(
       out: java.io.OutputStream,
       bufferSize: Int = DEFAULT_BUFFER_SIZE,
       totalBytes: Long,
       onProgress: (progress: Int) -> Unit
   ): Long {
       var bytesCopied: Long = 0
       val buffer = ByteArray(bufferSize)
       var bytes = read(buffer)
       var lastProgress = 0
       while (bytes >= 0) {
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
   ```

2. **Foreground Support & Progress Channel Setup**:
   ```kotlin
   private val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

   companion object {
       private const val NOTIFICATION_ID = 404
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
       
       val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, CHANNEL_ID)
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
               NOTIFICATION_ID, 
               notification, 
               android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
           )
       } else {
           ForegroundInfo(NOTIFICATION_ID, notification)
       }
   }
   ```

3. **Dynamic Resolution & Progress Updates in `doWork`**:
   ```kotlin
   override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
       val videoId = inputData.getString("videoId") ?: return@withContext Result.failure()
       var title = inputData.getString("title")
       var artist = inputData.getString("artist")
       
       // Inform user we are preparing/resolving
       setForeground(createForegroundInfo(0, title ?: "Song Download", artist ?: "Resolving details..."))
       
       val repo = OnlineSongRepository()
       
       // Dynamic Metadata Resolution if not supplied
       if (title == null || artist == null) {
           val songDetails = repo.getSongMetadata(videoId)
           title = songDetails?.title ?: "YouTube Audio ($videoId)"
           artist = songDetails?.artist ?: "Unknown Artist"
       }
       
       // Update notification with resolved title & artist
       setForeground(createForegroundInfo(0, title, artist))

       val safeTitle = title.replace(Regex("[/:*?\"<>|]"), "")
       val safeArtist = artist.replace(Regex("[/:*?\"<>|]"), "")
       val fileName = "$safeTitle - $safeArtist.m4a"

       val streamUrl = repo.getStreamUrl(videoId) ?: return@withContext Result.failure()
       val client = OkHttpClient()
       val request = Request.Builder().url(streamUrl).build()

       try {
           client.newCall(request).execute().use { response ->
               if (!response.isSuccessful) return@withContext Result.failure()
               val body = response.body ?: return@withContext Result.failure()
               val totalBytes = body.contentLength()

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

               // Copy stream chunk-by-chunk and update progress
               resolver.openOutputStream(uri)?.use { outputStream ->
                   body.byteStream().copyToWithProgress(outputStream, totalBytes = totalBytes) { progress ->
                       // Update notification directly via manager to bypass WorkManager IPC overhead
                       notificationManager.notify(NOTIFICATION_ID, createForegroundInfo(progress, title, artist).notification)
                   }
               }

               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                   contentValues.clear()
                   contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                   resolver.update(uri, contentValues, null, null)
               }
           }
           
           // Handle Album Art download...
           // [Keep existing albumArtUri cache logic here]
           
           // Final notification update
           val successNotification = androidx.core.app.NotificationCompat.Builder(applicationContext, CHANNEL_ID)
               .setContentTitle(title)
               .setContentText("Download complete")
               .setSubText(artist)
               .setSmallIcon(android.R.drawable.stat_sys_download_done)
               .setOngoing(false)
               .build()
           notificationManager.notify(NOTIFICATION_ID, successNotification)

           return@withContext Result.success()
       } catch (e: Exception) {
           e.printStackTrace()
           val failNotification = androidx.core.app.NotificationCompat.Builder(applicationContext, CHANNEL_ID)
               .setContentTitle(title)
               .setContentText("Download failed")
               .setSubText(artist)
               .setSmallIcon(android.R.drawable.stat_notify_error)
               .setOngoing(false)
               .build()
           notificationManager.notify(NOTIFICATION_ID, failNotification)
           return@withContext Result.failure()
       }
   }
   ```

---

## Verification Method

1. **Compilation Check**:
   Run Gradle build to ensure there are no compilation errors with new dependencies or references:
   ```cmd
   ./gradlew compileDebugSources
   ```

2. **Run Unit Tests**:
   Run the extractor tests to verify that `YouTubeExtractor` initializes properly:
   ```cmd
   ./gradlew test
   ```

3. **Runtime Intent Verification (via ADB)**:
   Once implemented, verify the share receiver activity by sending a mock share intent via ADB command lines:
   - Send standard Watch link:
     ```cmd
     adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
     ```
   - Send YouTube Music share string:
     ```cmd
     adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "Check out this song! https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVMdQw4w9WgXcQ"
     ```
   - Send youtu.be short link:
     ```cmd
     adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "https://youtu.be/dQw4w9WgXcQ"
     ```
   Verify that:
   - The translucent intent receiver intercepts the intent.
   - The app does not display a full UI screen, but instead shows a Toast message "Download started in background...".
   - A low-priority notification appears in the drawer detailing the song's title, artist, and a progress bar climbing from 0% to 100%.
   - The final audio file is successfully registered in `MediaStore` under the `Music/MyTube` directory.
