package com.bibo.android.features.diary.data

import com.bibo.android.core.database.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DiarySyncPolicyTest {
    @Test
    fun localUpdateKeepsPendingCreateForUnsyncedNewEntry() {
        assertEquals(
            SyncStatus.PENDING_CREATE,
            DiarySyncPolicy.statusAfterLocalUpdate(SyncStatus.PENDING_CREATE),
        )
    }

    @Test
    fun localUpdateMarksSyncedEntryAsPendingUpdate() {
        assertEquals(
            SyncStatus.PENDING_UPDATE,
            DiarySyncPolicy.statusAfterLocalUpdate(SyncStatus.SYNCED),
        )
    }

    @Test
    fun errorRetryRestoresDeleteCreateOrUpdateOperation() {
        assertEquals(
            SyncStatus.PENDING_DELETE,
            DiarySyncPolicy.statusForErrorRetry(deletedLocally = true, remoteId = "remote-id"),
        )
        assertEquals(
            SyncStatus.PENDING_CREATE,
            DiarySyncPolicy.statusForErrorRetry(deletedLocally = false, remoteId = null),
        )
        assertEquals(
            SyncStatus.PENDING_UPDATE,
            DiarySyncPolicy.statusForErrorRetry(deletedLocally = false, remoteId = "remote-id"),
        )
    }
}
