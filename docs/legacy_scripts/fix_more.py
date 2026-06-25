import re

# 1. HomeScreen.kt line 111 modifier
with open('app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt', 'r', encoding='utf-8') as f:
    hs = f.read()

# Fix ShimmerSongItem
hs = hs.replace('''fun ShimmerSongItem() {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier''', '''fun ShimmerSongItem() {
    Row(
        modifier = Modifier''')

with open('app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt', 'w', encoding='utf-8') as f:
    f.write(hs)

# 2. MiniPlayer.kt albumArtUri -> uri
with open('app/src/main/java/com/mark1/mytubemusic/ui/components/MiniPlayer.kt', 'r', encoding='utf-8') as f:
    mp = f.read()

mp = mp.replace('song.albumArtUri', 'song.uri')

with open('app/src/main/java/com/mark1/mytubemusic/ui/components/MiniPlayer.kt', 'w', encoding='utf-8') as f:
    f.write(mp)
