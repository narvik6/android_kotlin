package com.bibo.android.features.diary.domain

import com.bibo.android.core.database.SyncStatus

data class DiaryEntry(
    val localId: String,
    val remoteId: String?,
    val ownerUserId: String,
    val text: String?,
    val mood: Int?,
    val dateTime: String,
    val syncStatus: SyncStatus,
    val createdAt: String,
    val updatedAt: String,
) {
    val isSynced: Boolean = syncStatus == SyncStatus.SYNCED
}

data class DailyMood(
    val emoji: String,
    val trend: MoodTrend,
)

enum class MoodTrend {
    Better,
    Worse,
    Same,
}
