package org.example.domain.repository

import org.example.domain.models.Laureate
import org.example.domain.models.NobelPrize

// Чистый интерфейс, диктующий контракты для работы с данными
interface PrizeRepository {
    fun getAllPrizes(): List<NobelPrize>
    fun getPrize(year: String, category: String): NobelPrize?
    fun getLaureates(year: String, category: String): List<Laureate>?
}