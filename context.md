# MyTube Music - Project Context

## 1. Project Overview
- **App Name**: MyTube Music
- **Platform**: Android
- **Core Stack**: 
  - Android Application (`com.android.application` v8.2.0)
  - Kotlin (`org.jetbrains.kotlin.android` v1.9.24)
  - Jetpack Compose (BOM: `2024.10.00`)
  - KSP (`1.9.24-1.0.20`)
  - Target/Compile SDK: `34`
  - Min SDK: `26`

## 2. Architecture Summary
The app follows an MVVM (Model-View-ViewModel) architecture paired with an asynchronous Media3 background service.

### ViewModels
- **`LibraryViewModel.kt`**: Scans the local device (`MediaStore`) for `.mp3` files, groups them into albums/artists, stores them in Room DB, and manages search queries.
- **`PlayerViewModel.kt`**: Bridges the UI to the underlying `Media3` engine via `MediaController`. Manages playback state, queue, timeline progress, shuffle/repeat modes, sleep timers, and `.lrc` lyric parsing.

### Screen Composables
- **`MainActivity.kt`**: Entry point that dynamically handles permission requests and initializes the navigation graph (`AppNavHost`).
- **`OnboardingScreen.kt`**: Premium "Midnight Vinyl" themed screen to request storage/audio permissions.
- **`HomeScreen.kt`**: Displays the local library with tabs for Songs, Albums, and Artists. Features `ShimmerSongItem` loading states, grid layouts (`LazyVerticalGrid`), and a floating `MiniPlayer`.
- **`NowPlayingScreen.kt`**: Main playback UI featuring a spinning vinyl animation (`SpinningCDAnimation`), a glowing SeekBar, and synchronized lyrics overlay.
- **`DetailScreen.kt`**: Displays the tracklist for a selected Album or Artist clicked from the HomeScreen.
- **`QueueScreen.kt`** (Replaced): The queue has been refactored into a `QueueBottomSheet` directly accessible within `NowPlayingScreen`.

### Services
- **`MusicService.kt`**: Extends `MediaSessionService`. Runs ExoPlayer in the background, ensuring music plays when the app is minimized and handles media notifications.

### Utility Classes
- **`ShakeDetector.kt`**: Uses device accelerometer to skip tracks when the device is shaken.
- **`ArtworkScraper.kt`**: Scrapes Google Images via Jsoup for missing album art.
- **`PaletteExtractor.kt`**: Uses AndroidX Palette to extract vibrant dominant colors from bitmaps.
- **`LrcParser.kt`**: Parses standard `.lrc` text files into typed `LyricLine` objects with timestamps.

## 3. Current File Tree
```
MyTubeMusic/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/mark1/mytubemusic/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── db/ (AppDatabase.kt, SongDao.kt)
│       │   │   │   └── model/ (Entities.kt)
│       │   │   ├── repository/ (SongRepository.kt)
│       │   │   ├── service/ (MusicService.kt)
│       │   │   ├── ui/
│       │   │   │   ├── components/ (GlassCard.kt, MiniPlayer.kt, VinylDisc.kt)
│       │   │   │   ├── screens/ (DetailScreen.kt, HomeScreen.kt, NowPlayingScreen.kt, OnboardingScreen.kt, QueueScreen.kt)
│       │   │   │   └── theme/ (Color.kt, Theme.kt, Type.kt)
│       │   │   ├── util/ (ArtworkScraper.kt, Extensions.kt, PaletteExtractor.kt, ShakeDetector.kt)
│       │   │   ├── utils/ (LrcParser.kt)
│       │   │   └── viewmodel/ (LibraryViewModel.kt, PlayerViewModel.kt)
│       │   └── res/
│       │       ├── drawable/ (ic_launcher_background.xml, ic_launcher_foreground.xml)
│       │       ├── font/ (dm_serif_display_italic.ttf)
│       │       ├── mipmap-anydpi-v26/ (ic_launcher.xml, ic_launcher_round.xml)
│       │       └── values/ (strings.xml, themes.xml)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── context.md
```

## 4. Design Tokens & Theme
The design targets a **"Midnight Vinyl"** aesthetic defined in `Tokens` (`Color.kt`):

- **Backgrounds**: `bgDeep` (0xFF0D0B0F), `bgSurface` (0xFF161218), `bgElevated` (0xFF1E1A22)
- **Accents**: `accentPrimary` (0xFFB08DFF), `accentSecondary` (0xFFFFD98E)
- **Text**: `textPrimary` (0xFFF2EEF8), `textSecondary` (0xFF9A8FAA), `textDisabled` (0xFF4A3F55)
- **Effects**: `strokeSubtle` (0xFF2A2432), `glassTint` (0x22B08DFF)

**Typography** (`Type.kt`):
- `DmSerifDisplay` (Serif fallback used for `TitleLarge` 28sp / `TitleMedium` 20sp)
- `Inter` (SansSerif fallback used for `BodyMedium` 14sp / `BodySmall` 12sp)
- `JetBrainsMono` (Monospace fallback)

## 5. Navigation Graph
Housed in `MainActivity.kt` using `AppNavHost` with an overarching `SharedTransitionLayout`:
1. `onboarding` -> `OnboardingScreen` (transitions to `home` on permission grant)
2. `home` -> `HomeScreen` (transitions to `now_playing` via MiniPlayer, or `detail` via Album/Artist cards)
3. `now_playing` -> `NowPlayingScreen` (can pop back to `home` or `detail`)
4. `detail` -> `DetailScreen` (can pop back to `home`, transitions to `now_playing` via MiniPlayer)

## 6. State Management

### `LibraryViewModel.kt`
- `allSongs` (StateFlow<List<Song>>): Backs the full music library.
- `albums` (StateFlow<Map<String, List<Song>>>): Groups songs by album for the Albums tab grid.
- `artists` (StateFlow<Map<String, List<Song>>>): Groups songs by artist for the Artists tab grid.
- `searchQuery` (StateFlow<String>): Search bar input in `HomeScreen`.
- `filteredSongs` (StateFlow<List<Song>>): Drives the dynamically filtered Songs tab.
- `isScanning` (StateFlow<Boolean>): Drives the loading skeleton (`ShimmerSongItem`).
- `scanProgress` (StateFlow<String>): Text display during MediaStore scanning.
- `selectedDetailTitle` / `selectedDetailSongs` (StateFlow): Passes data directly to `DetailScreen`.

### `PlayerViewModel.kt`
- `currentSong` (StateFlow<Song?>): Global playing track metadata.
- `isPlaying` (StateFlow<Boolean>): Drives Play/Pause button icons across the app.
- `queue` (StateFlow<List<Song>>): Backs the `QueueBottomSheet`.
- `progress` / `duration` (StateFlow<Long>): Drives SeekBars and timers.
- `shuffleModeEnabled` (StateFlow<Boolean>): Drives shuffle toggle icon color.
- `repeatMode` (StateFlow<Int>): Cycles between OFF/ALL/ONE.
- `sleepTimerText` (StateFlow<String?>): Drives sleep timer countdown UI.
- `playbackSpeed` (StateFlow<Float>): Playback multiplier state.
- `currentLyrics` (StateFlow<List<LyricLine>>): Syncs karaoke lyrics in `NowPlayingScreen`.

## 7. Dependencies
From `build.gradle.kts`:
```kotlin
implementation("androidx.core:core-ktx:1.13.1")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
implementation("androidx.activity:activity-compose:1.9.0")
val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
implementation(composeBom)
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.animation:animation")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.navigation:navigation-compose:2.8.0")
implementation("androidx.media3:media3-exoplayer:1.3.1")
implementation("androidx.media3:media3-session:1.3.1")
implementation("androidx.media3:media3-ui:1.3.1")
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("io.coil-kt:coil-compose:2.6.0")
implementation("androidx.palette:palette-ktx:1.0.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("org.jsoup:jsoup:1.17.2")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.datastore:datastore-preferences:1.1.1")
```

## 8. Known Issues / TODOs
- **Fonts Placeholder**: `Type.kt` relies on default system fallbacks (`FontFamily.Serif`, `FontFamily.SansSerif`) even though `dm_serif_display_italic.ttf` is present in `res/font/`. The font family bindings need to be updated to use the actual `.ttf` resource.
- **Scraper Reliability**: `ArtworkScraper` parses Google Images via raw regex which is brittle and likely to fail if DOM structures change.
- **Legacy Files**: `QueueScreen.kt` still exists in the filesystem but has been bypassed by `QueueBottomSheet`. It should be deleted to prevent confusion.

## 9. Implementation Status from Instruction Checklist
*(Note: `instruction.md` does not exist natively in the file tree, but referring to the "MyTube Music — UI & UX Enhancement Instructions" from previous context)*:
- **DONE**: Upgrade NowPlayingScreen (Vinyl CD, glowing slider, haptic feedback).
- **DONE**: Migrate Queue to `ModalBottomSheet`.
- **DONE**: Add `ShimmerSongItem` and `EmptyLibrary` to `HomeScreen`.
- **DONE**: Upgrade `OnboardingScreen` to match Midnight Vinyl tokens.
- **DONE**: Add `DetailScreen` routing for Artists and Albums.
- **DONE**: Define exact global color Tokens and typography settings.
- **DONE**: Font Integration (Task 1)
- **DONE**: Project structure cleanup (Task 2)
- **DONE**: PillTabRow (Task 3a)
- **DONE**: SongListItem active highlight (Task 3b)
- **DONE**: MiniPlayer progress line + swipe-up (Task 4)
- **DONE**: AlbumArtistCard gradient scrim (Task 5)

## 10. Blockers / Questions
- **None**: All previous blockers (font placeholders and duplicate utility packages) have been fully resolved.
