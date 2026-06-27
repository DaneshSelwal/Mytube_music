# Handoff Report: YouTube Music WebView Browser (Milestone 3)

This report details the architectural investigation and design recommendations for integrating the YouTube Music WebView browser, ensuring background playback capability, and implementing a floating action download button.

---

## 1. Observation

During the read-only investigation, the following files and structural configurations were analyzed:

### Navigation and Screen Layout:
* **`app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt`**:
  * Line 191-192 defines the pill navigation tabs:
    ```kotlin
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Songs", "Albums", "Artists", "Playlists", "Online")
    ```
  * Lines 290–621 contain the view switcher that handles rendering tab content:
    ```kotlin
    when (selectedTabIndex) {
        0 -> { /* Songs */ }
        1 -> { /* Albums */ }
        2 -> { /* Artists */ }
        3 -> { /* Playlists */ }
        4 -> { /* Online */ }
    }
    ```
  * Lines 766–786 handle rendering the `MiniPlayer` overlay when a song is playing:
    ```kotlin
    if (currentSong != null) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) { ... }
    }
    ```

### Metadata and Download Architecture:
* **`app/src/main/java/com/mark1/mytubemusic/repository/OnlineSongRepository.kt`**:
  * Line 13 defines the song metadata extractor method:
    ```kotlin
    suspend fun getSongMetadata(videoId: String): Song? = youtubeExtractor.getSongMetadata(videoId)
    ```
* **`app/src/main/java/com/mark1/mytubemusic/util/YouTubeUrlParser.kt`**:
  * Line 15 defines the video ID extraction:
    ```kotlin
    fun extractVideoId(url: String): String? { ... }
    ```
* **`app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt`**:
  * Lines 105–109 list the inputs expected by `DownloadWorker`:
    ```kotlin
    val videoId = inputData.getString("videoId") ?: return@withContext Result.failure()
    val title = inputData.getString("title")
    val artist = inputData.getString("artist")
    var albumArtUri = inputData.getString("albumArtUri")
    ```
  * Lines 119–130 show that if metadata parameters are not passed to `DownloadWorker`, it fetches them dynamically:
    ```kotlin
    if (resolvedTitle == null || resolvedArtist == null) {
        val songDetails = repo.getSongMetadata(videoId)
        ...
    }
    ```

---

## 2. Logic Chain

Based on these observations, the recommended design aligns as follows:

1. **Tab Integration**:
   * Add `"Browser"` to the `tabs` list in `HomeScreen.kt`. This adds a 6th pill tab seamlessly.
   * Modifying the view switcher directly using `when (selectedTabIndex)` would dispose the WebView tab whenever the user switches tabs, killing active audio playback and resetting the page state.
   * To prevent this, wrap the screens in a `Box` where the tabs 0 to 4 are conditionally composed, but the WebView (`BrowserScreen`) remains composed with its layout offset shifted offscreen (`Modifier.offset(x = 10000.dp)`) when inactive. This keeps the WebView instance alive, maintaining the JavaScript execution stack and background audio state.

2. **Compose WebView and Background Playback**:
   * Create a subclass of `android.webkit.WebView` (`PlaybackWebView`) that overrides `onWindowVisibilityChanged` and `onVisibilityChanged`. When the screen turns off or the app is minimized, the system changes window visibility to `View.GONE` or `View.INVISIBLE`. Normally, `WebView` intercepts this and pauses HTML5 media players. By ignoring invisible/gone states in the overridden visibility methods, the WebView believes it is still visible, keeping background audio streaming active.
   * Configure `CookieManager` to accept third-party cookies so users can sign into Google accounts.
   * Configure the user agent string to mock an iPad (`Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/... Safari/604.1`). This forces YouTube Music to serve a highly responsive touch-friendly mobile website layout, but prevents Google's mobile web server from serving annoying "Open in App" download redirection overlays or restricting background audio as they do on standard phone browsers.
   * Explicitly set `mediaPlaybackRequiresUserGesture = false` inside `WebSettings` to allow seamless autoplay when changing pages or loading URLs.
   * Prevent calling `webView.onPause()` in the main Activity lifecycle to ensure the JS runtime continues executing background audio.

3. **Floating Download Button (FAB)**:
   * Keep a state tracking the current URL inside the `WebViewClient` (using `doUpdateVisitedHistory`).
   * Compute a reactive `videoId` using `YouTubeUrlParser.extractVideoId(currentUrl)`.
   * Bind the visibility of the FAB to `videoId != null` with Compose's `AnimatedVisibility`. This guarantees the button is only visible when viewing a playable/downloadable song or video page.
   * When clicked, spawn a coroutine in the local screen scope. Set an `isResolving` loading state to block double-tap actions.
   * Execute `OnlineSongRepository().getSongMetadata(videoId)` on `Dispatchers.IO` to resolve the track title, artist name, and album art thumbnail.
   * Construct `Data` containing the parameters and enqueue `DownloadWorker` via `WorkManager`. The progress is piped directly to the system notification channel as configured in `DownloadWorker`.

---

## 3. Caveats

* **Audio Focus Concurrency**: Since the WebView runs its own audio stream separate from ExoPlayer, playing music inside the WebView while ExoPlayer is already playing local music could result in overlapping audio. A `LaunchedEffect` is implemented in `BrowserScreen` to automatically pause ExoPlayer (`playerViewModel.player?.pause()`) when the user switches to the Browser tab, but starting ExoPlayer playback again (e.g. from the MiniPlayer widget) won't automatically stop the WebView's HTML5 media playback unless custom JavaScript is injected to pause the document's audio/video tags.
* **YouTube Signature Updates**: The `OnlineSongRepository` is backed by `NewPipe Extractor` for metadata resolution and stream decryption. If YouTube modifies its backend signature algorithms, NewPipe extraction may temporarily fail until the dependency library is updated in `build.gradle`.
* **Process Lifetimes**: While the custom visibility override keeps WebView audio playing when the app is minimized, the Android OS could eventually kill the application process to reclaim memory. However, since the app already runs a persistent foreground audio service (`MusicService`), the hosting process will generally remain protected from system termination.

---

## 4. Conclusion

The implementation of Milestone 3 is straightforward and can be completed by creating a single UI file `BrowserScreen.kt` and modifying `HomeScreen.kt` to append the new navigation tab. No system configuration or background service changes are needed since the existing foreground structure keeps the app process alive.

---

## 5. Verification Method

To verify the integration after code changes have been applied:
1. **Compilation Check**:
   * Compile the project using Gradle:
     ```powershell
     .\gradlew.bat assembleDebug
     ```
2. **Unit Test Runs**:
   * Run existing url parsing and extraction tests:
     ```powershell
     .\gradlew.bat test
     ```
3. **Manual Validation Cases**:
   * **Layout**: Confirm the 6th tab "Browser" shows up in the `PillTabRow`.
   * **Auto-Pause**: Verify that clicking the "Browser" tab halts active local media playback from the MiniPlayer.
   * **Background Play**: Play a song in the YouTube Music WebView tab. Minimize the app or lock the screen. Verify the audio continues to play continuously.
   * **FAB Visibility**: Navigate to a YouTube Music song (URL with `?v=...`). Verify the FAB animates in. Navigate back to the home page (`https://music.youtube.com/`). Verify the FAB disappears.
   * **Download Initiation**: Tap the FAB on a song page. Confirm a "Resolving metadata..." message appears, followed by a "Enqueued download" toast and a system notification showing progress. Confirm that once completed, the song becomes available in the "Songs" local tab.
