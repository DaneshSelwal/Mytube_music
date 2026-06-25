import os
import re

def rewrite_file(filepath, callback):
    if not os.path.exists(filepath): return
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = callback(content)
    if content != new_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")
    else:
        print(f"No changes for {filepath}")

# TASK 1c, 2: HomeScreen.kt
def update_home_screen(content):
    # Remove local MiniPlayer definition completely
    content = re.sub(r'@OptIn\(ExperimentalSharedTransitionApi::class\)\s*@Composable\s*fun SharedTransitionScope\.MiniPlayer\(.*?^}', '', content, flags=re.MULTILINE | re.DOTALL)
    
    # 2a: Search bar
    content = content.replace('Color.White.copy(alpha = 0.1f)', 'Tokens.bgSurface')
    content = content.replace('Color.White.copy(alpha = 0.5f)', 'Tokens.textSecondary')
    content = content.replace('Color.White', 'Tokens.accentPrimary', 1) # assuming cursor color is next? Actually let's just do targeted replace:
    content = re.sub(r'focusedContainerColor = Color\.White\.copy\(alpha = 0\.1f\)', 'focusedContainerColor = Tokens.bgSurface', content)
    content = re.sub(r'unfocusedContainerColor = Color\.White\.copy\(alpha = 0\.05f\)', 'unfocusedContainerColor = Tokens.bgSurface', content)
    content = re.sub(r'placeholder = \{ Text\(".*?", color = Color\.White\.copy\(alpha = 0\.5f\)\) \}', 'placeholder = { Text("Search songs or artists...", color = Tokens.textSecondary) }', content)
    content = re.sub(r'cursorColor = Color\.White', 'cursorColor = Tokens.accentPrimary', content)
    
    # 2b: SectionHeader
    content = re.sub(r'fontSize = 18\.sp, fontWeight = FontWeight\.Bold', 'style = MyTubeTypography.titleMedium', content)
    
    # 2c: Animate list items
    # For Songs (itemsIndexed)
    content = re.sub(r'(SongItem\(song = song, isCurrent = isCurrent, )(onClick = \{)', r'\1modifier = Modifier.animateItem(), \2', content)
    # For Albums and Artists (AlbumArtistCard)
    # Actually wait, AlbumArtistCard doesn't take modifier natively in our invocation? Let's check. If not, wrap or pass.
    # It's easier to just do modifier = Modifier.animateItem() if supported. I'll inject it.
    content = re.sub(r'(AlbumArtistCard\(title = album.*?)( \{)', r'\1, modifier = Modifier.animateItem()\2', content)
    content = re.sub(r'(AlbumArtistCard\(title = artist.*?)( \{)', r'\1, modifier = Modifier.animateItem()\2', content)
    
    # 2d & 2e: Haptics
    # Needs LocalHapticFeedback import
    if 'LocalHapticFeedback' not in content:
        content = content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.LocalHapticFeedback\nimport androidx.compose.ui.hapticfeedback.HapticFeedbackType')
    
    # Inside HomeScreen:
    content = re.sub(r'val backgroundBrush = Brush\.verticalGradient', 'val haptic = LocalHapticFeedback.current\n    val backgroundBrush = Brush.verticalGradient', content)
    content = re.sub(r'onTabSelect = \{ selectedTabIndex = it \}', 'onTabSelect = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedTabIndex = it }', content)
    
    # In SongItem:
    content = re.sub(r'fun SongItem\(song: Song, isCurrent: Boolean, onClick: \(\) -> Unit\)', 'fun SongItem(modifier: Modifier = Modifier, song: Song, isCurrent: Boolean, onClick: () -> Unit)', content)
    content = re.sub(r'Row\(\n\s*modifier = Modifier', 'val haptic = LocalHapticFeedback.current\n    Row(\n        modifier = modifier', content)
    content = re.sub(r'\.clickable \{ onClick\(\) \}', '.clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }', content)
    
    # Task 1c: Wrap MiniPlayer in AnimatedVisibility
    mini_player_call = '''androidx.compose.animation.AnimatedVisibility(
                    visible = currentSong != null,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)),
                    exit  = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
                ) {
                    com.mark1.mytubemusic.ui.components.MiniPlayer(
                        animatedVisibilityScope = animatedVisibilityScope,
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        progress = progress.toFloat(),
                        duration = duration,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.skipToNext() },
                        onPrev = { playerViewModel.skipToPrevious() },
                        onClick = onNavigateToNowPlaying
                    )
                }'''
    # We remove if (currentSong != null) { ... Box ... } and replace with Box + AnimatedVis
    content = re.sub(r'if \(currentSong != null\) \{\s*Box\(\s*modifier = Modifier\s*\.align\(Alignment\.BottomEnd\)\s*\.padding\(bottom = 24\.dp, end = 24\.dp\)\s*\) \{\s*MiniPlayer\([^)]+\)\s*\}\s*\}',
        f'Box(\n                    modifier = Modifier\n                        .align(Alignment.BottomEnd)\n                        .padding(bottom = 24.dp, end = 24.dp)\n                ) {{\n                    {mini_player_call}\n                }}', content)
        
    return content

rewrite_file('app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt', update_home_screen)

# TASK 1c: DetailScreen.kt
def update_detail_screen(content):
    if 'LocalHapticFeedback' not in content:
        content = content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.LocalHapticFeedback\nimport androidx.compose.ui.hapticfeedback.HapticFeedbackType')
        
    mini_player_call = '''androidx.compose.animation.AnimatedVisibility(
                    visible = currentSong != null,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)),
                    exit  = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
                ) {
                    com.mark1.mytubemusic.ui.components.MiniPlayer(
                        animatedVisibilityScope = animatedVisibilityScope,
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        progress = progress.toFloat(),
                        duration = duration,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.skipToNext() },
                        onPrev = { playerViewModel.skipToPrevious() },
                        onClick = onNavigateToNowPlaying
                    )
                }'''
    content = re.sub(r'if \(currentSong != null\) \{\s*Box\(\s*modifier = Modifier\s*\.align\(Alignment\.BottomEnd\)\s*\.padding\(bottom = 24\.dp, end = 24\.dp\)\s*\) \{\s*MiniPlayer\([^)]+\)\s*\}\s*\}',
        f'Box(\n                    modifier = Modifier\n                        .align(Alignment.BottomEnd)\n                        .padding(bottom = 24.dp, end = 24.dp)\n                ) {{\n                    {mini_player_call}\n                }}', content)
    return content

rewrite_file('app/src/main/java/com/mark1/mytubemusic/ui/screens/DetailScreen.kt', update_detail_screen)

# TASK 3: NowPlayingScreen.kt
def update_now_playing(content):
    if 'LocalHapticFeedback' not in content:
        content = content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.LocalHapticFeedback\nimport androidx.compose.ui.hapticfeedback.HapticFeedbackType')
    
    content = content.replace('Color.DarkGray', 'Tokens.bgDeep')
    content = content.replace('Color.Black', 'Tokens.bgDeep')
    content = content.replace('Color.White.copy(alpha = 0.5f)', 'Tokens.textDisabled.copy(alpha = 0.3f)')
    
    content = re.sub(r'fontSize = 12\.sp', 'style = MyTubeTypography.labelSmall', content)
    content = re.sub(r'fontWeight = FontWeight\.Bold', 'fontWeight = FontWeight.SemiBold', content)
    
    # Haptic to SeekBar
    content = re.sub(r'onValueChange = \{\s*isSeeking = true\s*seekProgress = it\s*\}', r'onValueChange = { \n                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)\n                isSeeking = true\n                seekProgress = it \n            }', content)
    
    # Haptic to Queue item
    content = re.sub(r'\.clickable \{\s*playerViewModel\.playQueue\(queue, index\)\s*\}', r'.clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); playerViewModel.playQueue(queue, index) }', content)
    
    # Sleep Timer live countdown
    # Find al sleepTimerText by playerViewModel.sleepTimerText.collectAsState()
    if 'sleepTimerText by' not in content:
        content = re.sub(r'val currentLyrics by playerViewModel\.currentLyrics\.collectAsState\(\)', r'val currentLyrics by playerViewModel.currentLyrics.collectAsState()\n    val sleepTimerText by playerViewModel.sleepTimerText.collectAsState()', content)
        
    old_sleep_chip = r'AssistChip\(\s*onClick = \{ showSleepTimerDialog = true \},\s*label = \{ Text\("Sleep", style = MyTubeTypography.labelSmall\) \},.*?colors = AssistChipDefaults.assistChipColors\(containerColor = Tokens.bgElevated\)\s*\)'
    new_sleep_chip = '''if (sleepTimerText != null) {
                AssistChip(
                    onClick = { playerViewModel.cancelSleepTimer() },
                    label = { Text(text = sleepTimerText!!, style = MyTubeTypography.labelSmall.copy(color = Tokens.accentSecondary)) },
                    leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp), tint = Tokens.accentSecondary) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Tokens.bgElevated, labelColor = Tokens.accentSecondary),
                    border = AssistChipDefaults.assistChipBorder(borderColor = Tokens.accentSecondary.copy(alpha = 0.3f))
                )
            } else {
                AssistChip(
                    onClick = { showSleepTimerDialog = true },
                    label = { Text("Sleep", style = MyTubeTypography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Tokens.bgElevated)
                )
            }'''
    content = re.sub(old_sleep_chip, new_sleep_chip, content, flags=re.DOTALL)
    
    # Ensure haptic variable exists inside NowPlayingScreen
    if 'val haptic =' not in content:
        content = re.sub(r'fun SharedTransitionScope\.NowPlayingScreen\([^)]+\)\s*\{', r'\g<0>\n    val haptic = LocalHapticFeedback.current', content)
    return content

rewrite_file('app/src/main/java/com/mark1/mytubemusic/ui/screens/NowPlayingScreen.kt', update_now_playing)

# TASK 4: GlassCard.kt
def update_glass_card(content):
    return content.replace('Color.White.copy(alpha = 0.08f)', 'Tokens.glassTint')
rewrite_file('app/src/main/java/com/mark1/mytubemusic/ui/components/GlassCard.kt', update_glass_card)

# TASK 5: PlayerViewModel.kt (Sleep Timer live countdown)
def update_viewmodel(content):
    # Find setSleepTimer method
    old_sleep = r'delay\(minutes \* 60 \* 1000L\)\s*pause\(\)\s*_sleepTimerText\.value = null'
    new_sleep = '''var remainingMs = minutes * 60 * 1000L
            while (remainingMs > 0) {
                val mins = remainingMs / 60000
                val secs = (remainingMs % 60000) / 1000
                _sleepTimerText.value = "%d:%02d remaining".format(mins, secs)
                kotlinx.coroutines.delay(1000L)
                remainingMs -= 1000L
            }
            pause()
            _sleepTimerText.value = null'''
    content = re.sub(old_sleep, new_sleep, content)
    
    # Add cancel method
    if 'fun cancelSleepTimer()' not in content:
        content = re.sub(r'fun setSleepTimer.*?\}', r'\g<0>\n\n    fun cancelSleepTimer() {\n        sleepTimerJob?.cancel()\n        _sleepTimerText.value = null\n    }', content, flags=re.DOTALL)
    return content
rewrite_file('app/src/main/java/com/mark1/mytubemusic/viewmodel/PlayerViewModel.kt', update_viewmodel)

