## 2026-06-27T03:36:21Z

You are the Worker/Implementer for Milestone 1 Refinement.
Your task is to implement the following stability and correctness improvements based on the reviews and challenger findings:

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Instructions:
1. Initialize your working directory: C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m1_refinement. Create BRIEFING.md and progress.md there.
2. In app/src/main/java/com/mark1/mytubemusic/util/YouTubeExtractor.kt:
   - In OkHttpDownloader.execute(), wrap the execution and response parsing in a `.use { response -> ... }` block to ensure OkHttpClient Response is always closed and does not leak connection sockets.
3. In app/src/main/java/com/mark1/mytubemusic/viewmodel/PlayerViewModel.kt:
   - In updateQueue(), retrieve and pass `albumArtUri` when instantiating `Song` so the play queue UI does not lose album art.
     (e.g., `albumArtUri = metadata.artworkUri?.toString() ?: metadata.extras?.getString("albumArtUri")`)
   - In loadLyricsForUri(uriString):
     - Add a guard clause at the very beginning to skip ContentResolver query for online tracks to avoid Securities/IllegalArgumentExceptions and log pollution:
       `if (uriString.startsWith("online:")) { _currentLyrics.value = emptyList(); return@launch }`
     - When replacing `.mp3` with `.lrc` for local files, handle other extensions (e.g. .m4a, .flac) robustly (replace the last dot extension with `.lrc` rather than just searching for `.mp3`).
4. In app/src/main/java/com/mark1/mytubemusic/service/MusicService.kt:
   - In onTaskRemoved(), change the logic so `stopSelf()` is only invoked if the player is not currently playing (`!player.playWhenReady`). This prevents swipe-dismissal from killing background audio playback.
5. In app/build.gradle.kts:
   - Remove the `exclude(group = "org.mozilla", module = "rhino")` from the NewPipe dependency. The Rhino JavaScript engine is required for YouTube signature decryption at runtime.
6. Compile and run the unit tests:
   gradlew.bat :app:testDebugUnitTest --tests "com.mark1.mytubemusic.YouTubeExtractorTest"
   Verify everything compiles and passes.
7. Write a handoff report at C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m1_refinement\handoff.md detailing the changes made.
8. Send a message to the Project Orchestrator (conversation ID: fff7b680-a468-4057-b0f0-b7022892117c) when complete.

Please report back when complete.
