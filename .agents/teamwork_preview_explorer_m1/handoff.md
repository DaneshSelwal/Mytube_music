# Handoff Report: Playback & Extractor Fix Investigation

## 1. Observation
We observed the following files, stack traces, and code segments in `C:\Users\selwa\Desktop\Music App\MyTubeMusic`:

1. **Unit Test Failure details**:
   From `app/build/test-results/testDebugUnitTest/TEST-com.mark1.mytubemusic.YouTubeExtractorTest.xml` line 7:
   > `Search failed with exception: Method e in android.util.Log not mocked. See https://developer.android.com/r/studio-ui/build/not-mocked for details.`
   
   This error is caused because `android.util.Log` is called during execution, which throws a stub exception in JUnit JVM test environments.

2. **YouTubeExtractor's dependency on Android SDK**:
   From `app/src/main/java/com/mark1/mytubemusic/util/YouTubeExtractor.kt`:
   - Line 3: `import android.util.Log`
   - Line 175-177: 
     ```kotlin
     private fun extractVideoId(url: String): String? = try {
         val uri = android.net.Uri.parse(url)
         uri.getQueryParameter("v") ?: uri.lastPathSegment?.takeIf { it.length == 11 }
     } catch (e: Exception) { null }
     ```
   
   Both `Log` and `Uri` are Android SDK classes which are not mocked under plain JRE/JVM.

3. **Eager Queue Resolution in PlayerViewModel**:
   From `app/src/main/java/com/mark1/mytubemusic/viewmodel/PlayerViewModel.kt` lines 270-276:
   ```kotlin
   // Step 2: Lazily resolve the rest of the queue one-by-one in background
   val remaining = songs.filterIndexed { i, _ -> i != startIndex }
   for (song in remaining) {
       val item = buildMediaItem(song) ?: continue
       p.addMediaItem(item)
   }
   ```
   This loops through the remaining songs in the queue and resolves the HTTP stream URLs for all of them using blocking network requests inside `buildMediaItem()` (via `onlineRepository.getStreamUrl(videoId)` on `Dispatchers.IO`).

---

## 2. Logic Chain

### Why the Unit Test Fails:
1. `YouTubeExtractorTest.kt` runs the `testSearchAndGetStreamUrl` test on a standard JVM (no emulator/device context).
2. The test calls `extractor.searchSongs(query)`.
3. Inside `searchSongs()`, the code loops over the results and attempts to parse each URL using `android.net.Uri.parse(url)` to get the `videoId` (lines 107-108).
4. Since `android.net.Uri` is an Android framework class, it throws `RuntimeException("Method parse in android.net.Uri not mocked")` in the JVM environment.
5. The local `try-catch` inside `extractVideoId` catches this exception and returns `null`. This results in the loop skipping all search results (`?: continue` at line 108).
6. Consequently, the `songs` list remains empty.
7. The method then tries to log the results: `Log.d(TAG, "Search '$query' → ${songs.size} results")`.
8. `Log.d` is also not mocked, throwing another `RuntimeException("Method d in android.util.Log not mocked")`.
9. The outer `try-catch` inside `searchSongs` catches this exception and calls `Log.e(TAG, "Search failed: ...", e)`.
10. `Log.e` also throws `Method e in android.util.Log not mocked`, which bubbles up, gets caught by the test's outer `try-catch` block, prints `"Search failed with exception: Method e in android.util.Log not mocked"`, and causes the test to complete with an empty list (concluding with a warning and passing falsely/gracefully instead of performing assertions).

### Why Online Playback Crashes/Fails:
1. **URL Expiration**: YouTube stream URLs resolved via NewPipe are signed and have short lifetimes (often expiring in under 1-5 hours). In `PlayerViewModel.kt`, the stream URLs for the entire queue are resolved eagerly in a background loop. By the time a user naturally listens to the 5th or 10th song in a queue, its pre-resolved stream URL has already expired. ExoPlayer then receives an `HTTP 403 Forbidden` or `410 Gone` error, crashing playback.
2. **UI & Thread Latency**: Eager resolution of all remaining tracks sequentially causes multiple back-to-back network requests. For large playlists, this consumes substantial network bandwidth and background processing, delaying initial playback start.
3. **Race Conditions**: Since `playSong` and `playQueue` launch parallel asynchronous coroutines in `viewModelScope`, rapid clicks on different songs trigger concurrent stream resolutions that conflict with each other. Stale resolution loops from a previous queue continue running, inserting incorrect items into the newly loaded queue.

---

## 3. Caveats
- We did not run the Android application on a physical device or emulator, as we are a read-only Explorer agent.
- We assume that NewPipe Extractor version `v0.24.2` is fully compatible with dynamic on-demand resolution via ExoPlayer's `ResolvingDataSource` framework.

---

## 4. Conclusion & Proposed Solutions

### Solution for Unit Test Failure (Question 1)
To run `YouTubeExtractorTest.kt` successfully in a standard JVM environment, we should make `YouTubeExtractor.kt` completely platform-agnostic, removing its dependencies on the Android SDK:

1. **Replace `android.util.Log`**:
   Define a custom logger object or helper inside `YouTubeExtractor.kt` that delegates to `android.util.Log` only when running on Android, falling back to standard `println` when running on JVM:
   ```kotlin
   object ExtractorLogger {
       private val isAndroid = try {
           Class.forName("android.util.Log")
           true
       } catch (e: ClassNotFoundException) {
           false
       }

       fun d(tag: String, msg: String) {
           if (isAndroid) android.util.Log.d(tag, msg) else println("[$tag] D: $msg")
       }
       fun w(tag: String, msg: String) {
           if (isAndroid) android.util.Log.w(tag, msg) else println("[$tag] W: $msg")
       }
       fun e(tag: String, msg: String, tr: Throwable? = null) {
           if (isAndroid) {
               android.util.Log.e(tag, msg, tr)
           } else {
               println("[$tag] E: $msg")
               tr?.printStackTrace()
           }
       }
   }
   ```
2. **Replace `android.net.Uri`**:
   Rewrite `extractVideoId(url: String)` using a standard Kotlin regular expression which runs identically on both JVM and Android:
   ```kotlin
   private fun extractVideoId(url: String): String? = try {
       val reg = Regex("(?:v=|/v/|/embed/|youtu\\.be/|\\?v=|&v=)([a-zA-Z0-9_-]{11})")
       reg.find(url)?.groupValues?.get(1)
   } catch (e: Exception) { null }
   ```

These modifications decouple `YouTubeExtractor` from the Android framework, enabling unit tests to run fully and successfully on JRE/JVM.

### Solution for Online Playback & Streaming Issues (Question 2)
Instead of eagerly resolving all stream URLs in `PlayerViewModel`, we must implement a **Just-In-Time (JIT) Dynamic Stream Resolver** using Media3's `ResolvingDataSource`:

1. **Simplify `PlayerViewModel`**:
   Modify `buildMediaItem(song)` to immediately return a `MediaItem` with the original stable URI (e.g. `online:videoId` or `contentUri`), completely skipping the eager network call:
   ```kotlin
   private suspend fun buildMediaItem(song: Song): MediaItem? {
       val extras = android.os.Bundle().apply {
           putLong("duration", song.duration)
           song.albumArtUri?.let { art -> putString("albumArtUri", art) }
       }
       val artworkUri = song.albumArtUri?.let { art -> android.net.Uri.parse(art) }
       val metadata = MediaMetadata.Builder()
           .setTitle(song.title)
           .setArtist(song.artist)
           .setAlbumTitle(song.album)
           .apply { artworkUri?.let { setArtworkUri(it) } }
           .setExtras(extras)
           .build()

       return MediaItem.Builder()
           .setMediaId(song.uri)
           .setUri(android.net.Uri.parse(song.uri)) // Pass original URI directly
           .setMediaMetadata(metadata)
           .build()
   }
   ```
   Remove Step 2's eager background resolution loop inside `playQueue` and `playSong`.

2. **Configure `ResolvingDataSource` in `MusicService.kt`**:
   In the service where `ExoPlayer` is built, wrap the default datasource in a `ResolvingDataSource`. This intercepts playback requests and fetches the YouTube stream URL on ExoPlayer's background loading thread only when that specific track is about to be played:
   ```kotlin
   import androidx.media3.datasource.ResolvingDataSource
   import com.mark1.mytubemusic.repository.OnlineSongRepository
   import kotlinx.coroutines.runBlocking

   // Inside MusicService.onCreate():
   val onlineRepository = OnlineSongRepository()
   val httpDataSourceFactory = DefaultHttpDataSource.Factory()
       .setAllowCrossProtocolRedirects(true)
       .setConnectTimeoutMs(15_000)
       .setReadTimeoutMs(15_000)
   val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

   val resolvingDataSourceFactory = ResolvingDataSource.Factory(
       dataSourceFactory,
       object : ResolvingDataSource.Resolver {
           override fun resolve(dataSpec: DataSpec): DataSpec {
               val uri = dataSpec.uri
               if (uri.scheme == "online") {
                   val videoId = uri.schemeSpecificPart
                   val streamUrl = runBlocking {
                       onlineRepository.getStreamUrl(videoId)
                   }
                   if (streamUrl != null) {
                       return dataSpec.buildUpon()
                           .setUri(android.net.Uri.parse(streamUrl))
                           .build()
                   }
               }
               return dataSpec
           }
       }
   )

   val mediaSourceFactory = DefaultMediaSourceFactory(resolvingDataSourceFactory)

   player = ExoPlayer.Builder(this)
       .setAudioAttributes(audioAttributes, true)
       .setHandleAudioBecomingNoisy(true)
       .setMediaSourceFactory(mediaSourceFactory)
       .build()
   ```

This architecture ensures:
- **Zero URL Expirations**: Stream URLs are requested fresh right before playback starts.
- **Instant Playlist Loading**: Adding 100 songs to a queue is immediate because it is a pure local operation.
- **Race Condition Immunity**: No overlapping viewModel coroutines are launched; ExoPlayer manages all track loading sequentially on its own thread.

---

## 5. Verification Method
1. **JUnit Test Verification**:
   Run the Gradle test command from the root directory:
   `gradlew.bat :app:testDebugUnitTest --tests com.mark1.mytubemusic.YouTubeExtractorTest`
   Inspect the results to verify the test completes without throwing Log-related or Uri-related exceptions, returning a successful status.
2. **App Playback Verification**:
   Deploy the application. Go to the "Online" tab, search for songs, select a song, and click play. Verify:
   - Playback starts immediately.
   - Adding a large number of songs to the queue does not lag the UI or lock up network resources.
   - Leaving the app playing a long queue doesn't result in HTTP 403 or 410 failures on subsequent tracks.
