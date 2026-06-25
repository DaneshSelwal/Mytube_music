import re

# 1. GlassCard.kt - import Tokens
with open('app/src/main/java/com/mark1/mytubemusic/ui/components/GlassCard.kt', 'r', encoding='utf-8') as f:
    gc = f.read()
if 'import com.mark1.mytubemusic.ui.theme.Tokens' not in gc:
    gc = gc.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport com.mark1.mytubemusic.ui.theme.Tokens')
with open('app/src/main/java/com/mark1/mytubemusic/ui/components/GlassCard.kt', 'w', encoding='utf-8') as f:
    f.write(gc)

# 2. DetailScreen.kt - import MiniPlayer
with open('app/src/main/java/com/mark1/mytubemusic/ui/screens/DetailScreen.kt', 'r', encoding='utf-8') as f:
    ds = f.read()
# We don't necessarily need to import if we use fully qualified names, but wait, the regex used com.mark1.mytubemusic.ui.components.MiniPlayer!
# Why did it say unresolved reference if it's fully qualified? 
# Ah, the MiniPlayer is an extension function on SharedTransitionScope.
# Wait, if we call MiniPlayer(...) inside AnimatedVisibility which is inside Box, we are inside SharedTransitionScope?
# In DetailScreen.kt, the root might be SharedTransitionScope.
# Let's import MiniPlayer anyway.
if 'import com.mark1.mytubemusic.ui.components.MiniPlayer' not in ds:
    ds = ds.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport com.mark1.mytubemusic.ui.components.MiniPlayer')
# Wait, if it's fully qualified, Kotlin doesn't allow fully qualified extension functions easily without import? Actually you can't fully qualify an extension function call easily!
ds = ds.replace('com.mark1.mytubemusic.ui.components.MiniPlayer(', 'MiniPlayer(')
with open('app/src/main/java/com/mark1/mytubemusic/ui/screens/DetailScreen.kt', 'w', encoding='utf-8') as f:
    f.write(ds)

# 3. HomeScreen.kt - line 111 modifier unresolved, and MiniPlayer import
with open('app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt', 'r', encoding='utf-8') as f:
    hs = f.read()
if 'import com.mark1.mytubemusic.ui.components.MiniPlayer' not in hs:
    hs = hs.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport com.mark1.mytubemusic.ui.components.MiniPlayer')
hs = hs.replace('com.mark1.mytubemusic.ui.components.MiniPlayer(', 'MiniPlayer(')

# What is on line 111? Let's check the area.
# In my previous refactor script I did: content = re.sub(r'Row\(\n\s*modifier = Modifier', 'val haptic = LocalHapticFeedback.current\n    Row(\n        modifier = modifier', content)
# Wait! I might have matched the FIRST Row in the file which is NOT in SongItem!
# Let's fix line 111 in HomeScreen.kt. We will just use regex to fix it, but let's read the file first to be safe, or just undo that generic replace.
