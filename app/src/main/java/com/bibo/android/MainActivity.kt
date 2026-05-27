package com.bibo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.bibo.android.core.navigation.AppRoot
import com.bibo.android.features.main.presentation.RootViewModel
import com.bibo.android.ui.theme.BIBOTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val rootViewModel: RootViewModel = koinViewModel()
            val uiState by rootViewModel.uiState.collectAsState()

            BIBOTheme(darkTheme = uiState.darkThemeEnabled) {
                AppRoot(rootViewModel = rootViewModel)
            }
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
