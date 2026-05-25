package org.example.domain.repository

import org.example.domain.models.Laureate
import org.example.domain.models.NobelPrize

interface PrizeRepository {
    suspend fun getAllPrizes(): List<NobelPrize>
    suspend fun getPrize(year: String, category: String): NobelPrize?
    suspend fun getLaureates(year: String, category: String): List<Laureate>?
    suspend fun getFavorites(userId: Int): List<NobelPrize>
    suspend fun prizeExists(prizeId: Int): Boolean
    suspend fun addFavorite(userId: Int, prizeId: Int): Boolean
    suspend fun removeFavorite(userId: Int, prizeId: Int): Boolean
}
