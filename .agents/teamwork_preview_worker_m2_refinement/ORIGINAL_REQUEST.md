## 2026-06-27T03:52:47Z
You are the Worker/Implementer for Milestone 2 Refinement.
Your task is to fix the quality and adversarial findings for the Share Sheet and Download workflow.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Instructions:
1. Initialize your working directory: C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m2_refinement. Create BRIEFING.md and progress.md there.
2. In app/src/main/java/com/mark1/mytubemusic/util/YouTubeUrlParser.kt:
   - Make the `youtubeHostPattern` regex matching case-insensitive by using `RegexOption.IGNORE_CASE` (e.g. `Regex(..., RegexOption.IGNORE_CASE)`).
3. In app/src/main/java/com/mark1/mytubemusic/util/YouTubeExtractor.kt:
   - Remove the duplicate private `extractVideoId(url: String)` method and instead call the centralized utility `YouTubeUrlParser.extractVideoId(url)`.
   - In the custom reflective `Log` fallback catch block, change `tr?.printStackTrace()` to `println(tr?.stackTraceToString())` to prevent test runner redirection from throwing unmocked method exceptions under JUnit JRE environments.
4. In app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt:
   - In `copyToWithProgress`, periodically check the worker's `isStopped` property (or check if a cancelled signal is active). If `isStopped` is true, throw a `CancellationException` to terminate the copying loop cooperatively and instantly.
   - Use a unique notification ID per download task, e.g. `val notificationId = id.hashCode()`, instead of the static `404` constant.
   - Wrap the download process in a `try-catch` block. If `isStopped` becomes true or an exception is thrown during download, make sure to delete the partially created/corrupted MediaStore URI using `applicationContext.contentResolver.delete(uri, null, null)` to prevent orphan corrupt files.
   - Use both `title` and `artist` when calculating the cache file name for missing artwork (e.g., `art_${(title + "_" + artist).hashCode()}.jpg` or `art_${(title + "_" + artist).hashCode()}`) to avoid artwork collisions on songs sharing the same title.
5. Search the codebase for other occurrences of artwork caching filenames using title hash code (e.g. searching for `"art_"` or `title.hashCode()`). Specifically check files:
   - `app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt`
   - `app/src/main/java/com/mark1/mytubemusic/ui/screens/DetailScreen.kt`
   - `app/src/main/java/com/mark1/mytubemusic/ui/components/MiniPlayer.kt`
   - Any other files.
   Update these filenames to also use the `(song.title + "_" + song.artist).hashCode()` format to ensure they load the correct artwork correctly.
6. Compile the code:
   gradlew.bat compileDebugSources
   And run tests:
   gradlew.bat test
   Verify they build and pass without error.
7. Write a handoff report at C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m2_refinement\handoff.md detailing the changes.
8. Send a message to the Project Orchestrator (conversation ID: fff7b680-a468-4057-b0f0-b7022892117c) when complete.

Please report back when complete.
