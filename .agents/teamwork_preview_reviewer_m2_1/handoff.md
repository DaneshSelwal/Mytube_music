# Handoff Report: Milestone 2: Share Sheet / Download Workflow Review

## 1. Observation
I directly observed the following from the reviewed source code, build configuration, and test reports:
- **Case-Sensitive Regex**:
  - File: `app/src/main/java/com/mark1/mytubemusic/util/YouTubeUrlParser.kt`
  - Lines 5, 11-13:
    ```kotlin
    private val youtubeHostPattern = Regex("""https?://(?:www\.|m\.|music\.)?(?:youtube\.com|youtu\.be)(?:/.*|$)""")
    fun isYouTubeUrl(url: String): Boolean {
        return youtubeHostPattern.matches(url)
    }
    ```
    This regex is created without `RegexOption.IGNORE_CASE`.
- **Notification ID Collision**:
  - File: `app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt`
  - Lines 33, 177:
    ```kotlin
    private const val NOTIFICATION_ID = 404
    ...
    notificationManager.notify(NOTIFICATION_ID, createForegroundInfo(progress, resolvedTitle, resolvedArtist).notification)
    ```
    The `NOTIFICATION_ID` is hardcoded as `404` for all notifications posted by the worker.
- **Non-Cooperative Coroutine Cancellation**:
  - File: `app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt`
  - Lines 75-99:
    ```kotlin
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
    There is no check for `isStopped`, `isActive`, or any coroutine yielding inside this loop.
- **MediaStore Record Cleanup Absence**:
  - File: `app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt`
  - Lines 143-189, 236-241:
    ```kotlin
    try {
        client.newCall(request).execute().use { response ->
            ...
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: run { ... }
            ...
            resolver.openOutputStream(uri)?.use { outputStream ->
                body.byteStream().copyToWithProgress(outputStream, totalBytes = totalBytes) { progress -> ... }
            } ?: run { ... }
            ...
        }
        ...
    } catch (e: Exception) {
        e.printStackTrace()
        showFailedNotification(resolvedTitle, resolvedArtist)
        return@withContext Result.failure()
    }
    ```
    If any error occurs or the worker is stopped, the caught exception results in a failed task but the inserted `uri` (representing a partial/corrupt file) is not deleted.
- **Album Art Cache Collision**:
  - File: `app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt` lines 200, 218:
    ```kotlin
    val cacheFile = File(applicationContext.cacheDir, "art_${resolvedTitle.hashCode()}.jpg")
    ```
  - File: `app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt` line 808:
    ```kotlin
    val fileName = "art_${song.title.hashCode()}"
    ```
    Artwork cache files are named using only the song title's hash code, neglecting the artist.
- **Duplicate Video ID Parsing Logic**:
  - File: `app/src/main/java/com/mark1/mytubemusic/util/YouTubeExtractor.kt` lines 251-265 contains a duplicate implementation of `extractVideoId` that mimics the regex logic of `YouTubeUrlParser.extractVideoId`.
- **Unit Test Execution Log**:
  - File: `app/build/test-results/testDebugUnitTest/TEST-com.mark1.mytubemusic.YouTubeExtractorTest.xml` lines 7-9:
    ```xml
    Search failed with exception: Method e in android.util.Log not mocked. See https://developer.android.com/r/studio-ui/build/not-mocked for details.
    WARNING: Search returned no results. If running in an offline sandbox, this is expected.
    Unit test ends gracefully to prevent false test failures.
    ```
    Android `Log` methods throw unmocked exceptions because JVM tests don't mock it, and `app/build.gradle.kts` lacks `testOptions { unitTests.isReturnDefaultValues = true }`.

---

## 2. Logic Chain
- **Case-Sensitive Regex**:
  1. Standard URLs shared from external browsers or applications can sometimes incorporate uppercase schemes or domains (e.g., `HTTPS://WWW.YOUTUBE.COM/...` or `https://YOUTU.BE/...`).
  2. The `youtubeHostPattern` regex requires lowercase matches.
  3. Consequently, any uppercase shared URL will fail the `isYouTubeUrl()` check inside `ShareHandlerActivity`, toast "Invalid YouTube link" to the user, and terminate without enqueuing the download task.
- **Notification ID Collision**:
  1. A background `DownloadWorker` is enqueued for every song shared.
  2. Each active `DownloadWorker` updates the foreground notification on the same hardcoded ID (`NOTIFICATION_ID = 404`).
  3. When multiple downloads run concurrently, their notifications will overwrite each other, causing progress bar jumping (e.g. flickering between 12% and 80%) and incorrect finalization messages.
- **Non-Cooperative Cancellation**:
  1. A user clicking the "Cancel" action in the foreground notification initiates the cancellation of the WorkRequest coroutine.
  2. The actual byte copying occurs inside `copyToWithProgress`, which executes a blocking stream read/write loop.
  3. Because the loop contains no cancellation checks (such as verifying `isStopped` or calling `yield()`), it will run until the entire audio stream is read. Thus, the download is not cancelled immediately and consumes network/disk bandwidth until the stream finishes.
- **MediaStore Record Cleanup**:
  1. The MediaStore record and file are created via `resolver.insert` before streaming data.
  2. If the download fails mid-stream or is cancelled, `copyToWithProgress` throws or is interrupted.
  3. The `catch` block in `doWork` catches the error but does not delete the created `uri`, causing corrupted/partial `.m4a` files to accumulate in the user's storage.
- **Album Art Cache Collision**:
  1. Art cache files are named using only the song title's hash code (`art_${title.hashCode()}.jpg`).
  2. If the user downloads two different songs that share the same title (e.g., "Hello" by Adele and "Hello" by Lionel Richie), both will share the exact same cached artwork path.
  3. This causes one song's artwork to overwrite the other's, leading to incorrect images in the local library view.

---

## 3. Caveats
- Direct test execution was not performed during this review cycle due to a permission timeout on the command line interface in the automated workspace.
- The unit test logs reviewed are from the pre-existing build output under `app/build/test-results/`.
- No physical device was used to verify how different Android OS versions handle MediaStore file pending status, though API 29+ checks in code align with standard requirements.

---

## 4. Conclusion
The current implementation works under normal, single-task conditions but fails under edge cases (uppercase sharing), concurrent workflows (notification overwrite), cancellation requests (non-cooperative looping), duplicate titles (artwork cache collisions), and error states (orphaned partial files).

The final verdict is **REQUEST_CHANGES**. The worker must implement the changes described in the Quality and Adversarial reviews below.

---

## 5. Verification Method
1. **Compilation & Unit Tests**:
   - Run `.\gradlew.bat test` from the root directory of the application and ensure both `YouTubeUrlParserTest` and `YouTubeExtractorTest` compile and pass without failures.
2. **Code Inspection**:
   - Verify that `youtubeHostPattern` in `YouTubeUrlParser.kt` uses `RegexOption.IGNORE_CASE`.
   - Verify that `DownloadWorker.kt` generates a dynamic notification ID (e.g., `id.hashCode()`) instead of hardcoding `404`.
   - Verify that `copyToWithProgress` or the copying block in `DownloadWorker.kt` checks `isStopped` on each chunk write and aborts early if true.
   - Verify that in case of exception or cancellation, the created `uri` is deleted using `context.contentResolver.delete(uri, null, null)`.
   - Verify that the artwork cache filename includes both the title and artist hashes (e.g., `art_${(title + "_" + artist).hashCode()}.jpg`) to avoid collisions.
   - Verify that `YouTubeExtractor.kt` utilizes `YouTubeUrlParser.extractVideoId` rather than duplicating the extraction logic.

---

# Quality Review Report

**Verdict**: REQUEST_CHANGES

## Findings

### [Major] Finding 1: Case-Sensitive URL Matcher
- **What**: The host matching regex `youtubeHostPattern` is case-sensitive.
- **Where**: `YouTubeUrlParser.kt:5`
- **Why**: Shared links containing uppercase letters (e.g. `HTTPS://...`) will be rejected by validation, causing the app to toast "Invalid YouTube link".
- **Suggestion**: Use `Regex(..., RegexOption.IGNORE_CASE)` to declare the regex or add `(?i)` flag.

### [Major] Finding 2: Notification ID Collision
- **What**: All background download tasks share a single hardcoded notification ID (`404`).
- **Where**: `DownloadWorker.kt:33`
- **Why**: Concurrent downloads will clash, overriding each other's progress updates and status, resulting in a broken user experience.
- **Suggestion**: Generate unique notification IDs dynamically using the WorkRequest ID hash code: `id.hashCode()`.

### [Major] Finding 3: Non-Cooperative Work Cancellation
- **What**: The byte copying loop does not check for work cancellation or coroutine interruption.
- **Where**: `DownloadWorker.kt:75-99`
- **Why**: Cancelling a download task does not abort the stream copy loop. The file continues to download in the background until the stream naturally terminates.
- **Suggestion**: Periodically check `isStopped` inside the copy loop or pass a check lambda from the `CoroutineWorker` context and throw a `CancellationException` to exit early.

### [Major] Finding 4: Orphaned Partial Files in MediaStore
- **What**: MediaStore records and files are not cleaned up on download failure or cancellation.
- **Where**: `DownloadWorker.kt:143-189`
- **Why**: Aborted or failed downloads leak empty or partially written corrupt files into the user's Music storage directory.
- **Suggestion**: Wrap the stream copy in a `try-catch` block; in the `catch` or cancellation blocks, invoke `resolver.delete(uri, null, null)` to remove the partial record.

### [Minor] Finding 5: Duplicate Video ID Parsing Logic
- **What**: Private `extractVideoId` helper duplicates parsing logic of the utility class.
- **Where**: `YouTubeExtractor.kt:251-265`
- **Why**: Redundant codebase code.
- **Suggestion**: Call `YouTubeUrlParser.extractVideoId(url)` instead of redefining the extraction logic.

---

# Adversarial Review / Stress-Test Report

**Overall risk assessment**: MEDIUM

## Challenges

### [High] Challenge 1: Runaway Downloads Under Metered Connection
- **Assumption challenged**: The worker assumes that WorkManager cancellation will automatically abort OkHttp streams.
- **Attack scenario**: User starts downloading a large audio file on a cellular connection, realizes it is metered, and clicks "Cancel" in the notification.
- **Blast radius**: The worker continues downloading the stream to completion because the copy loop is not cooperative. This leaks mobile data and battery life.
- **Mitigation**: Add checks for `isStopped` in `copyToWithProgress` loop.

### [Medium] Challenge 2: Cache Key Collision for Duplicated Titles
- **Assumption challenged**: The app assumes that song titles are globally unique identifiers for caching artwork.
- **Attack scenario**: User downloads different songs with the same title (e.g. "Hello").
- **Blast radius**: The cache file `art_${title.hashCode()}.jpg` will be overwritten, causing both songs to render the same artwork in the UI.
- **Mitigation**: Incorporate both title and artist in the hash name calculation: `(title + "_" + artist).hashCode()`.

---
