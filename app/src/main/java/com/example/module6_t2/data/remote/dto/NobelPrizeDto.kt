package com.example.module6_t2.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NobelPrizeResponse(
    val nobelPrizes: List<NobelPrizeDto> = emptyList()
)

@Serializable
data class NobelPrizeDto(
    val awardYear: String? = null,
    val category: TranslationDto? = null,
    val laureates: List<LaureateDto>? = null
)

@Serializable
data class LaureateDto(
    val id: String? = null,
    val knownName: TranslationDto? = null,
    val fullName: TranslationDto? = null,
    val motivation: TranslationDto? = null,
    val birth: BirthDto? = null
)

@Serializable
data class TranslationDto(
    val en: String? = null
)

@Serializable
data class BirthDto(
    val place: PlaceDto? = null
)

@Serializable
data class PlaceDto(
    val country: TranslationDto? = null,
    val countryNow: TranslationDto? = null
)