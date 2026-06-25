package com.mark1.mytubemusic.ui.screens

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mark1.mytubemusic.viewmodel.LibraryViewModel
import com.mark1.mytubemusic.ui.theme.Tokens
import com.mark1.mytubemusic.ui.theme.MyTubeTypography

@Composable
fun OnboardingScreen(viewModel: LibraryViewModel, onComplete: () -> Unit) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.scanDevice(context, onComplete)
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
            viewModel.scanDevice(context, onComplete)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Tokens.bgDeep, Tokens.bgSurface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MyTube",
                style = MyTubeTypography.displayLarge.copy(color = Tokens.accentPrimary)
            )
            Text(
                text = "Music",
                style = MyTubeTypography.titleLarge.copy(color = Tokens.textSecondary)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (isScanning) {
                CircularProgressIndicator(color = Tokens.accentPrimary)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Discovering your music...",
                    style = MyTubeTypography.bodyMedium.copy(color = Tokens.textPrimary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = scanProgress,
                    style = MyTubeTypography.labelSmall.copy(color = Tokens.textSecondary),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Welcome to a premium audio experience. We need permission to automatically find the music on your device.",
                    style = MyTubeTypography.bodyMedium.copy(color = Tokens.textSecondary),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { launcher.launch(permissionToRequest) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Tokens.accentPrimary,
                        contentColor = Tokens.bgDeep
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Grant Permission & Scan", style = MyTubeTypography.bodyMedium)
                }
            }
        }
    }
}
