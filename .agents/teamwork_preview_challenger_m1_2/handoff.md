# Verification Report: Playback & Extractor Fix (Milestone 1)

## 1. Observation
The following files were analyzed in the workspace `C:\Users\selwa\Desktop\Music App\MyTubeMusic`:
* `app/src/main/java/com/mark1/mytubemusic/util/YouTubeExtractor.kt`
* `app/src/main/java/com/mark1/mytubemusic/viewmodel/PlayerViewModel.kt`
* `app/src/main/java/com/mark1/mytubemusic/service/MusicService.kt`
* `app/src/test/java/com/mark1/mytubemusic/YouTubeExtractorTest.kt`

### Attempts to Run Tests
The verification command was proposed:
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.mark1.mytubemusic.YouTubeExtractorTest"
```
**Result**: The command execution timed out waiting for user approval.

### Code Observations
* **OkHttpDownloader execute()** (in `YouTubeExtractor.kt`, lines 91-124):
  ```kotlin
  override fun execute(request: NpRequest): NpResponse {
      val requestBuilder = Request.Builder().url(request.url())
      ...
      val response: Response = client.newCall(httpRequest).execute()

      // OkHttp 4.x uses Kotlin property syntax instead of Java-style method calls
      val responseHeaders = mutableMapOf<String, MutableList<String>>()
      response.headers.names().forEach { name ->
          responseHeaders[name] = response.headers(name).toMutableList()
      }

      val body = response.body?.string() ?: ""
      return NpResponse(...)
  }
  ```
* **MusicService ResolvingDataSource.Resolver** (in `MusicService.kt`, lines 41-60):
  ```kotlin
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
  ```
* **PlayerViewModel loadLyricsForUri()** (in `PlayerViewModel.kt`, lines 167-192):
  ```kotlin
  private fun loadLyricsForUri(uriString: String) {
      val context = appContext ?: return
      viewModelScope.launch(Dispatchers.IO) {
          try {
              val uri = android.net.Uri.parse(uriString)
              var dataPath: String? = null
              
              val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
              context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                  ...
              }
              ...
          } catch (e: Exception) {
              _currentLyrics.value = emptyList()
          }
      }
  }
  ```

---

## 2. Logic Chain
1. **Response Resource Leak**:
   * *Observation*: `OkHttpDownloader.execute()` creates a `Response` using `client.newCall(httpRequest).execute()`.
   * *Reasoning*: Although `response.body?.string()` closes the body stream when called successfully, if an exception is thrown before this statement (e.g. while parsing headers or names), the response object is never closed. This causes socket leaks under network failure scenarios.
2. **Useless Content Resolver Queries & Exception Pollution**:
   * *Observation*: `PlayerViewModel.loadLyricsForUri` receives the media URI (which is `online:videoId` for streaming songs) and calls `context.contentResolver.query(uri, ...)` with it.
   * *Reasoning*: The `online` scheme is not supported by Android's `ContentResolver`. Thus, this query is guaranteed to throw a `SecurityException` or `IllegalArgumentException` on every online song transition. While the exception is caught, it pollutes logs and introduces CPU overhead.
3. **Playback Architecture Improvements**:
   * *Observation*: `PlayerViewModel.playQueue` builds `MediaItem`s using stable, static IDs (`online:videoId`) without fetching stream URLs upfront. `MusicService` intercepts the stream loading dynamically using `ResolvingDataSource`.
   * *Reasoning*: This represents a significant design improvement. Resolving stream URLs on-demand via the ExoPlayer loader background thread avoids pre-fetching delays, prevents play queue initialization lag, and solves issues with expiring stream URLs.

---

## 3. Caveats
* **Offline Sandbox / Timeout**: Unit tests could not be run because the approval prompt timed out. The logic is validated via static analysis.
* **YouTube Cipher Maintenance**: The implementation is dependent on the `NewPipeExtractor` library version `v0.24.2`. If YouTube updates its cipher algorithms, playback will fail until the NewPipe library is updated.

---

## 4. Conclusion
The implementation of the dynamic on-demand stream resolution using `ResolvingDataSource` is logically correct and highly optimized compared to the previous design. It resolves the core issues of play queue initialization lag and expiring stream URLs. 

However, two minor flaws should be addressed:
1. **Leak Prevention**: Wrap the OkHttp `Response` in a `.use { ... }` block inside `OkHttpDownloader.execute()`.
2. **Lyric Loading Optimization**: In `PlayerViewModel.loadLyricsForUri`, add a guard clause `if (!uriString.startsWith("content:")) { _currentLyrics.value = emptyList(); return@launch }` to avoid throwing unnecessary exceptions.

---

## 5. Verification Method
* Run the Gradle task:
  ```powershell
  .\gradlew.bat :app:testDebugUnitTest --tests "com.mark1.mytubemusic.YouTubeExtractorTest"
  ```
* Verify in the Android Studio Logcat that no `SecurityException` is thrown when changing between online songs (confirming correct guard clause in `loadLyricsForUri`).
