package com.bibo.android.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.bibo.android.features.auth.presentation.AuthNavHost
import com.bibo.android.features.main.presentation.MainScreen
import com.bibo.android.features.main.presentation.RootViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot(
    rootViewModel: RootViewModel = koinViewModel(),
) {
    val uiState by rootViewModel.uiState.collectAsState()

    if (uiState.auth.isAuthorized) {
        MainScreen(
            email = uiState.auth.email.orEmpty(),
            darkThemeEnabled = uiState.darkThemeEnabled,
            onDarkThemeChange = rootViewModel::setDarkTheme,
            onLogout = rootViewModel::logout,
        )
    } else {
        AuthNavHost()
    }
}
