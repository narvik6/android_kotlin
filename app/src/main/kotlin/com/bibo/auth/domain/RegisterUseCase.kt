package com.bibo.auth.domain

import com.bibo.core.error.ConflictException
import com.bibo.core.error.ValidationException
import com.bibo.core.security.JwtService
import com.bibo.core.security.PasswordHasher

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtService: JwtService,
) {
    suspend fun execute(email: String, password: String): AuthResult {
        val normalizedEmail = normalizeAndValidateEmail(email)
        validatePassword(password)

        if (authRepository.findByEmail(normalizedEmail) != null) {
            throw ConflictException("User with this email already exists")
        }

        val user = authRepository.createUser(
            email = normalizedEmail,
            passwordHash = passwordHasher.hash(password),
        )

        return AuthResult(
            token = jwtService.createToken(user.id),
            user = user,
        )
    }

    private fun normalizeAndValidateEmail(email: String): String {
        val normalized = email.trim().lowercase()

        if (!EMAIL_REGEX.matches(normalized)) {
            throw ValidationException("Invalid email")
        }

        return normalized
    }

    private fun validatePassword(password: String) {
        if (password.length < MIN_PASSWORD_LENGTH) {
            throw ValidationException("Password must contain at least $MIN_PASSWORD_LENGTH characters")
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
