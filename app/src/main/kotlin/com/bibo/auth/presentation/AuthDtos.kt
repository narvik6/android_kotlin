package com.bibo.auth.presentation

import com.bibo.auth.domain.AuthResult
import com.bibo.auth.domain.User
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
)

fun AuthResult.toResponse(): AuthResponse =
    AuthResponse(
        token = token,
        user = user.toResponse(),
    )

private fun User.toResponse(): UserResponse =
    UserResponse(
        id = id,
        email = email,
    )
