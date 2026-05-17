package org.example.domain.models


// Модели для премий [cite: 231, 234]

data class Laureate(
    val id: String,
    val fullName: String,
    val motivation: String
)


data class NobelPrize(
    val year: String,
    val category: String,
    val laureates: List<Laureate> = emptyList()
)

// Модели для авторизации [cite: 226]

data class LoginRequest(
    val username: String,
    val password: String
)


data class TokenResponse(
    val token: String
)