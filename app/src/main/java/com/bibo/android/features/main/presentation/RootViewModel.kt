package com.bibo.android.features.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.core.datastore.AuthLocalData
import com.bibo.android.features.auth.domain.LogoutUseCase
import com.bibo.android.features.auth.domain.ObserveCurrentUserUseCase
import com.bibo.android.features.diary.domain.SyncPendingChangesUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val syncPendingChangesUseCase: SyncPendingChangesUseCase,
) : ViewModel() {
    private val authFlow = observeCurrentUserUseCase()

    val uiState: StateFlow<RootUiState> = combine(
        authFlow,
        observeThemeUseCase(),
    ) { auth, theme ->
        RootUiState(auth = auth, darkThemeEnabled = theme.darkThemeEnabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RootUiState(),
    )

    init {
        viewModelScope.launch {
            authFlow
                .map { it.isAuthorized }
                .distinctUntilChanged()
                .collectLatest { authorized ->
                    if (authorized) {
                        while (true) {
                            syncPendingChangesUseCase()
                            delay(60_000)
                        }
                    }
                }
        }
    }

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
