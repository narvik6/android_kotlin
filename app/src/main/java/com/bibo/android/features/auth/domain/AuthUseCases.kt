package com.bibo.android.features.auth.domain

import com.bibo.android.core.datastore.AuthLocalData
import com.bibo.android.core.util.AppResult
import kotlinx.coroutines.flow.Flow

class LoginUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): AppResult<User> =
        repository.login(email.trim(), password)
}

class RegisterUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): AppResult<User> =
        repository.register(email.trim(), password)
}

class LogoutUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.logout()
}

class ObserveCurrentUserUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthLocalData> = repository.observeCurrentUser()
}
