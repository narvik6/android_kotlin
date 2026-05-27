package com.bibo.android.features.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.core.datastore.AuthLocalData
import com.bibo.android.features.auth.domain.LogoutUseCase
import com.bibo.android.features.auth.domain.ObserveCurrentUserUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RootUiState(
    val auth: AuthLocalData = AuthLocalData(token = null, userId = null, email = null),
    val darkThemeEnabled: Boolean = false,
)

class RootViewModel(
    observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    observeThemeUseCase: ObserveThemeUseCase,
    private val setDarkThemeUseCase: SetDarkThemeUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    val uiState: StateFlow<RootUiState> = combine(
        observeCurrentUserUseCase(),
        observeThemeUseCase(),
    ) { auth, theme ->
        RootUiState(auth = auth, darkThemeEnabled = theme.darkThemeEnabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RootUiState(),
    )

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            setDarkThemeUseCase(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
