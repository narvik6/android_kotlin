package org.example.data.repository

import at.favre.lib.crypto.bcrypt.BCrypt
import org.example.data.database.DatabaseFactory.dbQuery
import org.example.data.database.UserTable
import org.example.domain.models.User
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

class UserRepositoryImpl {
    suspend fun getUserByUsername(username: String): User? = dbQuery {
        UserTable.selectAll().where { UserTable.username eq username }
            .map {
                User(
                    id = it[UserTable.id],
                    username = it[UserTable.username],
                    passwordHash = it[UserTable.passwordHash],
                    role = it[UserTable.role]
                )
            }.singleOrNull()
    }

    suspend fun createTestUserIfNotExists() = dbQuery {
        if (UserTable.selectAll().count() == 0L) {
            val hash = BCrypt.withDefaults().hashToString(12, "emilyspass".toCharArray())
            UserTable.insert {
                it[username] = "emilys"
                it[passwordHash] = hash
                it[role] = "user"
            }
        }
    }
}