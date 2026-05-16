package com.example.module6_t3.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.module6_t3.AuthApp
import com.example.module6_t3.domain.model.User
import com.example.module6_t3.domain.usecase.GetUserByIdUseCase
import com.example.module6_t3.domain.usecase.GetUsersUseCase
import com.example.module6_t3.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UsersUiState {
    data object Loading : UsersUiState
    data class Success(val users: List<User>) : UsersUiState
    data class Error(val message: String) : UsersUiState
}

sealed interface UserDetailUiState {
    data object Loading : UserDetailUiState
    data class Success(val user: User) : UserDetailUiState
    data class Error(val message: String) : UserDetailUiState
}

class UsersViewModel(
    private val getUsersUseCase: GetUsersUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _listState = MutableStateFlow<UsersUiState>(UsersUiState.Loading)
    val listState: StateFlow<UsersUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<UserDetailUiState>(UserDetailUiState.Loading)
    val detailState: StateFlow<UserDetailUiState> = _detailState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _listState.value = UsersUiState.Loading
            getUsersUseCase().fold(
                onSuccess = { users -> _listState.value = UsersUiState.Success(users) },
                onFailure = { error -> _listState.value = UsersUiState.Error(error.message ?: "Ошибка сети") }
            )
        }
    }

    fun loadUserDetail(id: Int) {
        viewModelScope.launch {
            _detailState.value = UserDetailUiState.Loading
            getUserByIdUseCase(id).fold(
                onSuccess = { user -> _detailState.value = UserDetailUiState.Success(user) },
                onFailure = { error -> _detailState.value = UserDetailUiState.Error(error.message ?: "Не удалось загрузить профиль") }
            )
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLogoutComplete()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AuthApp)
                UsersViewModel(
                    application.container.getUsersUseCase,
                    application.container.getUserByIdUseCase,
                    application.container.logoutUseCase
                )
            }
        }
    }
}