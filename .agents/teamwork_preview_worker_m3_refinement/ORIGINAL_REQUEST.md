## 2026-06-27T07:22:49Z
You are the Milestone 3 Refinement Worker. Your task is to resolve the Audio Overlap bug reported by Reviewer 1.

## Objective
Fix the issue where local audio does not pause when navigating to the "Browser" tab. This happens because the `BrowserScreen` composable is persistent in the view hierarchy (using `Modifier.offset(x = 10000.dp)` when inactive) to keep its memory state alive. The `LaunchedEffect(Unit)` within `BrowserScreen.kt` only triggers once on initial startup and does not fire when switching tabs.

## Working Directory
Your working directory for metadata (progress and handoff) is:
C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m3_refinement

## Instructions
1. **Modify `BrowserScreen.kt`** (`app/src/main/java/com/mark1/mytubemusic/ui/screens/BrowserScreen.kt`):
   - Update the `BrowserScreen` function signature to accept an `isActive: Boolean` parameter:
     `fun BrowserScreen(isActive: Boolean, playerViewModel: PlayerViewModel, modifier: Modifier = Modifier)`
   - Update the `LaunchedEffect` block to observe changes to `isActive`. When `isActive` is true, pause the local media player:
     ```kotlin
     LaunchedEffect(isActive) {
         if (isActive) {
             playerViewModel.player?.let {
                 if (it.isPlaying) {
                     it.pause()
                 }
             }
         }
     }
     ```

2. **Modify `HomeScreen.kt`** (`app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt`):
   - Locate where `BrowserScreen(...)` is instantiated (around lines 628-636).
   - Pass the parameter `isActive = selectedTabIndex == 5` to it.

3. **Verify and Build**:
   - Verify that the project builds and runs unit tests.
   - Run compilation command: `.\gradlew.bat compileDebugSources` (or `.\gradlew.bat assembleDebug`)
   - Run unit test command: `.\gradlew.bat test`
   - Document commands and results in your handoff report.

4. **Report Completion**:
   - Create a `handoff.md` file in your working directory `C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m3_refinement` summarizing the changes, verification results, and any details.
   - Send a message back to the parent once done, referencing the path to your handoff file.
   - Update `.agents/teamwork_preview_worker_m3_refinement/progress.md` with status checks as you progress.

MANDATORY INTEGRITY WARNING:
> DO NOT CHEAT. All implementations must be genuine. DO NOT
> hardcode test results, create dummy/facade implementations, or
> circumvent the intended task. A Forensic Auditor will independently
> verify your work. Integrity violations WILL be detected and your
> work WILL be rejected.
