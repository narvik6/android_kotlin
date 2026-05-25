package com.example.module6_t2.data.mapper

import com.example.module6_t2.data.remote.dto.NobelPrizeDto
import com.example.module6_t2.domain.model.Laureate
import com.example.module6_t2.domain.model.NobelPrize

fun NobelPrizeDto.toDomain(): NobelPrize {
    return NobelPrize(
        year = year,
        category = category.uppercase(),
        laureates = laureates.map { dto ->
            Laureate(
                id = dto.id.toString(), // Конвертируем Int ID из нашей БД в String для UI
                fullName = dto.fullName,
                motivation = dto.motivation ?: "No motivation provided...",
                birthCountry = null // Не отдает страну
            )
        }
    )
}