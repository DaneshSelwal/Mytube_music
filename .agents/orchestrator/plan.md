# Project Plan: MyTube Music Playback, Share Sheet & WebView

## Milestones

### Milestone 1: Playback & Extractor Fix (R1)
- **Objective**: Fix online song streaming and playback, and fix unit tests.
- **Tasks**:
  1. Fix the unit test `YouTubeExtractorTest` compiler/run issue: `Method e in android.util.Log not mocked`.
  2. Resolve online stream URL and fix playback via ExoPlayer without freezing, crashing, or closing the player.
  3. Ensure NewPipe Extractor is properly initialized, and network operations are not run on the main thread.
- **Verification**:
  - Run `./gradlew :app:testDebugUnitTest --tests "com.mark1.mytubemusic.YouTubeExtractorTest"`
  - Verify that search results play and start playing immediately on tap.

### Milestone 2: Share Sheet / Download Workflow (R2)
- **Objective**: Implement sharing intent receiver to automatically download YouTube/YouTube Music links.
- **Tasks**:
  1. Add Share Intent receiver for `SEND` action and `text/plain` mimeType in `AndroidManifest.xml` targeting MainActivity (or a dedicated entry point).
  2. In `MainActivity.kt`, intercept incoming share intents, extract the URL, and extract the video ID.
  3. Fetch song metadata (Title, Artist, Duration, Album Art) via the Extractor.
  4. Automatically enqueue a download task using `DownloadWorker` to save the song into public Music directory under `/MyTube/` as a valid `.m4a` file with correct tag labels and cached album art.
- **Verification**:
  - Verify that sharing a link to the app triggers the download workflow.
  - Verify downloaded file exists in `/MyTube/` as a `.m4a` file.

### Milestone 3: YouTube Music WebView Browser (R3)
- **Objective**: Add an in-app WebView browser tab with background playback and one-tap downloading.
- **Tasks**:
  1. Create a WebView tab in the app loading `https://music.youtube.com`.
  2. Support background media playback (audio continues when app is minimized or screen off).
  3. Add a floating "Download Current Song" button that reads the WebView's current URL, extracts the video ID, fetches metadata, and enqueues a download.
- **Verification**:
  - Verify WebView loads.
  - Verify background playback works when minimized.
  - Verify floating download button successfully downloads the current song.

### Milestone 4: Final E2E Integration and Audit
- **Objective**: Verify all features end-to-end and run the integrity audit.
- **Tasks**:
  1. Run all unit and instrumentation tests.
  2. Run the Forensic Auditor to check for integrity violations.
  3. Build the final APK and confirm acceptance criteria.
