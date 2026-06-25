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

## 9. Implementation Status (Full Audit)
- **DONE**: Refactor MiniPlayer.kt with Tokens, AsyncImage, AnimatedVisibility, and Haptics.
- **DONE**: NowPlayingScreen.kt visual polish (Mesh background, LyricsView, swipe gestures, bottom sheets, CD animation).
- **DONE**: HomeScreen.kt top bar collapsing and search bar tokens.
- **DONE**: DetailScreen.kt blurred hero image and scrim.
- **DONE**: MainActivity.kt enter/exit navigation transitions.
- **DONE**: QueueBottomSheet drag-to-reorder integration.

### HomeScreen.kt
- Renders: The main library interface with a tabbed layout (Songs, Albums, Artists), search bar, and shimmer loading state.
- Token gaps: Search bar container uses Color.White.copy(alpha = 0.1f) and text uses Color.White.copy(alpha = 0.5f) instead of Tokens.
- Inline styles: Uses ontSize = 18.sp and ontWeight = FontWeight.Bold in SectionHeader.
- TODOs: None found.

### NowPlayingScreen.kt
- Renders: The primary playback UI featuring an animated mesh background, spinning vinyl CD, synced lyrics, and playback controls.
- Token gaps: Canvas grooves use Color.White.copy(...). Background fallback animations use Color.DarkGray and Color.Black.
- Inline styles: Uses ontSize = 12.sp in extra controls and ontWeight = FontWeight.Bold in QueueItem.
- TODOs: None found.

### DetailScreen.kt
- Renders: A song list for a specific album or artist with a gradient background and a floating mini player at the bottom.
- Token gaps: None.
- Inline styles: None.
- TODOs: None found.

### OnboardingScreen.kt
- Renders: The initial permission request screen with a glowing background and a scanning progress indicator.
- Token gaps: None.
- Inline styles: None.
- TODOs: None found.

### components/GlassCard.kt
- Renders: A translucent, blurred card container.
- Token gaps: Uses Color.White.copy(alpha = 0.08f) instead of Tokens.glassTint.
- Inline styles: None.
- TODOs: None found.

### components/MiniPlayer.kt
- Renders: A compact floating playback controller with a play/pause and next button.
- Token gaps: Uses MaterialTheme.colorScheme instead of Tokens.
- Inline styles: Uses MaterialTheme.typography instead of MyTubeTypography.
- TODOs: Contains dummy Box and Column implementations at the bottom of the file that shadow Compose foundation equivalents.

### components/VinylDisc.kt
- Renders: A static or infinitely spinning vinyl record graphic.
- Token gaps: Uses Color.Black, Color.White.copy(...), and Color.DarkGray instead of Tokens.
- Inline styles: None.
- TODOs: None found.

---

## 10. Animation Audit

### Existing animations:
- NowPlayingScreen.kt: Infinite mesh background with PaletteExtractor. SpinningCDAnimation with continuous Animatable. LyricsView scroll and scale.
- HomeScreen.kt: Shimmer uses rememberInfiniteTransition. TopAppBar collapses smoothly.
- MainActivity.kt: slideIn/slideOut/fadeIn/fadeOut implemented on NavHost.
- QueueBottomSheet: Drag-to-reorder animations using reorderable.

### Missing animations:
- All resolved.

---

## 11. Haptic Feedback Audit
- **HAS haptic: yes**
  - NowPlayingScreen: Play/Pause (LongPress), Next/Prev (LongPress), Shuffle/Repeat/Show Lyrics/Sleep Timer (TextHandleMove).
- **Missing haptic:**
  - HomeScreen: Song item clicks, Tab selection.
  - MiniPlayer: Play/Pause/Next clicks.
  - NowPlayingScreen: SeekBar dragging, Queue item clicks.

---

## 12. Edge Cases & Empty States
- **Library is empty**: HomeScreen renders EmptyLibrary composable (a MusicNote icon with "Your library is empty. Try rescanning...").
- **A song has no album art**: AlbumArtistCard and SpinningCDAnimation render a dark Box with a MusicNote icon. MiniPlayer renders a MaterialTheme.colorScheme.primary square block.
- **An .lrc file is missing**: The "Show Lyrics" button is completely hidden (if (currentLyrics.isNotEmpty())), preventing access to the empty LyricsView.
- **The device is offline**: ArtworkScraper fails quietly and leaves the art null, causing the UI to gracefully fall back to the MusicNote icon.
- **Permissions are denied**: OnboardingScreen stays on the permission request state with the "Grant Permission & Scan" button permanently visible.

---

## 13. NowPlayingScreen Deep Audit
- **Exact layout structure**: Box (with glowing Canvas background) -> Top bar (Back/Queue icons) -> CD or Lyrics (AnimatedVisibility swap) -> Song Info (Title, Artist, Lyrics/Sleep Timer chips) -> Extra Controls (Sleep timer chip, speed chip) -> Timeline (Slider + elapsed/total times) -> Main Controls (Shuffle, Prev, Play/Pause, Next, Repeat).
- **Lyrics display**: Uses a LazyColumn inside LyricsView. Scrolls automatically to the active lyric using listState.animateScrollToItem(). The active line is styled with Tokens.accentPrimary and 	itleLarge. If no lyrics exist, the "Show Lyrics" toggle button is hidden.
- **Sleep timer**: Surfaced in the UI as a clickable chip in songInfo (next to the lyrics button) and inside extraControls. No visible live countdown, but it displays the selected minute interval (e.g., "15 Minutes") in the chip.
- **Playback speed**: Exposed via a surface chip in extraControls at the bottom. Tapping it opens an AlertDialog to select speeds from 0.5x to 2.0x.
- **Queue bottom sheet**: Triggered by the List icon in the top right. Rendered via ModalBottomSheet. Each item is a row showing title and artist. The currently active song is highlighted with Tokens.glassTint background and bold, accented text.
- **Visually inconsistent**: The dummy MiniPlayer at the bottom of other screens is highly inconsistent with the Midnight Vinyl aesthetic (using Material defaults). The Sleep Timer and Speed dialogs use standard AlertDialog layouts rather than custom bottom sheets, though they correctly use Tokens.bgElevated.

---

## 14. What To Do Next (Agent's Own Assessment)
1. **ui/components/MiniPlayer.kt**: Refactor entirely to use Tokens instead of MaterialTheme, add proper album art rendering instead of a dummy color box, wrap it in an AnimatedVisibility for smooth enter/exit, and remove the dummy Box/Column implementations.
2. **ui/screens/HomeScreen.kt**: Add Modifier.animateItemPlacement() to the LazyVerticalGrid and LazyColumn for smooth list layout changes during searches and tab switching.
3. **ui/screens/HomeScreen.kt**: Replace the inline Color.White.copy(...) hardcoded colors in the search OutlinedTextField with appropriate Tokens (e.g., Tokens.glassTint).
4. **ui/screens/NowPlayingScreen.kt**: Add haptic feedback (HapticFeedbackType.TextHandleMove) to the slider onValueChange and queue list item clicks to standardize the premium tactile feel across all playback controls.
5. **ui/components/VinylDisc.kt**: Delete this legacy file. It has snapping rotation bugs and hardcoded colors, and has already been superseded by the superior SpinningCDAnimation built directly into NowPlayingScreen.kt.

## 15. Remaining Polish Items (not yet implemented)
- MainActivity.kt: The SharedTransitionLayout navigation structure feels solid, but could potentially benefit from predictive back gesture support for a truly native feel.
- DetailScreen.kt: The transition between the grid in HomeScreen and the DetailScreen could use a shared element transition on the Album/Artist thumbnail. Currently, it just slides in.
- PlayerViewModel.kt: Error handling for unplayable files could be more robust (e.g. surfacing a SnackBar if Media3 fails to buffer).


## 16. Performance Notes
- **Recomposition Scoping**: NowPlayingScreen relies heavily on Canvas drawing and Modifier.graphicsLayer for background offsets and scaling to prevent excessive recomposition of layout nodes during animations.
- **State Usage**: StateFlow is leveraged across all ViewModels, minimizing unnecessary re-renders. PaletteExtractor is called off the main thread in LaunchedEffect to avoid blocking the UI during image processing.
- **LazyLists**: QueueBottomSheet and LyricsView use itemsIndexed and rememberLazyListState to optimize scroll performance and distance-aware rendering logic without recalculating positions globally.
