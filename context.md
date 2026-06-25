# MyTube Music - Project Context & Architecture

This document outlines the current state, architecture, and features of the **MyTube Music** Android application. It serves as a comprehensive context for any AI or developer looking to understand the project structure and provide further architectural or UI/UX enhancements.

## 1. Project Overview
- **Name**: MyTube Music
- **Platform**: Android
- **Core Stack**: Kotlin, Jetpack Compose (Material 3), AndroidX Media3 (MediaSession, MediaController, ExoPlayer), Room Database.
- **Goal**: A modern, premium local music player that behaves similarly to YouTube Music but strictly plays local files from the device.

## 2. High-Level Architecture

The app follows an MVVM (Model-View-ViewModel) architecture paired with a background MediaService for continuous playback.

### A. UI Layer (Jetpack Compose)
All screens are built using Jetpack Compose and Material 3, heavily utilizing `StateFlow` observation for reactive UI updates and `SharedTransitionScope` for seamless navigation animations between screens.
- **`MainActivity.kt`**: The entry point. Dynamically checks for `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` permissions upon launch.
- **`OnboardingScreen.kt`**: A premium, "Midnight Vinyl" styled screen designed to request file access permissions from the user with deep gradients and modern typography.
- **`HomeScreen.kt`**: 
  - Features the "Midnight Vinyl" design system (`Tokens` and `MyTubeTypography`) with deep backgrounds, glassmorphism (`Tokens.glassTint`), and vibrant accents.
  - Fully responsive to Landscape and Portrait modes.
  - Displays the entire scanned local music library with an animated pill-shaped TabRow for Songs, Albums, and Artists.
  - Albums and Artists are displayed in a sleek `LazyVerticalGrid` of `AlbumArtistCard`s with gradient scrims.
  - `SongItem`s feature active playing indicators (gradient bars) and elegant typography.
  - Includes a sleek, rounded `MiniPlayer` docked at the bottom with embedded `SharedTransition` elements for album art, play controls, and a bottom progress bar.
- **`NowPlayingScreen.kt`**: 
  - The main playback UI. Adaptable to both Landscape and Portrait orientations using fluid layouts.
  - Features a rotating vinyl record animation for the album art (`SpinningCDAnimation` customized as a vinyl record).
  - Immersive, pulsating mesh gradient background synced to the beat/progress of the playing song.
  - Typography is styled in aesthetic, magazine-like `DM Serif Display` and `Inter` via `Tokens`.
  - Incorporates `LyricsView` parser that reads local `.lrc` files to display synced karaoke-style lyrics with smooth animations.
  - Integrated with `LocalHapticFeedback` to provide tactile responses (long press / subtle ticks) on play controls and UI interactions.
  - Features quick-action chips/buttons for Toggling Lyrics and Sleep Timer settings.
- **`QueueBottomSheet`**: Replaced the standalone `QueueScreen` with an elegant `ModalBottomSheet` directly accessible from `NowPlayingScreen` for better context preservation and UX. Displays the active playlist queue.

### B. Presentation Layer (ViewModels)
- **`LibraryViewModel.kt`**: Handles file discovery. It queries the Android `MediaStore` specifically for audio files located in `/sdcard/Music/`. It extracts custom artist info from file names if standard metadata is missing.
- **`PlayerViewModel.kt`**: The bridge between the UI and the underlying `Media3` playback engine. It initializes a `MediaController` to communicate with the `MusicService`. It manages current progress, shuffling, repeating, playback speed, and sleep timers.

### C. Service & Playback Layer
- **`MusicService.kt`**: Extends `MediaSessionService`. This background service holds the `ExoPlayer` instance and the `MediaSession`. It ensures music continues playing even if the UI is closed. 
  - *Note*: It explicitly overrides `onTaskRemoved()` so that if the user swipes the app away from their Recents screen, the service immediately calls `player.pause()` and `stopSelf()`, killing the playback as expected by the user.

### D. Utilities & Features
- **`ShakeDetector.kt`**: Hooks into the Android `SensorManager` (accelerometer). When the user physically shakes the device, it triggers a callback to skip to the next track.
- **`PaletteExtractor.kt`**: Extracts the most vibrant and aesthetic dynamic colors from album art to theme the background gradients.
- **`ArtworkScraper.kt`**: An intelligent web scraper using `Jsoup` that queries Google Images for missing album art (e.g. "Song Title Artist Name album art"), caching the results locally.
- **`LrcParser.kt`**: Reads and parses `.lrc` lyric files to power the synced lyrics screen.
- **Custom Adaptive Icon**: Located in `res/mipmap-anydpi-v26/`, featuring a sky-blue YouTube-style background with a white play button foreground.

## 3. Data Flow
1. The app launches and checks permissions.
2. `LibraryViewModel` queries `MediaStore` and populates the `songs`, `albums`, and `artists` states.
3. The user taps a song or album card on `HomeScreen`, calling `playerViewModel.playQueue(list, index)`.
4. `PlayerViewModel` converts the `Song` list into `MediaItem`s and sends them to the `MediaController`.
5. The `MediaController` commands the `ExoPlayer` inside `MusicService` to start playback.
6. The `PlayerViewModel` polls progress and state, updating the Compose UI reactively.

## 4. Current State & Known Details
- **Media Location**: Songs are specifically loaded from the local `/Music/` directory. Duplicate files previously pushed via ADB have been cleaned up via a custom PowerShell script.
- **Installation Pipeline**: Built via local Gradle scripts (`run_build.bat`) and pushed to the Realme Pad 2 via ADB (`install_and_run.bat`).
- **Database**: `AppDatabase` (Room) is configured but currently secondary to `MediaStore` for immediate file discovery.
