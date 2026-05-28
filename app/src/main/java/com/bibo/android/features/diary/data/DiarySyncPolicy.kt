package com.bibo.android.features.diary.data

import com.bibo.android.core.database.SyncStatus

object DiarySyncPolicy {
    fun statusAfterLocalUpdate(current: SyncStatus): SyncStatus =
        if (current == SyncStatus.PENDING_CREATE) {
            SyncStatus.PENDING_CREATE
        } else {
            SyncStatus.PENDING_UPDATE
        }

    fun statusForErrorRetry(deletedLocally: Boolean, remoteId: String?): SyncStatus =
        when {
            deletedLocally -> SyncStatus.PENDING_DELETE
            remoteId == null -> SyncStatus.PENDING_CREATE
            else -> SyncStatus.PENDING_UPDATE
        }
}
