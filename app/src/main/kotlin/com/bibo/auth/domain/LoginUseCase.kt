package com.bibo.auth.domain

import com.bibo.core.error.UnauthorizedException
import com.bibo.core.security.JwtService
import com.bibo.core.security.PasswordHasher

class LoginUseCase(
    private val authRepository: AuthRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtService: JwtService,
) {
    suspend fun execute(email: String, password: String): AuthResult {
        val normalizedEmail = normalizeAndValidateEmail(email)
        val user = authRepository.findByEmail(normalizedEmail)
            ?: throw UnauthorizedException("Invalid email or password")

        if (!passwordHasher.verify(password, user.passwordHash)) {
            throw UnauthorizedException("Invalid email or password")
        }

        return AuthResult(
            token = jwtService.createToken(user.id),
            user = user,
        )
    }

}
