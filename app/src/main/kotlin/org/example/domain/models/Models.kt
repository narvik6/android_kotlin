package org.example.domain.models

// Чистые бизнес-сущности без привязки к JSON
data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val role: String
)

data class Laureate(
    val id: Int,
    val fullName: String,
    val motivation: String? = null,
    val portraitUrl: String? = null
)

data class NobelPrize(
    val id: Int = 0,
    val year: String,
    val category: String,
    val laureates: List<Laureate> = emptyList()
)

data class LoginRequest(
    val username: String,
    val password: String
)