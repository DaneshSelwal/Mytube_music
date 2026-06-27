## 2026-06-27T09:15:14Z
You are the Worker/Implementer for Milestone 2: Share Sheet / Download Workflow.
Your task is to implement the Share Sheet receiver and the download progress notifications.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Instructions:
1. Initialize your working directory: C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m2. Create BRIEFING.md and progress.md there.
2. Read the Explorer's handoff report for Milestone 2 at C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_explorer_m2\handoff.md.
3. Edit app/src/main/AndroidManifest.xml:
   - Declare the `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />` permission.
   - Register a dedicated activity `.ShareHandlerActivity` with `Theme.Translucent.NoTitleBar` to receive `android.intent.action.SEND` intents with `text/plain` mimeType.
   - Register or update the WorkManager `SystemForegroundService` with `foregroundServiceType="dataSync"` (using tools:node="merge" as described in the handoff).
4. Create app/src/main/java/com/mark1/mytubemusic/util/YouTubeUrlParser.kt:
   - Parse and extract a URL from sharing text.
   - Match and extract the 11-char video ID from YouTube/YouTube Music URLs (including queries like ?v=, /embed/, /shorts/, youtu.be/ links).
5. Modify app/src/main/java/com/mark1/mytubemusic/util/YouTubeExtractor.kt and app/src/main/java/com/mark1/mytubemusic/repository/OnlineSongRepository.kt:
   - Add a public method `getSongMetadata(videoId)` that fetches the video page from NewPipe and returns a `Song` object containing title, artist, duration (length * 1000L), and high-res album art URL.
6. Create app/src/main/java/com/mark1/mytubemusic/ShareHandlerActivity.kt:
   - Extends Activity. In onCreate(), handle SEND intents containing URLs.
   - If a valid YouTube link is found, extract the videoId, start the WorkManager download request via DownloadWorker, show a Toast message, and call finish() immediately.
7. Modify app/src/main/java/com/mark1/mytubemusic/worker/DownloadWorker.kt:
   - Add progress tracking with custom buffer copying `copyToWithProgress(out, totalBytes) { progress -> ... }`.
   - Setup foreground info/notification with dataSync foreground service type (using Android 14 API 34 compliance).
   - If title/artist are not supplied in the input data, dynamically resolve them via `OnlineSongRepository.getSongMetadata(videoId)`.
   - Update notification progress from 0% to 100%, show "Download complete" or "Download failed" notifications.
8. Compile the debug sources using:
   gradlew.bat compileDebugSources
   And run tests:
   gradlew.bat test
   Ensure it compiles without errors.
9. Write a handoff report at C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m2\handoff.md detailing the changes.
10. Send a message to the Project Orchestrator (conversation ID: fff7b680-a468-4057-b0f0-b7022892117c) when complete.

Please report back when complete.
