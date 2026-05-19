package org.example.domain.usecase

import at.favre.lib.crypto.bcrypt.BCrypt
import org.example.data.repository.UserRepositoryImpl
import org.example.domain.models.LoginRequest


class AuthUseCase(private val userRepository: UserRepositoryImpl) {

    suspend fun authenticate(request: LoginRequest): Boolean {

        val user = userRepository.getUserByUsername(request.username) ?: return false

        val result = BCrypt.verifyer().verify(request.password.toCharArray(), user.passwordHash)
        return result.verified
    }
}