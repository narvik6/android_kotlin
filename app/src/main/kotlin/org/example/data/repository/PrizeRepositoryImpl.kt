package org.example.data.repository

import org.example.data.database.DatabaseFactory.dbQuery
import org.example.data.database.LaureateTable
import org.example.data.database.PrizeTable
import org.example.data.database.UserPrizeTable
import org.example.domain.models.Laureate
import org.example.domain.models.NobelPrize
import org.example.domain.repository.PrizeRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime

class PrizeRepositoryImpl : PrizeRepository {
    override suspend fun getAllPrizes(): List<NobelPrize> = dbQuery {
        PrizeTable.selectAll().map { row ->
            val prizeId = row[PrizeTable.id]
            val laureates = fetchLaureatesForPrize(prizeId)

            row.toNobelPrize(laureates)
        }
    }

    override suspend fun getPrize(year: String, category: String): NobelPrize? = dbQuery {
        PrizeTable.selectAll().where { (PrizeTable.awardYear eq year) and (PrizeTable.category eq category) }
            .map { row ->
                val prizeId = row[PrizeTable.id]
                val laureates = fetchLaureatesForPrize(prizeId)
                row.toNobelPrize(laureates)
            }.firstOrNull()
    }

    override suspend fun getLaureates(year: String, category: String): List<Laureate>? = dbQuery {
        val prize =
            PrizeTable.selectAll().where { (PrizeTable.awardYear eq year) and (PrizeTable.category eq category) }
                .firstOrNull() ?: return@dbQuery null

        fetchLaureatesForPrize(prize[PrizeTable.id])
    }

    override suspend fun getFavorites(userId: Int): List<NobelPrize> = dbQuery {
        (PrizeTable innerJoin UserPrizeTable)
            .selectAll()
            .where { UserPrizeTable.userId eq userId }
            .map { row ->
                val prizeId = row[PrizeTable.id]
                val laureates = fetchLaureatesForPrize(prizeId)
                row.toNobelPrize(laureates)
            }
    }

    private fun ResultRow.toNobelPrize(laureates: List<Laureate>) = NobelPrize(
        id = this[PrizeTable.id],
        year = this[PrizeTable.awardYear],
        category = this[PrizeTable.category],
        fullName = this[PrizeTable.fullName],
        motivation = this[PrizeTable.motivation],
        detailLink = this[PrizeTable.detailLink],
        laureates = laureates
    )

    private fun fetchLaureatesForPrize(prizeId: Int): List<Laureate> {
        return LaureateTable.selectAll().where { LaureateTable.prizeId eq prizeId }
            .map { lRow ->
                Laureate(
                    id = lRow[LaureateTable.id],
                    fullName = lRow[LaureateTable.fullName],
                    portion = lRow[LaureateTable.portion],
                    motivation = lRow[LaureateTable.motivation],
                    portraitUrl = lRow[LaureateTable.portraitUrl]
                )
            }
    }

    override suspend fun prizeExists(prizeId: Int): Boolean = dbQuery {
        PrizeTable.selectAll()
            .where { PrizeTable.id eq prizeId }
            .count() > 0
    }

    override suspend fun addFavorite(userId: Int, prizeId: Int): Boolean = dbQuery {
        val exists = UserPrizeTable.selectAll()
            .where { (UserPrizeTable.userId eq userId) and (UserPrizeTable.prizeId eq prizeId) }
            .count() > 0

        if (!exists) {
            UserPrizeTable.insert {
                it[this.userId] = userId
                it[this.prizeId] = prizeId
                it[addedAt] = LocalDateTime.now()
            }
            return@dbQuery true
        }
        false
    }

    override suspend fun removeFavorite(userId: Int, prizeId: Int): Boolean = dbQuery {
        UserPrizeTable.deleteWhere {
            (UserPrizeTable.userId eq userId) and (UserPrizeTable.prizeId eq prizeId)
        } > 0
    }
}
