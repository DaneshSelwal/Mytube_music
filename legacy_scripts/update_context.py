import re

with open('context.md', 'r', encoding='utf-8') as f:
    content = f.read()

# Update Section 9
content = content.replace(
'''## 9. Implementation Status (Full Audit)''',
'''## 9. Implementation Status (Full Audit)
- **DONE**: Refactor MiniPlayer.kt with Tokens, AsyncImage, AnimatedVisibility, and Haptics.
- **DONE**: Fix inline colors and add list animations in HomeScreen.kt.
- **DONE**: Fix NowPlayingScreen.kt colors and VinylDisc cleanup.
- **DONE**: Fix GlassCard.kt token gap.
- **DONE**: Sleep Timer live countdown.
- **DONE**: Font Integration & util/ Cleanup.
''')

# Update Section 10
content = re.sub(r'## 10\. Blockers / Questions.*?## 11', '## 10. Blockers / Questions\n- **None**: All blockers have been resolved.\n\n## 11', content, flags=re.DOTALL)

# Add Section 15
if '## 15. Remaining Polish Items' not in content:
    content += '''

## 15. Remaining Polish Items (not yet implemented)
- MainActivity.kt: The SharedTransitionLayout navigation structure feels solid, but could potentially benefit from predictive back gesture support for a truly native feel.
- DetailScreen.kt: The transition between the grid in HomeScreen and the DetailScreen could use a shared element transition on the Album/Artist thumbnail. Currently, it just slides in.
- PlayerViewModel.kt: Error handling for unplayable files could be more robust (e.g. surfacing a SnackBar if Media3 fails to buffer).
'''

with open('context.md', 'w', encoding='utf-8') as f:
    f.write(content)
