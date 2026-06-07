package com.bibo.android.features.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.core.datastore.AuthLocalData
import com.bibo.android.core.network.ServerAvailabilityMonitor
import com.bibo.android.core.sync.SyncQueue
import com.bibo.android.features.auth.domain.LogoutUseCase
import com.bibo.android.features.auth.domain.ObserveCurrentUserUseCase
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
    val serverAvailable: Boolean = false,
)

class RootViewModel(
    observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    observeThemeUseCase: ObserveThemeUseCase,
    private val setDarkThemeUseCase: SetDarkThemeUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val syncAppDataUseCase: SyncAppDataUseCase,
    private val serverAvailabilityMonitor: ServerAvailabilityMonitor,
) : ViewModel() {
    private val authFlow = observeCurrentUserUseCase()

    val uiState: StateFlow<RootUiState> = combine(
        authFlow,
        observeThemeUseCase(),
        serverAvailabilityMonitor.available,
    ) { auth, theme, serverAvailable ->
        RootUiState(
            auth = auth,
            darkThemeEnabled = theme.darkThemeEnabled,
            serverAvailable = auth.isAuthorized && serverAvailable,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RootUiState(),
    )

    init {
        viewModelScope.launch {
            authFlow
                .map { auth -> auth.userId?.takeIf { auth.isAuthorized } }
                .distinctUntilChanged()
                .collectLatest { userId ->
                    if (userId != null) {
                        runCatching {
                            syncAppDataUseCase()
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

class SyncAppDataUseCase(
    private val syncQueue: SyncQueue,
) {
    suspend operator fun invoke(): Boolean = syncQueue.syncCurrentUser()
}
