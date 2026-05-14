package com.example.module6_t2.data.repository

import com.example.module6_t2.data.mapper.toDomain
import com.example.module6_t2.data.remote.dto.NobelPrizeResponse
import com.example.module6_t2.domain.model.NobelPrize
import com.example.module6_t2.domain.repository.NobelRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class NobelRepositoryImpl(private val client: HttpClient) : NobelRepository {
    override suspend fun getNobelPrizes(year: String?, category: String?): Result<List<NobelPrize>> {
        return try {
            val response: NobelPrizeResponse = client.get("https://api.nobelprize.org/2.1/nobelPrizes") {
                parameter("limit", 25) // Грузим 25 штук для примера
                parameter("offset", 0)

                // Если фильтры не пустые, добавляем их в запрос
                if (!year.isNullOrBlank()) parameter("nobelPrizeYear", year)
                if (!category.isNullOrBlank() && category != "All") {
                    parameter("nobelPrizeCategory", category.lowercase())
                }
            }.body()

            Result.success(response.nobelPrizes.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}