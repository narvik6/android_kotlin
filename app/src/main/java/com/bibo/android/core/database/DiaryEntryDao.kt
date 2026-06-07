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
        WHERE ownerUserId = :ownerUserId
        AND deletedLocally = 0
        AND (remoteId IS NOT NULL OR syncStatus != 'SYNCED')
        ORDER BY dateTime DESC
        """,
    )
    fun observeActiveEntries(ownerUserId: String): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE ownerUserId = :ownerUserId AND localId = :localId LIMIT 1")
    suspend fun getByLocalId(ownerUserId: String, localId: String): DiaryEntryEntity?

    @Query("SELECT * FROM diary_entries WHERE ownerUserId = :ownerUserId AND remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(ownerUserId: String, remoteId: String): DiaryEntryEntity?

    @Query(
        """
        SELECT * FROM diary_entries
        WHERE ownerUserId = :ownerUserId
        AND remoteId IS NOT NULL
        AND syncStatus = 'SYNCED'
        """,
    )
    suspend fun getSyncedRemoteEntries(ownerUserId: String): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entries WHERE ownerUserId = :ownerUserId AND syncStatus != 'SYNCED' ORDER BY updatedAt ASC")
    suspend fun getPendingEntries(ownerUserId: String): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entries WHERE ownerUserId = :ownerUserId")
    suspend fun getAllEntries(ownerUserId: String): List<DiaryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DiaryEntryEntity)

    @Update
    suspend fun update(entity: DiaryEntryEntity)

    @Delete
    suspend fun delete(entity: DiaryEntryEntity)
}
