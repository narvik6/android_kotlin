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
    tableName = "sync_operations",
    indices = [
        Index("ownerUserId"),
        Index(value = ["ownerUserId", "entityType", "entityLocalId"]),
        Index(value = ["ownerUserId", "createdAt"]),
    ],
)
data class SyncOperationEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val entityType: SyncEntityType,
    val entityLocalId: String,
    val operationType: SyncOperationType,
    val payload: String?,
    val createdAt: String,
)

enum class SyncEntityType {
    DIARY_ENTRY,
    MEDITATION_SESSION,
}

enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE,
}

@Entity(
    tableName = "search_history",
    primaryKeys = ["ownerUserId", "query"],
)
data class SearchHistoryEntity(
    val ownerUserId: String,
    val query: String,
    val createdAt: String,
)
