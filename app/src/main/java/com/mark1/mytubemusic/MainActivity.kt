package com.mark1.mytubemusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.mark1.mytubemusic.data.db.AppDatabase
import com.mark1.mytubemusic.repository.SongRepository
import com.mark1.mytubemusic.ui.screens.HomeScreen
import com.mark1.mytubemusic.ui.screens.NowPlayingScreen
import com.mark1.mytubemusic.ui.screens.OnboardingScreen
import com.mark1.mytubemusic.ui.screens.DetailScreen
import com.mark1.mytubemusic.ui.theme.MyTubeMusicTheme
import com.mark1.mytubemusic.viewmodel.LibraryViewModel
import com.mark1.mytubemusic.viewmodel.PlayerViewModel
import com.mark1.mytubemusic.util.ShakeDetector

class MainActivity : ComponentActivity() {
    private var shakeDetector: ShakeDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mytube_music_db"
        ).build()
        val repository = SongRepository(database.songDao())
        
        val libraryViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(repository) as T
            }
        })[LibraryViewModel::class.java]

        val playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
        playerViewModel.initialize(this)
        
        shakeDetector = ShakeDetector(this) {
            playerViewModel.skipToNext()
        }

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        setContent {
            MyTubeMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(libraryViewModel, playerViewModel, if (hasPermission) "home" else "onboarding")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeDetector?.start()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.stop()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(libraryViewModel: LibraryViewModel, playerViewModel: PlayerViewModel, startDestination: String = "onboarding") {
    val navController = rememberNavController()
    SharedTransitionLayout {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("onboarding") {
                OnboardingScreen(
                    viewModel = libraryViewModel,
                    onComplete = {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    animatedVisibilityScope = this@composable,
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    onNavigateToNowPlaying = {
                        navController.navigate("now_playing")
                    },
                    onNavigateToDetail = {
                        navController.navigate("detail")
                    }
                )
            }
            composable("now_playing") {
                NowPlayingScreen(
                    animatedVisibilityScope = this@composable,
                    playerViewModel = playerViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToQueue = { /* No-op since we use bottom sheet locally */ }
                )
            }
            composable("detail") {
                DetailScreen(
                    animatedVisibilityScope = this@composable,
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToNowPlaying = { navController.navigate("now_playing") }
                )
            }
        }
    }
}
