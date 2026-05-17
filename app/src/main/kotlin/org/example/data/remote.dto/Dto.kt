package org.example.data.remote.dto

import kotlinx.serialization.Serializable
import org.example.domain.models.Laureate
import org.example.domain.models.LoginRequest
import org.example.domain.models.NobelPrize

// Модели для отдачи наружу
@Serializable
data class LaureateDto(
    val id: String,
    val fullName: String,
    val motivation: String
)

@Serializable
data class NobelPrizeDto(
    val year: String,
    val category: String,
    val laureates: List<LaureateDto> = emptyList()
)

// Модели для авторизации (запрос/ответ)
@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class TokenResponseDto(
    val token: String
)

// Мапперы (Extension-функции для удобной конвертации)

// Из DTO в Domain (для входящего запроса авторизации)
fun LoginRequestDto.toDomain() = LoginRequest(
    username = this.username,
    password = this.password
)

// Из Domain в DTO (для исходящих ответов)
fun Laureate.toDto() = LaureateDto(
    id = this.id,
    fullName = this.fullName,
    motivation = this.motivation
)

fun NobelPrize.toDto() = NobelPrizeDto(
    year = this.year,
    category = this.category,
    laureates = this.laureates.map { it.toDto() }
)