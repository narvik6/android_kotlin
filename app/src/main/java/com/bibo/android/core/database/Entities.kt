package com.bibo.android.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    indices = [
        Index("ownerUserId"),
        Index("remoteId"),
        Index(value = ["ownerUserId", "dateTime"]),
        Index(value = ["ownerUserId", "syncStatus"]),
    ],
)
data class DiaryEntryEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val ownerUserId: String,
    val text: String?,
    val mood: Int?,
    val dateTime: String,
    val syncStatus: SyncStatus,
    val deletedLocally: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(
    tableName = "meditation_sessions",
    indices = [
        Index("ownerUserId"),
        Index("remoteId"),
        Index(value = ["ownerUserId", "startedAt"]),
        Index(value = ["ownerUserId", "syncStatus"]),
    ],
)
data class MeditationSessionEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val ownerUserId: String,
    val startedAt: String,
    val endedAt: String,
    val durationSeconds: Long,
    val note: String?,
    val syncStatus: SyncStatus,
    val deletedLocally: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(
    tableName = "search_history",
    primaryKeys = ["ownerUserId", "query"],
)
data class SearchHistoryEntity(
    val ownerUserId: String,
    val query: String,
    val createdAt: String,
)
