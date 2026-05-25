package org.example.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import kotlinx.serialization.json.Json
import org.example.data.database.DatabaseFactory.dbQuery
import org.example.data.database.PrizeTable
import org.example.data.database.LaureateTable
import org.example.data.remote.dto.ExternalLaureate
import org.example.data.remote.dto.ExternalNobelResponse
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

class SyncPrizesUseCase {
    private val client = HttpClient.newHttpClient()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun syncPrizesFromApi() {
        val alreadyExists = dbQuery { PrizeTable.selectAll().count() > 0L }
        if (alreadyExists) return

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.nobelprize.org/2.1/nobelPrizes?limit=25"))
                .GET()
                .build()

            val response = withContext(Dispatchers.IO) {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            }
            if (response.statusCode() == 200) {
                val apiData = jsonParser.decodeFromString<ExternalNobelResponse>(response.body())

                dbQuery {
                    apiData.nobelPrizes.forEach { externalPrize ->
                        val awardYear = externalPrize.awardYear.take(4)
                        val category = externalPrize.category.en ?: "Unknown"
                        val fullName = externalPrize.categoryFullName?.en?.take(255)
                        val motivation = buildPrizeMotivation(externalPrize.laureates)
                        val detailLink = externalPrize.links?.firstOrNull { link ->
                            link.rel == "nobelPrize"
                        }?.href?.take(255) ?: externalPrize.links?.firstOrNull()?.href?.take(255)

                        val prizeId = PrizeTable.insert {
                            it[PrizeTable.awardYear] = awardYear
                            it[PrizeTable.category] = category
                            it[PrizeTable.fullName] = fullName
                            it[PrizeTable.motivation] = motivation
                            it[PrizeTable.detailLink] = detailLink
                        }[PrizeTable.id]

                        externalPrize.laureates?.forEach { externalLaureate ->
                            val portrait = externalLaureate.links?.find { it.rel == "portrait" }?.href

                            LaureateTable.insert {
                                it[LaureateTable.prizeId] = prizeId
                                it[LaureateTable.fullName] =
                                    (externalLaureate.fullName?.en ?: externalLaureate.knownName?.en ?: "Unknown").take(
                                        255
                                    )
                                it[LaureateTable.portion] = externalLaureate.portion?.take(10)
                                it[LaureateTable.motivation] = externalLaureate.motivation?.en
                                it[LaureateTable.portraitUrl] = portrait?.take(255)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error syncing prizes: ${e.message}")
        }
    }

    private fun buildPrizeMotivation(laureates: List<ExternalLaureate>?): String? =
        laureates
            ?.mapNotNull { it.motivation?.en }
            ?.distinct()
            ?.joinToString(separator = "; ")
            ?.takeIf { it.isNotBlank() }
}
