package com.example.module6_t3.domain.usecase

import com.example.module6_t3.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): Result<Unit> {
        return repository.login(username, password)
    }
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() {
        repository.logout()
    }
}

class CheckAuthUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Boolean {
        return repository.getToken() != null
    }
}