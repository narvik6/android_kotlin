package com.example.module6_t3.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.module6_t3.AuthApp
import com.example.module6_t3.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, pass: String) {
        if (username.isBlank() || pass.isBlank()) {
            _uiState.value = LoginUiState.Error("Заполните все поля")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            loginUseCase(username, pass).fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(error.message ?: "Ошибка авторизации")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AuthApp)
                LoginViewModel(application.container.loginUseCase)
            }
        }
    }
}