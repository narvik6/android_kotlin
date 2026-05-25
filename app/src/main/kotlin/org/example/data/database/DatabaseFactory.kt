package org.example.data.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = requiredEnv("DB_JDBC_URL")
            driverClassName = "org.postgresql.Driver"
            username = requiredEnv("DB_USER")
            password = requiredEnv("DB_PASSWORD")

            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(UserTable, PrizeTable, LaureateTable, UserPrizeTable)
        }


    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        withContext<T>(Dispatchers.IO) {
            suspendTransaction<T> { block() }
        }

    private fun requiredEnv(name: String): String =
        System.getenv(name) ?: error("Environment variable $name is required")
}
