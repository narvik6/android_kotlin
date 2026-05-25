package com.example.module6_t2.data.mapper

import com.example.module6_t2.data.remote.dto.LaureateDto
import com.example.module6_t2.data.remote.dto.NobelPrizeDto
import com.example.module6_t2.domain.model.Laureate
import com.example.module6_t2.domain.model.NobelPrize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

fun NobelPrizeDto.toDomain(): NobelPrize {
    val prizeYear = year.asText() ?: awardYear.asText() ?: awardYearSnakeCase.asText().orEmpty()
    val prizeLaureates = laureates.ifEmpty {
        listOfNotNull(
            fullName?.let { name ->
                LaureateDto(
                    id = id,
                    fullName = name,
                    motivation = motivation
                )
            } ?: fullNameSnakeCase?.let { name ->
                LaureateDto(
                    id = id,
                    fullName = name,
                    motivation = motivation
                )
            }
        )
    }

    return NobelPrize(
        year = prizeYear,
        category = category.lowercase(),
        laureates = prizeLaureates.map { dto ->
            Laureate(
                id = dto.id.toString(),
                fullName = dto.fullName ?: dto.fullNameSnakeCase.orEmpty(),
                motivation = dto.motivation ?: "No motivation provided...",
                birthCountry = dto.birthCountry ?: dto.birthCountrySnakeCase,
                birthPlace = dto.birthPlace ?: dto.birthPlaceSnakeCase,
                portraitUrl = dto.portraitUrl ?: dto.portraitUrlSnakeCase
            )
        }
    )
}

private fun JsonElement?.asText(): String? = this?.jsonPrimitive?.contentOrNull
