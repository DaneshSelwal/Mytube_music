package com.mark1.mytubemusic.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

class AuthManager(private val context: Context) {

    // IMPORTANT: Replace with your actual Spotify Client ID
    private val CLIENT_ID = "YOUR_SPOTIFY_CLIENT_ID"
    private val REDIRECT_URI = Uri.parse("mytubemusic://callback")
    
    private val authService = AuthorizationService(context)

    fun getAuthIntent(): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("https://accounts.spotify.com/authorize"),
            Uri.parse("https://accounts.spotify.com/api/token")
        )

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            REDIRECT_URI
        ).setScopes("user-library-read", "playlist-read-private").build()

        return authService.getAuthorizationRequestIntent(authRequest)
    }
    
    fun dispose() {
        authService.dispose()
    }
}
