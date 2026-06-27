# Original User Request

## Initial Request — 2026-06-27T03:19:58Z

Fix online playback issues in MyTube Music, add a Share Intent receiver for downloading shared links, and implement a Brave-style WebView tab for YouTube Music with background playback and one-tap downloading.

Working directory: C:\Users\selwa\Desktop\Music App\MyTubeMusic
Integrity mode: development

## Requirements

### R1. YouTube Music Playback & Extractor Fix
Fix the online song streaming and playback mechanism. Tapping an online song must resolve the stream URL and play successfully via ExoPlayer without freezing, crashing, or closing the player. Correct NewPipe Extractor initialization or thread/coroutine issues (e.g. Log class not mocked in tests, network on main thread, or missing service hooks).

### R2. Share Sheet / Download Workflow
Implement a Share Intent receiver (handling `android.intent.action.SEND` for `text/plain` URLs) in the app. When a YouTube or YouTube Music link (e.g. shared from Brave Browser) is shared with MyTube Music, the app must:
1. Parse the URL and extract the video ID.
2. Resolve the song metadata (Title, Artist, Duration, Album Art) using the extractor.
3. Automatically enqueue a download task using the existing `DownloadWorker` to download the song into the public Music directory with proper tag labels and cached album art.

### R3. YouTube Music WebView Browser
Implement an in-app WebView browser tab that loads the mobile YouTube Music site (`https://music.youtube.com`). It must:
1. Support background media playback (so music keeps playing when the app is minimized or the device screen is off).
2. Include a floating "Download Current Song" button that reads the current WebView URL, extracts the video ID, fetches metadata, and downloads the song with correct labels.

## Acceptance Criteria

### Playback & Extractor
- [ ] Online search results play successfully on tap; the MiniPlayer appears immediately and changes state to playing.
- [ ] The unit test `YouTubeExtractorTest` compiles and runs successfully without throwing `Method e in android.util.Log not mocked`.

### Share & Download
- [ ] Sharing a YouTube Music link from Brave Browser to MyTube Music launches the app and triggers a notification showing download progress.
- [ ] The downloaded song file is saved in the public Music folder under `/MyTube/` as a valid `.m4a` file with correct title and artist tags.

### WebView Browser
- [ ] The WebView tab loads YouTube Music mobile site.
- [ ] Tapping the Download button in the WebView successfully starts downloading the currently active song.
- [ ] Audio from the WebView continues playing when the app is minimized.
