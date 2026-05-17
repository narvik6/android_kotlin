package org.example.data.repository

import org.example.domain.models.Laureate
import org.example.domain.models.NobelPrize
import org.example.domain.repository.PrizeRepository // Импортируем интерфейс из домена

class PrizeRepositoryImpl : PrizeRepository {

    private val prizes = listOf(
        NobelPrize(
            year = "2023",
            category = "physics",
            laureates = listOf(
                Laureate("1", "Pierre Agostini", "for experimental methods that generate attosecond pulses of light"),
                Laureate("2", "Ferenc Krausz", "for experimental methods that generate attosecond pulses of light")
            )
        ),
        NobelPrize(
            year = "2023",
            category = "literature",
            laureates = listOf(
                Laureate("3", "Jon Fosse", "for his innovative plays and prose which give voice to the unsayable")
            )
        )
    )

    override fun getAllPrizes(): List<NobelPrize> {
        return prizes
    }

    override fun getPrize(year: String, category: String): NobelPrize? {
        return prizes.find { it.year == year && it.category.equals(category, ignoreCase = true) }
    }

    override fun getLaureates(year: String, category: String): List<Laureate>? {
        return getPrize(year, category)?.laureates
    }
}