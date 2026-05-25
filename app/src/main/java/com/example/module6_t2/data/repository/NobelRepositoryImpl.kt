package com.example.module6_t2.data.repository

import com.example.module6_t2.data.mapper.toDomain
import com.example.module6_t2.data.remote.dto.NobelPrizeDto
import com.example.module6_t2.domain.model.DomainError
import com.example.module6_t2.domain.model.DomainResult
import com.example.module6_t2.domain.model.NobelPrize
import com.example.module6_t2.domain.repository.NobelRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

class NobelRepositoryImpl(private val client: HttpClient) : NobelRepository {
    override suspend fun getNobelPrizes(year: String?, category: String?): DomainResult<List<NobelPrize>> {
        return try {
            val response: List<NobelPrizeDto> = client.get("http://localhost:8080/prizes").body()


            var domainList = response.map { it.toDomain() }

            // Локальная фильтрация данных
            if (!year.isNullOrBlank()) {
                domainList = domainList.filter { it.year == year }
            }
            if (!category.isNullOrBlank() && category != "All") {
                domainList = domainList.filter { it.category.equals(category, ignoreCase = true) }
            }

            DomainResult.Success(domainList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            DomainResult.Error(DomainError.Server(e.response.status.value))
        } catch (e: SerializationException) {
            DomainResult.Error(DomainError.Parsing)
        } catch (e: IOException) {
            DomainResult.Error(DomainError.Network)
        } catch (e: Exception) {
            DomainResult.Error(DomainError.Unknown(e.localizedMessage ?: "Неизвестная ошибка"))
        }
    }
}
