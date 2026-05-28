package com.bibo.android.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {
    @Query(
        """
        SELECT * FROM diary_entries
        WHERE ownerUserId = :ownerUserId AND deletedLocally = 0
        ORDER BY dateTime DESC
        """,
    )
    fun observeActiveEntries(ownerUserId: String): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): DiaryEntryEntity?

    @Query("SELECT * FROM diary_entries WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): DiaryEntryEntity?

    @Query(
        """
        SELECT * FROM diary_entries
        WHERE ownerUserId = :ownerUserId
        AND syncStatus IN ('PENDING_CREATE', 'PENDING_UPDATE', 'PENDING_DELETE', 'ERROR')
        ORDER BY updatedAt ASC
        """,
    )
    suspend fun getPendingEntries(ownerUserId: String): List<DiaryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DiaryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DiaryEntryEntity>)

    @Update
    suspend fun update(entity: DiaryEntryEntity)

    @Delete
    suspend fun delete(entity: DiaryEntryEntity)

    @Query("DELETE FROM diary_entries WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)
}
