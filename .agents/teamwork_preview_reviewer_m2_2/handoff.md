# Milestone 2 Review Report — Share Sheet / Download Workflow

This report presents the objective quality and adversarial review for the implementation of the Share Sheet and Download Workflow.

---

## 1. Observation

### Observation 1: Lack of Cancellation Responsiveness in Download Loop
In `app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt`, the input stream is copied to the output stream inside the member function `copyToWithProgress` (lines 75-99):
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
Direct observation shows that the `while (bytes >= 0)` loop does not check the `isStopped` property of the `CoroutineWorker` class or check for thread/coroutine cancellation.

### Observation 2: Orphaned/Corrupted Partial Files on Failure or Cancellation
In `app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt`, inside `doWork()`:
- At line 167, the entry is inserted into the MediaStore database:
```kotlin
val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
```
- At line 174, `resolver.openOutputStream(uri)` is opened and written to using `copyToWithProgress`.
- At line 236, the general catch block catches exceptions:
```kotlin
        } catch (e: Exception) {
            e.printStackTrace()
            showFailedNotification(resolvedTitle, resolvedArtist)
            return@withContext Result.failure()
        }
```
Direct observation shows that if an exception occurs during download (e.g., network failure, cancellation), the partially written file at `uri` is never deleted, leaving corrupted/incomplete music files in the user's MediaStore.

### Observation 3: Notification ID Collision for Concurrent Downloads
In `app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt` (lines 32-35):
```kotlin
    companion object {
        private const val NOTIFICATION_ID = 404
        private const val CHANNEL_ID = "download_channel"
    }
```
Direct observation shows that a single static integer `NOTIFICATION_ID = 404` is used for all notifications. When multiple background downloads are enqueued concurrently, their notifications will overwrite each other, causing clashing UI updates.

### Observation 4: Unmocked Log Exception Escaping in JVM Tests
In `app/src/test/java/com/mark1/mytubemusic/YouTubeExtractorTest.kt` (which runs in a pure JVM environment, meaning Android's `Log` class is not mocked by default), we observed that the test output file `app/build/test-results/testDebugUnitTest/TEST-com.mark1.mytubemusic.YouTubeExtractorTest.xml` recorded:
```
Search failed with exception: Method e in android.util.Log not mocked. See https://developer.android.com/r/studio-ui/build/not-mocked for details.
```
In `app/src/main/java/com/mark1/mytubemusic/util/YouTubeExtractor.kt`, the custom `Log` object (lines 23-71) attempts to catch reflection exceptions:
```kotlin
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (eMethod != null) {
            try {
                eMethod.invoke(null, tag, msg, tr)
            } catch (e: Exception) {
                println("[$tag] E: $msg")
                tr?.printStackTrace()
            }
        }
```
Direct observation shows that calling `tr?.printStackTrace()` inside the catch block causes an unmocked method exception to escape under certain test runners that intercept standard error (`System.err`) and redirect it to Android's `Log`.

---

## 2. Logic Chain

1. **Cancellation Responsiveness**: Standard coroutine execution is cooperative. When a worker is stopped (e.g. by the user clicking "Cancel" in the notification, which sends a cancel PendingIntent to `WorkManager`), the `CoroutineWorker` sets `isStopped = true`. Because the `copyToWithProgress` loop (Observation 1) does not check `isStopped`, it continues fetching data from the network and writing it to storage, completing the entire file download anyway.
2. **Partial Files**: Because `uri` is created and partially filled with bytes before failure (Observation 2), and the catch block does not contain any cleanup logic (such as `resolver.delete(uri, null, null)`), failed/cancelled downloads leave orphaned, corrupt audio files in the user's storage.
3. **Notification Collisions**: Because `NOTIFICATION_ID` is a constant `404` (Observation 3), multiple instances of `DownloadWorker` running concurrently will use the same notification slot, clashing and overwriting each other's progress and status updates.
4. **Log Exceptions**: In JUnit JVM environment, calling any stub method on `android.util.Log` throws `RuntimeException("Method ... not mocked")`. The reflective `Log` wrapper catches this on `eMethod.invoke`, but when it executes `tr?.printStackTrace()` (Observation 4), the test runner redirects `System.err` to Android's `Log.e()`, which throws the unmocked method exception again outside of the `Log` wrapper's try-catch block, bubbling up to the test suite.

---

## 3. Caveats

- We assumed that `isStopped` is sufficient to check for cancellation within `copyToWithProgress`. In practice, checking `isStopped` at every buffer chunk is highly efficient and fully sufficient.
- The standard error redirection to `Log` depends on the Gradle Android test runner configuration. However, changing `tr?.printStackTrace()` to a string format like `println(tr?.stackTraceToString())` safely bypasses the standard error interception in all unit test runners.

---

## 4. Conclusion

The implementation is functionally complete and correctly integrates with `NewPipeExtractor` and `WorkManager` for background downloading. However, it lacks robustness under adversarial conditions (cancellation, network loss, and concurrent downloads) and contains minor testing log issues. 

**Verdict**: **REQUEST_CHANGES** (due to major robustness issues, specifically cancellation responsiveness, partial file leakage, and notification clashing).

### Suggested Fixes:
1. **Responsive Cancellation & File Cleanup**:
   Update `DownloadWorker.doWork()` to handle cancellation and failures by checking `isStopped` in the download loop, and deleting the incomplete file from MediaStore on failure/cancellation:
   ```kotlin
   var uri: Uri? = null
   try {
       ...
       uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
       resolver.openOutputStream(uri)?.use { outputStream ->
           body.byteStream().copyToWithProgress(outputStream, totalBytes = totalBytes) { progress ->
               if (isStopped) {
                   throw CancellationException("Worker stopped during copy")
               }
               ...
           }
       }
       ...
   } catch (e: Exception) {
       uri?.let { resolver.delete(it, null, null) }
       ...
   }
   ```
2. **Unique Notification IDs**:
   Use a unique notification ID for each download worker instance (e.g. `id.hashCode()`):
   ```kotlin
   val notificationId = id.hashCode()
   ```
3. **Safe Test Logging**:
   Update the fallback catch blocks in the custom `Log` object within `YouTubeExtractor.kt` to use `tr?.stackTraceToString()` instead of `tr?.printStackTrace()`.

---

## 5. Verification Method

### Test Commands:
Run local unit tests to verify:
```cmd
gradlew.bat test
```

### Files to Inspect:
- `app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt` (check for `isStopped` check and cleanup of `uri` on failure).
- `app/src/main/java/com/mark1/mytubemusic/util/YouTubeExtractor.kt` (check for custom `Log` implementation and lack of `tr?.printStackTrace()` in JVM environment).
