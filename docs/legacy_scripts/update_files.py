import re

# Read current context.md
with open('context.md', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove old Section 9 and 10
content = re.split(r'## 9\. Implementation Status', content)[0].strip()

new_content = '''
## 9. Implementation Status (Full Audit)

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
- NowPlayingScreen.kt: Background mesh uses nimateColorAsState for palette transitions; pulseScale uses nimateFloatAsState for beat-matching. SpinningCDAnimation uses Animatable.animateTo for smooth startup and slow-down. AnimatedVisibility (fade) for swapping Lyrics and CD. Shared element transition (sharedElement) on the CD art.
- HomeScreen.kt: Shimmer uses ememberInfiniteTransition with nimateFloat.
- VinylDisc.kt: Rotation uses ememberInfiniteTransition with nimateFloat.

### Missing animations:
- **NowPlayingScreen background**: Yes, it animates smoothly between two palette colors on track change (nimateColorAsState).
- **VinylDisc**: No, it uses an infinite transition that resets/snaps when playback pauses, unlike the custom SpinningCDAnimation in NowPlaying.
- **MiniPlayer**: Missing AnimatedVisibility enter/exit transitions; it simply mounts/unmounts instantly based on if (currentSong != null).
- **HomeScreen**: List items do not animate in (missing nimateItemPlacement or staggered fades).
- **Navigation**: Nav graph enter/exit transitions are defined in MainActivity with SharedTransitionScope providing shared element morphs.

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
'''

with open('context.md', 'w', encoding='utf-8') as f:
    f.write(content + '\n\n' + new_content.strip())

readme_content = '''# MyTube Music

**MyTube Music** is a premium, locally-hosted Android music player built with Jetpack Compose. It features a bespoke "Midnight Vinyl" design system, leveraging warm-tinted dark themes, glassmorphism, fluid animations, and rich haptics to deliver a high-end audio experience.

## ? Features

- **Midnight Vinyl Aesthetic**: A carefully crafted dark mode design using deep purples, warm surfaces, and sky blue accents.
- **Dynamic Now Playing**: A stunning playback screen featuring a beat-matching pulse background, palette-extracted ambient glows, and a mathematically accurate spinning vinyl record.
- **Synchronized Lyrics**: Built-in .lrc parsing to display scrolling, karaoke-style lyrics.
- **Smart Library Management**: Automatically scans your device for .mp3 files, grouping them by Artists and Albums.
- **Automated Album Art**: Seamlessly scrapes Google Images in the background to fetch missing album covers.
- **Premium Interactions**: Integrated haptic feedback and shared element transitions for a fluid, tactile UX.

## ?? Tech Stack

- **UI**: Jetpack Compose (BOM 2024.10.00)
- **Architecture**: MVVM
- **Playback**: Media3 (ExoPlayer) & MediaSessionService
- **Database**: Room
- **Image Loading**: Coil
- **Utilities**: AndroidX Palette, Jsoup, Coroutines

## ?? Design Tokens

MyTube Music relies on a strict color token system to maintain its identity:
- gDeep (#0D0B0F) - Core background
- gSurface (#161218) - Cards and sheets
- ccentPrimary (#B08DFF) - Primary interactive elements

## ?? Getting Started

1. Clone the repository.
2. Open the project in Android Studio (Jellyfish or later).
3. Build and run on a device running Android 8.0 (API 26) or higher.
'''

with open('README.md', 'w', encoding='utf-8') as f:
    f.write(readme_content)
