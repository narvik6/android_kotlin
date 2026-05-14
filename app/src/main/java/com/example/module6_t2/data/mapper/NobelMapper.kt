package com.example.module6_t2.data.mapper

import com.example.module6_t2.data.remote.dto.NobelPrizeDto
import com.example.module6_t2.domain.model.Laureate
import com.example.module6_t2.domain.model.NobelPrize

fun NobelPrizeDto.toDomain(): NobelPrize {
    return NobelPrize(
        year = awardYear ?: "Unknown Year",
        category = category?.en?.uppercase() ?: "UNKNOWN CATEGORY",
        laureates = laureates?.map { dto ->
            Laureate(
                id = dto.id ?: "",
                // Имя может быть в fullName или knownName
                fullName = dto.fullName?.en ?: dto.knownName?.en ?: "Unknown Name",
                motivation = dto.motivation?.en ?: "No motivation provided...",
                birthCountry = dto.birth?.place?.country?.en ?: dto.birth?.place?.countryNow?.en
            )
        } ?: emptyList()
    )
}