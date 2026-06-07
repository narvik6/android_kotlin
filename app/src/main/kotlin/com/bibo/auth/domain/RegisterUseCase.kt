package com.bibo.auth.domain

import com.bibo.core.error.ConflictException
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

}
