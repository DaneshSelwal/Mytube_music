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
All screens are built using Jetpack Compose and Material 3, heavily utilizing `StateFlow` observation for reactive UI updates.
- **`MainActivity.kt`**: The entry point. It dynamically checks for `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` permissions upon launch. If granted, it routes directly to `home`; otherwise, it routes to `onboarding`.
- **`OnboardingScreen.kt`**: A simple screen designed to request file access permissions from the user.
- **`HomeScreen.kt`**: Features a premium dark gradient background. Displays the entire scanned local music library. Includes a glassmorphism floating `MiniPlayer` docked at the bottom when a song is actively playing.
- **`NowPlayingScreen.kt`**: The main playback UI. Features a rotating vinyl record animation for the album art, dynamic background colors extracted using `Palette`, and standard playback controls (play, pause, next, previous). It also includes a button to view the current queue.
- **`QueueScreen.kt`**: Displays the active playlist queue. Highlights the currently playing song and allows users to tap any song in the list to instantly skip to it via `skipToIndex`.

### B. Presentation Layer (ViewModels)
- **`LibraryViewModel.kt`**: Handles file discovery. It queries the Android `MediaStore` specifically for audio files located in `/sdcard/Music/`. This ensures fast, reliable scanning without the overhead or permission issues of raw file directory traversal or `DocumentFile`.
- **`PlayerViewModel.kt`**: The bridge between the UI and the underlying `Media3` playback engine. It initializes a `MediaController` to communicate with the `MusicService`. It exposes `StateFlows` for `currentSong`, `isPlaying`, `progress`, `duration`, and `queue`.

### C. Service & Playback Layer
- **`MusicService.kt`**: Extends `MediaSessionService`. This background service holds the `ExoPlayer` instance and the `MediaSession`. It ensures music continues playing even if the UI is closed. 
  - *Note*: It explicitly overrides `onTaskRemoved()` so that if the user swipes the app away from their Recents screen, the service immediately calls `player.pause()` and `stopSelf()`, killing the playback as expected by the user.

### D. Utilities & Features
- **`ShakeDetector.kt`**: Hooks into the Android `SensorManager` (accelerometer). When the user physically shakes the device, it triggers a callback to the `PlayerViewModel` to skip to the next track.
- **Custom Adaptive Icon**: Located in `res/mipmap-anydpi-v26/`, featuring a sky-blue YouTube-style background with a white play button foreground.

## 3. Data Flow
1. The app launches and checks permissions.
2. `LibraryViewModel` queries `MediaStore` and populates the `songs` list.
3. The user taps a song on `HomeScreen`, calling `playerViewModel.playQueue(songs, index)`.
4. `PlayerViewModel` converts the `Song` list into `MediaItem`s and sends them to the `MediaController`.
5. The `MediaController` commands the `ExoPlayer` inside `MusicService` to start playback.
6. The `PlayerViewModel` polls progress and state, updating the Compose UI reactively.

## 4. Current State & Known Details
- **Media Location**: Songs are specifically loaded from the local `/Music/` directory. Duplicate files previously pushed via ADB have been cleaned up via a custom PowerShell script.
- **Installation Pipeline**: Built via local Gradle scripts (`run_build.bat`) and pushed to the Realme Pad 2 via ADB (`install_and_run.bat`).
- **Database**: `AppDatabase` (Room) is configured but currently secondary to `MediaStore` for immediate file discovery.

## 5. Next Steps for AI Suggestions
If you are reading this context to provide UI or functional suggestions, focus on:
1. **Premium Aesthetics**: Enhancing Compose animations (Shared Element Transitions, advanced glassmorphism, dynamic color theming).
2. **Library Management**: Adding Albums, Artists, or Playlists sorting to `HomeScreen.kt`.
3. **Playback Features**: Implementing Shuffle, Repeat Mode, and Lyrics support.
