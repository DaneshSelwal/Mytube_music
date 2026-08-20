# Spotify OAuth Setup

To make the Spotify Import feature work in production, you need to register this application with the Spotify Developer Dashboard.

## 1. Create a Spotify App
1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and log in.
2. Click **Create app**.
3. Set the **App name** to `MyTube Music` (or your preferred name).
4. Set the **App description**.
5. Set the **Redirect URI** to exactly:
   ```
   mytubemusic://callback
   ```
6. Check the terms and click **Save**.

## 2. Update the Source Code
1. In the Dashboard, copy your **Client ID**.
2. Open `app/src/main/java/com/mark1/mytubemusic/service/AuthManager.kt`.
3. Replace the placeholder with your actual Client ID:
   ```kotlin
   private val CLIENT_ID = "YOUR_SPOTIFY_CLIENT_ID" // <- Paste it here
   ```

## 3. How it Works
When a user clicks "Import from Spotify" on the onboarding screen:
1. `AppAuth` launches a secure browser tab to `accounts.spotify.com`.
2. The user logs in and grants permission for the `user-library-read` scope.
3. Spotify redirects the browser back to `mytubemusic://callback`.
4. The Android OS intercepts this URI scheme (configured in `app/build.gradle.kts`) and brings the app back to the foreground.
5. The app can then exchange the code for an Access Token and query the user's liked songs from the Spotify API.

*(Note: The token exchange and API query are currently scaffolded and fall back to the mock generator until the Client ID is populated).*
