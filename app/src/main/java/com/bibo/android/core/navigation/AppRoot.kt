package com.bibo.android.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
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
        val userId = uiState.auth.userId ?: return
        key(userId) {
            MainScreen(
                userScopeKey = userId,
                darkThemeEnabled = uiState.darkThemeEnabled,
                serverAvailable = uiState.serverAvailable,
                onDarkThemeChange = rootViewModel::setDarkTheme,
                onLogout = rootViewModel::logout,
            )
        }
    } else {
        AuthNavHost()
    }
}
