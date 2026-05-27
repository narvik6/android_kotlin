package com.bibo.android.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.core.util.AppResult
import com.bibo.android.features.auth.domain.LoginUseCase
import com.bibo.android.features.auth.domain.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setEmail(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun setPassword(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun login() {
        submit { email, password -> loginUseCase(email, password) }
    }

    fun register() {
        submit { email, password -> registerUseCase(email, password) }
    }

    private fun submit(
        action: suspend (email: String, password: String) -> AppResult<*>,
    ) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.length < 6) {
            _uiState.update {
                it.copy(error = "Введите email и пароль не короче 6 символов")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = action(state.email, state.password)) {
                is AppResult.Success -> _uiState.update { it.copy(loading = false) }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.error.message)
                }
            }
        }
    }
}
