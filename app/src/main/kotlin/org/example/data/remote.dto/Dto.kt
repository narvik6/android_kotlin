package org.example.data.remote.dto

import kotlinx.serialization.Serializable
import org.example.domain.models.Laureate
import org.example.domain.models.LoginRequest
import org.example.domain.models.NobelPrize

@Serializable
data class LaureateDto(
    val id: Int,
    val fullName: String,
    val motivation: String? = null,
    val portraitUrl: String? = null
)

@Serializable
data class NobelPrizeDto(
    val id: Int,
    val year: String,
    val category: String,
    val laureates: List<LaureateDto> = emptyList()
)

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class TokenResponseDto(
    val token: String
)

fun LoginRequestDto.toDomain() = LoginRequest(
    username = this.username,
    password = this.password
)

fun Laureate.toDto() = LaureateDto(
    id = this.id,
    fullName = this.fullName,
    motivation = this.motivation,
    portraitUrl = this.portraitUrl
)

fun NobelPrize.toDto() = NobelPrizeDto(
    id = this.id,
    year = this.year,
    category = this.category,
    laureates = this.laureates.map { it.toDto() }
)

@Serializable
data class ExternalNobelResponse(val nobelPrizes: List<ExternalPrize>)

@Serializable
data class ExternalPrize(
    val awardYear: String,
    val category: ExternalText,
    val laureates: List<ExternalLaureate>? = null
)

@Serializable
data class ExternalText(val en: String? = null)

@Serializable
data class ExternalLaureate(
    val fullName: ExternalText? = null,
    val knownName: ExternalText? = null,
    val portion: String? = null,
    val motivation: ExternalText? = null,
    val links: List<ExternalLink>? = null
)

@Serializable
data class ExternalLink(
    val rel: String,
    val href: String
)