package org.example.data.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

object UserTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20).default("user")

    override val primaryKey = PrimaryKey(id)
}

object PrizeTable : Table("prizes") {
    val id = integer("id").autoIncrement()
    val awardYear = varchar("award_year", 4)
    val category = varchar("category", 50)
    val fullName = varchar("full_name", 255).nullable()
    val motivation = text("motivation").nullable()
    val detailLink = varchar("detail_link", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

object LaureateTable : Table("laureates") {
    val id = integer("id").autoIncrement()
    val prizeId = reference("prize_id", PrizeTable.id)
    val fullName = varchar("full_name", 255)
    val portion = varchar("portion", 10).nullable()
    val motivation = text("motivation").nullable()
    val portraitUrl = varchar("portrait_url", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

object UserPrizeTable : Table("user_prizes") {
    val userId = reference("user_id", UserTable.id)
    val prizeId = reference("prize_id", PrizeTable.id)
    val addedAt = datetime("added_at")

    override val primaryKey = PrimaryKey(userId, prizeId)
}