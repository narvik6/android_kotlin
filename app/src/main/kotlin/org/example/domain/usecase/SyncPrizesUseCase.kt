package org.example.domain.usecase

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import kotlinx.serialization.json.Json
import org.example.data.database.DatabaseFactory.dbQuery
import org.example.data.database.PrizeTable
import org.example.data.database.LaureateTable
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

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val apiData = jsonParser.decodeFromString<ExternalNobelResponse>(response.body())

                dbQuery {
                    apiData.nobelPrizes.forEach { externalPrize ->
                        val prizeId = PrizeTable.insert {
                            it[awardYear] = externalPrize.awardYear.take(4)
                            it[category] = externalPrize.category.en ?: "Unknown"
                        }[PrizeTable.id]

                        externalPrize.laureates?.forEach { externalLaureate ->
                            val portrait = externalLaureate.links?.find { it.rel == "portrait" }?.href
                            
                            LaureateTable.insert {
                                it[this.prizeId] = prizeId
                                it[fullName] = (externalLaureate.fullName?.en ?: externalLaureate.knownName?.en ?: "Unknown").take(255)
                                it[portion] = externalLaureate.portion?.take(10)
                                it[motivation] = externalLaureate.motivation?.en
                                it[portraitUrl] = portrait?.take(255)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error syncing prizes: ${e.message}")
        }
    }
}