package com.bibo.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.bibo.android.core.navigation.AppRoot
import com.bibo.android.features.main.presentation.RootViewModel
import com.bibo.android.ui.theme.BIBOTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // The timer still works if notifications are denied; the service starts with a best-effort notification.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            val rootViewModel: RootViewModel = koinViewModel()
            val uiState by rootViewModel.uiState.collectAsState()

            BIBOTheme(darkTheme = uiState.darkThemeEnabled) {
                AppRoot(rootViewModel = rootViewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun PreviewContent() {
    BIBOTheme {
        MainActivityPreviewStub()
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PreviewContent()
}

@Composable
private fun MainActivityPreviewStub() {
    androidx.compose.material3.Text("BIBO")
}
