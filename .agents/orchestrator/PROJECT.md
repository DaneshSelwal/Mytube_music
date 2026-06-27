# Project: MyTube Music Enhancements

## Architecture
- Model-View-ViewModel (MVVM) architecture with Room Database.
- ExoPlayer via Media3 MusicService for playback.
- NewPipe Extractor for searching and retrieving stream URLs.
- Android WorkManager with DownloadWorker for downloading.
- Jetpack Compose for UI.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|---|---|---|---|
| 1 | Playback & Extractor Fix | Fix YouTubeExtractor unit test and ExoPlayer online playback | None | DONE |
| 2 | Share Sheet / Download | Handle SEND Intents to download shared links | M1 | DONE |
| 3 | WebView Browser | WebView tab, background audio, floating download button | M1, M2 | DONE |
| 4 | Final E2E & Audit | Verify tests, coverage, audit checks | M1, M2, M3 | DONE |

## Interface Contracts
### YouTubeExtractor ↔ ExoPlayer
- `YouTubeExtractor.getStreamUrl(videoId)` returns direct playback URLs which are loaded into MediaItem and played by ExoPlayer in `MusicService`.
### Share Intent ↔ DownloadWorker
- Intent of action `android.intent.action.SEND` contains share URL.
- App resolves metadata and triggers `DownloadWorker` via WorkManager.
### WebView ↔ DownloadWorker
- WebView page displays YouTube Music link.
- Floating action button gets current WebView URL, extracts video ID, retrieves metadata, and enqueues `DownloadWorker`.
