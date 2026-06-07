package com.bibo.android.features.meditation.domain

data class MeditationSession(
    val localId: String,
    val remoteId: String?,
    val ownerUserId: String,
    val startedAt: String,
    val endedAt: String,
    val durationSeconds: Long,
    val note: String?,
    val createdAt: String,
    val updatedAt: String,
    val pendingOperation: PendingSyncOperation? = null,
)

enum class PendingSyncOperation {
    Create,
    Update,
    Delete,
}
