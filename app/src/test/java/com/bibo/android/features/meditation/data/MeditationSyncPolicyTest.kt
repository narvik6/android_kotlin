package com.bibo.android.features.meditation.data

import com.bibo.android.core.database.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class MeditationSyncPolicyTest {
    @Test
    fun localUpdateKeepsPendingCreateForUnsyncedNewSession() {
        assertEquals(
            SyncStatus.PENDING_CREATE,
            MeditationSyncPolicy.statusAfterLocalUpdate(SyncStatus.PENDING_CREATE),
        )
    }

    @Test
    fun localUpdateMarksSyncedSessionAsPendingUpdate() {
        assertEquals(
            SyncStatus.PENDING_UPDATE,
            MeditationSyncPolicy.statusAfterLocalUpdate(SyncStatus.SYNCED),
        )
    }

    @Test
    fun errorRetryRestoresDeleteCreateOrUpdateOperation() {
        assertEquals(
            SyncStatus.PENDING_DELETE,
            MeditationSyncPolicy.statusForErrorRetry(deletedLocally = true, remoteId = "remote-id"),
        )
        assertEquals(
            SyncStatus.PENDING_CREATE,
            MeditationSyncPolicy.statusForErrorRetry(deletedLocally = false, remoteId = null),
        )
        assertEquals(
            SyncStatus.PENDING_UPDATE,
            MeditationSyncPolicy.statusForErrorRetry(deletedLocally = false, remoteId = "remote-id"),
        )
    }
}
