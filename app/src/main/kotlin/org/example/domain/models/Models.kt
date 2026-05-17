package org.example.domain.models

import kotlinx.serialization.Serializable

// Модели для премий [cite: 231, 234]
@Serializable
data class Laureate(
    val id: String,
    val fullName: String,
    val motivation: String
)

@Serializable
data class NobelPrize(
    val year: String,
    val category: String,
    val laureates: List<Laureate> = emptyList()
)

// Модели для авторизации [cite: 226]
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class TokenResponse(
    val token: String
)