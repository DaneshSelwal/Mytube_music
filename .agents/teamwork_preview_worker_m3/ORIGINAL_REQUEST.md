## 2026-06-27T04:03:00Z
You are the Milestone 3 Worker. Your task is to implement the YouTube Music WebView Browser.

## Objective
Implement an in-app WebView browser tab that loads the mobile YouTube Music site (`https://music.youtube.com`), supports background media playback, and includes a floating "Download Current Song" button.

## Working Directory
Your working directory for metadata (progress and handoff) is:
C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m3

## Instructions
1. **Implement `BrowserScreen.kt`**:
   - Create the file `app/src/main/java/com/mark1/mytubemusic/ui/screens/BrowserScreen.kt`.
   - Use the proposed screen in `C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_explorer_m3\proposed_BrowserScreen.kt` as reference.
   - Note: Make sure to import `androidx.compose.ui.viewinterop.AndroidView` for the `AndroidView` composable used in the screen. Make sure all other imports and classes compile correctly.

2. **Modify `HomeScreen.kt`**:
   - Apply changes to `app/src/main/java/com/mark1/mytubemusic/ui/screens/HomeScreen.kt` to integrate the "Browser" tab.
   - Use the proposed patch at `C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_explorer_m3\proposed_HomeScreen.patch` as reference.
   - **Crucial Index Fix**: In the explorer's patch, the check to offset the WebView tab when inactive contains a bug: it checks `if (selectedTabIndex == 6)` to render the screen. Since "Browser" is the 6th tab in a 0-indexed list (`"Songs", "Albums", "Artists", "Playlists", "Online", "Browser"`), its index is `5`. Replace the hardcoded `6` with `5` or `tabs.indexOf("Browser")` to ensure the tab renders when active.

3. **Verify and Build**:
   - Verify the codebase compiles and passes unit tests.
   - Run compilation command:
     `.\gradlew.bat compileDebugSources` (or `.\gradlew.bat assembleDebug`)
   - Run test command:
     `.\gradlew.bat test`
   - Capture these command outputs/results in your handoff report.

4. **Report Findings**:
   - Create a `handoff.md` file in your working directory `C:\Users\selwa\Desktop\Music App\MyTubeMusic\.agents\teamwork_preview_worker_m3` summarizing the changes, verification results, and any details.
   - Send a message back to the parent once done, referencing the path to your handoff file.
   - Update `.agents/teamwork_preview_worker_m3/progress.md` with status checks as you progress.
