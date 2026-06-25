# MyTube Music

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
