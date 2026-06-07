package com.bibo.android.features.diary.domain

data class DiaryEntry(
    val localId: String,
    val remoteId: String?,
    val ownerUserId: String,
    val text: String?,
    val mood: Int?,
    val dateTime: String,
    val createdAt: String,
    val updatedAt: String,
    val pendingOperation: PendingSyncOperation? = null,
)

enum class PendingSyncOperation {
    Create,
    Update,
    Delete,
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
