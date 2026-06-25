<div align="center">

<img src="app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" width="100" height="100" alt="MyTube Music Logo"/>

# MyTube Music

**A premium local music player for Android — built with Jetpack Compose and Material 3.**

*Your local library. Nothing else.*

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/min%20SDK-26%20(Android%208.0)-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)

[Download APK](#installation) · [Features](#features) · [Screenshots](#screenshots) · [Build](#building-from-source)

</div>

---

## Features

<table>
<tr>
<td width="50%">

**Playback**
- Background playback via Media3 + ExoPlayer
- Shake-to-skip (accelerometer)
- Sleep timer with live countdown
- Adjustable playback speed (0.5× – 2.0×)
- Shuffle and repeat modes (Off / All / One)
- Swipe left/right to skip tracks

</td>
<td width="50%">

**Library**
- Scans local `/Music/` directory automatically
- Tabs for Songs, Albums, and Artists
- Search across your entire library
- Album and artist detail views
- Room database for fast access

</td>
</tr>
<tr>
<td width="50%">

**Now Playing**
- Spinning vinyl record animation
- Pulsing mesh gradient background (palette-synced)
- Synced karaoke-style lyrics (.lrc files)
- Drag-to-reorder queue
- Swipe down to dismiss

</td>
<td width="50%">

**Design**
- "Midnight Vinyl" dark theme
- DM Serif Display + Inter typography
- Glassmorphism MiniPlayer
- Shimmer loading states
- Haptic feedback throughout

</td>
</tr>
</table>

---

## Screenshots

> Screenshots coming soon. Build and run the app to see it in action.

<!-- To add screenshots: place images in docs/screenshots/ and uncomment below
<div align="center">
<img src="docs/screenshots/home.png" width="200"/>
<img src="docs/screenshots/now_playing.png" width="200"/>
<img src="docs/screenshots/lyrics.png" width="200"/>
<img src="docs/screenshots/queue.png" width="200"/>
</div>
-->

---

## Installation

### Option 1 — Download APK (Recommended for most users)

1. [Download MyTube Music APK directly](https://github.com/DaneshSelwal/Mytube_music/raw/main/MyTubeMusic.apk)
2. Save the `MyTubeMusic.apk` file to your device
3. On your Android device, go to **Settings → Security → Install unknown apps**
   and allow installation from your browser or file manager
4. Open the downloaded APK and tap **Install**
5. Launch **MyTube Music** and grant storage permission when prompted
6. Place your `.mp3` files in the `/Music/` folder on your device

> **Minimum requirement**: Android 8.0 (API 26) or higher

### Option 2 — Build from source

See the [Building from Source](#building-from-source) section below.

---

## Adding Music

MyTube Music reads from the `/Music/` directory on your device's internal storage.

**Via file manager**: Copy `.mp3` files directly into `/sdcard/Music/`

**Via ADB** (for developers):
```bash
adb push your_song.mp3 /sdcard/Music/
```

**Lyrics support**: Place a `.lrc` file with the same name as your song in the same directory:
```
/sdcard/Music/
├── Bohemian Rhapsody.mp3
└── Bohemian Rhapsody.lrc   ← synced lyrics file
```

---

## Building from Source

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with API 34 installed
- A physical Android device or emulator running API 26+

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/YOUR_USERNAME/MyTubeMusic.git
cd MyTubeMusic
```

**2. Open in Android Studio**

Open the project folder in Android Studio and let Gradle sync complete.

**3. Build and run**
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or use the **Run** button in Android Studio with a connected device.

### Build scripts (Windows)

The project includes convenience scripts:
```
run_build.bat       — builds the APK via Gradle
install_and_run.bat — installs and launches on connected device via ADB
```

### Setting up release signing (for maintainers)

To enable automatic signed APK releases via GitHub Actions:

1. Generate a keystore if you don't have one:
```bash
keytool -genkey -v -keystore mytubemusic.jks   -keyalg RSA -keysize 2048 -validity 10000   -alias mytubemusic
```

2. Base64-encode the keystore file:
```bash
# macOS / Linux:
base64 -i mytubemusic.jks | pbcopy

# Windows (PowerShell):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("mytubemusic.jks")) | clip
```

3. In your GitHub repository go to:
   **Settings → Secrets and variables → Actions → New repository secret**

   Add these four secrets:

   | Secret name | Value |
   |---|---|
   | `SIGNING_KEY` | The base64 string from step 2 |
   | `KEY_ALIAS` | The alias you chose (e.g. `mytubemusic`) |
   | `KEY_STORE_PASSWORD` | Your keystore password |
   | `KEY_PASSWORD` | Your key password |

4. Push a version tag to trigger a release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

The APK will appear automatically on the GitHub Releases page.

---

## Architecture

```
┌─────────────────────────────────────────┐
│              UI Layer                   │
│  Jetpack Compose · Material 3           │
│  HomeScreen · NowPlayingScreen          │
│  DetailScreen · OnboardingScreen        │
└────────────────┬────────────────────────┘
                 │ StateFlow / collectAsState
┌────────────────▼────────────────────────┐
│           ViewModel Layer               │
│  LibraryViewModel · PlayerViewModel     │
└────────┬───────────────┬────────────────┘
         │               │
┌────────▼──────┐  ┌─────▼──────────────┐
│  Repository   │  │  MediaController   │
│  Room DB      │  │  (Media3)          │
│  MediaStore   │  └─────┬──────────────┘
└───────────────┘        │
                  ┌──────▼──────────────┐
                  │   MusicService      │
                  │   ExoPlayer         │
                  │   MediaSession      │
                  └─────────────────────┘
```

**Stack**: Kotlin · Jetpack Compose · Media3 (ExoPlayer + MediaSession) · Room · Colab · Jsoup

---

## Project Structure

```
app/src/main/java/com/mark1/mytubemusic/
├── data/
│   ├── db/          # Room database (AppDatabase, SongDao)
│   └── model/       # Data entities (Song, Album, Artist)
├── repository/      # SongRepository — single source of truth
├── service/         # MusicService (ExoPlayer + MediaSession)
├── ui/
│   ├── components/  # GlassCard, MiniPlayer, VinylDisc
│   ├── screens/     # All screen composables
│   └── theme/       # Color tokens, typography, Material theme
├── util/            # ArtworkScraper, PaletteExtractor,
│                    # ShakeDetector, LrcParser, Extensions
└── viewmodel/       # LibraryViewModel, PlayerViewModel
```

---

## Permissions

| Permission | Reason |
|---|---|
| `READ_MEDIA_AUDIO` (Android 13+) | Read music files from device storage |
| `READ_EXTERNAL_STORAGE` (Android 12 and below) | Read music files from device storage |
| `FOREGROUND_SERVICE` | Keep music playing when app is in background |
| `VIBRATE` | Haptic feedback on controls |
| `INTERNET` | Fetch missing album art from the web |

---

## Contributing

Contributions are welcome. To contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m "Add: your feature description"`
4. Push to your branch: `git push origin feature/your-feature-name`
5. Open a Pull Request

Please make sure the project builds without errors before submitting.

---

## License

```
MIT License

Copyright (c) 2025 Danesh Selwal

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```
