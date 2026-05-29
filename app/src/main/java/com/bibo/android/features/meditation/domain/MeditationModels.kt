package com.bibo.android.features.meditation.domain

import com.bibo.android.core.database.SyncStatus

data class MeditationSession(
    val localId: String,
    val remoteId: String?,
    val ownerUserId: String,
    val startedAt: String,
    val endedAt: String,
    val durationSeconds: Long,
    val note: String?,
    val syncStatus: SyncStatus,
    val createdAt: String,
    val updatedAt: String,
) {
    val isSynced: Boolean = syncStatus == SyncStatus.SYNCED
}
